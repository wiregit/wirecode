package com.limegroup.gnutella;

import java.io.IOException;

/**
 * Thrown id the downloaded file is incomplete
 */
public class ExceededTotalUploadLimitException extends IOException {
	public ExceededTotalUploadLimitException() {
		super("Exceeded the Total Upload Limit"); 
	}
	public ExceededTotalUploadLimitException(String msg) { super(msg); }
}
