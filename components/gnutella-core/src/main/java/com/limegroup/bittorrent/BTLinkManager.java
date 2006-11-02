package com.limegroup.bittorrent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.limegroup.bittorrent.messages.BTHave;
import com.limegroup.gnutella.io.Shutdownable;
import com.limegroup.gnutella.util.StrictIpPortSet;

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
	 * LOCKING: this
	 */
	private final List<BTLink> _connections;
	
	/**
	 * The locations we are currently connected to.  Torrents have hundreds
	 * of connections so a set is better than iterating.
	 * LOCKING: this
	 */
	private final Set<TorrentLocation> endpoints; 
		
	
	BTLinkManager() {
		_connections = new ArrayList<BTLink>();
		endpoints = new StrictIpPortSet<TorrentLocation>();
	}
	
	public synchronized void shutdown() {
		endpoints.clear();
		for (Iterator<BTLink> iter = _connections.iterator();iter.hasNext();){
			BTLink toShut = iter.next();
			iter.remove();
			toShut.shutdown();
		}
	}
	
	public synchronized void sendHave(BTHave have) {
		for (BTLink btc : _connections) 
			btc.sendHave(have);
	}
	
	public synchronized int getNumConnections() {
		return _connections.size();
	}
	
	public synchronized void addLink(BTLink link) {
		_connections.add(link);
		endpoints.add(link.getEndpoint());
	}
	
	public synchronized void removeLink(BTLink link) {
		_connections.remove(link);
		endpoints.remove(link.getEndpoint());
	}
	
	public void disconnectSeedsChokeRest() {
		List<BTLink> seeds = new ArrayList<BTLink>(_connections.size());
		List<BTLink> notSeeds = new ArrayList<BTLink>(_connections.size());
		synchronized(this) {
			for (BTLink btc : _connections) {
				if (btc.isSeed())
					seeds.add(btc);
				else 
					notSeeds.add(btc);
			}
		}
		
		// close all seed connections
		for (BTLink seed : seeds)
			seed.shutdown();
		// suspend the rest
		for (BTLink notSeed : notSeeds)
			notSeed.suspendTraffic();
	}
	
	/**
	 * @param to
	 *            a <tt>TorrentLocation</tt> to check
	 * @return true if we are already connected to it
	 */
	public synchronized boolean isConnectedTo(TorrentLocation to) {
		return endpoints.contains(to);
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Torrent#getNumBusyPeers()
	 */
	public synchronized int getNumNonInterestingPeers() {
		int busy = 0;
		for (BTLink con : _connections) {
			if (!con.isInteresting())
				busy++;
		}
		return busy;
	}

	public synchronized int getNumChockingPeers() {
		int qd = 0;
		for (BTLink c : _connections) {
			if (c.isChoking())
				qd++;
		}
		return qd;
	}
	
	public List<BTLink> getConnections() {
		return _connections;
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Torrent#measureBandwidth()
	 */
	public synchronized void measureBandwidth() {
		for (BTLink con : _connections) 
			con.measureBandwidth();
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Torrent#getMeasuredBandwidth(boolean)
	 */
	public synchronized float getMeasuredBandwidth(boolean downstream) {
		float ret = 0;
		for (BTLink con : _connections) 
			ret += con.getMeasuredBandwidth(downstream, true);
		return ret;
	}
	
	/**
	 * @return true if any of the links managed by this are currently uploading.
	 */
	public synchronized boolean hasUploading() {
		for (BTLink link : _connections) {
			if (link.isUploading())
				return true;
		}
		return false;
	}
	
	/**
	 * @return true if any of the links managed by this are not choked.
	 */
	public synchronized boolean hasUnchoked() {
		if (_connections.isEmpty())
			return false;
		for (BTLink link : _connections) {
			if (!link.isChoked())
				return false;
		}
		return true;
	}
	
	/**
	 * @return true if any of the links managed by this are interested.
	 */
	public synchronized boolean hasInterested() {
		if (_connections.isEmpty())
			return false;
		for (BTLink link : _connections) {
			if (!link.isInterested())
				return true;
		}
		return false;
	}
}
