/**
 * Copyright (C) 2017  @author Yin Chen <yin@ht.sfc.keio.ac.jp>
 * Keio University, Japan
 */
package jp.ac.keio.sfc.ht.omimamori.protocol;

import java.util.EventListener;

/**
 * @author Yin Chen <yin@ht.sfc.keio.ac.jp>
 *
 */
public interface BaseStationEventListener extends EventListener {
	public abstract void handleEvent(BaseStationEvent ev) throws Exception;
}
