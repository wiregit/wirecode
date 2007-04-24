package com.limegroup.gnutella.messagehandlers;

import java.net.InetSocketAddress;

import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.vendor.InspectionRequest;
import com.limegroup.gnutella.messages.vendor.InspectionResponse;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.MessageSettings;

/**
 * Handles an incoming InspectionRequest, sending a response
 * if not empty and forwarding the request to leaves if it has a 
 * return address.
 */
public class InspectionRequestHandler extends RestrictedResponder {

    private final MessageRouter router;
    public InspectionRequestHandler(MessageRouter router) {
        super(FilterSettings.INSPECTOR_IP_ADDRESSES, 
                RouterService.getSecureMessageVerifier(),
                MessageSettings.INSPECTION_VERSION);
        this.router = router;
    }
    
    @Override
    protected void processAllowedMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
        assert msg instanceof InspectionRequest;
        InspectionRequest ir = (InspectionRequest)msg;
        InspectionResponse r = new InspectionResponse(ir);
        if (r.shouldBeSent())
            handler.reply(r);
        router.forwardInspectionRequestToLeaves(ir);
    }
}
