package com.limegroup.gnutella.messagehandlers;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;

import org.limewire.inspection.Inspector;
import org.limewire.security.SecureMessageVerifier;
import org.limewire.util.CommonUtils;

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
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.MessageSettings;
import com.limegroup.gnutella.simpp.SimppManager;

/**
 * Handles an incoming InspectionRequest, sending a response
 * if not empty and forwarding the request to leaves if it has a 
 * return address.
 */
public class InspectionRequestHandler extends RestrictedResponder {
    
    private static final String INSPECTION_FILE = "inspection.props";

    private final Provider<MessageRouter> router;
    private final Inspector inspector;
    
    @Inject
    public InspectionRequestHandler(Provider<MessageRouter> router, NetworkManager networkManager, 
            SimppManager simppManager, 
            UDPReplyHandlerFactory udpReplyHandlerFactory, UDPReplyHandlerCache udpReplyHandlerCache,
            Inspector inspector, @Named("inspection") SecureMessageVerifier inspectionVerifier,
            @Named("messageExecutor") ExecutorService dispatch) {
        super(FilterSettings.INSPECTOR_IP_ADDRESSES, 
                inspectionVerifier,
                MessageSettings.INSPECTION_VERSION, networkManager, simppManager, udpReplyHandlerFactory, udpReplyHandlerCache, dispatch);
        this.router = router;
        this.inspector = inspector;
        this.inspector.load(new File(CommonUtils.getCurrentDirectory(),INSPECTION_FILE));
    }
    
    @Override
    protected void processAllowedMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
        assert msg instanceof InspectionRequest;
        InspectionRequest ir = (InspectionRequest)msg;
        InspectionResponse r = new InspectionResponse(ir, inspector);
        if (r.shouldBeSent())
            handler.reply(r);
        router.get().forwardInspectionRequestToLeaves(ir);
    }
}
