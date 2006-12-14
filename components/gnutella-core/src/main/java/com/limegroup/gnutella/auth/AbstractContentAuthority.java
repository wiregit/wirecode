package com.limegroup.gnutella.auth;


public abstract class AbstractContentAuthority implements ContentAuthority {

	private final long timeout;
	
	public AbstractContentAuthority(long timeout) {
		this.timeout = timeout;
	}
	
	public long getTimeout() {
		return timeout;
	}
}
