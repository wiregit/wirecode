package com.limegroup.gnutella.messagehandlers;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;

import org.limewire.collection.FixedSizeExpiringSet;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.UDPReplyHandlerCache;
import com.limegroup.gnutella.UDPReplyHandlerFactory;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.vendor.UDPCrawlerPing;
import com.limegroup.gnutella.messages.vendor.UDPCrawlerPong;
import com.limegroup.gnutella.messages.vendor.UDPCrawlerPongFactory;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.simpp.SimppManager;

public class UDPCrawlerPingHandler extends RestrictedResponder {
    
    /**
     * keeps a list of the people who have requested our connection lists. 
     * used to make sure we don't get ping-flooded. 
     * not final so that tests won't take forever.
     */
    private FixedSizeExpiringSet<InetAddress> _UDPListRequestors 
        = new FixedSizeExpiringSet<InetAddress>(2000, 10*60 * 1000); //10 minutes.
    
    private final UDPCrawlerPongFactory udpCrawlerPongFactory;
    
    @Inject
    public UDPCrawlerPingHandler(NetworkManager networkManager,
            SimppManager simppManager,
            UDPReplyHandlerFactory udpReplyHandlerFactory,
            UDPReplyHandlerCache udpReplyHandlerCache,
            UDPCrawlerPongFactory udpCrawlerPongFactory,
            @Named("messageExecutor") ExecutorService dispatch
            ) {
        super(FilterSettings.CRAWLER_IP_ADDRESSES, networkManager,
                simppManager, udpReplyHandlerFactory, udpReplyHandlerCache, dispatch);
        this.udpCrawlerPongFactory = udpCrawlerPongFactory;
    }
    
	protected void processAllowedMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
		assert msg instanceof UDPCrawlerPing;
		if (!_UDPListRequestors.add(handler.getInetAddress()))
			return;
		
		UDPCrawlerPong pong = udpCrawlerPongFactory.createUDPCrawlerPong((UDPCrawlerPing)msg);
		handler.reply(pong);
	}
	
}
