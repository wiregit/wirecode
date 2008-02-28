package com.limegroup.bittorrent.dht;

import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureListener;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.db.impl.DHTValueImpl;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.routing.Version;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.bittorrent.ManagedTorrent;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.dht.DHTEvent;
import com.limegroup.gnutella.dht.DHTEventListener;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.util.KUIDUtils;

@Singleton
public class DHTPeerPublisherImpl implements DHTPeerPublisher {

    /**
     * Value type associated with DHT lookup for a peer seeding a torrent.
     */
    private static final DHTValueType BT_PEER_TRIPLE = DHTValueType.valueOf("LimeBT Peer Triple",
            "PEER");

    private static final Log LOG = LogFactory.getLog(DHTPeerPublisher.class);

    private final DHTManager dhtManager;

    private final ApplicationServices applicationServices;

    private final NetworkManager networkManager;

    /**
     * List of published torrents mapped to the time it was published.
     */
    private final Map<ManagedTorrent, Time> publishedTorrents = new HashMap<ManagedTorrent, Time>();

    /**
     * List of torrents a peer is sharing but not published yet since DHT was
     * unavailable.
     */
    private final ArrayList<ManagedTorrent> torrentsWaitingForDHTList = new ArrayList<ManagedTorrent>();
    /**
     * List of torrents waiting to see if it was published properly
     */
    private final ArrayList<ManagedTorrent> torrentsWaitingForPutResultList = new ArrayList<ManagedTorrent>();

    @Inject
    DHTPeerPublisherImpl(DHTManager dhtManager, ApplicationServices applicationServices,
            NetworkManager networkManager) {

        this.dhtManager = dhtManager;
        this.applicationServices = applicationServices;
        this.networkManager = networkManager;
    }

    public void init() {
        dhtManager.addEventListener(new dhtEventListenerForPublisher());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.bittorrent.DHTPeerLocator#publish(com.limegroup.bittorrent.TorrentLocation)
     */
    public void publishYourself(ManagedTorrent managedTorrent) {
        LOG.debug("In publishYourself");
        if (managedTorrent.isActive() && networkManager.acceptedIncomingConnection()
                && !isPublished(managedTorrent) && !torrentsWaitingForDHTList.contains(managedTorrent)
                && !torrentsWaitingForPutResultList.contains(managedTorrent)) {

            LOG.debug("Passed Initial checks");

            URN urn = managedTorrent.getMetaInfo().getURN();

            if (urn == null) {
                LOG.debug("urn not available");
                return;
            }

            // holding a lock on DHT to ensure dht does not change status after
            // we acquired it
            synchronized (dhtManager) {
                MojitoDHT mojitoDHT = dhtManager.getMojitoDHT();

                if (LOG.isDebugEnabled())
                    LOG.debug("DHT: " + mojitoDHT);

                if (mojitoDHT == null || !mojitoDHT.isBootstrapped()) {
                    LOG.debug("DHT is null or unable to bootstrap");
                    torrentsWaitingForDHTList.add(managedTorrent);
                    return;
                }

                // encodes ip, port and peerid into a array of bytes
                byte[] msg = getBTHost().getEncoded();
                // creates a KUID from torrent's metadata
                KUID key = KUIDUtils.toKUID(urn);

                if (msg != null) {
                    // publish the information in the DHT
                    DHTFuture<StoreResult> future = mojitoDHT.put(key, new DHTValueImpl(
                            BT_PEER_TRIPLE, Version.ZERO, msg));                    
                    // ensure publishing was successful
                    future.addDHTFutureListener(new PutResultDHTFutureListener(managedTorrent));
                    torrentsWaitingForPutResultList.add(managedTorrent);
                }
            }
        }
    }

    /**
     * Implements the DHTEventListener to perform future attempts at publishing
     * a peer based on DHTEvent.
     */
    private class dhtEventListenerForPublisher implements DHTEventListener {

        /**
         * This method is invoked when a DHTEvent occurs and if DHT is connected
         * then it tries to publish the peer again.
         */
        public void handleDHTEvent(DHTEvent evt) {
            if (evt.getType() == DHTEvent.Type.CONNECTED) {
                while (!torrentsWaitingForDHTList.isEmpty()) {
                    publishYourself(torrentsWaitingForDHTList.remove(0));
                }
            }
        }
    }

    /**
     * creates a BTConnectionTriple object for the local node
     * 
     * @return returns a BTConnectionTriple object of the local node
     */
    private BTConnectionTriple getBTHost() {
        return new BTConnectionTriple(networkManager.getAddress(), networkManager.getPort(),
                applicationServices.getMyBTGUID());
    }

    /**
     * This class listens to see if the peer was published successfully
     */
    public class PutResultDHTFutureListener implements DHTFutureListener<StoreResult> {

        private final ManagedTorrent managedTorrent;

        public PutResultDHTFutureListener(ManagedTorrent managedTorrent) {
            this.managedTorrent = managedTorrent;
        }

        public void handleFutureSuccess(StoreResult result) {
            addAsPublished(managedTorrent, new Time(System.currentTimeMillis()));
            torrentsWaitingForPutResultList.remove(managedTorrent);
        }

        public void handleCancellationException(CancellationException e) {
            torrentsWaitingForPutResultList.remove(managedTorrent);
            publishYourself(managedTorrent);
        }

        public void handleExecutionException(ExecutionException e) {
            torrentsWaitingForPutResultList.remove(managedTorrent);
            publishYourself(managedTorrent);
        }

        public void handleInterruptedException(InterruptedException e) {
            torrentsWaitingForPutResultList.remove(managedTorrent);
            publishYourself(managedTorrent);
        }
    }

    /**
     * Returns whether the peer has already published him/her self as someone
     * sharing the given torrent file
     * 
     * @param managedTorrent The torrent we are dealing with
     * @return whether the peer's information is already published or not
     */
    private boolean isPublished(ManagedTorrent managedTorrent) {
        return publishedTorrents.containsKey(managedTorrent);
    }

    /**
     * Adds a torrent to the map to indicate the peer has published themself as
     * someone sharing the file. Also records the time it was published.
     * 
     * @param managedTorrent The torrent we are dealing with
     */
    private void addAsPublished(ManagedTorrent managedTorrent, Time time) {
        publishedTorrents.put(managedTorrent, time);
        LOG.debug("published");
    }
}
