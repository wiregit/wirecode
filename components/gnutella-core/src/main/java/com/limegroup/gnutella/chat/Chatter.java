package com.limegroup.gnutella.chat;
/**
 * the chatter interface.  an implementation is passed to the
 * gui, so that messages can be sent and recieved.
 *
 */

import java.io.*;

public interface Chatter {
	
	/** sends a message across the socket */
	public void sendMessage(String msg) throws IOException;
	
	/** recieves a message from the socket */
	public String recieveMessage() throws IOException;

	/** close a session */
	public void stop();

}


