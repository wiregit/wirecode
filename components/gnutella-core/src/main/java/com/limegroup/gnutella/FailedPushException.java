package com.limegroup.gnutella;

import java.io.IOException;

/**
 * Thrown id the downloaded file is incomplete
 */
public class FailedPushException extends IOException {
	public FailedPushException() {
		super("Already attempted and failed a push"); 
	}
	public FailedPushException(String msg) { super(msg); }
}
