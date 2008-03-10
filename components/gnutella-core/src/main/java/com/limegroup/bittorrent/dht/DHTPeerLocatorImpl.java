package com.limegroup.bittorrent.dht;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.InvalidDataException;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureAdapter;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.result.FindValueResult;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.bittorrent.ManagedTorrent;
import com.limegroup.bittorrent.TorrentEvent;
import com.limegroup.bittorrent.TorrentEventListener;
import com.limegroup.bittorrent.TorrentLocation;
import com.limegroup.bittorrent.TorrentManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.dht.DHTEvent;
import com.limegroup.gnutella.dht.DHTEventListener;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.util.KUIDUtils;

/**
 * Given an instance of torrent, locates peers in DHT seeding that file.</br>
 * Also re-attempts to locate peers if DHT was not available.
 */
@Singleton
public class DHTPeerLocatorImpl implements DHTPeerLocator {

    private static final Log LOG = LogFactory.getLog(DHTPeerLocator.class);

    private final DHTManager dhtManager;

    private final TorrentManager torrentManager;

    /**
     * List of torrents for which a peer is trying to locate a seeder in DHT but
     * was unsuccessful since DHT is unavailable.
     */
    private final List<URN> torrentsWaitingForDHTList = new ArrayList<URN>();

    @Inject
    DHTPeerLocatorImpl(DHTManager dhtManager, TorrentManager torrentManager) {
        this.dhtManager = dhtManager;
        this.torrentManager = torrentManager;
    }

    /**
     * Adds the required listeners to TorrentManager and DHTManager. This method
     * should only be called once.
     */
    public void init() {
        // listens for the TorrentEvent TRACKER_FAILED to start locating a
        // peer
        torrentManager.addEventListener(new LocatorTorrentEventListener());
        // listens for the DHTEvent CONNECTED to re-attempt locating a peer
        dhtManager.addEventListener(new DHTEventListenerForLocator());
    }

    /**
     * Searches in the MojitoDHT for peers sharing the given torrent. Torrent's
     * infoHash is used as the key.
     * 
     * @param managedTorrent a <code>ManagedTorrent</code> instance of the
     *        torrent.
     */
    public void locatePeer(ManagedTorrent managedTorrent) {
        LOG.debug("In locatePeer");
        if (managedTorrent.isActive()) {
            URN urn = managedTorrent.getMetaInfo().getURN();
            // holding a lock on torrentsWaitingForDHTList here so that we do
            // not perform search for a same torrent twice.
            synchronized (torrentsWaitingForDHTList) {
                if (!torrentsWaitingForDHTList.contains(urn)) {
                    LOG.debug("Passed Initial checks");

                    // holding a lock on DHT to ensure dht does not change
                    // status after we acquired it
                    synchronized (dhtManager) {
                        MojitoDHT mojitoDHT = dhtManager.getMojitoDHT();
                        if (LOG.isDebugEnabled())
                            LOG.debug("DHT:" + mojitoDHT);
                        if (mojitoDHT == null || !mojitoDHT.isBootstrapped()) {
                            LOG.debug("DHT is null or unable to bootstrap");
                            torrentsWaitingForDHTList.add(urn);
                            return;
                        }
                        proceedSearch(urn, mojitoDHT);
                    }
                }
            }
        }
    }

    /**
     * Looks in the given DHT for a peer seeding the torrent file specified by
     * infoHash.
     * 
     * @param infoHash hashed data of the torrent file.
     * @param mojitoDHT an instance of the MojitoDHT.
     */
    private void proceedSearch(URN urn, MojitoDHT mojitoDHT) {
        // checking if the torrent is active, getTorrentForURN returns null if
        // there is no active managedTorrent for the given urn.
        if (torrentManager.getTorrentForURN(urn) != null) {
            // creates a KUID from torrent's metadata
            KUID key = KUIDUtils.toKUID(urn);
            // creates an entity key from the KUID and DHTValueType
            EntityKey eKey = EntityKey.createEntityKey(key, DHTPeerLocatorUtils.BT_PEER_TRIPLE);
            // retrieves the future
            final DHTFuture<FindValueResult> future = mojitoDHT.get(eKey);
            // handle the result retrieved in the future
            future.addDHTFutureListener(new LocatePeerResultHandler(urn, mojitoDHT));

        }
    }

