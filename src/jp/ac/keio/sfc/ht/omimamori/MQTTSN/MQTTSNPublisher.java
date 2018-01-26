/**
 * Copyright (C) 2017  @author Yin Chen <yin@ht.sfc.keio.ac.jp>
 * Keio University, Japan
 */
package jp.ac.keio.sfc.ht.omimamori.MQTTSN;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.TooManyListenersException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import javax.lang.model.util.ElementKindVisitor6;

import org.eclipse.paho.mqttsn.udpclient.MqttsCallback;
import org.eclipse.paho.mqttsn.udpclient.MqttsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.ac.keio.sfc.ht.omimamori.basestation.BaseStation;
import jp.ac.keio.sfc.ht.omimamori.exceptions.OMIException;
import jp.ac.keio.sfc.ht.omimamori.protocol.BaseStationEvent;
import jp.ac.keio.sfc.ht.omimamori.protocol.BaseStationEventListener;

/**
 * @author Yin Chen <yin@ht.sfc.keio.ac.jp>
 *
 */
public class MQTTSNPublisher implements BaseStationEventListener, MqttsCallback,Runnable{

	//Logger
	final static Logger logger = LoggerFactory.getLogger(MQTTSNPublisher.class);
	
	// MQTT related
	private MqttsClient mqClient; 	// client

	static protected String server; 			// name of server hosting the broker
	static protected int port; 				// broker's port
	protected String mqttsClientId; 		// client id
	private boolean mqttsCleanStart=false;
	private short mqttsKeepAliveDuration = 600; // seconds

	private int maxMqttsMsgLength;  	//bytes
	private int minMqttsMsgLength;	//bytes
	private int maxRetries;
	private int ackTime;				//seconds

	protected boolean connected; 		// true if connected to a broker
	protected Hashtable<Integer, String> topicTable;
	private String tName;

	private boolean pubFlag;   //indicates a pub has to be sent when REGACK is received
	private String pubTopic;
	private byte[] pubMsg;
	private int pubQos;
	private boolean pubRetained;

	private boolean autoReconnect=false;
	
	BlockingQueue<BaseStationEvent> buffer;
	
	
	
//Serial port related	

	protected  static int TIME_OUT = 10000;  // time out for serial connecting to omimamori receiver
	protected  static int BAUD_RATE = 38400; //baud rate of omimamori receiver
	protected  static String PORT_NAME = "/dev/ttyWISUN";  // serial port name
	
	private BaseStation bs;
	
