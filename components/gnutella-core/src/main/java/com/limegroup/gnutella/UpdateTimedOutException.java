/**
 * file: UpdateTimedOutException.java
 * auth: afisk
 * desc: Exception thrown when the connection to the LimeWire
 *       server has timed out during an update.
 */

package com.limegroup.gnutella;

import java.lang.Exception;

public class UpdateTimedOutException extends Exception {
	public UpdateTimedOutException() {super("Update Timed Out");}
	public UpdateTimedOutException(String msg) {super(msg);}
}

