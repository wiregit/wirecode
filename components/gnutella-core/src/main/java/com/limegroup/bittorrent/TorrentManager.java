package com.limegroup.bittorrent;

import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.io.NIOSocket;
import com.limegroup.gnutella.*;
import com.limegroup.bittorrent.handshaking.IncomingBTHandshaker;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.bittorrent.ManagedTorrent;

/**
 * Class which manages active torrents and dispatching of 
 * incoming BT connections.
 *   
 * Active torrents are torrents either in downloading or 
 * seeding state.
 * 
 * There number of active torrents cannot exceed certain limit.
 * 
 * After a torrent finishes its download, it stays in seeding state
 * indefinitely.  If the user wishes to start a new torrent download 
 * and the limit for active torrents is reached, the seeding torrent
 * with the best upload:download ratio gets terminated.
 * 
 * If active torrent limit is reached and none of the torrents are seeding,
 * the new torrent is queued.
 */
public class TorrentManager 
implements ConnectionAcceptor, TorrentLifecycleListener {
	

	private static final Log LOG = LogFactory.getLog(TorrentManager.class);

	/**
	 * The list of active torrents.
	 */
	private Set /* <ManagedTorrent> */_active = new HashSet();
	
	/**
	 * The list of active torrents that are seeding.
	 * INVARIANT: subset of _active.
	 */
	private Set /* <ManagedTorrent> */_seeding = new HashSet();


	/**
	 * Initializes this. Always call this method before starting any torrents.
	 */
	public void initialize() {
		if (LOG.isDebugEnabled())
			LOG.debug("initializing TorrentManager");
		
		// register ourselves as an acceptor.
		StringBuffer word = new StringBuffer();
		word.append((char)19);
		word.append("BitTorrent");
		RouterService.getConnectionDispatcher().addConnectionAcceptor(
				this,
				new String[]{word.toString()},
				false,false);
	}
	
	/**
	 * @return number of allowed torrents for this speed.. this should
	 * probably be a setting
	 */
	private static int getMaxActiveTorrents() {
		int speed = ConnectionSettings.CONNECTION_SPEED.getValue();
		if (speed <= SpeedConstants.MODEM_SPEED_INT)
			return 1;
		else if (speed <= SpeedConstants.CABLE_SPEED_INT)
			return 2;
		else if (speed <= SpeedConstants.T1_SPEED_INT)
			return 4;
		else
			return 6;

	}
	
	/**
	 * @return active torrent for the given infoHash, null if no such.
	 */
	public synchronized ManagedTorrent getTorrentForHash(byte[] infoHash) {
		for (Iterator iter = _active.iterator(); iter.hasNext();) {
			ManagedTorrent torrent = (ManagedTorrent) iter.next();
			if (Arrays.equals(torrent.getInfoHash(), infoHash))
				return torrent;
		}
		return null;
	}
	
	public void acceptConnection(String word, Socket sock) {
		IncomingBTHandshaker shaker = 
			new IncomingBTHandshaker((NIOSocket)sock, this);
		shaker.startHandshaking();
	}

	public synchronized void torrentComplete(ManagedTorrent t) {
		Assert.that(_active.contains(t));
		_seeding.add(t);
	}

	public synchronized void torrentStarted(ManagedTorrent t) {
		// if the user is adding a seeding torrent.. 
		// effectively restart it
		if (_seeding.remove(t))
			_active.remove(t);
			
		// unless we implement force start the # active will never be greater
		// than the limit.
		while (_active.size() >= getMaxActiveTorrents()) {
			ManagedTorrent best = null;
			for (Iterator iter = _seeding.iterator(); iter.hasNext();) {
				ManagedTorrent torrent = (ManagedTorrent) iter.next();
				if (best == null || torrent.getRatio() > best.getRatio())
					best = torrent;
			}
			if (best != null) 
				best.stop();
		}
		
		_active.add(t);
	}

	public synchronized void torrentStopped(ManagedTorrent t) {
		_active.remove(t);
		_seeding.remove(t);
		
	}
	
	public synchronized boolean allowNewTorrent() {
		return _active.size() - _seeding.size() < getMaxActiveTorrents();
	}
	
	public synchronized int getNumActiveTorrents() {
		return _active.size();
	}
}
