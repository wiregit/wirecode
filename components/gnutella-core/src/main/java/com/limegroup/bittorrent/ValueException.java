package com.limegroup.bittorrent;

import java.io.IOException;

/**
 * Exception to throw in case received bencoded data does not 
 * match the structure we expect.
 */
public class ValueException extends IOException {
	private static final long serialVersionUID = 3990038438042291913L;

	public ValueException() {
		super();
	}

	public ValueException(String arg0) {
		super(arg0);
	}

}
