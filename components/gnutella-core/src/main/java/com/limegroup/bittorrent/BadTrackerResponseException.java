package com.limegroup.bittorrent;

import java.io.IOException;

public class BadTrackerResponseException extends IOException {

	private static final long serialVersionUID = 1096901978709423214L;

	public BadTrackerResponseException() {
		super();
	}

	public BadTrackerResponseException(String s) {
		super(s);
	}
}
