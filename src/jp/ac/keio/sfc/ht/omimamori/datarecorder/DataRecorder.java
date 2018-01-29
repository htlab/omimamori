/**
 * Copyright (C) 2017  @author Yin Chen <yin@ht.sfc.keio.ac.jp>
 * Keio University, Japan
 */
package jp.ac.keio.sfc.ht.omimamori.datarecorder;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yin Chen <yin@ht.sfc.keio.ac.jp>
 *
 */
public class DataRecorder implements MqttCallback, Runnable {

	final static Logger logger = LoggerFactory.getLogger(DataRecorder.class);
	
	// Private instance variables
	private IMqttClient 		client;
	private String 				clientID;
	private String 				brokerUrl;
	private MqttConnectOptions 	conOpt;
	private String password;
	private String userName;
	private String topic;
	
	private String db_userName;
	private String db_password;
	private String db_url;
	private String db_driver;
	private Connection conn = null;
	
	private BlockingQueue<String[]> data_buffer;
	/**
	 * @param password 
	 * @param userName 
	 * @param clientId2 
	 * @param url 
	 * 
	 */
	public DataRecorder(String url, String clientId, String userName, String password, String topic, String db_url, String db_driver, String db_userName,String db_password) {
		
		this.brokerUrl = url;
    	
    	this.password = password;
    	this.userName = userName;
    	this.topic = topic;
    	this.clientID = clientId;
    	
    	this.db_driver = db_driver;
		this.db_password = db_password;
		this.db_url = db_url;
		this.db_userName = db_userName;
		
		this.data_buffer = new LinkedBlockingQueue<String[]> ();
		
		try {

			
			connectToMQTTServer();
			Class.forName(db_driver).newInstance();
			
			new Thread(this).run();
		} catch (MqttException e) {
			
			logger.error("MQTT client initilizaion failed",e);
			System.exit(1);
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			logger.error("Loading database driver failed",e);
			System.exit(1);
		} 						
		
	}

	
	void connectToMQTTServer() throws MqttException{
		MemoryPersistence persistence = new MemoryPersistence();
		client = new MqttClient(brokerUrl, this.clientID, persistence);
		client.setCallback(this);
		
		conOpt = new MqttConnectOptions();
		conOpt.setCleanSession(true);
		
    	if(password != null ) {
	    	  conOpt.setPassword(this.password.toCharArray());
	    	}
	    if(userName != null) {
	    	  conOpt.setUserName(this.userName);
	    }
	    client.connect(conOpt);
	    client.subscribe(topic, 2);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.MqttCallback#connectionLost(java.lang.Throwable)
	 */
	@Override
	public void connectionLost(Throwable cause) {
		// TODO Auto-generated method stub
		logger.error("Connection to MQTT broker " + this.client.getServerURI() + " is lost" );
		while(true){
			try {
				client.close();
				logger.error("Reconnecting to MQTT broker... ");
			    
				connectToMQTTServer();
				logger.error("Done...");
				break;
			} catch (MqttException e) {
				// TODO Auto-generated catch block
				logger.error("Failed...",e);
				logger.error("Wait for 1 minuetes");
				try {
					Thread.sleep(1000 * 60);
					
				} catch (InterruptedException e1) {
					// Ignor
				}
				alert("Omimamori-DataRecorder: Connecting to MQTT broker is constantly failing.");
			}
		}
		
		
		
	}
	void alert(String msg){
		
	}
	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.MqttCallback#messageArrived(java.lang.String, org.eclipse.paho.client.mqttv3.MqttMessage)
	 */
	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		// TODO Auto-generated method stub
		logger.debug(topic + " " +message);
		String[] mqttData = {topic,message.toString()};
		data_buffer.add(mqttData);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.MqttCallback#deliveryComplete(org.eclipse.paho.client.mqttv3.IMqttDeliveryToken)
	 */
	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		// TODO Auto-generated method stub

	}

	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
        
		
		// Default settings:
		String topic 		= "";
		int qos 			= 2;
		String broker 		= "omimamori-server.ht.sfc.keio.ac.jp";
		int port 			= 1883;
		String clientId 	= "Omimamori-recorder";
		String subTopic		= "/#";
		boolean ssl = false;
		String password = null;
		String userName = null;
		String db_userName = null;
		String db_password = null;
		String db_url = null;
		String db_driver = null;
		
