package com.limegroup.gnutella.chat;
/**
 * the class that serves as the interface between a Chat
 * instance and the gui.
 * 
 *@author rsoule
 */

public interface Chatter {

	// Operations
	public void stop();
	public void send(String message);
	public String getMessage();

}
