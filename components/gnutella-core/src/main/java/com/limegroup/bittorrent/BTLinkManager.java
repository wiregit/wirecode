package com.limegroup.bittorrent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.limegroup.bittorrent.messages.BTHave;
import com.limegroup.gnutella.io.Shutdownable;
import com.limegroup.gnutella.util.IpPort;

class BTLinkManagerFactory {
	private static BTLinkManagerFactory instance;
	public static BTLinkManagerFactory instance() {
		if (instance == null)
			instance = new BTLinkManagerFactory();
		return instance;
	}
	protected BTLinkManagerFactory(){}
	
	public BTLinkManager getLinkManager() {
		return new BTLinkManager();
	}
}

class BTLinkManager implements Shutdownable {
	/**
	 * The list of BTConnections that this torrent has.
	 * LOCKING: the list is synchronized on itself; it is modified
	 * only from the NIODispatcher thread, so no locking is required
	 * when iterating on that thread.
	 */
	private final List<BTLink> _connections;
	
	BTLinkManager() {
		_connections = Collections.synchronizedList(new ArrayList<BTLink>());
	}
	
	public void shutdown() {
		List<BTLink> copy = new ArrayList<BTLink>(_connections);
		for(BTLink con : copy) 
			con.shutdown(); 
	}
	
	public void sendHave(BTHave have) {
		for (BTLink btc : _connections) 
			btc.sendHave(have);
	}
	
	public int getNumConnections() {
		return _connections.size();
	}
	
	public void addLink(BTLink link) {
		_connections.add(link);
	}
	
	public void removeLink(BTLink link) {
		_connections.remove(link);
	}
	
	public void disconnectSeedsChokeRest() {
		List<BTLink> seeds = new ArrayList<BTLink>(_connections.size());
		for (BTLink btc : _connections) {
			if (btc.isSeed())
				seeds.add(btc);
			else 
				btc.suspendTraffic();
		}
		
		// close all seed connections
		for (BTLink seed : seeds)
			seed.shutdown();
	}
	
	/**
	 * @param to
	 *            a <tt>TorrentLocation</tt> to check
	 * @return true if we are already connected to it
	 */
	public boolean isConnectedTo(TorrentLocation to) {
		synchronized(_connections) {
			for (BTLink btc : _connections) {
				IpPort addr = btc.getEndpoint(); 
				if (IpPort.COMPARATOR.compare(addr, to) == 0)
					return true;
			}
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Torrent#getNumBusyPeers()
	 */
	public  int getNumNonInterestingPeers() {
		int busy = 0;
		synchronized(_connections) {
			for (BTLink con : _connections) {
				if (!con.isInteresting())
					busy++;
			}
		}
		return busy;
	}

	public int getNumChockingPeers() {
		int qd = 0;
		synchronized(_connections) {
			for (BTLink c : _connections) {
				if (c.isChoking())
					qd++;
			}
		}
		return qd;
	}
	
	public List<BTLink> getConnections() {
		return _connections;
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Torrent#measureBandwidth()
	 */
	public void measureBandwidth() {
		synchronized(_connections) {
			for (BTLink con : _connections) 
				con.measureBandwidth();
		}
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Torrent#getMeasuredBandwidth(boolean)
	 */
	public float getMeasuredBandwidth(boolean downstream) {
		float ret = 0;
		synchronized(_connections) {
			for (BTLink con : _connections) 
				ret += con.getMeasuredBandwidth(downstream, true);
		}
		return ret;
	}
}
