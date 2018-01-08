/**
 * Copyright (C) 2017  @author Yin Chen <yin@ht.sfc.keio.ac.jp>
 * Keio University, Japan
 */
package jp.ac.keio.sfc.ht.omimamori.basestation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.TooManyListenersException;

import org.eclipse.paho.mqttsn.udpclient.MqttsCallback;
import org.eclipse.paho.mqttsn.udpclient.MqttsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import jp.ac.keio.sfc.ht.omimamori.protocol.BaseStationEvent;
import jp.ac.keio.sfc.ht.omimamori.protocol.BaseStationEventListener;



/**
 * @author Yin Chen <yin@ht.sfc.keio.ac.jp>
 *
 */
public class BaseStation  {
	
//Logger
	final static Logger logger = LoggerFactory.getLogger(BaseStation.class);
	
//Serial port related
	protected static  int TIME_OUT = 10000;  // time out for serial connecting to omimamori receiver
	public static int BAUD_RATE = 38400; //baud rate of omimamori receiver
	public static String PORT_NAME = "/dev/ttyWISUN";  // serial port name
	SerialPort serialPort = null;
	private List<BaseStationEventListener> sensorEventListenerList = new LinkedList<BaseStationEventListener>();
	protected SerialReader reader;
	protected SerialWriter writer;

//MQTT related
	

	static 	String srv = "localhost"; 	// default gateway
	static	int port = 20000; 			// default port
	static	String clientId = "mqtts_console_" + System.currentTimeMillis(); 		// default client id
	static	boolean cleanStart=false;

	static	int maxMqttsMsgLength=60;
	static	int minMqttsMsgLength=2;
	static	int maxRetries=2;
	static	int ackTime=3;
	static	boolean autoReconnect=true;


	
	
	
	public static void parseOptions(String[] args) {

		String usage = "Usage: java -jar %s "
				+ "-o <serial port> "
				+ "-b <baudRate> "
				+ "-s <MQTT-SN gateway> "
				+ "-p <gateway port> "
				+ "-id <client id> "
				+ "-cs <0 = false: else true>"
				+ "-autoReconnect <0 = false: else true>";
		if (args.length == 0) {
			System.err.println("ERROR: arguments required!");
			System.err.println(String.format(usage, args[0]));
			System.exit(1);
		}

		
		

		// parse command line arguments -s server -p port -id clientId
		// and overwrite default values if present
		int i = 0;
		String arg;
		while (i < args.length && args[i].startsWith("-")) {
			arg = args[i];
			if (args[i].equals("-o")) {
				PORT_NAME = args[++i];
			} 
			if (args[i].equals("-b")) {
				BAUD_RATE = Integer.parseInt(args[++i]);
			} 
			
			if (arg.equals("-s")) {
				srv = args[i++];
			}
			if (arg.equals("-p")) {
				port = Integer.parseInt(args[i++]);
			}
			if (arg.equals("-id")) {
				clientId = args[i++];
			}
			if (arg.equals("-cs")) {
				int cs=Integer.parseInt(args[i++]);
				if(cs==0) cleanStart=false; else cleanStart=true;
			}
/*			if (arg.equals("-log")) {
				try {
					ClientLogger.setLogFile(args[i++]);
				} catch (MqttsException e) {
					e.printStackTrace();
				} 
			}
			if (arg.equals("-level")) {
				ClientLogger.setLogLevel(Integer.parseInt(args[i++]));	
			}*/
			if (arg.equals("-autoReconnect")) {
				if (args[i++].equals("0")) autoReconnect=false;
				else autoReconnect=true;
			}
		}
	}
	
