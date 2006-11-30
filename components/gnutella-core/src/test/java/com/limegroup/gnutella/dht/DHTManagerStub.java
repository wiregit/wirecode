package com.limegroup.gnutella.dht;

import java.net.SocketAddress;
import java.util.List;

import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.mojito.MojitoDHT;

public class DHTManagerStub implements DHTManager {
    
    public void addActiveDHTNode(SocketAddress hostAddress) {}
    
    public void addPassiveDHTNode(SocketAddress hostAddress) {}

    public void addressChanged() {}

    public List<IpPort> getActiveDHTNodes(int maxNodes) {return null;}

    public MojitoDHT getMojitoDHT() {return null;}

    public boolean isActiveNode() {return true;}

    public boolean isRunning() {return true;}

    public boolean isWaitingForNodes() {return false;}

    public void start(boolean activeMode) {}

    public void stop() {}

    public boolean isBootstrapped() {
        return true;
    }

    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {}

    public int getVendor() {
        return 1;
    }
    
    public int getVersion() {
        return 1;
    }
}
