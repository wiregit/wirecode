package com.limegroup.bittorrent;

import java.io.IOException;

/**
 * Exception to throw in case there was a problem decoding bencoded data.
 */
public class BadTrackerResponseException extends IOException {
	private static final long serialVersionUID = 1096901978709423214L;

	public BadTrackerResponseException() {
		super();
	}

	public BadTrackerResponseException(String arg0) {
		super(arg0);
	}

}
