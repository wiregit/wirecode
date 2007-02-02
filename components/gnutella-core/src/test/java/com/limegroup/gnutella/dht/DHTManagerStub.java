package com.limegroup.gnutella.dht;

import java.net.SocketAddress;
import java.util.List;
import java.util.Set;

import org.limewire.io.IpPort;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.result.FindValueResult;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;

public class DHTManagerStub implements DHTManager {
    
    public void addActiveDHTNode(SocketAddress hostAddress) {}
    
    public void addPassiveDHTNode(SocketAddress hostAddress) {}

    public void addressChanged() {}

    public List<IpPort> getActiveDHTNodes(int maxNodes) {return null;}

    public MojitoDHT getMojitoDHT() {return null;}

    public boolean isActiveNode() {return true;}

    public boolean isRunning() {return true;}

    public boolean isWaitingForNodes() {return false;}
    
    public void addEventListener(DHTEventListener listener) {}

    public void dispatchEvent(DHTEvent event) {}

    public void removeEventListener(DHTEventListener listener) {}

    public void start(boolean activeMode) {}

    public void stop() {}

    public boolean isBootstrapped() {
        return true;
    }

    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {}

    public Vendor getVendor() {
        return Vendor.UNKNOWN;
    }
    
    public Version getVersion() {
        return Version.UNKNOWN;
    }

    public DHTFuture<FindValueResult> getAltLocs(URN urn) {
        return null;
    }

    public DHTFuture<FindValueResult> getPushProxies(GUID guid) {
        return null;
    }

    public DHTFuture<StoreResult> putAltLoc(FileDesc fd) {
        return null;
    }
    
    public DHTFuture<StoreResult> putPushProxy(GUID guid, Set<? extends IpPort> proxies) {
        return null;
    }
}
