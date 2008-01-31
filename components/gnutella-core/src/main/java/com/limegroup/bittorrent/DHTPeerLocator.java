package com.limegroup.bittorrent;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;

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
import org.limewire.mojito.db.impl.DHTValueImpl;
import org.limewire.mojito.result.FindValueResult;
import org.limewire.mojito.routing.Version;
import org.limewire.nio.observer.Shutdownable;

import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.dht.DHTManager;


public class DHTPeerLocator implements PeerLocator {
    
    //used to identify the DHT
    public static final DHTValueType BT_PEER_TRIPLE = DHTValueType.valueOf("LimeBT Peer Triple", "PEER");
    
    private static final Log LOG = LogFactory.getLog(DHTPeerLocator.class);
    
    private final DHTManager          MANAGER;
    private final ApplicationServices APPLICATION_SERVICES; //not sure why needed
    private final NetworkManager      NETWORK_MANAGER; //not sure why needed
    
    private final BTMetaInfo TORRENT_META; //not sure why needed
    private final ManagedTorrent TORRENT; //not sure why needed
    private final ScheduledExecutorService INVOKER;
    
    DHTPeerLocator(DHTManager manager, ApplicationServices applicationServices,
                    NetworkManager networkManager, ManagedTorrent torrent, 
                    BTMetaInfo torrentMeta) {

        this.MANAGER             = manager;
        this.APPLICATION_SERVICES = applicationServices;
        this.NETWORK_MANAGER      = networkManager;
        
        this.TORRENT     = torrent;
        this.TORRENT_META = torrentMeta;
        
        this.INVOKER     = torrent.getNetworkScheduledExecutorService();
        
     //   this.mojitoDHT = MojitoFactory.createDHT("Torrent Tracker");
    }
    
    /*private BTConnectionTriple getBTHost(TorrentLocation torLoc) {
        return new BTConnectionTriple(NETWORK_MANAGER.getAddress(), 
                NETWORK_MANAGER.getPort(),
                APPLICATION_SERVICES.getMyBTGUID());
    }*/
    
    private BTConnectionTriple getBTHost(TorrentLocation torLoc) {
        LOG.info("torrent name in publish: " + TORRENT_META.getName());
        LOG.info("IP being registered: "+torLoc.getAddress());
        return new BTConnectionTriple(torLoc.getAddress().getBytes(), 
                torLoc.getPort(),
                torLoc.getPeerID());
    }
    
    private boolean isSelf(BTConnectionTriple triple) {        
        return     compare(NETWORK_MANAGER.getAddress(), triple.getIP())
                && NETWORK_MANAGER.getPort() == triple.getPort()
                && compare(APPLICATION_SERVICES.getMyBTGUID(), triple.getPeerID());
    }
        
    public void publish(TorrentLocation torLoc) {
        //TODO remove all system.out
        LOG.debug("hello");
        
        synchronized (MANAGER) {
            try {
            Thread.sleep(10000);
            } catch (InterruptedException ie) {}
            MojitoDHT mojitoDHT = MANAGER.getMojitoDHT();
        
            LOG.debug(mojitoDHT);
            if(mojitoDHT!=null)
                LOG.debug(!mojitoDHT.isBootstrapped());
           
            if (mojitoDHT == null || !mojitoDHT.isBootstrapped()) {
                return;
            }
        
            LOG.debug("check 1");
            
            if (mojitoDHT.isFirewalled()) {
               //TODO add myself to the DHT
               // return;
            }
        
            LOG.debug("check 2");
            
            //encodes ip, port and peerid into a array of bytes 
            byte[] msg = getBTHost(torLoc).getEncoded();
            //gets KUID for the torrent info
            KUID key = KUID.createWithBytes(TORRENT_META.getInfoHash());
        
           // System.out.println(msg);
            
            // TODO: possible retries?
            mojitoDHT.put(key, new DHTValueImpl(BT_PEER_TRIPLE, Version.ZERO, msg));
        }
    }
    
