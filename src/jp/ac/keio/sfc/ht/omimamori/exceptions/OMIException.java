/**
 * Copyright (C) 2017  @author Yin Chen <yin@ht.sfc.keio.ac.jp>
 * Keio University, Japan
 */
package jp.ac.keio.sfc.ht.omimamori.exceptions;

/**
 * @author Yin Chen <yin@ht.sfc.keio.ac.jp>
 *
 */
public class OMIException extends Exception {

	/**
	 * 
	 */
	public OMIException() {
		super();
	}

	/**
	 * @param message
	 */
	public OMIException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param cause
	 */
	public OMIException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 * @param cause
	 */
	public OMIException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public OMIException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}

}