		// Parse the arguments -
				for (int i=0; i<args.length; i++) {
					// Check this is a valid argument
					if (args[i].startsWith("-")) {
						String arg = args[i];
						// Handle arguments that take no-value
						switch(arg) {
							case "-help": case "?":	printHelp(); return;
						}

						// Now handle the arguments that take a value and
						// ensure one is specified
						if (i == args.length -1 || args[i+1].charAt(0) == '-') {
							System.out.println("Missing value for argument: "+args[i]);
							printHelp();
							return;
						}
						switch(arg) {
							
							case "-t": topic = args[++i];                  break;
							case "-b": broker = args[++i];                 break;
							case "-p": port = Integer.parseInt(args[++i]); break;
							case "-id": clientId = args[++i];				  break;
							case "-v": ssl = Boolean.valueOf(args[++i]).booleanValue(); break;
							case "-u": userName = args[++i];               break;
							case "-z": password = args[++i];               break;
							case "-du": db_userName = args[++i];               break;
							case "-dz": db_password = args[++i];               break;
							case "-durl": db_url = args[++i];               break;
							case "-ddr": db_driver = args[++i];               break;
							default:
								System.out.println("Unrecognised argument: "+args[i]);
								printHelp();
								return;
						}
					} else {
						System.out.println("Unrecognised argument: "+args[i]);
						printHelp();
						return;
					}
				}

		String protocol = "tcp://";

	    if (ssl) {
	      protocol = "ssl://";
	    }
	    
	    String url = protocol + broker + ":" + port;
	    DataRecorder recorder;
	    recorder = new DataRecorder(url, clientId,userName,password,topic, db_url,db_driver,db_userName,db_password);
		
	}

	   static void printHelp() {
		      System.out.println(
		          "Syntax:\n\n" +
		              "    DataRecorder [-help] [-a publish|subscribe] [-t <topic>]" +
		              "    [-b <hostname|IP address>] [-p <brokerport>] [-i <clientID>]\n\n" +
		              "    -help  Print this help text and quit\n" +
		              "    -t  Subscribe to <topic> instead of the default\n" +
		              "            (publish: \"Sample/Java/v3\", subscribe: \"Sample/#\")\n" +
		              "    -b  Use this name/IP address instead of the default (omimamori-server.ht.sfc.keio.ac.jp)\n" +
		              "    -p  Use this port instead of the default (1883)\n" +
		              "    -i  Use this client ID instead of Omimamori-recorder\n" +
		              "     \n\n Security Options \n" +
		              "     -u Username \n" +
		              "     -z Password \n" +
		              "     -durl Database URL \n" +
		              "     -ddr Database driver \n" +
		              "     -du Database Username \n" +
		              "     -dz Database Password \n" +
		              "     \n\n SSL Options \n" +
		              "    -v  SSL enabled; true - (default is false) "
		          );
		      }

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	
	
	
	public void run() {
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String[] mqttdata = null;
		final String sql = "INSERT INTO `omimamori_DB`.`RSSI_Measurements` (`beacon_mac`,`bs_sn`,`RSSI`,`seq_num`,`pan_id`,`time`) "+
		"VALUES(?,?,?,?,?,?)";
		while(true){
			
			try {
				if( mqttdata == null ){
					// if mqttdata is null then take a new element from data_buffer
					// it is not, then it means a SQL insert operation failed, 
					// so we need to try it again. The mqttdata is update in the condition 
					// of while loop below.
					mqttdata = data_buffer.take();
				}
				
				if(conn == null || conn.isClosed()){
					conn =  DriverManager.getConnection(this.db_url,this.db_userName,this.db_password);
				}
				
				
				do{
				  
				String [] topics = mqttdata[0].split("/");
				String[] msg = mqttdata[1].split(" ");
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, topics[2]);
				pstmt.setString(2, topics[1]);
				pstmt.setBigDecimal(3, new BigDecimal(msg[2]));
				pstmt.setInt(4, Integer.parseInt(msg[1]));
				pstmt.setString(5,msg[0]);
				pstmt.setTimestamp(6,new Timestamp(Long.parseLong(msg[3])));
				
				logger.debug("Write to database [topic:"+mqttdata[0] +",message:" +mqttdata[1] + "]");
				pstmt.execute();
				logger.debug("Done!");	
					
					
				}while((mqttdata = data_buffer.poll(1,TimeUnit.MINUTES)) != null);// insert until the buffer is empty within 1 minite.
				logger.info("No more data is avaiable. Close database connection.");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				logger.error("",e);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				logger.error("Connecting to database "+this.db_url+" failed",e);
			}finally{
				try { rs.close(); } catch (Exception e) { /* ignored */ } finally{};
			    try { pstmt.close(); } catch (Exception e) { /* ignored */ }finally{};
			    try { conn.close(); } catch (Exception e) { /* ignored */ }finally{};
			}
				
			
		}		
	}
}
