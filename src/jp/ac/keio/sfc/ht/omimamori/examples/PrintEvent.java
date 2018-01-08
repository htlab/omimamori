/**
 * Copyright (C) 2017  @author Yin Chen <yin@ht.sfc.keio.ac.jp>
 * Keio University, Japan
 */
package jp.ac.keio.sfc.ht.omimamori.examples;

import java.util.TooManyListenersException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.ac.keio.sfc.ht.omimamori.basestation.BaseStation;
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
		
		
		BaseStation.parseOptions(args);
		PrintEvent printer = new PrintEvent();
		BaseStation bs = new BaseStation();
		try {
			bs.addSensorEventListener(printer);
		} catch (TooManyListenersException e1) {
						
			logger.error("Add eventlistener failed!", e1);
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