    /**
     * Gets the torrent's location and adds it as an endPoint.
     * 
     * @param managedTorrent The torrent we are dealing with.
     * @param entity DHTValueEntity containing the DHTValue we are looking for.
     */
    private void dispatch(ManagedTorrent managedTorrent, DHTValueEntity entity) {
        DHTValue value = entity.getValue();
        LOG.debug("dispatch entered");
        try {
            final TorrentLocation torLoc = DHTPeerLocatorUtils.decode(value.getValue());
            managedTorrent.addEndpoint(torLoc);
        } catch (IllegalArgumentException iae) {
            // if the payload passed in to decode was incorrect
            LOG.error("Invalid payload");
        } catch (InvalidDataException ide) {
            // if the network information stored was incorrect
            LOG.error("Invalid network information");
        }
    }

    /**
     * Listens for a TRACKER_FAILED event and launched search for alternate
     * location.
     */
    private class LocatorTorrentEventListener implements TorrentEventListener {
        public void handleTorrentEvent(TorrentEvent evt) {
            if (evt.getType() == TorrentEvent.Type.TRACKER_FAILED) {
                LOG.debug("TRACKER_FAILED_EVENT");
                locatePeer(evt.getTorrent());
            }
        }
    }

    /**
     * Implements the DHTEventListener to perform future attempts at locating a
     * peer based on DHTEvent.
     */
    private class DHTEventListenerForLocator implements DHTEventListener {
        /**
         * This method is invoked when a DHTEvent occurs and if DHT is connected
         * then it tries to search for a peer.
         */
        public void handleDHTEvent(DHTEvent evt) {
            LOG.debug("In handle event");
            List<URN> torrentsWaitingForDHTListCopy;
            if (evt.getType() == DHTEvent.Type.CONNECTED) {
                MojitoDHT dht;
                synchronized (dhtManager) {
                    dht = dhtManager.getMojitoDHT();
                    if (dht == null || !dht.isBootstrapped()) {
                        LOG.error("Incorrect DHTEvent generated");
                        return;
                    }
                }
                synchronized (torrentsWaitingForDHTList) {
                    torrentsWaitingForDHTListCopy = torrentsWaitingForDHTList;
                    torrentsWaitingForDHTList.clear();
                }
                for (int i = 0; i < torrentsWaitingForDHTListCopy.size(); i++) {
                    proceedSearch(torrentsWaitingForDHTListCopy.get(i), dht);
                }
            }
        }
    }

    /**
     * This class listens to see a result was successfully retrieved.
     */
    private class LocatePeerResultHandler extends DHTFutureAdapter<FindValueResult> {
        private final URN urn;

        private final MojitoDHT dht;

        public LocatePeerResultHandler(URN urn, MojitoDHT dht) {
            this.urn = urn;
            this.dht = dht;
        }

        @Override
        public void handleFutureSuccess(FindValueResult result) {
            LOG.debug("handle result");
            ManagedTorrent managedTorrent = torrentManager.getTorrentForURN(urn);
            if (result.isSuccess() && managedTorrent != null) {
                LOG.debug("successful result");
                for (DHTValueEntity entity : result.getEntities()) {
                    dispatch(managedTorrent, entity);
                }

                for (EntityKey entityKey : result.getEntityKeys()) {
                    if (!entityKey.getDHTValueType().equals(DHTPeerLocatorUtils.BT_PEER_TRIPLE)) {
                        continue;
                    }

                    try {
                        DHTFuture<FindValueResult> future = dht.get(entityKey);
                        FindValueResult resultFromKey = future.get();

                        if (resultFromKey.isSuccess()) {
                            for (DHTValueEntity entity : resultFromKey.getEntities()) {
                                dispatch(managedTorrent, entity);
                            }
                        }
                    } catch (ExecutionException e) {
                        LOG.error("ExecutionException", e);
                    } catch (InterruptedException e) {
                        LOG.error("InterruptedException", e);
                    }
                }
            }
        }
    }
}