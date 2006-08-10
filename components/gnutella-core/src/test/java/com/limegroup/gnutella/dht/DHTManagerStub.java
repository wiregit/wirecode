package com.limegroup.gnutella.dht;

import java.net.SocketAddress;
import java.util.List;

import com.limegroup.gnutella.LifecycleEvent;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.mojito.MojitoDHT;

public class DHTManagerStub extends Object implements DHTManager {
    public void addDHTNode(SocketAddress hostAddress) {}

    public void addressChanged() {}

    public List<IpPort> getActiveDHTNodes(int maxNodes) {return null;}

    public MojitoDHT getMojitoDHT() {return null;}

    public boolean isActiveNode() {return false;}

    public boolean isRunning() {return true;}

    public boolean isWaitingForNodes() {return false;}

    public void start(boolean activeMode) {}

    public void stop() {}

    public void switchMode(boolean toActiveMode) {}

    public void handleLifecycleEvent(LifecycleEvent evt) {}

    public int getVersion() {
        return 1;
    }
}
