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
import java.util.LinkedList;
import java.util.List;
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
import jp.ac.keio.sfc.ht.omimamori.exceptions.OMIException;
import jp.ac.keio.sfc.ht.omimamori.protocol.BaseStationEvent;
import jp.ac.keio.sfc.ht.omimamori.protocol.BaseStationEventListener;



/**
 * @author Yin Chen <yin@ht.sfc.keio.ac.jp>
 *
 */
public class BaseStation  {
	

	final static Logger logger = LoggerFactory.getLogger(BaseStation.class);
    protected  int time_out = 10000;  // time out for serial connecting to omimamori receiver
    protected  int baund_rate = 38400; //baud rate of omimamori receiver
    protected  String port_name = "/dev/ttyUSB7";  // serial port name
	SerialPort serialPort = null;
	private List<BaseStationEventListener> sensorEventListenerList = new LinkedList<BaseStationEventListener>();
	
	
	protected SerialReader reader;
	protected SerialWriter writer;
	
	
	
    /**
     * @throws OMIException 
     *
     */
    public BaseStation(String port, int baund_rate) throws OMIException{
        this(port,baund_rate, 10000);
    }
    /**
     * @throws OMIException 
     *
     */
    public BaseStation(String port, int baund_rate, int time_out ) throws OMIException {
        // TODO Auto-generated constructor stub
        super();
        
        this.port_name = port;
        this.baund_rate = baund_rate;
        this.time_out = time_out;
        // Initialize Omimamori receiver
        try {
            logger.info("Connect to Omimamori receiver at serial port {} baudrate {} with waiting time {} milisecs", port_name, baund_rate, time_out);
            serialPort = openSerialPort(port_name, baund_rate, time_out);
            reader = new SerialReader(serialPort.getInputStream(),this);
            
            writer = new SerialWriter(serialPort.getOutputStream(),this);
            
            serialPort.addEventListener(reader);
            
        } catch (Exception e) {
            logger.error("Connection to Omimamori receiver failed!", e);
            throw new OMIException("Connection to Omimamori receiver failed!",e);
            
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


		
    public static void main ( String[] args ){
        
        String PORT_NAME = "/dev/WiSUN";
        int BAUND_RATE = 38400;
        int TIME_OUT = 10000;
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
                BAUND_RATE = Integer.parseInt(args[++i]);
            } else {
                System.err.println("ERROR: invalid option " + args[i]);
                System.err.println(usage);
                System.exit(1);
            }
        }
        
        try {
			new BaseStation(PORT_NAME,BAUND_RATE,TIME_OUT);
		} catch (OMIException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			System.exit(-1);
		}
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
