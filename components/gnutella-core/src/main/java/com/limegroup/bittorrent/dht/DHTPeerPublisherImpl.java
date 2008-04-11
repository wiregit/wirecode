package com.limegroup.bittorrent.dht;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.NetworkUtils;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureAdapter;
import org.limewire.mojito.db.impl.DHTValueImpl;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.routing.Version;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.bittorrent.ManagedTorrent;
import com.limegroup.bittorrent.TorrentEvent;
import com.limegroup.bittorrent.TorrentEventListener;
import com.limegroup.bittorrent.TorrentLocation;
import com.limegroup.bittorrent.TorrentManager;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.dht.DHTEvent;
import com.limegroup.gnutella.dht.DHTEventListener;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.util.KUIDUtils;

/**
 * Given an instance of torrent, stores the peer in DHT as someone seeding that
 * file. Also re-attempts to publish the peer if DHT was not available.
 */
@Singleton
public class DHTPeerPublisherImpl implements DHTPeerPublisher {

    private static final Log LOG = LogFactory.getLog(DHTPeerPublisher.class);

    private final Provider<DHTManager> dhtManager;

    private final Provider<ApplicationServices> applicationServices;

    private final Provider<NetworkManager> networkManager;

    private final Provider<TorrentManager> torrentManager;

    /**
     * List of published torrents mapped to the time it was published.
     */
    private final Map<URN, Long> publishedTorrents = new HashMap<URN, Long>();

    /**
     * List of torrents a peer is sharing but not published yet since DHT was
     * unavailable.
     */
    private final List<URN> torrentsWaitingForDHTList = new ArrayList<URN>();

    @Inject
    DHTPeerPublisherImpl(Provider<DHTManager> dhtManager,
            Provider<ApplicationServices> applicationServices,
            Provider<NetworkManager> networkManager, Provider<TorrentManager> torrentManager) {

        this.dhtManager = dhtManager;
        this.applicationServices = applicationServices;
        this.networkManager = networkManager;
        this.torrentManager = torrentManager;
    }

    /**
     * Adds a <code>TorrentEventListener</code> to <code>TorrentManager</code>
     * and a DHTEventListener to DHTManager.This method should only be called
     * once.
     */
    public void init() {
        // listens for the TorrentEvent FIRST_CHUNK_VERIFIED to start publishing
        torrentManager.get().addEventListener(new TorrentEventListenerForPublisher());
        // listens for the DHTEvent CONNECTED to re-attempt publishing
        dhtManager.get().addEventListener(new DHTEventListenerForPublisher());
    }

    /**
     * Publishes the local host in DHT as a peer sharing the given torrent .
     * Torrent's infoHash is used as the key.
     * 
     * @param managedTorrent a managedTorrent instance of the torrent.
     */
    public void publishYourself(ManagedTorrent managedTorrent) {
        LOG.debug("In publishYourself");

        URN urn = managedTorrent.getMetaInfo().getURN();

        if (managedTorrent.isActive() && networkManager.get().acceptedIncomingConnection()
                && !isPublished(urn)) {
            // holding a lock on torrentsWaitingForDHTList here so that we do
            // publish for a same torrent twice.
            synchronized (torrentsWaitingForDHTList) {
                // initial check
                if (!torrentsWaitingForDHTList.contains(urn)) {

                    LOG.debug("Passed Initial checks");
                    // holding a lock on DHT to ensure dht does not change
                    // status
                    // after we acquired it
                    DHTManager manager = dhtManager.get();
                    synchronized (manager) {
                        MojitoDHT mojitoDHT = manager.getMojitoDHT();

                        if (LOG.isDebugEnabled())
                            LOG.debug("DHT: " + mojitoDHT);

                        if (mojitoDHT == null || !mojitoDHT.isBootstrapped()) {
                            LOG.debug("DHT is null or unable to bootstrap");
                            torrentsWaitingForDHTList.add(urn);
                            return;
                        }
                        proceedPublishing(urn, mojitoDHT);
                    }
                }
            }
        }
    }