	public static void main( String[] args ){
		
	
		String srv = "localhost"; 	// default gateway
		int port = 20000; 			// default port
		String clientId = "mqtts_console_" + System.currentTimeMillis(); 		// default client id
		boolean cleanStart=false;

		int maxMqttsMsgLength=60;
		int minMqttsMsgLength=2;
		int maxRetries=2;
		int ackTime=3;
		boolean autoReconnect=true;
		
		
		
		String usage = "Usage: "
				+ "-o <serial port> "
				+ "-b <baudRate> "
				+ "-s <MQTT-SN gateway> "
				+ "-p <gateway port> "
				+ "-id <client id> "
				+ "-cs <0 = false: else true>"
				+ "-autoReconnect <0 = false: else true>";
		if (args.length == 0) {
			System.err.println("ERROR: arguments required!");
			System.err.println(usage);
			System.exit(1);
		}

		
		

		// parse command line arguments -s server -p port -id clientId
		// and overwrite default values if present
		
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-o")) {
				PORT_NAME = args[++i];
			} else  if (args[i].equals("-b")) {
				BAUD_RATE = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-s")) {
				srv = args[++i];
			}else if (args[i].equals("-p")) {
				port = Integer.parseInt(args[++i]);
			}else if (args[i].equals("-id")) {
				clientId = args[++i];
			}else if (args[i].equals("-cs")) {
				int cs=Integer.parseInt(args[++i]);
				if(cs==0) cleanStart=false; else cleanStart=true;
			}
/*			else if (arg.equals("-log")) {
				try {
					ClientLogger.setLogFile(args[i++]);
				} catch (MqttsException e) {
					e.printStackTrace();
				} 
			}
			else if (arg.equals("-level")) {
				ClientLogger.setLogLevel(Integer.parseInt(args[i++]));	
			}*/
			else if (args[i].equals("-autoReconnect")) {
				if (args[++i].equals("0")) autoReconnect=false;
				else autoReconnect=true;
			}
			else {
				System.err.println("ERROR: invalid option " + args[i]);
				System.err.println(usage);
				System.exit(1);
			}
		}
		
		
		
	
		
		
		MQTTSNPublisher pub;

		
		logger.info("Connect to MQTT-SN gateway  {} at port  {} with waiting time {} milisecs", srv, port);
		
		// create console and launch the thread
		try {
			pub = new MQTTSNPublisher(srv,port,clientId,cleanStart,
					maxMqttsMsgLength,minMqttsMsgLength,maxRetries,ackTime,autoReconnect);
			new Thread(pub).run();
		} catch (OMIException e1) {
			// TODO Auto-generated catch block
			logger.error("MQTTSNPublisher Initializaion failed", e1);
			System.exit(-1);
			
		}
		
		
		
		

    }
	
	
	/* 
	 * Constructor
	 * initialize fields and connect to broker
	 */

	public MQTTSNPublisher(String server, int port, String clientId, boolean cleanStart,
			int maxMqttsMsgLength, int minMqttsMsgLength, 
			int maxRetries, int ackWaitingTime, boolean autoReconnect) throws OMIException {

		this.topicTable = new Hashtable<Integer, String>();
		this.pubFlag = false; this.pubTopic = null;
		this.server = server;
		this.port = port;
		this.mqttsClientId = clientId;
		this.mqttsCleanStart= cleanStart;

		this.maxMqttsMsgLength= maxMqttsMsgLength;
		this.minMqttsMsgLength= minMqttsMsgLength;
		this.maxRetries= maxRetries;
		this.ackTime= ackWaitingTime;

		this.autoReconnect=autoReconnect;

		this.connected = false;

		mqClient = new MqttsClient (this.server ,this.port,
				this.maxMqttsMsgLength, this.minMqttsMsgLength, 
				this.maxRetries, this.ackTime, this.autoReconnect);
		mqClient.registerHandler(this);

		logger.info("mqttsn java client version "+
				MqttsClient.version + " started, ");
		if (autoReconnect) logger.info("autoreconnect= true");
		else logger.info("autoreconnect= false");

		
		
		
		
		connect();
		
		this.buffer = new LinkedBlockingDeque<BaseStationEvent> ();
		
		this.bs = new BaseStation(PORT_NAME, BAUD_RATE);
		
		try {
			
			bs.addSensorEventListener(this);
		} catch (TooManyListenersException e) {
						
			throw new OMIException("Add eventlistener failed!",e);
			
		} 
		
				

	}
	
	public void connect() throws OMIException {
		try {
			if (mqClient == null) {
				logger.info("Starting MQTTS-SN java client version "+
						MqttsClient.version);
				mqClient = new MqttsClient (this.server ,this.port,
						maxMqttsMsgLength, minMqttsMsgLength, maxRetries,
						ackTime);
				mqClient.registerHandler(this);
			}
			//			cleanStart= false;
			//mqClient.connect(this.mqttsClientId,mqttsCleanStart,mqttsKeepAliveDuration);
			mqClient.connect(this.mqttsClientId,mqttsCleanStart,mqttsKeepAliveDuration,
					"down",1,this.mqttsClientId,true);
		} catch (Exception e){
			connected = false;
			throw new OMIException("connection to " + server + " failed!",e);
			//logger.error("exception: ", e); 
			//System.out.println("Exiting ... ");
			//System.exit(0);
		}	
	}
	
	public boolean publish(String topic, String msg, int qos, boolean retained) {
		byte[] message = msg.getBytes();

		return publish(topic, message, qos, retained);
	}

	public boolean publish(String topic, byte[] msg, int qos, boolean retained) {

		boolean retVal = false;

		Iterator<Integer> iter = topicTable.keySet().iterator();
		Iterator<String> iterVal = topicTable.values().iterator();
		Integer ret = new Integer(-1);
		while (iter.hasNext()) { //check whether topic is in topicTable
			Integer topicId = (Integer)iter.next();			
			String tname = (String)iterVal.next();
			if(tname.equals(topic)) {
				ret = topicId;
				break;
			}
		}
		int topicID = ret.intValue();
		if (topicID == -1) { //topic not in topicTable, have to register it
			register(topic);
			pubFlag = false;  //set the flag and wait for REG ACK
			pubTopic= topic; //store the values for later publish
			pubMsg = msg;
			pubQos = qos;
			pubRetained = retained;
			//System.out.println("** topic not in table, have to register it first");
		} else {
			try {
				retVal = mqClient.publish(topicID, msg, qos, retained);
				//System.out.println("** published: \"" + topic + ": " + 
				//		Utils.hexString(msg) + "\"");
			} catch (Exception e) {
				logger.error("publish exception: ", e);
			}
		}
		return retVal;
	}
	public boolean register(String topicName) {
		
		boolean regValue;
		regValue = mqClient.register(topicName);
		if (regValue){
			this.tName = topicName;
		}
		return regValue;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.paho.mqttsn.udpclient.MqttsCallback#publishArrived(boolean, int, int, byte[])
	 */
	@Override
	public int publishArrived(boolean retain, int QoS, int topicId, byte[] thisPayload) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.mqttsn.udpclient.MqttsCallback#connected()
	 */
	@Override
	public void connected() {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.mqttsn.udpclient.MqttsCallback#disconnected(int)
	 */
	@Override
	public void disconnected(int returnType) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.mqttsn.udpclient.MqttsCallback#unsubackReceived()
	 */
	@Override
	public void unsubackReceived() {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.mqttsn.udpclient.MqttsCallback#subackReceived(int, int, int)
	 */
	@Override
	public void subackReceived(int grandesQos, int topicId, int returnCode) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.mqttsn.udpclient.MqttsCallback#pubCompReceived()
	 */
	@Override
	public void pubCompReceived() {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.mqttsn.udpclient.MqttsCallback#pubAckReceived(int, int)
	 */
	@Override
	public void pubAckReceived(int topicId, int returnCode) {
		// TODO Auto-generated method stub
		
	}





	/* (non-Javadoc)
	 * @see org.eclipse.paho.mqttsn.udpclient.MqttsCallback#connectSent()
	 */
	@Override
	public void connectSent() {
		// TODO Auto-generated method stub
		
	}
	//callback: REGACK received
	public void regAckReceived(int topicId, int returnCode) {

		//		System.out.println("** registered: topic= " + tName + 
		//				" topicId= "+ topicId + " rc= " + returnCode);
		topicTable.put(new Integer(topicId), tName);
		tName=null;

		if (pubFlag) {
			publish(pubTopic, pubMsg, pubQos, pubRetained);
			pubFlag = false;
		}

	}

	//callback: REGISTER received
	public void registerReceived(int topicId, String topicName) {
		//		System.out.println("** REG received: topic= " + topicName + 	
		//				" topicId= "+ topicId);
		topicTable.put(new Integer (topicId), topicName);
	}

	


	/* (non-Javadoc)
	 * @see jp.ac.keio.sfc.ht.omimamori.protocol.BaseStationEventListener#handleEvent(jp.ac.keio.sfc.ht.omimamori.protocol.BaseStationEvent)
	 */
	@Override
	public void handleEvent(BaseStationEvent ev) throws Exception {
		logger.info( "Received: " + ev.toString());	
		buffer.put(ev);
		
	}


	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		
		BaseStationEvent ev;
		String topic, msg;
		int pubInterval = 200;
		
		boolean res;
		while(true){
			try{
				ev = buffer.take();
				topic = "/" + mqttsClientId + "/" + ev.tr_mac ;
				
				msg = ev.seq_num + " " + Double.toString(ev.rssi) +  " " + Long.toString(ev.timestamp);
				logger.info("Publishing: " + topic + " "+ msg);
				for(int count = 0;  count < 100; count++){
					Thread.sleep(pubInterval * count);
					res =  this.publish(topic, msg, 2, true);
					if (!res){				
						
					}else{
						break;
					} 
				}				
			}catch (Throwable t){
				logger.error("Publishing failed",t);
			}
			
			
		}
		
				
		
		
	}

}
