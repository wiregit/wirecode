package org.limewire.mojito.handler.response2;

import java.io.IOException;
import java.math.BigInteger;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.concurrent2.AsyncFuture;
import org.limewire.mojito.entity.DefaultPingEntity;
import org.limewire.mojito.entity.PingEntity;
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
    
    private final Contact sender;
    
    private final PingIterator pinger;
    
    private volatile int parallelism;
    
    private volatile int maxParallelPingFailures;
    
    private final ProcessCounter processCounter = new ProcessCounter(4);
    
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
    protected void doStart(AsyncFuture<PingEntity> future) throws IOException {
        process(0);
    }
    
    private void process(int decrement) throws IOException {
        try {
            preProcess(decrement);
            while (processCounter.hasNext()) {
                if (!pinger.hasNext()) {
                    break;
                }
                
                pinger.pingNext(context, this);
                processCounter.increment();
            }
        } finally {
            postProcess();
        }
    }
    
    private void preProcess(int decrement) {
        processCounter.decrement(decrement);
    }
    
    private void postProcess() {
        int count = processCounter.get();
        if (count == 0) {
            setException(new IOException());
        }
    }
    
    @Override
    protected void processResponse(ResponseMessage message, 
            long time, TimeUnit unit) throws IOException {
        
        try {
            processResponse0(message, time, unit);
        } finally {
            process(1);
        }
    }
    
    private void processResponse0(ResponseMessage message, 
            long time, TimeUnit unit) throws IOException {
        
        PingResponse response = (PingResponse)message;
        
        Contact node = response.getContact();
        SocketAddress externalAddress = response.getExternalAddress();
        BigInteger estimatedSize = response.getEstimatedSize();
        
        if (node.getContactAddress().equals(externalAddress)) {
            if (LOG.isErrorEnabled()) {
                LOG.error(node + " is trying to set our external address to its address!");
            }
            
            return;
        }
        
        // Check if the other Node has the same ID as we do
        if (context.isLocalNodeID(node.getNodeID()) && sender == null) {
            
            // If so check if this was a Node ID collision
            // test ping. To do so see if we've set a customized
            // sender which has a different Node ID than our
            // actual Node ID
            
            if (LOG.isErrorEnabled()) {
                LOG.error(node + " is trying to spoof our Node ID");
            }
            
            return;
        }
        
        if (sender == null) {
            context.setExternalAddress(externalAddress);
            context.addEstimatedRemoteSize(estimatedSize);
        }
        
        setValue(new DefaultPingEntity(node, externalAddress, 
                estimatedSize, time, unit));
    }

    @Override
    protected void processTimeout(KUID nodeId, SocketAddress dst, 
            RequestMessage message, long time, TimeUnit unit) throws IOException {
        
        try {
            processTimeout0(nodeId, dst, message, time, unit);
        } finally {
            process(1);
        }
    }
    
    private void processTimeout0(KUID nodeId, SocketAddress dst, 
            RequestMessage message, long time, TimeUnit unit) throws IOException {
        
        if (LOG.isInfoEnabled()) {
            LOG.info("Timeout: " + ContactUtils.toString(nodeId, dst));
        }
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
