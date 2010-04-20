package org.limewire.mojito.handler.response;

import java.io.IOException;
import java.math.BigInteger;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito.Context2;
import org.limewire.mojito.KUID;
import org.limewire.mojito.entity.DefaultPingEntity;
import org.limewire.mojito.entity.PingEntity;
import org.limewire.mojito.messages.MessageFactory;
import org.limewire.mojito.messages.MessageHelper2;
import org.limewire.mojito.messages.PingResponse;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.messages.ResponseMessage;
import org.limewire.mojito.routing.Contact;

/**
 * This class pings a given number of hosts in parallel and returns the 
 * first successful ping.
 */
public class PingResponseHandler2 extends AbstractResponseHandler2<PingEntity> {

    //private static final Log LOG = LogFactory.getLog(PingResponseHandler2.class);
    
    private final Pinger pinger;
    
    /**
     * 
     */
    public PingResponseHandler2(Context2 context, 
            SocketAddress dst, 
            long timeout, TimeUnit unit) {
        this(context, new SocketAddressPinger(dst), timeout, unit);
    }
    
    /**
     * 
     */
    public PingResponseHandler2(Context2 context, 
            KUID contactId,
            SocketAddress dst, 
            long timeout, TimeUnit unit) {
        this(context, new SocketAddressPinger(contactId, dst), 
                timeout, unit);
    }
    
    /**
     * 
     */
    public PingResponseHandler2(Context2 context, 
            Contact dst, long timeout, TimeUnit unit) {
        this(context, new ContactPinger(dst), timeout, unit);
    }
    
    /**
     * 
     */
    public PingResponseHandler2(Context2 context, 
            Contact src, Contact dst, long timeout, TimeUnit unit) {
        this(context, new ContactPinger(src, dst), timeout, unit);
    }
    
    /**
     * 
     */
    private PingResponseHandler2(Context2 context, 
            Pinger pinger, long timeout, TimeUnit unit) {
        super(context, timeout, unit);
        
        this.pinger = pinger;
    }
    
    @Override
    protected void start() throws IOException {
        pinger.ping(this, timeout, unit);
    }
    
    /**
     * 
     */
    private boolean isCollisionPing() {
        if (pinger instanceof ContactPinger) {
            return ((ContactPinger)pinger).isCollisionPing();
        }
        return false;
    }
    
    @Override
    protected void processResponse(RequestMessage request, 
            ResponseMessage response, long time, TimeUnit unit) throws IOException {
        
        PingResponse pr = (PingResponse)response;
        
        Contact node = pr.getContact();
        
        SocketAddress externalAddress = pr.getExternalAddress();
        BigInteger estimatedSize = pr.getEstimatedSize();
        
        if (node.getContactAddress().equals(externalAddress)) {
            /*if (LOG.isErrorEnabled()) {
                LOG.error(node + " is trying to set our external address to its address!");
            }*/
            
            setException(new IOException());
            return;
        }
        
        // Check if the other Node has the same ID as we do
        if (context.isLocalNodeID(node.getNodeID()) 
                && !isCollisionPing()) {
            
            // If so check if this was a Node ID collision
            // test ping. To do so see if we've set a customized
            // sender which has a different Node ID than our
            // actual Node ID
            
            /*if (LOG.isErrorEnabled()) {
                LOG.error(node + " is trying to spoof our Node ID");
            }*/
            
            setException(new IOException());
            return;
        }
        
        setValue(new DefaultPingEntity(node, externalAddress, 
                estimatedSize, time, unit));
    }

    /**
     * 
     */
    private static interface Pinger {
        
        /**
         * 
         */
        public void ping(PingResponseHandler2 handler,
                long timeout, TimeUnit unit) throws IOException;
    }
    
    /**
     * 
     */
    private static class SocketAddressPinger implements Pinger {
        
        private final KUID contactId;
        
        private final SocketAddress dst;
        
        public SocketAddressPinger(SocketAddress dst) {
            this(null, dst);
        }
        
        public SocketAddressPinger(KUID contactId, SocketAddress dst) {
            this.contactId = contactId;
            this.dst = dst;
        }

        @Override
        public void ping(PingResponseHandler2 handler,
                long timeout, TimeUnit unit) throws IOException {
            
            Context2 context = handler.getContext();
            MessageHelper2 messageHelper = context.getMessageHelper();
            RequestMessage request = messageHelper.createPingRequest(dst);
            
            handler.send(contactId, dst, request, timeout, unit);
        }
    }
    
    /**
     * 
     */
    private static class ContactPinger implements Pinger {
        
        private final Contact src;
        
        private final Contact dst;
        
        /**
         * 
         */
        public ContactPinger(Contact dst) {
            this(null, dst);
        }
        
        /**
         * 
         */
        public ContactPinger(Contact src, Contact dst) {
            this.src = src;
            this.dst = dst;
        }
        
        /**
         * 
         */
        public boolean isCollisionPing() {
            return src != null;
        }
        
        @Override
        public void ping(PingResponseHandler2 handler,
                long timeout, TimeUnit unit) throws IOException {
            
            KUID contactId = dst.getNodeID();
            SocketAddress addr = dst.getContactAddress();
            
            Context2 context = handler.getContext();
            MessageHelper2 messageHelper = context.getMessageHelper();
            
            RequestMessage request = null;
            if (src == null) {
                request = messageHelper.createPingRequest(addr);
            } else {
                MessageFactory messageFactory 
                    = messageHelper.getMessageFactory();
                request = messageFactory.createPingRequest(src, addr);
            }
            
            handler.send(contactId, addr, request, timeout, unit);
        }
    }
}
