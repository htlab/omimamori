/**
 * Copyright (C) 2017  @author Yin Chen <yin@ht.sfc.keio.ac.jp>
 * Keio University, Japan
 */
package jp.ac.keio.sfc.ht.omimamori.protocol;

/**
 * @author Yin Chen <yin@ht.sfc.keio.ac.jp>
 *
 */
final class BaseStationCMD {
	public static  void parseCMD(String rawcmd, BaseStationEvent ev ){
		if (rawcmd != null){
			String[] stringArray = rawcmd.split(",");
			String[] cmdArray = stringArray[0].split(" ");
			ev.raw = rawcmd;
			ev.cmd = cmdArray[0];
			ev.version = cmdArray[1];	
			switch (ev.cmd){
				case "tagr": tagr(ev.version,stringArray, ev);
					break;
				default://TODO add exeption 
					break;
				// TODO	add other CMD handlers		
				}
		}
		
		
		
	};
	
	public static void tagr(String version,String[] stringArray, BaseStationEvent ev){
		switch (version) {
		case "3": tagr3(stringArray,ev);break;
		default: break;
		
		}
	}
	public static void tagr3(String[] stringArray, BaseStationEvent ev) {
		
		if(stringArray.length != 7 ){
			//TODO throw exception
		}
		ev.tr_mac = stringArray[1];
		ev.rssi = Double.parseDouble(stringArray[2]);
		ev.pan_id = stringArray[3];
		ev.seq_num = Integer.parseInt(stringArray[4]);
		ev.payload_IE = stringArray[5];
		ev.payload = stringArray[6];
		
	}
	
}
