package com.limegroup.bittorrent;

import com.limegroup.bittorrent.messages.BTMessage;
import com.limegroup.gnutella.io.ChannelWriter;

/**
 * A <tt>ChannelWriter</tt> with some BT-specific functionality
 */
public interface BTChannelWriter extends ChannelWriter {

	/**
	 * writes out a keepalive ([0000]) to this channel
	 */
	public void sendKeepAlive();

	/**
	 * Enqueues another message for the remote host
	 * 
	 * @param m
	 *            the BTMessage to enqueue
	 * @return true if the message was enqueued, false if not.
	 */
	public void enqueue(BTMessage m);
	
	/**
	 * Initializes this writer (optional)
	 */
	public void init();

}