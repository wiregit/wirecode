package com.limegroup.gnutella.dht.db;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.limewire.core.settings.DHTSettings;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.mojito.KUID;
import org.limewire.mojito.db.Value;
import org.limewire.mojito.routing.Version;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.util.KUIDUtils;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileView;
import com.limegroup.gnutella.library.GnutellaFiles;
import com.limegroup.gnutella.tigertree.HashTree;
import com.limegroup.gnutella.tigertree.HashTreeCache;

/**
 * The {@link AltLocPublisher} publishes the localhost as an
 * {@link AlternateLocation} to the DHT.
 */
@Singleton
public class AltLocPublisher extends Publisher {
    
    private static final String TIMESTAMP_KEY 
        = AltLocPublisher.class.getName() + ".TIMESTAMP_KEY";
    
    @InspectablePrimitive(value = "The number of values that have been published")
    private static final AtomicInteger PUBLISH_COUNT = new AtomicInteger();
    
    private final DHTManager manager;
    
    private final NetworkManager networkManager;
    
    private final ApplicationServices applicationServices;
    
    private final Provider<HashTreeCache> tigerTreeCache;

    private final FileView gnutellaFileView;
    
    private volatile long publishTime = -1L;
    
    /**
     * Creates a {@link AltLocPublisher}
     */
    @Inject
    public AltLocPublisher(DHTManager manager,
            NetworkManager networkManager, 
            ApplicationServices applicationServices,
            @GnutellaFiles FileView gnutellaFileView, 
            Provider<HashTreeCache> tigerTreeCache) {
        this (manager, networkManager, applicationServices, 
                gnutellaFileView, tigerTreeCache, 
                DHTSettings.LOCATION_PUBLISHER_FREQUENCY.getTimeInMillis(),
                TimeUnit.MILLISECONDS);
    }
    
    /**
     * Creates a {@link AltLocPublisher}
     */
    public AltLocPublisher(DHTManager manager,
            NetworkManager networkManager, 
            ApplicationServices applicationServices,
            @GnutellaFiles FileView gnutellaFileView, 
            Provider<HashTreeCache> tigerTreeCache,
            long frequency, TimeUnit unit) {
        super(frequency, unit);
        
        this.manager = manager;
        this.networkManager = networkManager;
        this.applicationServices = applicationServices;
        this.gnutellaFileView = gnutellaFileView;
        this.tigerTreeCache = tigerTreeCache;
    }
    
    /**
     * Sets the publish time.
     * 
     * @see DHTSettings#PUBLISH_LOCATION_EVERY
     */
    public void setPublishTime(long time, TimeUnit unit) {
        publishTime = unit.toMillis(time);
    }
    
    /**
     * Returns the publish time or the default value.
     * 
     * @see DHTSettings#PUBLISH_LOCATION_EVERY
     */
    public long getPublishTime(TimeUnit unit) {
        long publishTime = this.publishTime;
        if (publishTime != -1L) {
            return unit.convert(publishTime, TimeUnit.MILLISECONDS);
        }
        
        return DHTSettings.PUBLISH_LOCATION_EVERY.getTime(unit);
    }
    
    /**
     * Returns the publish time or the default value in milliseconds.
     */
    public long getPublishTimeInMillis() {
        return getPublishTime(TimeUnit.MILLISECONDS);
    }
    
    /**
     * Tries to publish the localhost as an {@link AlternateLocation} 
     * to the DHT
     */
    @Override
    protected void publish() {
        if (!DHTSettings.PUBLISH_ALT_LOCS.getValue()) {
            return;
        }
        
        byte[] guid = applicationServices.getMyGUID();
        int port = networkManager.getPort();
        boolean firewalled = !networkManager.acceptedIncomingConnection();
        boolean supportsTLS = networkManager.isIncomingTLSEnabled();
        
        HashTreeCache cache = tigerTreeCache.get();
        
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
                HashTree hashTree = cache.getHashTree(urn);
                byte[] ttroot = null;
                if (hashTree != null) {
                    ttroot = hashTree.getRootHashBytes();
                }
                
                DefaultAltLocValue value = new DefaultAltLocValue(
                        Version.ZERO, guid, port, fileSize, 
                        ttroot, firewalled, supportsTLS);
                
                publish(primaryKey, value.serialize());
                
                setTimeStamp(fd);
                PUBLISH_COUNT.incrementAndGet();
            }
        } finally {
            gnutellaFileView.getReadLock().unlock();
        }
    }
    
    /**
     * Publishes the given {@link Value} to the DHT
     */
    protected void publish(KUID key, Value value) {
        manager.enqueue(key, value);
    }
    
    /**
     * Returns the {@link FileDesc}'s time stamp or 0L if it hasn't
     * been published yet.
     */
    private static long getTimeStamp(FileDesc fd) {
        Long timeStamp = (Long)fd.getClientProperty(TIMESTAMP_KEY);
        return timeStamp != null ? timeStamp : 0L;
    }
    
    /**
     * Sets the {@link FileDesc}'s time stamp to "now".
     */
    private static void setTimeStamp(FileDesc fd) {
        fd.putClientProperty(TIMESTAMP_KEY, System.currentTimeMillis());
    }
    
    /**
     * Returns true if the {@link FileDesc} needs to be re-published.
     */
    private boolean isPublishRequired(FileDesc fd) {
        long time = System.currentTimeMillis() - getTimeStamp(fd);
        return time >= getPublishTimeInMillis();
    }
}
