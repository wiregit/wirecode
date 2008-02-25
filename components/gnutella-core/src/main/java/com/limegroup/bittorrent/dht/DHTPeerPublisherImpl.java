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
import com.limegroup.bittorrent.ManagedTorrent;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.dht.DHTEvent;
import com.limegroup.gnutella.dht.DHTEventListener;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.util.KUIDUtils;

public class DHTPeerPublisherImpl implements DHTPeerPublisher {

    // used to identify the DHT
    private static final DHTValueType BT_PEER_TRIPLE = DHTValueType.valueOf("LimeBT Peer Triple",
            "PEER");

    private static final Log LOG = LogFactory.getLog(DHTPeerPublisher.class);

    private final DHTManager dhtManager;

    private final ApplicationServices applicationServices;

    private final NetworkManager networkManager;

    private final Map<ManagedTorrent, Time> publishedTorrents = new HashMap<ManagedTorrent, Time>();

    private final ArrayList<ManagedTorrent> torrentWaitingList = new ArrayList<ManagedTorrent>();

    @Inject
    DHTPeerPublisherImpl(DHTManager dhtManager, ApplicationServices applicationServices,
            NetworkManager networkManager) {

        this.dhtManager = dhtManager;
        this.applicationServices = applicationServices;
        this.networkManager = networkManager;
        dhtManager.addEventListener(new PublishDHTEventListener());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.bittorrent.DHTPeerLocator#publish(com.limegroup.bittorrent.TorrentLocation)
     */
    public void publishYourself(ManagedTorrent managedTorrent) {
        LOG.debug("ENTERED PUBLISH" + isPublished(managedTorrent)
                + !networkManager.acceptedIncomingConnection() + managedTorrent.isActive());
        // TODO change to check if incoming connections allowed
        if (!isPublished(managedTorrent) && !networkManager.acceptedIncomingConnection()
                && managedTorrent.isActive()) {

            // holding the lock on dhtManaged until the end since if we release
            // the lock earlier publishDHTEventListener and
            // mojitoDHT will get modified.
            synchronized (dhtManager) {
                URN urn = managedTorrent.getMetaInfo().getURN();

                if (urn == null) {
                    return;
                }

                MojitoDHT mojitoDHT = dhtManager.getMojitoDHT();

                if (LOG.isDebugEnabled())
                    LOG.debug(mojitoDHT);

                if (mojitoDHT == null || !mojitoDHT.isBootstrapped()) {
                    LOG.debug("DHT is null or unable to bootstrap"
                            + torrentWaitingList.contains(managedTorrent));
                    LOG.debug(torrentWaitingList.indexOf(managedTorrent));
                    LOG.debug(managedTorrent);
                    if (!torrentWaitingList.contains(managedTorrent)) {
                        LOG.debug("Waiting to retry");
                        torrentWaitingList.add(managedTorrent);
                    }
                    return;
                }

                LOG.debug("check 1");
                // encodes ip, port and peerid into a array of bytes
                byte[] msg = getBTHost().getEncoded();
                // creates a KUID from torrent's metadata
                KUID key = KUIDUtils.toKUID(urn);

                if (msg != null) {
                    // stores the information in the DHT
                    DHTFuture<StoreResult> future = mojitoDHT.put(key, new DHTValueImpl(
                            BT_PEER_TRIPLE, Version.ZERO, msg));
                    LOG.debug("waiting for put result");
                    future.addDHTFutureListener(new putResultDHTFutureListener(managedTorrent));
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
        LOG.debug("IP being registered: " + new String(networkManager.getAddress()) + "PORT = "
                + networkManager.getPort());
        return new BTConnectionTriple(networkManager.getAddress(), networkManager.getPort(),
                applicationServices.getMyBTGUID());
    }

    /**
     * This class listens to see if the DHT gets connected and if so it tries to
     * publish the peer again
     */
    private class PublishDHTEventListener implements DHTEventListener {

        public PublishDHTEventListener() {
        }

        public void handleDHTEvent(DHTEvent evt) {
            if (evt.getType() == DHTEvent.Type.CONNECTED) {
                while (!torrentWaitingList.isEmpty()) {
                    publishYourself(torrentWaitingList.remove(0));
                }
            }
        }
    }

    /**
     * This class listens to see if the peer was published successfully
     */
    private class putResultDHTFutureListener implements DHTFutureListener<StoreResult> {

        private final ManagedTorrent managedTorrent;

        public putResultDHTFutureListener(ManagedTorrent managedTorrent) {
            this.managedTorrent = managedTorrent;
        }

        public void handleFutureSuccess(StoreResult result) {
            addAsPublished(managedTorrent, new Time(System.currentTimeMillis()));
        }

        public void handleCancellationException(CancellationException e) {
            publishYourself(managedTorrent);
        }

        public void handleExecutionException(ExecutionException e) {
            publishYourself(managedTorrent);
        }

        public void handleInterruptedException(InterruptedException e) {
            publishYourself(managedTorrent);
        }
    }

    /**
     * Returns whether the peer has already published him/her self as someone
     * sharing the given torrent file
     * 
     * @param managedTorrent the torrent we are dealing with
     * @return whether the peer's information is already published or not
     */
    private boolean isPublished(ManagedTorrent managedTorrent) {
        return publishedTorrents.containsKey(managedTorrent);
    }

    /**
     * Adds a torrent the map to indicate the peer has published themself as
     * someone sharing the file. Also records the time it was published
     * 
     * @param managedTorrent the torrent we are dealing with
     */
    private void addAsPublished(ManagedTorrent managedTorrent, Time time) {
        LOG.debug("published");
        publishedTorrents.put(managedTorrent, time);
    }
}