	public static void main ( String[] args ){
		parseOptions(args);
		new BaseStation();
		while(true){
			try {
				Thread.sleep(10 * 1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
    }
	
	/**
	 * 
	 */
	public BaseStation() {
		// TODO Auto-generated constructor stub
		 super();
		// Initialize Omimamori receiver  
		try {
			logger.info("Connect to Omimamori receiver at serial port {} baudrate {} with waiting time {} milisecs", PORT_NAME, BAUD_RATE, TIME_OUT);
			serialPort = openSerialPort(PORT_NAME, BAUD_RATE, TIME_OUT);
			reader = new SerialReader(serialPort.getInputStream(),this);

			writer = new SerialWriter(serialPort.getOutputStream(),this);

			serialPort.addEventListener(reader);

		} catch (Exception e) {
			logger.error("Connection to Omimamori receiver failed!", e);
			System.exit(-1);
		} 
		try {
			logger.info("Connect to MQTT-SN gateway  {} at port  {} with waiting time {} milisecs", srv, port);
			
			// create console and launch the thread
			MQTTSNPublisher pub = new MQTTSNPublisher(srv,port,clientId,cleanStart,
					maxMqttsMsgLength,minMqttsMsgLength,maxRetries,ackTime,autoReconnect);
		} catch (Exception e) {
			logger.error("Connection to Omimamori receiver failed!", e);
			System.exit(-1);
		} 
		

		logger.info("Connection succeeded!");
		
		 

		serialPort.notifyOnDataAvailable(true);

		//(new Thread(this)).start();
	}
		
	public void addSensorEventListener(BaseStationEventListener lsnr) throws TooManyListenersException {
		if (!sensorEventListenerList.contains(lsnr)) {
			if (!sensorEventListenerList.add(lsnr)) {
				throw new TooManyListenersException("Adding sensor event failed!");
			}
		}
	}

	public void removeSensorEventListener(BaseStationEventListener lsnr) {
		sensorEventListenerList.remove(lsnr);
	}

	public void clearSensorEventListener() {
		logger.info("Clear all sensor event listeners!...");
		sensorEventListenerList.clear();
	}

	protected void triggerEventHandler(BaseStationEvent ev) throws Exception {
		for (BaseStationEventListener lsnr : sensorEventListenerList) {
			lsnr.handleEvent(ev);
		}
	}

	
    /**
     * Handles the input coming from the serial port. A new line character
     * is treated as the end of a block in this example. 
     */
    public static class SerialReader implements SerialPortEventListener 
    {
        private BaseStation owner;
    	private InputStream in;
        private byte[] buffer = new byte[1024];
        
        public SerialReader ( InputStream in, BaseStation b )
        {
            this.in = in;
            this.owner = b;
        }
        
        public void serialEvent(SerialPortEvent event) {
            int data;
            int pre_data = 0x0A;
            
            
            try
            {
                int len = 0;
                while ( ( data = in.read()) > -1 )
                {
                    if ( data == 0x0D && pre_data == 0x0A) { // End with 0x0A 0x0D
                    	
                        break;
                    }
                    buffer[len++] = (byte) data;
                    pre_data = data;
                }
                //String cmd = String.format("%s,%s", LocalDateTime.now(), new String(buffer,0,len-1));
                
                String cmd = new String(buffer,0,len-1);
                logger.info(cmd);
                try {
					owner.triggerEventHandler(new BaseStationEvent(this, cmd));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					logger.error("Trigger Event failed",e);
				}
                
                
                
                //System.out.println(bytesToHexString(buffer,len));
                //BaseStation.logger.info(","+ new String(buffer,0,len));
            }
            catch ( IOException e )
            {
                logger.error("I/O Err", e);
                System.exit(-1);
            }             
        }

    }
    
    
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHexString(byte[] bytes, int len) {
		
		char[] hexChars = new char[len * 2];
		for (int j = 0; j < len; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
    /** */
    public static class SerialWriter implements Runnable 
    {
    	private BaseStation owner;
    	OutputStream out;
        
        public SerialWriter ( OutputStream out, BaseStation bs)
        {
            this.out = out;
            this.owner = bs;
        }
        
        public void run ()
        {
            try
            {                
                int c = 0;
                while ( ( c = System.in.read()) > -1 )
                {
                    this.out.write(c);
                }                
            }
            catch ( IOException e )
            {
                e.printStackTrace();
                System.exit(-1);
            }            
        }
    }

	
	
	/**
	 * @param serialPortName
	 *            : name of the port to open
	 * @param timeout
	 *            : maximum waiting time
	 * @param baudrate
	 * 			  : baudrate of the port
	 * @return:
	 * @throws UnsupportedCommOperationException
	 * @throws NoSuchPortException
	 * @throws PortInUseException
	 * @throws IOException
	 */
	private SerialPort openSerialPort(String serialPortName, int baudrate, int timeout)
			throws UnsupportedCommOperationException, NoSuchPortException, PortInUseException, IOException {

		System.setProperty("gnu.io.rxtx.SerialPorts", serialPortName);
		@SuppressWarnings("rawtypes")
		//Enumeration portList = CommPortIdentifier.getPortIdentifiers();
		// CommPortIdentifier id = null;
		//while (portList.hasMoreElements()) {
		//	logger.info(portList.nextElement());
		//}

		CommPortIdentifier portId = CommPortIdentifier.getPortIdentifier(serialPortName);
		SerialPort port = (SerialPort) portId.open(this.getClass().getName(), timeout);
		port.setSerialPortParams(baudrate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
		port.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
		return port;
	}


		

	public static class MQTTSNPublisher implements  MqttsCallback{
		
		static private BaseStation owner;
		// MQTT related
		private MqttsClient mqClient; 	// client

		protected String server; 			// name of server hosting the broker
		protected int port; 				// broker's port
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
		
		/* 
		 * Constructor
		 * initialize fields and connect to broker
		 */

		public MQTTSNPublisher(String server, int port, String clientId, boolean cleanStart,
				int maxMqttsMsgLength, int minMqttsMsgLength, 
				int maxRetries, int ackWaitingTime, boolean autoReconnect) {

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
			System.out.println("");

			connect();		

		}
		
		public void connect() {
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
				logger.error("connection to " + server + " failed!");
				logger.error("exception: ", e); 
				//System.out.println("Exiting ... ");
				//System.exit(0);
			}	
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
		 * @see org.eclipse.paho.mqttsn.udpclient.MqttsCallback#regAckReceived(int, int)
		 */
		@Override
		public void regAckReceived(int topicId, int returnCode) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see org.eclipse.paho.mqttsn.udpclient.MqttsCallback#registerReceived(int, java.lang.String)
		 */
		@Override
		public void registerReceived(int topicId, String topicName) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see org.eclipse.paho.mqttsn.udpclient.MqttsCallback#connectSent()
		 */
		@Override
		public void connectSent() {
			// TODO Auto-generated method stub
			
		}
		
	}

	
}
