package com.limegroup.bittorrent;

import org.limewire.concurrent.SchedulingThreadPool;
import org.limewire.nio.channel.ChannelWriter;

import com.limegroup.bittorrent.messages.BTMessage;

/**
 * A <tt>ChannelWriter</tt> with some BT-specific functionality
 */
public interface BTChannelWriter extends ChannelWriter {

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
	 * @param scheduler the <tt>SchedulingThreadPool</tt> to use when performing
	 * time-related tasks
	 * @param keepAliveInterval how often to send keepalives if there is no
	 * other traffic
	 */
	public void init(SchedulingThreadPool scheduler, int keepAliveInterval);

}