package com.limegroup.bittorrent;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.db.impl.DHTValueImpl;
import org.limewire.mojito.result.FindValueResult;
import org.limewire.mojito.routing.Version;

import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.dht.DHTManager;

public class DHTPeerLocatorImpl implements DHTPeerLocator {

    // used to identify the DHT
    private static final DHTValueType BT_PEER_TRIPLE = DHTValueType.valueOf("LimeBT Peer Triple",
            "PEER");

    private static final Log LOG = LogFactory.getLog(DHTPeerLocator.class);

    private final DHTManager dhtManager;

    private final ApplicationServices applicationServices;

    private final NetworkManager networkManager;

    private final BTMetaInfo torrentMeta;

    private final ManagedTorrent torrent;

    private final ScheduledExecutorService invoker;

    private volatile ScheduledFuture<?> scheduledPublish;

    private static final int MIN_WAITING_TIME = 1000;

    private volatile boolean published;

    DHTPeerLocatorImpl(DHTManager dhtManager, ApplicationServices applicationServices,
            NetworkManager networkManager, ManagedTorrent torrent, BTMetaInfo torrentMeta) {

        this.dhtManager = dhtManager;
        this.applicationServices = applicationServices;
        this.networkManager = networkManager;

        this.torrent = torrent;
        this.torrentMeta = torrentMeta;

        this.invoker = torrent.getNetworkScheduledExecutorService();

        this.published = false;
    }

    public void publishYourSelf() {
        LOG.debug("TRYING TO PUBLISH" + torrent.isDownloading());
        // if (networkManager.acceptedIncomingConnection()) {
        if (torrent.isDownloading() && !published) {
            LOG.debug("GOOD TO PUBLISH");
            publish();
        } else {
            reschedulePublishYourSelf();
        }
        // }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.bittorrent.DHTPeerLocator#startSearching()
     */
    public void startSearching() {
        // creates a KUID from torrent's metadata
        KUID key = KUID.createWithBytes(torrentMeta.getInfoHash());
        // creates an entity key from the KUID and DHTValueType
        final EntityKey eKey = EntityKey.createEntityKey(key, BT_PEER_TRIPLE);

        LOG.debug("started searching");
        synchronized (dhtManager) {
            final MojitoDHT dht = dhtManager.getMojitoDHT();

            LOG.debug(dht);
            if (dht == null || !dht.isBootstrapped()) {
                LOG.debug("DHT is null or unable to bootstrap");
                return;
            }

            Runnable processSearchResult = new Runnable() {
                public void run() {
                    final DHTFuture<FindValueResult> future = dht.get(eKey);
                    try {
                        for (DHTValueEntity entity : future.get().getEntities()) {
                            dispatch(entity);
                        }
                    } catch (ExecutionException e) {
                        LOG.error("ExecutionException", e);
                    } catch (InterruptedException e) {
                        LOG.error("InterruptedException", e);
                    }
                }
            };
            invoker.execute(processSearchResult);
        }
    }

    /**
     * Tries to publish the local host on DHT
     */
    private void reschedulePublishYourSelf() {
        final Runnable publishYourSelf = new Runnable() {
            public void run() {
                publishYourSelf();
            }
        };
        LOG.debug("scheduling new publish yourself");
        if (scheduledPublish != null)
            scheduledPublish.cancel(true);
        scheduledPublish = invoker.schedule(publishYourSelf, MIN_WAITING_TIME,
                TimeUnit.MILLISECONDS);
    }

    /**
     * Tries to publish the local host on DHT
     */
    private void reschedulePublish() {
        final Runnable publish = new Runnable() {
            public void run() {
                publish();
            }
        };
        LOG.debug("scheduling new publish");
        if (scheduledPublish != null)
            scheduledPublish.cancel(true);
        scheduledPublish = invoker.schedule(publish, MIN_WAITING_TIME,
                TimeUnit.MILLISECONDS);
    }

    /**
     * creates a BTConnectionTriple object for the local node
     * 
     * @return returns a BTConnectionTriple object of the local node
     */
    private BTConnectionTriple getBTHost() {
        LOG.debug("torrent name in publish: " + torrentMeta.getName());
        LOG.debug("IP being registered: " + new String(networkManager.getAddress()) + "PORT = "
                + networkManager.getPort());
        return new BTConnectionTriple(networkManager.getAddress(), networkManager.getPort(),
                applicationServices.getMyBTGUID());
    }

    private boolean isSelf(BTConnectionTriple triple) {
        return compare(networkManager.getAddress(), triple.getIP())
                && networkManager.getPort() == triple.getPort()
                && compare(applicationServices.getMyBTGUID(), triple.getPeerID());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.bittorrent.DHTPeerLocator#publish(com.limegroup.bittorrent.TorrentLocation)
     */
    private void publish() {
        synchronized (dhtManager) {
            MojitoDHT mojitoDHT = dhtManager.getMojitoDHT();

            LOG.debug(mojitoDHT);

            if (mojitoDHT == null || !mojitoDHT.isBootstrapped()) {
                reschedulePublish();
            } else {
                LOG.debug("check 1");

                // encodes ip, port and peerid into a array of bytes
                byte[] msg = getBTHost().getEncoded();
                // creates a KUID from torrent's metadata
                KUID key = KUID.createWithBytes(torrentMeta.getInfoHash());

                if (msg != null) {
                    // stores the information in the DHT
                    mojitoDHT.put(key, new DHTValueImpl(BT_PEER_TRIPLE, Version.ZERO, msg));
                    LOG.debug("PUBLISHED");
                    published = true;
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
    private void dispatch(DHTValueEntity entity) {
        LOG.debug(entity.toString()); // REMOVE

        DHTValue value = entity.getValue();

        final BTConnectionTriple triple = new BTConnectionTriple(value.getValue());

        if (triple.getSuccess() && !isSelf(triple)) {
            LOG.debug("torrent name in dispatch: " + value);
            LOG.debug("TRIPLE IP:" + new String(triple.getIP()));
            LOG.debug("TRIPLE PORT:" + triple.getPort());
            LOG.debug("TRIPLE PEERID:" + new String(triple.getPeerID()));
            try {
                LOG.debug("ADDING TORRENT TO PEER LIST");
                torrent.addEndpoint(new TorrentLocation(InetAddress.getByName(new String(triple
                        .getIP())), triple.getPort(), triple.getPeerID()));
            } catch (UnknownHostException e) {
                LOG.error("UnknownHostException", e);
            }
            LOG.info("DISPATCH SUCCESSFULL");
        }
    }

    private static boolean compare(byte[] a, byte[] b) {
        if (a.length != b.length)
            return false;

        for (int i = 0; i < a.length; i++)
            if (a[i] != b[i])
                return false;

        return true;

    }
}
