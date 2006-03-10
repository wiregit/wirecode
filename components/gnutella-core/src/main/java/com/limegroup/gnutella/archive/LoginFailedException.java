package com.limegroup.gnutella.archive;

import java.io.IOException;

public class LoginFailedException extends IOException {

	private static final long serialVersionUID = -2605188468237382226L;

	public LoginFailedException() {
		super();
	}

	public LoginFailedException(String s) {
		super(s);
	}

}