    /**
     * Publishes the local host's network information in the given DHT as a peer
     * seeding the torrent file specified by infoHash.
     * 
     * @param infoHash hashed data of the torrent file.
     * @param mojitoDHT an instance of the MojitoDHT.
     */
    private void proceedPublishing(URN urn, MojitoDHT mojitoDHT) {
        // checking if the torrent is active, getTorrentForURN returns null if
        // there is no active managedTorrent for the given urn.
        if (torrentManager.get().getTorrentForURN(urn) != null) {
            try {
                TorrentLocation torLoc = new TorrentLocation(InetAddress.getByName(NetworkUtils
                        .ip2string((networkManager.get().getAddress()))), networkManager.get()
                        .getPort(), applicationServices.get().getMyBTGUID());

                // encodes ip, port and peerid into a array of bytes
                byte[] msg = DHTPeerLocatorUtils.encode(torLoc);
                // creates a KUID from torrent's metadata
                KUID key = KUIDUtils.toKUID(urn);

                if (msg.length != 0) {
                    // publish the information in the DHT
                    DHTFuture<StoreResult> future = mojitoDHT.put(key, new DHTValueImpl(
                            DHTPeerLocatorUtils.BT_PEER_TRIPLE, Version.ZERO, msg));
                    // ensure publishing was successful
                    future.addDHTFutureListener(new PublishYourselfResultHandler(urn));
                    if (LOG.isDebugEnabled())
                        LOG.debug("IP:PORT of peer published: " + torLoc.getAddress() + ":"
                                + torLoc.getPort());
                }
            } catch (IllegalArgumentException iae) {
                LOG.error("Invalid network information", iae);
            } catch (UnknownHostException uhe) {
                LOG.error("Invalid IP address");
            }
        }
    }

    /**
     * Returns whether the peer has already published him/her self as someone
     * sharing the given torrent file.
     * 
     * @param managedTorrent The torrent we are dealing with.
     * @return whether the peer's information is already published or not.
     */
    private boolean isPublished(URN urn) {
        return publishedTorrents.containsKey(urn);
    }

    /**
     * Adds a torrent to the map to indicate the peer has published itself as
     * someone sharing the file. Also records the time it was published.
     * 
     * @param managedTorrent The torrent we are dealing with.
     */
    private void addAsPublished(URN urn, Long time) {
        publishedTorrents.put(urn, time);
        LOG.debug("published");
    }

    /**
     * Handles the <code>TorrentEvent FIRST_CHUNK_VERIFIED</code>. Upon this
     * event generation, it will try to publish the local peer as someone
     * seeding given torrent file.
     * 
     */
    private class TorrentEventListenerForPublisher implements TorrentEventListener {
        public void handleTorrentEvent(TorrentEvent evt) {
            if (evt.getType() == TorrentEvent.Type.FIRST_CHUNK_VERIFIED) {
                LOG.debug("FIRST_CHUNK_VERIFIED_EVENT");
                publishYourself(evt.getTorrent());
            }
        }
    }

    /**
     * Implements the DHTEventListener to perform future attempts at publishing
     * a peer based on DHTEvent.
     */
    private class DHTEventListenerForPublisher implements DHTEventListener {

        /**
         * This method is invoked when a DHTEvent occurs and if DHT is connected
         * then it tries to publish the peer again.
         */
        public void handleDHTEvent(DHTEvent evt) {            
            List<URN> torrentsWaitingForDHTListCopy;
            if (evt.getType() == DHTEvent.Type.CONNECTED) {
                DHTManager manager = dhtManager.get();
                MojitoDHT dht;
                synchronized (dhtManager) {
                    dht = manager.getMojitoDHT();
                    if (dht == null || !dht.isBootstrapped()) {
                        LOG.error("Incorrect DHTEvent generated");
                        return;
                    }
                }
                synchronized (torrentsWaitingForDHTList) {
                    torrentsWaitingForDHTListCopy = torrentsWaitingForDHTList;
                    torrentsWaitingForDHTList.clear();
                }
                // copying the array then iterating since reading is cheaper
                // than removing
                for (int i = 0; i < torrentsWaitingForDHTListCopy.size(); i++) {                    
                    proceedPublishing(torrentsWaitingForDHTListCopy.get(i), dht);
                }
            }
        }
    }

    /**
     * This class listens to see if the peer was published successfully. If
     * publishing was unsuccessful, then ....
     */
    private class PublishYourselfResultHandler extends DHTFutureAdapter<StoreResult> {

        private final URN urn;

        public PublishYourselfResultHandler(URN urn) {
            this.urn = urn;
        }

        @Override
        public void handleFutureSuccess(StoreResult result) {
            addAsPublished(urn, new Long(System.currentTimeMillis()));
        }
    }
}