    public Shutdownable startSearching() {
        //gets KUID for the torrent info
        KUID key = KUID.createWithBytes(TORRENT_META.getInfoHash());
        //get exact location of DHT
        EntityKey eKey = EntityKey.createEntityKey(key, BT_PEER_TRIPLE);
        
        LOG.debug("key: " + key);
        LOG.debug("ekey: " + eKey);
        synchronized (MANAGER) {
            MojitoDHT dht = MANAGER.getMojitoDHT();
            //TODO REMOVE
            LOG.debug(dht);
            if (dht == null || !dht.isBootstrapped()) {
                LOG.debug("NOT BOOTSTRAPPED");
                return null;
            }
            
            LOG.debug("dht: " + dht.get(eKey));
            final DHTFuture<FindValueResult> future = dht.get(eKey);
            future.addDHTFutureListener(new PushBTPeerHandler(dht));
            LOG.debug("future:" + future);
            return new Shutdownable() {
                public void shutdown() {
                    future.cancel(true);
                }
            };
        }    
    }
    
    //TODO modify comments
    /**gets the torrent's location and add it to the lookup list
     * 
     * @param entity DHTValueEntity containing the DHTValue we are looking for
     * @return true if a peer seeding the torrent is found, false otherwise
     */ 
    private boolean dispatch(DHTValueEntity entity) {
        LOG.debug(entity.toString()); // REMOVE
        
        DHTValue value = entity.getValue();
            
        final BTConnectionTriple triple = new BTConnectionTriple(value.getValue());
        
        if (triple.getSuccess() && !isSelf(triple)) {
      //  if (triple.getSuccess()) {
            LOG.info("torrent name in dispatch: " + TORRENT_META.getName());
            LOG.info("TRIPLE IP:" + new String(triple.getIP())); // REMOVE
            LOG.info("TRIPLE PORT:" + triple.getPort()); // REMOVE
            LOG.info("TRIPLE PEERID:" + new String(triple.getPeerID())); // REMOVE
            
            Runnable endpointAdder = new Runnable() {
                public void run() {
                    try {
                        TORRENT.addEndpoint(
                                new TorrentLocation(
                                        InetAddress.getByName(new String(triple.getIP())), 
                                        triple.getPort(),triple.getPeerID()));
                    } catch (UnknownHostException e) {
                        LOG.error("UnknownHostException", e);
                    }
                }
            };
            
            INVOKER.execute(endpointAdder);
            return true;
        }
        
        return false;
    }
        

    private class PushBTPeerHandler extends DHTFutureAdapter<FindValueResult> {
        private final MojitoDHT dht;
        
        public PushBTPeerHandler(MojitoDHT dht) {
            this.dht = dht;
        }
        
        @Override
        public void handleFutureSuccess(FindValueResult result) {
            boolean success = false;
            
            if (result.isSuccess()) {
                for (DHTValueEntity entity : result.getEntities()) {
                    LOG.debug("entered if clause");
                    if (dispatch(entity)) {
                        success = true;
                    }
                    
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

                                if (dispatch(entity)) {
                                    success = true;
                                }
                                
                            }
                        }
                    } catch (ExecutionException e) {
                        LOG.error("ExecutionException", e);
                    } catch (InterruptedException e) {
                        LOG.error("InterruptedException", e);
                    } 
                }
            }
            LOG.info("success: " + success);
            TORRENT.notifyPeerLocatorComplete(success);
        }
        
    
        
        @Override
        public void handleCancellationException(CancellationException e) {
            LOG.error("CancellationException", e);  
            
            TORRENT.notifyPeerLocatorComplete(false);
        }

        @Override
        public void handleExecutionException(ExecutionException e) {
            LOG.error("ExecutionException", e);
            
            TORRENT.notifyPeerLocatorComplete(false);
        }

        @Override
        public void handleInterruptedException(InterruptedException e) {
            LOG.error("InterruptedException", e);
            
            TORRENT.notifyPeerLocatorComplete(false);
        }
        
    }
    
    private static boolean compare(byte[] a, byte[] b) {        
        if (a.length != b.length)  return false;
        
        //TODO remove the check i<b.length
        for ( int i=0 ; i<a.length && i<b.length ; i++ )
            if (a[i] != b[i])  return false;
        
        return true;
        
    }
}
