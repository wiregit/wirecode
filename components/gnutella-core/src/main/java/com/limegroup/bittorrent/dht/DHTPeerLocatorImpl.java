package com.limegroup.bittorrent.dht;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureAdapter;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.result.FindValueResult;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.bittorrent.ManagedTorrent;
import com.limegroup.bittorrent.TorrentLocation;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.dht.DHTEvent;
import com.limegroup.gnutella.dht.DHTEventListener;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.util.KUIDUtils;

@Singleton
public class DHTPeerLocatorImpl implements DHTPeerLocator {

    /**
     * Value type associated with DHT lookup for a peer seeding a torrent.
     */
    private static final DHTValueType BT_PEER_TRIPLE = DHTValueType.valueOf("LimeBT Peer Triple",
            "PEER");

    private static final Log LOG = LogFactory.getLog(DHTPeerLocator.class);

    private final DHTManager dhtManager;

    private final ApplicationServices applicationServices;

    private final NetworkManager networkManager;

    /**
     * List of torrents for which a peer is trying to locate a seeder in DHT;
     * but unable since DHT is unavailable.
     */
    private final ArrayList<ManagedTorrent> torrentsWaitingForDHTList = new ArrayList<ManagedTorrent>();


    @Inject
    DHTPeerLocatorImpl(DHTManager dhtManager, ApplicationServices applicationServices,
            NetworkManager networkManager) {
        this.dhtManager = dhtManager;
        this.applicationServices = applicationServices;
        this.networkManager = networkManager;
    }

    public void init() {
        dhtManager.addEventListener(new dhtEventListenerForLocator());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.bittorrent.DHTPeerLocator#startSearching()
     */
    public void locatePeer(ManagedTorrent managedTorrent) {
        LOG.debug("In locatePeer");
        if (managedTorrent.isActive() && !torrentsWaitingForDHTList.contains(managedTorrent)) {
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
                    LOG.debug("DHT:" + mojitoDHT);
                if (mojitoDHT == null || !mojitoDHT.isBootstrapped()) {
                    LOG.debug("DHT is null or unable to bootstrap");
                    torrentsWaitingForDHTList.add(managedTorrent);
                    return;
                }

                // creates a KUID from torrent's metadata
                KUID key = KUIDUtils.toKUID(urn);
                // creates an entity key from the KUID and DHTValueType
                EntityKey eKey = EntityKey.createEntityKey(key, BT_PEER_TRIPLE);

                // retrieves the future
                final DHTFuture<FindValueResult> future = mojitoDHT.get(eKey);
                // handle the result retrieved in the future
                future.addDHTFutureListener(new PushBTPeerHandler(managedTorrent, mojitoDHT));                
            }
        }
    }

    /**
     * Implements the DHTEventListener to perform future attempts at locating a
     * peer based on DHTEvent.
     */
    private class dhtEventListenerForLocator implements DHTEventListener {
        /**
         * This method is invoked when a DHTEvent occurs and if DHT is connected
         * then it tries to locate a peer again.
         */
        public void handleDHTEvent(DHTEvent evt) {
            LOG.debug("In handle event");
            if (evt.getType() == DHTEvent.Type.CONNECTED) {
                while (!torrentsWaitingForDHTList.isEmpty()) {
                    locatePeer(torrentsWaitingForDHTList.remove(0));
                }
            }
        }
    }

    /**
     * This class listens to see a result was successfully retrieved.
     */
    private class PushBTPeerHandler extends DHTFutureAdapter<FindValueResult> {
        private final ManagedTorrent managedTorrent;

        private final MojitoDHT dht;

        public PushBTPeerHandler(ManagedTorrent managedTorrent, MojitoDHT dht) {
            this.managedTorrent = managedTorrent;
            this.dht = dht;
        }

        @Override
        public void handleFutureSuccess(FindValueResult result) {
            LOG.debug("handle result");
            if (result.isSuccess()) {
                LOG.debug("successful result");
                for (DHTValueEntity entity : result.getEntities()) {
                    dispatch(managedTorrent, entity);
                }

                for (EntityKey entityKey : result.getEntityKeys()) {
                    if (!entityKey.getDHTValueType().equals(BT_PEER_TRIPLE)) {
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
            final BTConnectionTriple triple = new BTConnectionTriple(value.getValue());

            if (!isSelf(triple)) {
                try {
                    LOG.debug("Adding torrent location as end point");
                    managedTorrent.addEndpoint(new TorrentLocation(InetAddress
                            .getByName(new String(triple.getIP())), triple.getPort(), triple
                            .getPeerID()));
                } catch (UnknownHostException e) {
                    LOG.error("UnknownHostException", e);
                }
                LOG.debug("Successful dispatch");
            }
        } catch (IOException exception) {
            // if the network information stored was incorrect
            LOG.error("Invalid network information");
        }
    }

    /**
     * Verifies if the network information found in the DHT is not about the
     * local host.
     * 
     * @param triple BTConnectionTriple instance of a peer found in the DHT.
     * @return Returns whether the location found is the same as local host or
     *         not
     */
    private boolean isSelf(BTConnectionTriple triple) {
        return networkManager.getAddress().equals(triple.getIP())
                && networkManager.getPort() == triple.getPort()
                && applicationServices.getMyBTGUID().equals(triple.getPeerID());
    }
}