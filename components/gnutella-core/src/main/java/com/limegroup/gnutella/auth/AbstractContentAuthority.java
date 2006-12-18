package com.limegroup.gnutella.auth;


public abstract class AbstractContentAuthority implements ContentAuthority {

	private final long timeout;
	
	public AbstractContentAuthority(long timeout) {
		this.timeout = timeout;
	}
	
	public long getTimeout() {
		return timeout;
	}
	
	/**
	 * Does nothing.
	 */
	public void initialize() throws Exception {
	}
	
	/**
	 * Does nothing.
	 */
	public void shutdown() {
	}
}
