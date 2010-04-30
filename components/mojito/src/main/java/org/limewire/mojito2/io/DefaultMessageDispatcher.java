package org.limewire.mojito2.io;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.NetworkUtils;
import org.limewire.mojito2.Context;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.message.Message;
import org.limewire.mojito2.message.MessageID;
import org.limewire.mojito2.message.NodeRequest;
import org.limewire.mojito2.message.PingRequest;
import org.limewire.mojito2.message.PingResponse;
import org.limewire.mojito2.message.RequestMessage;
import org.limewire.mojito2.message.ResponseMessage;
import org.limewire.mojito2.message.StoreRequest;
import org.limewire.mojito2.message.ValueRequest;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.routing.RouteTable;
import org.limewire.mojito2.settings.NetworkSettings;
import org.limewire.mojito2.util.ContactUtils;
import org.limewire.mojito2.util.HostFilter;

public class DefaultMessageDispatcher extends MessageDispatcher {

    private static final Log LOG 
        = LogFactory.getLog(DefaultMessageDispatcher.class);

    private final Context context;
    
    private final DefaultMessageHandler defaultHandler;
    
    private final PingRequestHandler ping;
    
    private final NodeRequestHandler node;
    
    private final ValueRequestHandler value;
    
    private final StoreRequestHandler store;
    
    public DefaultMessageDispatcher(Context context, 
            StoreForward storeForward) {
        
        this.context = context;
        
        RouteTable routeTable 
            = context.getRouteTable();
        
        defaultHandler = new DefaultMessageHandler(
                routeTable, storeForward);
        
        ping = new PingRequestHandler(context);
        node = new NodeRequestHandler(context);
        value = new ValueRequestHandler(context, node);
        store = new StoreRequestHandler(context);
    }
    
    /**
     * 
     */
    public Context getContext() {
        return context;
    }

    /**
     * 
     */
    private boolean allow(Message message) {
        HostFilter hostFilter = context.getHostFilter();
        if (hostFilter != null) {
            Contact src = message.getContact();
            SocketAddress addr = src.getContactAddress();
            return hostFilter.allow(addr);
        }
        return true;
    }
    
    @Override
    public void handleMessage(Message message) throws IOException {
        // Make sure we're not receiving messages from ourself.
        Contact node = message.getContact();
        KUID nodeId = node.getNodeID();
        SocketAddress src = node.getContactAddress();
        
        if (context.isLocalContactAddress(src)
                || (context.isLocalNodeID(nodeId) 
                        && !(message instanceof PingResponse))) {
            
            if (LOG.isErrorEnabled()) {
                String msg = "Received a message of type " 
                    + message.getClass().getName() 
                    + " from " + node 
                    + " which is equal to our local Node " 
                    + context.getLocalNode();
                
                LOG.error(msg);
            }
            
            return;
        }
        
        if (!NetworkUtils.isValidSocketAddress(src)) {
            if (LOG.isErrorEnabled()) {
                LOG.error(node + " has an invalid IP:Port");
            }
            return;
        }
        
        // Make sure we're not mixing IPv4 and IPv6 addresses.
        // See RouteTableImpl.add() for more info!
        if (!ContactUtils.isSameAddressSpace(context.getLocalNode(), node)) {
            
            // Log as ERROR so that we're not missing this
            if (LOG.isErrorEnabled()) {
                LOG.error(node + " is from a different IP address space than local Node");
            }
            return;
        }
        
        if (!allow(message)) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Dropping message from " + node);
            }
            
            return;
        }
        
        super.handleMessage(message);
    }

    @Override
    protected void handleRequest(RequestMessage request) throws IOException {
        
        // A Node that is marked as firewalled must not respond
        // to REQUESTS!
        if (context.getLocalNode().isFirewalled()
                && NetworkSettings.DROP_REQUEST_IF_FIREWALLED.getValue()) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Local Node is firewalled, dropping " + request);
            }
            
            return;
        }
        
        defaultHandler.handleRequest(request);
        
        if (request instanceof PingRequest) {
            ping.handleRequest(request);
        } else if (request instanceof NodeRequest) {
            node.handleRequest(request);
        } else if (request instanceof ValueRequest) {
            value.handleRequest(request);
        } else if (request instanceof StoreRequest) {
            store.handleRequest(request);
        } else {
            unhandledRequest(request);
        }
    }
    
    @Override
    protected void handleResponse(ResponseHandler callback, RequestHandle request,
            ResponseMessage response, long time, TimeUnit unit) throws IOException {
        
        Contact node = response.getContact();
        SocketAddress src = node.getContactAddress();
        MessageID messageId = response.getMessageId();
        
        // The remote Node thinks it's firewalled but it responded 
        // for some odd reason which it shouldn't regardless if it
        // is really firewalled (it didn't receive our request in
        // first place) or pretends to be firewalled in which is
        // a hint that it doesn't want to be contacted. Anyways, it
        // is a bug on the other side (a Mojito compatible impl).
        if (node.isFirewalled() 
                && NetworkSettings.DROP_RESPONE_IF_FIREWALLED.getValue()) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Dropping " + response + " because sender is firewalled");
            }
            return;
        }

        // Check the SecurityToken in the MessageID to figure out
        // whether or not we have ever sent a Request to that Host!
        if (messageId instanceof MessageID.Tagging) {
            if (!((MessageID.Tagging)messageId).isFor(src)) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn(response.getContact() + " sent us an unsolicited response: " + response);
                }
                return;
            }
        }
        
        node.setRoundTripTime(unit.toMillis(time));
        
        super.handleResponse(callback, request, response, time, unit);
        defaultHandler.handleResponse(request, response, time, unit);
    }
    
    @Override
    protected void handleLateResponse(ResponseMessage response) throws IOException {
        defaultHandler.handleLateResponse(response);
    }
    
    @Override
    protected void handleTimeout(ResponseHandler callback, 
            RequestHandle request, long time, TimeUnit unit) throws IOException {
        
        super.handleTimeout(callback, request, time, unit);
        defaultHandler.handleTimeout(request, time, unit);
    }

    /**
     * 
     */
    protected void unhandledRequest(RequestMessage message) throws IOException {
        if (LOG.isErrorEnabled()) {
            LOG.error("Unhandled Request: " + message);
        }
    }
}
