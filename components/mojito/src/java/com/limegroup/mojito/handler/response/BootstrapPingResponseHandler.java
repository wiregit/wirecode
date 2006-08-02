package com.limegroup.mojito.handler.response;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.exceptions.BootstrapTimeoutException;
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
 */
public class BootstrapPingResponseHandler<V> extends AbstractResponseHandler<Contact> {
    
    private static final Log LOG = LogFactory.getLog(BootstrapPingResponseHandler.class);
    
    /** The number of pings to send in parallel. */
    private int parallelism = KademliaSettings.PARALLEL_PINGS.getValue();
    
    /** The list of hosts to ping */
    private Set<V> hostsToPing;
    
    /** The list of hosts that have failed (mutually exclusive with hostsToPing) */
    private Set<SocketAddress> failedHosts = new HashSet<SocketAddress>();
    
    private final Context context;
    
    /** The number of active pings */
    private int activePings = 0;
    
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
                iter.hasNext() && (activePings < parallelism); ) {
            
            V host = iter.next();
            iter.remove();
            
            if(host instanceof Contact) {
                Contact contact = (Contact) host;
                
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Sending bootstrap ping to contact: " + contact);
                }
                
                PingRequest request = context.getMessageHelper()
                    .createPingRequest(contact.getContactAddress());
                
                context.getMessageDispatcher().send(contact, request, this);
                
            } else { //it has to be a SocketAddress
                
                SocketAddress address = (SocketAddress) host;
                if (!NetworkUtils.isValidSocketAddress(address)) {
                    continue;
                }
                
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Sending bootstrap ping to address: " + address);
                }
                
                PingRequest request = context.getMessageHelper()
                    .createPingRequest(address);
                context.getMessageDispatcher().send(address, request, this);
            }
            
            activePings++;
        }
        
        if (activePings == 0) {
            setException(new Exception("All SocketAddresses were invalid and there are no Hosts left to Ping"));
        }
    }
    
    private boolean shouldStop() {
        return ((hostsToPing.isEmpty()) 
                || (failedHosts.size() >= KademliaSettings.MAX_BOOTSTRAP_FAILURES.getValue()));
    }

    @Override
    protected synchronized void response(ResponseMessage message, long time) throws IOException {
        PingResponse response = (PingResponse)message;
        SocketAddress externalAddress = response.getExternalAddress();
        
        Contact node = response.getContact();
        if (node.getContactAddress().equals(externalAddress)) {
            if (hostsToPing.isEmpty()) {
                setException(new Exception(node + " is trying to set our external address to its address!"));
            } else {
                if (LOG.isWarnEnabled()) {
                    LOG.warn(node + " is trying to set our external address to its address!");
                }
                sendPings();
            }
            return;
        }
        
        if(LOG.isDebugEnabled()) {
            LOG.debug("Got bootstrap ping response from " + message.getContact());
        }
        
        context.setExternalSocketAddress(externalAddress);
        context.addEstimatedRemoteSize(response.getEstimatedSize());
        
        setReturnValue(message.getContact());
    }

    @Override
    protected synchronized void timeout(KUID nodeId, SocketAddress dst, RequestMessage message, long time) throws IOException {
        
        if(LOG.isDebugEnabled()) {
            LOG.debug("Timeout on bootstrap ping to " + ContactUtils.toString(nodeId, dst));
        }
        
        // this is to make sure hostsToPing is a true Set (i.e. contains no publicates)
        assert (!failedHosts.contains(dst)); 
        failedHosts.add(dst);
        activePings--;
        
        if(shouldStop()) {
            setException(new BootstrapTimeoutException(failedHosts));
            return;
        }
        sendPings();
    }
    
    @Override
    protected synchronized void error(KUID nodeId, SocketAddress dst, RequestMessage message, Exception e) {
        
        if(LOG.isErrorEnabled()) {
            LOG.error("Bootstrap error: "+e);
        }
        
        if(e instanceof SocketException && !shouldStop()) {
            try {
                timeout(nodeId, dst, message, -1L);
            } catch (IOException err) {
                LOG.error("IOException", err);
                
                if (hostsToPing.isEmpty()) {
                    setException(err);
                }
            }
        } else {
            setException(e);
        }
    }
}