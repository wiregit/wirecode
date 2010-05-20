package com.limegroup.gnutella.dht.db;

import java.io.Closeable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.core.settings.DHTSettings;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.routing.Version;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.dht.util.KUIDUtils;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileView;
import com.limegroup.gnutella.library.GnutellaFiles;
import com.limegroup.gnutella.tigertree.HashTree;
import com.limegroup.gnutella.tigertree.HashTreeCache;

/**
 * 
 */
@Singleton
public class AltLocPublisher implements Closeable {

    private static final ScheduledExecutorService EXECUTOR 
        = Executors.newSingleThreadScheduledExecutor(
            ExecutorsHelper.defaultThreadFactory(
                "AltLocPublisherThread"));
    
    private static final String TIMESTAMP_KEY 
        = AltLocPublisher.class.getName() + ".TIMESTAMP_KEY";
    
    private final PublisherQueue queue;
    
    private final NetworkManager networkManager;
    
    private final ApplicationServices applicationServices;
    
    private final Provider<HashTreeCache> tigerTreeCache;

    private final FileView gnutellaFileView;
    
    private final long frequency;
    
    private final TimeUnit unit;
    
    private boolean open = true;
    
    private ScheduledFuture<?> future;
    
    /**
     * 
     */
    @Inject
    public AltLocPublisher(PublisherQueue queue,
            NetworkManager networkManager, 
            ApplicationServices applicationServices,
            @GnutellaFiles FileView gnutellaFileView, 
            Provider<HashTreeCache> tigerTreeCache) {
        this (queue, networkManager, applicationServices, 
                gnutellaFileView, tigerTreeCache, 
                DHTSettings.LOCATION_PUBLISHER_FREQUENCY.getTimeInMillis(),
                TimeUnit.MILLISECONDS);
    }
    
    /**
     * 
     */
    public AltLocPublisher(PublisherQueue queue,
            NetworkManager networkManager, 
            ApplicationServices applicationServices,
            @GnutellaFiles FileView gnutellaFileView, 
            Provider<HashTreeCache> tigerTreeCache,
            long frequency, TimeUnit unit) {
        
        this.queue = queue;
        this.networkManager = networkManager;
        this.applicationServices = applicationServices;
        this.gnutellaFileView = gnutellaFileView;
        this.tigerTreeCache = tigerTreeCache;
        
        this.frequency = frequency;
        this.unit = unit;
    }
    
    public synchronized void start() {
        if (!open) {
            throw new IllegalStateException();
        }
        
        if (future != null && !future.isDone()) {
            return;
        }
        
        Runnable task = new Runnable() {
            @Override
            public void run() {
                publish();
            }
        };
        
        future = EXECUTOR.scheduleWithFixedDelay(
                task, frequency, frequency, unit);
    }
    
    public synchronized void stop() {
        if (future != null) {
            future.cancel(true);
        }
    }

    @Override
    public synchronized void close() {
        open = false;
        stop();
    }
    
    private void publish() {
        if (!DHTSettings.PUBLISH_ALT_LOCS.getValue()) {
            return;
        }
        
        byte[] guid = applicationServices.getMyGUID();
        int port = networkManager.getPort();
        boolean firewalled = !networkManager.acceptedIncomingConnection();
        boolean supportsTLS = networkManager.isIncomingTLSEnabled();
        
        gnutellaFileView.getReadLock().lock();
        try {
            for(FileDesc fd : gnutellaFileView) {
                
                // Skip all Non-Rare files!
                if (!fd.isRareFile()) {
                    continue;
                }
                
                // Skip all files that have been published
                if (!isPublishRequired(fd)) {
                    continue;
                }
                
                URN urn = fd.getSHA1Urn();
                KUID primaryKey = KUIDUtils.toKUID(urn);
                
                long fileSize = fd.getFileSize();
                HashTree hashTree = tigerTreeCache.get().getHashTree(urn);
                byte[] ttroot = null;
                if (hashTree != null) {
                    ttroot = hashTree.getRootHashBytes();
                }
                
                AltLocValue2 value = new AltLocValue2.Impl(
                        Version.ZERO, guid, port, fileSize, 
                        ttroot, firewalled, supportsTLS);
                
                queue.put(primaryKey, value.serialize());
                setTimeStamp(fd);
            }
        } finally {
            gnutellaFileView.getReadLock().unlock();
        }
    }
    
    private static long getTimeStamp(FileDesc fd) {
        Long timeStamp = (Long)fd.getClientProperty(TIMESTAMP_KEY);
        return timeStamp != null ? timeStamp : 0L;
    }
    
    private static void setTimeStamp(FileDesc fd) {
        fd.putClientProperty(TIMESTAMP_KEY, System.currentTimeMillis());
    }
    
    private static boolean isPublishRequired(FileDesc fd) {
        long time = System.currentTimeMillis() - getTimeStamp(fd);
        long every = DHTSettings.PUBLISH_LOCATION_EVERY.getTimeInMillis();
        return time >= every;
    }
}
