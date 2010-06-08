package org.limewire.mojito.io;

import java.io.IOException;
import java.math.BigInteger;
import java.net.SocketAddress;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.entity.DefaultPingEntity;
import org.limewire.mojito.entity.PingEntity;
import org.limewire.mojito.message.MessageFactory;
import org.limewire.mojito.message.MessageHelper;
import org.limewire.mojito.message.PingResponse;
import org.limewire.mojito.message.RequestMessage;
import org.limewire.mojito.message.ResponseMessage;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.settings.PingSettings;
import org.limewire.mojito.util.EntryImpl;
import org.limewire.mojito.util.MaxStack;

/**
 * This class pings a given number of hosts in parallel and returns the 
 * first successful ping.
 */
public class PingResponseHandler extends AbstractResponseHandler<PingEntity> {
    
    private static final Log LOG 
        = LogFactory.getLog(PingResponseHandler.class);
    
    private final MaxStack stack = new MaxStack(
            PingSettings.PARALLEL_PINGS.getValue());
    
    private final Pinger pinger;
    
    /**
     * Creates a {@link PingResponseHandler} that sends a <tt>PING</tt>
     * to the given {@link SocketAddress}.
     */
    public PingResponseHandler(Context context, 
            SocketAddress dst, 
            long timeout, TimeUnit unit) {
        this(context, new SocketAddressPinger(dst), timeout, unit);
    }
    
    /**
     * Creates a {@link PingResponseHandler} that sends a <tt>PING</tt>
     * to the given {@link SocketAddress}.
     */
    public PingResponseHandler(Context context, 
            KUID contactId,
            SocketAddress dst, 
            long timeout, TimeUnit unit) {
        this(context, new SocketAddressPinger(contactId, dst), 
                timeout, unit);
    }
    
    /**
     * Creates a {@link PingResponseHandler} that sends a <tt>PING</tt>
     * to the given {@link Contact}.
     */
    public PingResponseHandler(Context context, 
            Contact dst, long timeout, TimeUnit unit) {
        this(context, new ContactPinger(dst), timeout, unit);
    }
    
    /**
     * Creates a {@link PingResponseHandler} that sends a <tt>PING</tt>
     * from the given source to the destination {@link Contact}.
     */
    public PingResponseHandler(Context context, 
            Contact src, Contact dst, long timeout, TimeUnit unit) {
        this(context, new ContactPinger(src, dst), timeout, unit);
    }
    
    /**
     * Creates a {@link PingResponseHandler} that sends a <tt>PING</tt>
     * from the given source to the array of {@link Contact}s.
     */
    public PingResponseHandler(Context context, 
            Contact src, Contact[] dst, long timeout, TimeUnit unit) {
        this(context, new ContactPinger(src, dst), timeout, unit);
    }
    
    /**
     * Creates a {@link PingResponseHandler}.
     */
    private PingResponseHandler(Context context, 
            Pinger pinger, long timeout, TimeUnit unit) {
        super(context, timeout, unit);
        
        this.pinger = pinger;
    }
    
    @Override
    protected void start() throws IOException {
        process(0);
    }
    
    private void process(int count) throws IOException {
        try {
            preProcess(count);
            
            while (stack.hasFree()) {
                if (!pinger.hasMore()) {
                    break;
                }
                
                pinger.ping(this, timeout, unit);
                stack.push();
            }
            
        } finally {
            postProcess();
        }
    }
    
    private void preProcess(int count) {
        stack.pop(count);
    }
    
    private void postProcess() {
        int count = stack.poll();
        if (count == 0) {
            complete();
        }
    }
    
    /**
     * Called if the process is complete. Should never happen!
     */
    private void complete() {
        setException(new IllegalStateException());
    }
    
    /**
     * Returns {@code true} if this {@link PingResponseHandler} is 
     * sending <tt>PINGs</tt> that are testing for a {@link KUID}
     * collision.
     */
    private boolean isCollisionPing() {
        if (pinger instanceof ContactPinger) {
            return ((ContactPinger)pinger).isCollisionPing();
        }
        return false;
    }
    
    @Override
    protected void processResponse(RequestHandle request, 
            ResponseMessage response, long time, TimeUnit unit) throws IOException {
        
        PingResponse pong = (PingResponse)response;
        
        Contact src = pong.getContact();
        KUID contactId = src.getContactId();
        SocketAddress externalAddress = pong.getExternalAddress();
        BigInteger estimatedSize = pong.getEstimatedSize();
        
        if (src.getContactAddress().equals(externalAddress)) {
            if (LOG.isErrorEnabled()) {
                LOG.error(src + " is trying to set our external address to its address!");
            }
            
            setException(new IOException());
            return;
        }
        
        Contact localhost = context.getLocalhost();
        KUID localhostId = localhost.getContactId();
        
        // Check if the other Node has the same ID as we do
        if (contactId.equals(localhostId) 
                && !isCollisionPing()) {
            
            // If so check if this was a Node ID collision
            // test ping. To do so see if we've set a customized
            // sender which has a different Node ID than our
            // actual Node ID
            
            if (LOG.isErrorEnabled()) {
                LOG.error(src + " is trying to spoof our Node ID");
            }
            
            setException(new IOException());
            return;
        }
        
        setValue(new DefaultPingEntity(src, externalAddress, 
                estimatedSize, time, unit));
    }
    
