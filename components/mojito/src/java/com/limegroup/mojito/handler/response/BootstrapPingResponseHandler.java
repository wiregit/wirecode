package com.limegroup.mojito.handler.response;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.event.BootstrapTimeoutException;
import com.limegroup.mojito.handler.AbstractResponseHandler;
import com.limegroup.mojito.messages.PingRequest;
import com.limegroup.mojito.messages.PingResponse;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.util.ContactUtils;

/**
 * This class pings a given number of hosts in parallel 
 * and returns the first successfull ping.
 *
 */
public class BootstrapPingResponseHandler<V> extends AbstractResponseHandler<Contact> {
    
    private static final Log LOG = LogFactory.getLog(BootstrapPingResponseHandler.class);
    
    /** The number of pings to send in parallel. TODO: separate from lookup parameter? */
    private static final int parallelism = 2*KademliaSettings.LOOKUP_PARAMETER.getValue();
    
    /** The list of hosts to ping */
    private Set<V> hostsToPing;
    
    /** The list of hosts that have failed (mutually exclusive with hostsToPing) */
    private Set<SocketAddress> failedHosts;
    
    private final Context context;
    
    /** The number of active pings */
    private int activePings = 0;
    
    /** The total number of failed pings */
    private int failedPings = 0;
    
    private boolean finished;
    
    public BootstrapPingResponseHandler(Context context, Set<V> hosts) {
        super(context);
        this.context = context;
        this.hostsToPing = hosts;
    }
    
    @Override
    protected synchronized void start() throws Exception {
        sendPings();
    }


    private void sendPings() throws IOException{
        for (Iterator<V> iter = hostsToPing.iterator(); 
                            iter.hasNext() && (activePings < parallelism);) {
            V host = iter.next();
            iter.remove();
            
            PingRequest request;
            SocketAddress address;
            if(host instanceof Contact) {
                Contact contact = (Contact) host;
                
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Sending bootstrap ping to contact: " + contact);
                }
                
                address = contact.getContactAddress();
                request = context.getMessageHelper().createPingRequest(address);
                context.getMessageDispatcher().send(contact.getNodeID(), address, request, this);
                
            } else { //it has to be a SocketAddress
                address = (SocketAddress) host;
                
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Sending bootstrap ping to address: " + address);
                }
                
                request = context.getMessageHelper().createPingRequest(address);
                context.getMessageDispatcher().send(null, address, request, this);
            }
            activePings++;
        }
    }
    
    private boolean shouldStop() {
        return ((hostsToPing.isEmpty()) 
                || (failedPings >= KademliaSettings.MAX_BOOTSTRAP_FAILURES.getValue()));
    }

    @Override
    protected synchronized void response(ResponseMessage message, long time) throws IOException {
        if(finished) {
            return;
        } 
        
        PingResponse response = (PingResponse)message;
        SocketAddress externalAddress = response.getExternalAddress();
        
        Contact node = response.getContact();
        if (node.getContactAddress().equals(externalAddress)) {
            setException(new Exception(node + " is trying to set our external address to its address!"));
            return;
        }
        
        if(LOG.isDebugEnabled()) {
            LOG.debug("Got bootstrap ping response from " + message.getContact());
        }
        
        context.setExternalSocketAddress(externalAddress);
        context.addEstimatedRemoteSize(response.getEstimatedSize());
        
        finished = true;
        
        setReturnValue(message.getContact());
    }
    
    @Override
    protected synchronized void error(KUID nodeId, SocketAddress dst, RequestMessage message, Exception e) {
        if(finished) {
            return;
        }
        
        if(LOG.isErrorEnabled()) {
            LOG.error("Bootstrap error: "+e);
        }
        
        if(e instanceof SocketException && !shouldStop()) {
            try {
                timeout(nodeId, dst, message, -1L);
            } catch (IOException err) {
                LOG.error("IOException", err);
                
                if (hostsToPing.isEmpty()) {
                    finished = true;
                    setException(err);
                }
            }
        } else {
            finished = true;
            setException(e);
        }
    }

    @Override
    protected synchronized void timeout(KUID nodeId, SocketAddress dst, RequestMessage message, long time) throws IOException {
        if(finished) {
            return;
        }
        
        if(LOG.isDebugEnabled()) {
            LOG.debug("Timeout on bootstrap ping to " + ContactUtils.toString(nodeId,dst));
        }
        
        failedPings++;
        assert (!failedHosts.contains(dst));
        failedHosts.add(dst);
        activePings--;
        
        if(shouldStop()) {
            finished = true;
            setException(new BootstrapTimeoutException(failedHosts));
            return;
        }
        sendPings();
    }
}
