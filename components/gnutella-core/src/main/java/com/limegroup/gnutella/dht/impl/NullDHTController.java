package com.limegroup.gnutella.dht.impl;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;

import org.limewire.io.IpPort;
import org.limewire.mojito.MojitoDHT;

import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.dht.DHTController;

/**
 * A class that uses the Null Object pattern to avoid
 * repetitive null checks in DHTManagers
 */
class NullDHTController implements DHTController {

	public void addActiveDHTNode(SocketAddress hostAddress) {}

	public void addPassiveDHTNode(SocketAddress hostAddress) {}

	public List<IpPort> getActiveDHTNodes(int maxNodes) {
		return Collections.emptyList();
	}

	public MojitoDHT getMojitoDHT() {
		return null;
	}

	public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {}

	public boolean isActiveNode() {
		return false;
	}

	public boolean isRunning() {
		return false;
	}

	public boolean isWaitingForNodes() {
		return false;
	}
	
	public boolean isBootstrapped() {
		return false;
	}

	public void sendUpdatedCapabilities() {}

	public void start() {}

	public void stop() {}

}
