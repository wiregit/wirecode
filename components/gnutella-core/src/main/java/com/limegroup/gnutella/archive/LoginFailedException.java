package com.limegroup.gnutella.archive;

import java.io.IOException;

public class LoginFailedException extends IOException {

	public static final String REPOSITORY_VERSION =
		"$Header: /gittmp/cvs_drop/repository/limewire/components/gnutella-core/src/main/java/com/limegroup/gnutella/archive/Attic/LoginFailedException.java,v 1.1.2.2 2005-11-02 20:59:38 tolsen Exp $";
	
	private static final long serialVersionUID = -2605188468237382226L;

	public LoginFailedException() {
		super();
	}

	public LoginFailedException(String s) {
		super(s);
	}

}
