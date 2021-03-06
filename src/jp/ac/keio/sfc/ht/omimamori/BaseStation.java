/**
 * Copyright (C) 2017  @author Yin Chen <yin@ht.sfc.keio.ac.jp>
 * Keio University, Japan
 */
package jp.ac.keio.sfc.ht.omimamori;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.TooManyListenersException;

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



/**
 * @author Yin Chen <yin@ht.sfc.keio.ac.jp>
 *
 */
public class BaseStation {
	

	final static Logger logger = LoggerFactory.getLogger(BaseStation.class);
	protected static  int TIME_OUT = 10000;  // time out for serial connecting to omimamori receiver
	protected static int BAUD_RATE = 38400; //baud rate of omimamori receiver
	protected static String PORT_NAME = "/dev/ttyUSB7";  // serial port name
	SerialPort serialPort = null;
	
	protected SerialReader reader;
	protected SerialWriter writer;
	
	protected static void parseOptions(String[] args) {

		String usage = "Usage: java -jar ConvertorServer.jar  -o <portName> -b <baudRate>";
		if (args.length == 0) {
			System.err.println("ERROR: arguments required!");
			System.err.println(usage);
			System.exit(1);
		}
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-o")) {
				PORT_NAME = args[++i];
			} else if (args[i].equals("-b")) {
				BAUD_RATE = Integer.parseInt(args[++i]);
			} else {
				System.err.println("ERROR: invalid option " + args[i]);
				System.err.println(usage);
				System.exit(1);
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
			reader = new SerialReader(serialPort.getInputStream());

			writer = new SerialWriter(serialPort.getOutputStream());

			serialPort.addEventListener(reader);

		} catch (Exception e) {
			logger.error("Connection to Omimamori receiver failed!", e);
			System.exit(-1);
		} 

		logger.info("Connection succeeded!");
		
		 

		serialPort.notifyOnDataAvailable(true);

		//(new Thread(this)).start();
	}

	
	
	



	
    /**
     * Handles the input coming from the serial port. A new line character
     * is treated as the end of a block in this example. 
     */
    public static class SerialReader implements SerialPortEventListener 
    {
        private InputStream in;
        private byte[] buffer = new byte[1024];
        
        public SerialReader ( InputStream in )
        {
            this.in = in;
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
                System.out.println(LocalDateTime.now()+","+new String(buffer,0,len-1));
                
                
                
                //System.out.println(bytesToHexString(buffer,len));
                //BaseStation.logger.info(","+ new String(buffer,0,len));
            }
            catch ( IOException e )
            {
                e.printStackTrace();
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
        OutputStream out;
        
        public SerialWriter ( OutputStream out )
        {
            this.out = out;
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
}
