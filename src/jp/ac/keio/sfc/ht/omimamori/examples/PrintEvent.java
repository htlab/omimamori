/**
 * Copyright (C) 2017  @author Yin Chen <yin@ht.sfc.keio.ac.jp>
 * Keio University, Japan
 */
package jp.ac.keio.sfc.ht.omimamori.examples;

import java.util.TooManyListenersException;

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
public class PrintEvent implements BaseStationEventListener {

	final static Logger logger = LoggerFactory.getLogger(PrintEvent.class);
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		
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
		BaseStation bs;
		PrintEvent printer = new PrintEvent();

		try {
		
			bs = new BaseStation(PORT_NAME,BAUND_RATE,TIME_OUT);
			bs.addSensorEventListener(printer);
		} catch (TooManyListenersException e1) {
						
			logger.error("Add eventlistener failed!", e1);
			System.exit(-1);
			
		} catch (OMIException e) {
			logger.error(e.getMessage(), e);
			System.exit(-1);
		}
		
		while(true){
			try {
				Thread.sleep(10 * 1000);
			} catch (InterruptedException e) {
				logger.error("Sleep interupted!", e);
			}
		}
	}

	/* (non-Javadoc)
	 * @see jp.ac.keio.sfc.ht.omimamori.protocol.BaseStationEventListener#handleSensorEvent(jp.ac.keio.sfc.ht.omimamori.protocol.BaseStationEvent)
	 */
	@Override
	public void handleEvent(BaseStationEvent ev) throws Exception {
		
		System.out.println(ev.getRawCMD());
		System.out.println(ev);
		
	}
	

}
