package com.limegroup.gnutella.messagehandlers;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.limewire.collection.FixedSizeExpiringSet;
import org.limewire.io.IP;

import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.filters.IPList;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.vendor.UDPCrawlerPing;
import com.limegroup.gnutella.messages.vendor.UDPCrawlerPong;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.simpp.SimppListener;
import com.limegroup.gnutella.simpp.SimppManager;

public class UDPCrawlerPingHandler implements MessageHandler, SimppListener {
    
    /** ips that can crawl us */
    private volatile IPList crawlers;
    
    public UDPCrawlerPingHandler() {
        crawlers = new IPList();
        crawlers.add("*.*.*.*");
        SimppManager.instance().addListener(this);
        updateCrawlers();
    }
    
    private void updateCrawlers() {
        IPList newCrawlers = new IPList();
        try {
            for (String ip : FilterSettings.CRAWLER_IP_ADDRESSES.getValue())
                newCrawlers.add(new IP(ip));
            if (newCrawlers.isValidFilter(false))
                crawlers = newCrawlers;
        } catch (IllegalArgumentException badSimpp) {}
    }
    
    public void simppUpdated() {
        updateCrawlers();
    }
    
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
	 * used to protect us from ping-flooding and to allow only specific ips to crawl.
	 * 
	 * @param r the <tt>ReplyHandler</tt> on which the reply is to be sent
	 * @return true if its ok to reply.
	 */
	private boolean allowUDPPing(ReplyHandler r) {
		//this also takes care of multiple instances running on the same ip address.
		return crawlers.contains(new IP(r.getInetAddress().getHostAddress())) && 
        _UDPListRequestors.add(r.getInetAddress());
	}
}
