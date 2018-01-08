/**
 * Copyright (C) 2017  @author Yin Chen <yin@ht.sfc.keio.ac.jp>
 * Keio University, Japan
 */
package jp.ac.keio.sfc.ht.omimamori.protocol;

import java.util.EventObject;

/**
 * @author Yin Chen <yin@ht.sfc.keio.ac.jp>
 *
 */
public class BaseStationEvent extends EventObject {
	
	String raw;
	String cmd;
	String version;
	String tr_mac;
	double rssi;
	String pan_id;
	int seq_num;
	String payload_IE;
	String payload;
	
	/**
	 * @param source
	 */
	public BaseStationEvent(Object source, String rawcmd) {
		super(source);
		BaseStationCMD.parseCMD(rawcmd ,this);
	}
	
/*	public BaseStationEvent(String rawcmd){
		this(new Object(),  cmd);
	}*/
	
	public String getRawCMD(){
		return raw;
	}

	
	public String toString(){		
		return String.format("cmd:  %s, version: %s, tr_mac: %s, rssi: %f, PAN_ID: %s, SEQ_NUM: %d, PAYLOAD_IE: %s, PAYLOAD: %s", 
				cmd, version, tr_mac, rssi, pan_id, seq_num, payload_IE, payload);
		
	}
	
}
