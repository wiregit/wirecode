package com.limegroup.gnutella.dht;

import java.net.SocketAddress;
import java.util.List;

import com.limegroup.gnutella.LifecycleEvent;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.mojito.MojitoDHT;

public class DHTControllerStub implements DHTController {
    
    private byte[] localNodeId;
    
    public DHTControllerStub() {};
    
    public DHTControllerStub(byte[] localNodeId) {
        this.localNodeId = localNodeId;
    }

    public void addActiveDHTNode(SocketAddress hostAddress) {
    }
    
    public void addPassiveDHTNode(SocketAddress hostAddress) {
    }

    public List<IpPort> getActiveDHTNodes(int maxNodes) {
        return null;
    }

    public int getDHTVersion() {
        return 0;
    }

    public MojitoDHT getMojitoDHT() {
        return null;
    }

    public void handleConnectionLifecycleEvent(LifecycleEvent evt) {
    }

    public void init() {
    }

    public boolean isActiveNode() {
        return false;
    }

    public boolean isRunning() {
        return false;
    }

    public boolean isWaitingForNodes() {
        return false;
    }

    public void sendUpdatedCapabilities() {
    }

    public void start() {
    }

    public void stop() {
    }

}