    @Override
    protected synchronized void processTimeout(RequestHandle request, 
            long time, TimeUnit unit) throws IOException {
        
        int count = stack.poll();
        if (count == 1 && !pinger.hasMore()) {
            super.processTimeout(request, time, unit);
            return;
        }
        
        process(1);
    }
    
    @Override
    protected synchronized void processException(
            RequestHandle request, Throwable exception) {
        
        int count = stack.poll();
        if (count == 1 && !pinger.hasMore()) {
            super.processException(request, exception);
            return;
        }
        
        try {
            process(1);
        } catch (Throwable t) {
            uncaughtException(t);
        }
    }
    
    private void uncaughtException(Throwable t) {
        setException(t);
    }
    
    /**
     * An interface to send <tt>PINGs</tt>.
     */
    private static interface Pinger {
        
        /**
         * Returns {@code true} if the {@link Pinger} has more
         * hosts to ping.
         */
        public boolean hasMore();
        
        /**
         * Sends a <tt>PING</tt> to the next host in the queue.
         */
        public void ping(PingResponseHandler handler,
                long timeout, TimeUnit unit) throws IOException;
    }
    
    /**
     * An implementation of {@link Pinger} that sends <tt>PINGs</tt> 
     * to {@link SocketAddress}es.
     */
    private static class SocketAddressPinger implements Pinger {
        
        private final Entry<KUID, SocketAddress>[] entries;
        
        private int index = 0;
        
        public SocketAddressPinger(SocketAddress dst) {
            this(wrap(null, dst));
        }
        
        public SocketAddressPinger(KUID contactId, SocketAddress dst) {
            this(wrap(contactId, dst));
        }
        
        public SocketAddressPinger(Entry<KUID, SocketAddress>[] entries) {
            this.entries = entries;
        }

        @Override
        public boolean hasMore() {
            return index < entries.length;
        }

        @Override
        public void ping(PingResponseHandler handler,
                long timeout, TimeUnit unit) throws IOException {
            
            Entry<KUID, SocketAddress> entry = entries[index++];
            
            KUID contactId = entry.getKey();
            SocketAddress dst = entry.getValue();
            
            Context context = handler.getContext();
            MessageHelper messageHelper = context.getMessageHelper();
            RequestMessage request = messageHelper.createPingRequest(dst);
            
            handler.send(contactId, dst, request, timeout, unit);
        }
        
        @SuppressWarnings("unchecked")
        private static Entry<KUID, SocketAddress>[] wrap(
                KUID contactId, SocketAddress address) {
            return new Entry[] { new EntryImpl<KUID, SocketAddress>(contactId, address) };
        }
    }
    
    /**
     * An implementation of {@link Pinger} that sends <tt>PINGs</tt> 
     * to {@link Contact}s.
     */
    private static class ContactPinger implements Pinger {
        
        private final Contact src;
        
        private final Contact[] contacts;
        
        private int index = 0;
        
        public ContactPinger(Contact dst) {
            this(null, new Contact[] { dst });
        }
        
        public ContactPinger(Contact src, Contact dst) {
            this(src, new Contact[] { dst });
        }
        
        public ContactPinger(Contact src, Contact[] contacts) {
            this.src = src;
            this.contacts = contacts;
        }
        
        @Override
        public boolean hasMore() {
            return index < contacts.length;
        }

        /**
         * Returns {@code true} if the {@link Pinger} is sending
         * <tt>PINGs</tt> that check for collisions.
         */
        public boolean isCollisionPing() {
            return src != null;
        }
        
        @Override
        public void ping(PingResponseHandler handler,
                long timeout, TimeUnit unit) throws IOException {
            
            Contact dst = contacts[index++];
            
            KUID contactId = dst.getContactId();
            SocketAddress addr = dst.getContactAddress();
            
            Context context = handler.getContext();
            MessageHelper messageHelper = context.getMessageHelper();
            
            RequestMessage request = null;
            if (src == null) {
                request = messageHelper.createPingRequest(addr);
            } else {
                MessageFactory messageFactory 
                    = messageHelper.getMessageFactory();
                request = messageFactory.createPingRequest(src, addr);
            }
            
            long adaptiveTimeout = dst.getAdaptativeTimeout(timeout, unit);
            handler.send(contactId, addr, request, adaptiveTimeout, unit);
        }
    }
}
