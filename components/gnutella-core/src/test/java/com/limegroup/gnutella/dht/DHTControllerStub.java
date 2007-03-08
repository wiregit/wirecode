package com.limegroup.gnutella.dht;

import java.net.SocketAddress;
import java.util.List;

import org.limewire.io.IpPort;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.routing.Contact;

import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;

public class DHTControllerStub implements DHTController {
    
    private MojitoDHT dht;

    public DHTControllerStub(MojitoDHT dht) {
        this.dht = dht;
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
        return dht;
    }

    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
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

	public boolean isBootstrapped() {
		return dht.isBootstrapped();
	}

    public void addContact(Contact node) {
    }

    public DHTMode getDHTMode() {
        return null;
    }
}
