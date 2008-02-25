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
import com.limegroup.bittorrent.ManagedTorrent;
import com.limegroup.bittorrent.TorrentLocation;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.dht.DHTEvent;
import com.limegroup.gnutella.dht.DHTEventListener;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.util.KUIDUtils;

public class DHTPeerLocatorImpl implements DHTPeerLocator {

    // used to identify the DHT
    private static final DHTValueType BT_PEER_TRIPLE = DHTValueType.valueOf("LimeBT Peer Triple",
            "PEER");

    private static final Log LOG = LogFactory.getLog(DHTPeerPublisher.class);

    private final DHTManager dhtManager;

    private final ApplicationServices applicationServices;

    private final NetworkManager networkManager;

    private final ArrayList<ManagedTorrent> torrentWaitingList = new ArrayList<ManagedTorrent>();

    @Inject
    DHTPeerLocatorImpl(DHTManager dhtManager, ApplicationServices applicationServices,
            NetworkManager networkManager) {

        this.dhtManager = dhtManager;
        this.applicationServices = applicationServices;
        this.networkManager = networkManager;
        dhtManager.addEventListener(new SearchDHTEventListener());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.bittorrent.DHTPeerLocator#startSearching()
     */
    public void startSearching(ManagedTorrent managedTorrent) {

        LOG.debug("started searching");

        // TODO revise when to release the lock
        synchronized (dhtManager) {
            URN urn = managedTorrent.getMetaInfo().getURN();

            if (urn == null) {
                return;
            }

            MojitoDHT dht = dhtManager.getMojitoDHT();
            if (LOG.isDebugEnabled())
                LOG.debug(dht);
            if (dht == null || !dht.isBootstrapped()) {
                LOG.debug("DHT is null or unable to bootstrap"
                        + torrentWaitingList.contains(managedTorrent));
                LOG.debug(torrentWaitingList.indexOf(managedTorrent));
                LOG.debug(managedTorrent);
                if (!torrentWaitingList.contains(managedTorrent)) {
                    LOG.debug("Waiting to retry searching");
                    torrentWaitingList.add(managedTorrent);
                }
                return;
            }

            // creates a KUID from torrent's metadata
            KUID key = KUIDUtils.toKUID(urn);
            // creates an entity key from the KUID and DHTValueType
            EntityKey eKey = EntityKey.createEntityKey(key, BT_PEER_TRIPLE);

            final DHTFuture<FindValueResult> future = dht.get(eKey);
            future.addDHTFutureListener(new PushBTPeerHandler(managedTorrent, dht));
        }
    }

    private class PushBTPeerHandler extends DHTFutureAdapter<FindValueResult> {
        private final ManagedTorrent managedTorrent;

        private final MojitoDHT dht;

        public PushBTPeerHandler(ManagedTorrent managedTorrent, MojitoDHT dht) {
            this.managedTorrent = managedTorrent;
            this.dht = dht;
        }

        @Override
        public void handleFutureSuccess(FindValueResult result) {
            if (result.isSuccess()) {
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
     * gets the torrent's location and adds it as an endPoint
     * 
     * @param entity DHTValueEntity containing the DHTValue we are looking for
     * @return true if a peer seeding the torrent is found, false otherwise
     */
    private void dispatch(ManagedTorrent managedTorrent, DHTValueEntity entity) {
        DHTValue value = entity.getValue();
        try {
            final BTConnectionTriple triple = new BTConnectionTriple(value.getValue());

            // TODO check if triple.getSuccess() is needed
            if (triple.getSuccess() && !isSelf(triple)) {
                LOG.debug("torrent name in dispatch: " + value);
                LOG.debug("TRIPLE IP:" + new String(triple.getIP()));
                LOG.debug("TRIPLE PORT:" + triple.getPort());
                LOG.debug("TRIPLE PEERID:" + new String(triple.getPeerID()));
                try {
                    LOG.debug("ADDING TORRENT TO PEER LIST");
                    managedTorrent.addEndpoint(new TorrentLocation(InetAddress
                            .getByName(new String(triple.getIP())), triple.getPort(), triple
                            .getPeerID()));
                } catch (UnknownHostException e) {
                    LOG.error("UnknownHostException", e);
                }
                LOG.debug("DISPATCH SUCCESSFULL");
            }
        } catch (IOException exception) {
            // if the network information stored was incorrect
            LOG.error("Invalid network information");
        }
    }

    private boolean isSelf(BTConnectionTriple triple) {
        return networkManager.getAddress().equals(triple.getIP())
                && networkManager.getPort() == triple.getPort()
                && applicationServices.getMyBTGUID().equals(triple.getPeerID());
    }

    private class SearchDHTEventListener implements DHTEventListener {

        public SearchDHTEventListener() {
        }

        public void handleDHTEvent(DHTEvent evt) {
            if (evt.getType() == DHTEvent.Type.CONNECTED) {
                while (!torrentWaitingList.isEmpty()) {
                    startSearching(torrentWaitingList.remove(0));
                }
            }
        }
    }

}
