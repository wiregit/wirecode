package com.limegroup.gnutella.messagehandlers;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.limewire.collection.FixedSizeExpiringSet;

import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.vendor.UDPCrawlerPing;
import com.limegroup.gnutella.messages.vendor.UDPCrawlerPong;

public class UDPCrawlerPingHandler implements MessageHandler {
	public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
		assert msg instanceof UDPCrawlerPing;
		if (!allowUDPPing(handler))
			return;
		
		UDPCrawlerPong pong = new UDPCrawlerPong((UDPCrawlerPing)msg);
		handler.reply(pong);
	}
	/**
	 * keeps a list of the people who have requested our connection lists. 
	 * used to make sure we don't get ping-flooded. 
	 * not final so that tests won't take forever.
	 */
	private FixedSizeExpiringSet<InetAddress> _UDPListRequestors 
		= new FixedSizeExpiringSet<InetAddress>(2000, 10*60 * 1000); //10 minutes.
	
	/**
	 * whether the reply handler should receive a reply.  
	 * used to protect us from ping-flooding.
	 * 
	 * @param r the <tt>ReplyHandler</tt> on which the reply is to be sent
	 * @return true if its ok to reply.
	 */
	private boolean allowUDPPing(ReplyHandler r) {
		//this also takes care of multiple instances running on the same ip address.
		return _UDPListRequestors.add(r.getInetAddress());
	}
}
