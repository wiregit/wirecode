package com.limegroup.gnutella;

import java.io.IOException;

/**
 * Thrown id the downloaded file is incomplete
 */
public class ExceededPerHostLimitException extends IOException {
	public ExceededPerHostLimitException() {
		super("Exceeded the Per Host Upload Limit"); 
	}
	public ExceededPerHostLimitException(String msg) { super(msg); }
}
