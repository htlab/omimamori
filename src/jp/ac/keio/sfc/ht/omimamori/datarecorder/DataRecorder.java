/**
 * Copyright (C) 2017  @author Yin Chen <yin@ht.sfc.keio.ac.jp>
 * Keio University, Japan
 */
package jp.ac.keio.sfc.ht.omimamori.datarecorder;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yin Chen <yin@ht.sfc.keio.ac.jp>
 *
 */
public class DataRecorder implements MqttCallback {

	final static Logger logger = LoggerFactory.getLogger(DataRecorder.class);
	
	// Private instance variables
	private IMqttClient 		client;
	private String 				brokerUrl;
	private MqttConnectOptions 	conOpt;
	private String password;
	private String userName;
	private String topic;
	
	/**
	 * @param password 
	 * @param userName 
	 * @param clientId2 
	 * @param url 
	 * 
	 */
	public DataRecorder(String url, String clientId, String userName, String password, String topic) {
		// TODO Auto-generated constructor stub
		this.brokerUrl = url;
    	
    	this.password = password;
    	this.userName = userName;
    	this.topic = topic;
		try {
			MemoryPersistence persistence = new MemoryPersistence();
			client = new MqttClient(brokerUrl, clientId, persistence);
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
			
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			logger.error("MQTT client initilizaion failed",e);
			System.exit(1);
		}
		

		
		
		
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.MqttCallback#connectionLost(java.lang.Throwable)
	 */
	@Override
	public void connectionLost(Throwable cause) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.MqttCallback#messageArrived(java.lang.String, org.eclipse.paho.client.mqttv3.MqttMessage)
	 */
	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		// TODO Auto-generated method stub
		System.out.println(topic + " " +message);
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
	    recorder = new DataRecorder(url, clientId,userName,password,topic);
		
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
		              "     \n\n SSL Options \n" +
		              "    -v  SSL enabled; true - (default is false) "
		          );
		      }
}
