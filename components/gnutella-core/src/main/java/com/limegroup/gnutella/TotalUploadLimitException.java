package com.limegroup.gnutella;

import java.io.IOException;

/**
 * Thrown id the downloaded file is incomplete
 */
public class TotalUploadLimitException extends IOException {
	public TotalUploadLimitException() {
		super("Exceeded the Total Upload Limit"); 
	}
	public TotalUploadLimitException(String msg) { super(msg); }
}
