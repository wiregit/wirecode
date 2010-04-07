package org.limewire.mojito.handler.response2;

import java.io.IOException;
import java.math.BigInteger;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.concurrent2.AsyncFuture;
import org.limewire.mojito.entity.DefaultPingEntity;
import org.limewire.mojito.entity.PingEntity;
import org.limewire.mojito.exceptions.DHTBackendException;
import org.limewire.mojito.exceptions.DHTBadResponseException;
import org.limewire.mojito.exceptions.DHTException;
import org.limewire.mojito.messages.PingResponse;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.messages.ResponseMessage;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.settings.PingSettings;
import org.limewire.mojito.util.ContactUtils;

/**
 * This class pings a given number of hosts in parallel and returns the 
 * first successful ping.
 */
public class PingResponseHandler extends AbstractResponseHandler<PingEntity> {

    private static final Log LOG = LogFactory.getLog(PingResponseHandler.class);
    
    private final AtomicInteger active = new AtomicInteger();
    
    private final AtomicInteger failures = new AtomicInteger();
    
    private final Contact sender;
    
    private final PingIterator pinger;
    
    private volatile int parallelism;
    
    private volatile int maxParallelPingFailures;
    
    public PingResponseHandler(Context context, PingIterator pinger) {
        this(context, null, pinger);
    }
    
    public PingResponseHandler(Context context, 
            Contact sender, PingIterator pinger) {
        super(context);
        
        this.sender = sender;
        this.pinger = pinger;
        
        setParallelism(-1);
        setMaxParallelPingFailures(-1);
    }
    
    public void setParallelism(int parallelism) {
        if (parallelism < 0) {
            this.parallelism = PingSettings.PARALLEL_PINGS.getValue();
        } else if (parallelism > 0) {
            this.parallelism = parallelism;
        } else {
            throw new IllegalArgumentException("parallelism=" + parallelism);
        }
    }
    
    public int getParallelism() {
        return parallelism;
    }
    
    public void setMaxParallelPingFailures(int maxParallelPingFailures) {
        if (maxParallelPingFailures < 0) {
            this.maxParallelPingFailures 
                = PingSettings.MAX_PARALLEL_PING_FAILURES.getValue();
        } else {
            this.maxParallelPingFailures = maxParallelPingFailures;
        }
    }
    
    public int getMaxParallelPingFailures() {
        return maxParallelPingFailures;
    }
    
    @Override
    protected void doStart(AsyncFuture<PingEntity> future) 
            throws DHTException, IOException {
        
        if (!pinger.hasNext()) {
            throw new DHTException("No hosts to ping");
        }
        
        pingNextAndThrowExceptionIfDone(new DHTException(
                "All SocketAddresses were invalid and there are no Hosts left to Ping: "
                    + context.getLocalNode() + "; " + pinger));
    }
    
    @Override
    protected void processResponse(ResponseMessage message, 
            long time, TimeUnit unit) throws IOException {
        
        PingResponse response = (PingResponse)message;
        
        active.decrementAndGet();
        
        Contact node = response.getContact();
        SocketAddress externalAddress = response.getExternalAddress();
        BigInteger estimatedSize = response.getEstimatedSize();
        
        if (node.getContactAddress().equals(externalAddress)) {
            pingNextAndThrowExceptionIfDone(new DHTBadResponseException(node 
                    + " is trying to set our external address to its address!"));
            return;
        }
        
        // Check if the other Node has the same ID as we do
        if (context.isLocalNodeID(node.getNodeID())) {
            
            // If so check if this was a Node ID collision
            // test ping. To do so see if we've set a customized
            // sender which has a different Node ID than our
            // actual Node ID
            
            if (sender == null) {
                pingNextAndThrowExceptionIfDone(new DHTBadResponseException(node 
                        + " is trying to spoof our Node ID"));
            } else {
                setValue(new DefaultPingEntity(node, externalAddress, 
                        estimatedSize, time, TimeUnit.MILLISECONDS));
            }
            return;
        }
        
        context.setExternalAddress(externalAddress);
        context.addEstimatedRemoteSize(estimatedSize);
        
        setValue(new DefaultPingEntity(node, externalAddress, 
                estimatedSize, time, unit));
    }

    @Override
    protected void processTimeout(KUID nodeId, SocketAddress dst, 
            RequestMessage message, long time, TimeUnit unit) throws IOException {
        
        active.decrementAndGet();
        failures.incrementAndGet();
        
        if (LOG.isInfoEnabled()) {
            LOG.info("Timeout: " + ContactUtils.toString(nodeId, dst));
        }
        
        if (giveUp()) {
            if (!hasActive()) {
                fireTimeoutException(nodeId, dst, message, time);
            } // else wait for the last response, timeout or error
        } else {
            pingNextAndThrowExceptionIfDone(
                    createTimeoutException(nodeId, dst, message, time));
        }
    }
    
    @Override
    protected void processError(KUID nodeId, SocketAddress dst, 
            RequestMessage message, IOException e) {
        
        active.decrementAndGet();
        failures.incrementAndGet();
        
        if(e instanceof SocketException && !giveUp()) {
            try {
                processTimeout(nodeId, dst, message, -1L, TimeUnit.MILLISECONDS);
            } catch (IOException err) {
                LOG.error("IOException", err);
                
                if (!pinger.hasNext()) {
                    setException(new DHTException(err));
                }
            }
        } else {
            setException(new DHTBackendException(nodeId, dst, message, e));
        }
    }

    private void pingNextAndThrowExceptionIfDone(Exception err) throws IOException {
        while (pinger.hasNext() && canMore()) {
            if (pinger.pingNext(context, this)) {
                active.incrementAndGet();
            }
        }
        
        if (!hasActive()) {
            setException(err);
        }
    }
    
    private boolean hasActive() {
        return 0 < active.get();
    }
    
    private boolean canMore() {
        return active.get() < parallelism;
    }
    
    private boolean giveUp() {
        return !pinger.hasNext() || failures.get() >= maxParallelPingFailures;
    }
    
    /**
     * The PingIterator interfaces allows PingResponseHandler to
     * send ping requests to any type of contacts like SocketAddress
     * or an actual Contact.
     */
    public static interface PingIterator {
        
        /**
         * Returns true if there are more elements to ping.
         */
        public boolean hasNext();
        
        /**
         * Sends a ping to the next element.
         */
        public boolean pingNext(Context context, 
                PingResponseHandler responseHandler) throws IOException;
    }
}
