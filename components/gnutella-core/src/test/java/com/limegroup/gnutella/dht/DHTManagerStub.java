package com.limegroup.gnutella.dht;

import java.net.SocketAddress;
import java.util.List;

import org.limewire.io.IpPort;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;

import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.messages.vendor.DHTContactsMessage;

public class DHTManagerStub implements DHTManager {
    
    public void addActiveDHTNode(SocketAddress hostAddress) {}
    
    public void addPassiveDHTNode(SocketAddress hostAddress) {}

    public void addressChanged() {}

    public List<IpPort> getActiveDHTNodes(int maxNodes) {
        return null;
    }

    public MojitoDHT getMojitoDHT() {
        return null;
    }

    public DHTMode getDHTMode() {
        return DHTMode.ACTIVE;
    }

    public boolean isRunning() {return true;}

    public boolean isWaitingForNodes() {return false;}
    
    public void addEventListener(DHTEventListener listener) {}

    public void dispatchEvent(DHTEvent event) {}

    public void removeEventListener(DHTEventListener listener) {}

    public void start(DHTMode mode) {}

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

    public void handleDHTContactsMessage(DHTContactsMessage msg) {
    }
}
