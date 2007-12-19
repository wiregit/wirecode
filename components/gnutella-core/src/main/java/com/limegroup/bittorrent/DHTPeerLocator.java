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
    
    public static final DHTValueType BT_PEER_TRIPLE = DHTValueType.valueOf("LimeBT Peer Triple", "LIME_BT_PEER");
    
    private static final Log LOG = LogFactory.getLog(DHTPeerLocator.class);
    

    private final DHTManager          manager;
    private final ApplicationServices applicationServices;
    private final NetworkManager      networkManager;
    
    private final BTMetaInfo torrentMeta;
    private final ManagedTorrent torrent;
    private final ScheduledExecutorService invoker;
    
    DHTPeerLocator(DHTManager manager, ApplicationServices applicationServices,
                    NetworkManager networkManager, ManagedTorrent torrent, 
                    BTMetaInfo torrentMeta) {

        this.manager             = manager;
        this.applicationServices = applicationServices;
        this.networkManager      = networkManager;
        
        this.torrent     = torrent;
        this.torrentMeta = torrentMeta;
        
        this.invoker     = torrent.getNetworkScheduledExecutorService();
    }
    
    private BTConnectionTriple getBTHost() {
        return new BTConnectionTriple(networkManager.getAddress(), 
                networkManager.getPort(),
                applicationServices.getMyBTGUID());
    }
    
    private boolean isSelf(BTConnectionTriple triple) {
        return     networkManager.getAddress().equals(triple.getIP())
                && networkManager.getPort() == triple.getPort()
                && applicationServices.getMyBTGUID().equals(triple.getPeerID());
    }
        
    public void publish() {
        synchronized (manager) {
            MojitoDHT dht = manager.getMojitoDHT();
        
            if (dht == null || !dht.isBootstrapped()) {
                return;
            }
            
            if (dht.isFirewalled()) {
                return;
            }
            
            byte[] msg = getBTHost().getEncoded();
            KUID key = KUID.createWithBytes(torrentMeta.getInfoHash());
        
            // TODO: possible retries?
            dht.put(key, new DHTValueImpl(BT_PEER_TRIPLE, Version.ZERO, msg));
        }
    }
    
    public Shutdownable startSearching() {
      
        KUID key = KUID.createWithBytes(torrentMeta.getInfoHash());
        EntityKey eKey = EntityKey.createEntityKey(key, BT_PEER_TRIPLE);
        
        
        synchronized (manager) {
            MojitoDHT dht = manager.getMojitoDHT();
            if (dht == null || !dht.isBootstrapped()) {
                return null;
            }
            
            final DHTFuture<FindValueResult> future = dht.get(eKey);
            future.addDHTFutureListener(new PushBTPeerHandler(dht));
            
            return new Shutdownable() {
                public void shutdown() {
                    future.cancel(true);
                }
            };
        }    
    }
    
    private boolean dispatch(DHTValueEntity entity) {
        
        DHTValue value = entity.getValue();
            
        final BTConnectionTriple triple = new BTConnectionTriple(value.getValue());
        
        if (triple.getSuccess() && !isSelf(triple)) {
            
            Runnable endpointAdder = new Runnable() {
                public void run() {
                    try {
                        torrent.addEndpoint(
                                new TorrentLocation(
                                        InetAddress.getByAddress(triple.getIP()),
                                        triple.getPort(), 
                                        triple.getPeerID()));
                    } catch (UnknownHostException e) {
                        LOG.error("UnknownHostException", e);
                    }

                }
            };
            
            invoker.execute(endpointAdder);
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
            
            torrent.notifyPeerLocatorComplete(success);
        }
        
    
        
        @Override
        public void handleCancellationException(CancellationException e) {
            LOG.error("CancellationException", e);  
            
            torrent.notifyPeerLocatorComplete(false);
        }

        @Override
        public void handleExecutionException(ExecutionException e) {
            LOG.error("ExecutionException", e);
            
            torrent.notifyPeerLocatorComplete(false);
        }

        @Override
        public void handleInterruptedException(InterruptedException e) {
            LOG.error("InterruptedException", e);
            
            torrent.notifyPeerLocatorComplete(false);
        }
        
    }
}
