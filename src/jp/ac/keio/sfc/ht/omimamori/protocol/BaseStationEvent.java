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
	
	public String raw;
	public String cmd;
	public String version;
	public String tr_mac;
	public double rssi;
	public String pan_id;
	public int seq_num;
	public String payload_IE;
	public String payload;
	public long timestamp;
	
	/**
	 * @param source
	 */
	public BaseStationEvent(Object source, String rawcmd, long timestamp) {
		super(source);
		this.timestamp = timestamp;
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
