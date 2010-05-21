package com.limegroup.gnutella.dht.db;

import java.util.concurrent.TimeUnit;

import org.limewire.core.settings.DHTSettings;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.routing.Version;
import org.limewire.mojito2.storage.DHTValue;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AlternateLocation;
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
    
    private final PublisherQueue queue;
    
    private final NetworkManager networkManager;
    
    private final ApplicationServices applicationServices;
    
    private final Provider<HashTreeCache> tigerTreeCache;

    private final FileView gnutellaFileView;
    
    /**
     * Creates a {@link AltLocPublisher}
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
     * Creates a {@link AltLocPublisher}
     */
    public AltLocPublisher(PublisherQueue queue,
            NetworkManager networkManager, 
            ApplicationServices applicationServices,
            @GnutellaFiles FileView gnutellaFileView, 
            Provider<HashTreeCache> tigerTreeCache,
            long frequency, TimeUnit unit) {
        super(frequency, unit);
        
        this.queue = queue;
        this.networkManager = networkManager;
        this.applicationServices = applicationServices;
        this.gnutellaFileView = gnutellaFileView;
        this.tigerTreeCache = tigerTreeCache;
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
                
                publish(primaryKey, value.serialize());
                setTimeStamp(fd);
            }
        } finally {
            gnutellaFileView.getReadLock().unlock();
        }
    }
    
    /**
     * Publishes the given {@link DHTValue} to the DHT
     */
    protected void publish(KUID key, DHTValue value) {
        queue.put(key, value);
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
    private static boolean isPublishRequired(FileDesc fd) {
        long time = System.currentTimeMillis() - getTimeStamp(fd);
        long every = DHTSettings.PUBLISH_LOCATION_EVERY.getTimeInMillis();
        return time >= every;
    }
}
