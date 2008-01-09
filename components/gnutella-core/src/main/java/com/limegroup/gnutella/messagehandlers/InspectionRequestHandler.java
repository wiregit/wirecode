package com.limegroup.gnutella.messagehandlers;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.limewire.collection.Periodic;
import org.limewire.security.SecureMessageVerifier;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.UDPReplyHandlerCache;
import com.limegroup.gnutella.UDPReplyHandlerFactory;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.vendor.InspectionRequest;
import com.limegroup.gnutella.messages.vendor.InspectionResponse;
import com.limegroup.gnutella.messages.vendor.InspectionResponseFactory;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.MessageSettings;
import com.limegroup.gnutella.simpp.SimppManager;

/**
 * Handles an incoming InspectionRequest, sending a response
 * if not empty and forwarding the request to leaves if it has a 
 * return address.
 */
public class InspectionRequestHandler extends RestrictedResponder {
    
    /** Send back encoded responses this often */
    private final static int SEND_INTERVAL = 500;
    
    private final Provider<MessageRouter> router;
    private final InspectionResponseFactory factory;
    private final Periodic sender;
    
    /** 
     * List of responses to be sent.
     * LOCKING: this
     */
    private final List<InspectionResponse> queue = new LinkedList<InspectionResponse>();
    
    /**
     * Where to send the enqueued replies.
     * (no need to support multiple destinations at present)  
     */
    private ReplyHandler currentHandler;
    
    @Inject
    public InspectionRequestHandler(Provider<MessageRouter> router, NetworkManager networkManager, 
            SimppManager simppManager, 
            UDPReplyHandlerFactory udpReplyHandlerFactory, UDPReplyHandlerCache udpReplyHandlerCache,
            InspectionResponseFactory factory, @Named("inspection") SecureMessageVerifier inspectionVerifier,
            @Named("messageExecutor") ExecutorService dispatch,
            @Named("backgroundExecutor") ScheduledExecutorService background) {
        super(FilterSettings.INSPECTOR_IP_ADDRESSES, 
                inspectionVerifier,
                MessageSettings.INSPECTION_VERSION, networkManager, simppManager, udpReplyHandlerFactory, udpReplyHandlerCache, dispatch);
        this.router = router;
        this.factory = factory;
        sender = new Periodic(new Runnable() {
            public void run() {
                send();
            }
        }, background);
        
    }
    
    @Override
    protected void processAllowedMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
        assert msg instanceof InspectionRequest;
        InspectionRequest ir = (InspectionRequest)msg;
        
        // send first response back right away
        InspectionResponse []r = factory.createResponses(ir);
        if (r.length > 0 && r[0].shouldBeSent())
            handler.reply(r[0]);
        router.get().forwardInspectionRequestToLeaves(ir);
        
        // schedule the rest of the responses if any
        if (r.length < 2)
            return;
        
        synchronized(this) {
            for (int i = 1; i < r.length; i++)
                queue.add(r[i]);
            currentHandler = handler;
        }
        sender.rescheduleIfSooner(SEND_INTERVAL);
    }
    
    private void send() {
        InspectionResponse resp = null;
        ReplyHandler handler = null;
        synchronized(this) {
            if (!queue.isEmpty()) {
                resp = queue.remove(0);
                handler = currentHandler;
            }
        }
        
        if (resp == null || handler == null) {
            sender.unschedule();
            return;
        }
        
        if (resp.shouldBeSent())
            handler.reply(resp);
        sender.rescheduleIfLater(SEND_INTERVAL);
    }
}
