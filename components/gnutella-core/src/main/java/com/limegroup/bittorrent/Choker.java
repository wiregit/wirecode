package com.limegroup.bittorrent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.bittorrent.settings.BittorrentSettings;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UploadManager;
import com.limegroup.gnutella.io.NIODispatcher;
import com.limegroup.gnutella.util.ThreadPool;

public class Choker {
	
	private static final Log LOG = LogFactory.getLog(Choker.class);
	
	/*
	 * used to order BTConnections according to the average 
	 * download or upload speed.
	 */
	private static final Comparator DOWNLOAD_SPEED_COMPARATOR = 
		new SpeedComparator(true);
	private static final Comparator UPLOAD_SPEED_COMPARATOR = 
		new SpeedComparator(false);
	
	/*
	 * orders BTConnections by the round they were unchoked.
	 */
	private static final Comparator UNCHOKE_COMPARATOR =
		new UnchokeComparator();

	/*
	 * time in milliseconds between choking/unchoking of connections
	 */
	private static final int RECHOKE_TIMEOUT = 10 * 1000;
	
	private final ManagedTorrent torrent;
	private final ThreadPool invoker;
	
	
	private int unchokesSinceLast;
	
	private volatile PeriodicChoker periodic;
	
	/**
	 * whether to choke all connections
	 */
	private boolean _globalChoke = false;
	
	public Choker(ManagedTorrent torrent, ThreadPool invoker) {
		this.torrent = torrent;
		this.invoker = invoker;
	}
	
	public void stop() {
		if (periodic != null)
			periodic.stopped = true;
	}
	
	public void scheduleRechoke() {
		stop();
		periodic = new PeriodicChoker();
		RouterService.schedule(periodic, RECHOKE_TIMEOUT, 0);
	}
	
	public void rechoke() {
		invoker.invokeLater(new Rechoker(torrent.getConnections()));
	}
	
	private class PeriodicChoker implements Runnable {
		private volatile int round;
		volatile boolean stopped;
		public void run() {
			if (stopped)
				return;
			
			if (LOG.isDebugEnabled())
				LOG.debug("scheduling rechoke");
			
			List l;
			List connections = torrent.getConnections();
			if (round++ % 3 == 0) {
				synchronized(connections) {
					l = new ArrayList(connections);
				}
				Collections.shuffle(l);
			} else
				l = connections;
			
			
			NIODispatcher.instance().invokeLater(new Rechoker(l, true));
			
			RouterService.schedule(this, RECHOKE_TIMEOUT, 0);
		}
	}
	
	/**
	 * Chokes all connections instantly and does not unchoke any of them until
	 * it is set to false again. This effectively suspends all uploads and it is
	 * used while we are moving the files from the incomplete folder to the
	 * complete folder.
	 * 
	 * @param choke
	 *            whether to choke all connections.
	 */
	public void setGlobalChoke(final boolean choke) {
		invoker.invokeLater(new Runnable() {
			public void run() {
				_globalChoke = choke;
				if (choke) {
					for (Iterator iter = torrent.getConnections().iterator(); 
					iter.hasNext();) {
						BTConnection btc = (BTConnection) iter.next();
						btc.sendChoke();
					}
				} else
					rechoke();
			}
		});
	}
	
	private class Rechoker implements Runnable {
		private final List connections;
		private final boolean forceUnchokes;
		Rechoker(List connections) {
			this(connections, false);
		}
		
		Rechoker(List connections, boolean forceUnchokes) {
			this.connections = connections;
			this.forceUnchokes = forceUnchokes;
		}
		
		public void run() {
			if (_globalChoke) {
				LOG.debug("global choke");
				return;
			}
			
			if (torrent.isComplete())
				seedRechoke();
			else
				leechRechoke();
		}
		
		private void leechRechoke() {
			List fastest = new ArrayList(connections.size());
			for (Iterator iter = connections.iterator(); iter.hasNext();) {
				BTConnection con = (BTConnection) iter.next();
				if (con.isInterested() && con.shouldBeInterested())
					fastest.add(con);
			}
			Collections.sort(fastest,DOWNLOAD_SPEED_COMPARATOR);
			
			// unchoke the fastest connections that are interested in us
			int numFast = getNumUploads() - 1;
			for(int i = fastest.size() - 1; i >= numFast; i--)
				fastest.remove(i);
			// unchoke optimistically at least one interested connection
			int optimistic = Math.max(1,
					BittorrentSettings.TORRENT_MIN_UPLOADS.getValue() - fastest.size());
			
			
			for (Iterator iter = connections.iterator(); iter.hasNext();) {
				BTConnection con = (BTConnection) iter.next();
				if (fastest.remove(con)) 
					con.sendUnchoke(periodic.round);
				else if (optimistic > 0 && con.shouldBeInterested()) {
					boolean wasChoked = con.isChoked();
					con.sendUnchoke(periodic.round); // this is weird but that's how Bram does it
					if (con.isInterested() && wasChoked) 
						optimistic--;
				} else 
					con.sendChoke();
			}
		}
		
		private void seedRechoke() {
			int numForceUnchokes = 0;
			if (forceUnchokes) {
				int x = (getNumUploads() + 2) / 3;
				numForceUnchokes = Math.max(0, x + periodic.round % 3) / 3 -
				unchokesSinceLast;
			}
			
			List preferred = new ArrayList();
			int newLimit = periodic.round - 3;
			for (Iterator iter = connections.iterator(); iter.hasNext();) {
				BTConnection con = (BTConnection) iter.next();
				if (!con.isChoked() && con.isInterested() && 
						con.shouldBeInterested()) {
					if (con.getUnchokeRound() < newLimit)
						con.setUnchokeRound(-1);
					preferred.add(con);
				}
			}
			
			int numKept = getNumUploads() - numForceUnchokes;
			if (preferred.size() > numKept) {
				Collections.sort(preferred,UNCHOKE_COMPARATOR);
				preferred = preferred.subList(0, numKept);
			}
			
			int numNonPref = getNumUploads() - preferred.size();
			
			if (forceUnchokes)
				unchokesSinceLast = 0;
			else
				unchokesSinceLast += numNonPref;
			
			for (Iterator iter = connections.iterator(); iter.hasNext();) {
				BTConnection con = (BTConnection) iter.next();
				if (preferred.contains(con))
					continue;
				if (!con.isInterested())
					con.sendChoke();
				else if (con.isChoked() && numNonPref > 0 && 
						con.shouldBeInterested()) {
						con.sendUnchoke(periodic.round);
						numNonPref--;
				}
				else {
					if (numNonPref == 0 || !con.shouldBeInterested())
						con.sendChoke();
					else
						numNonPref--;
				}
			}
		}
	}
	
	/**
	 * @return the number of uploads that should be unchoked
	 * Note: Copied verbatim from mainline BT
	 */
	private static int getNumUploads() {
		int uploads = BittorrentSettings.TORRENT_MAX_UPLOADS.getValue();
		if (uploads > 0)
			return uploads;
		
		float rate = UploadManager.getUploadSpeed();
		if (rate == Float.MAX_VALUE)
			return 7; //"unlimited, just guess something here..." - Bram
		else if (rate < 9000)
			return 2;
		else if (rate < 15000)
			return 3;
		else if (rate < 42000)
			return 4;
		else 
			return (int) Math.sqrt(rate * 0.6f);

	}
	
	/*
	 * Compares two BTConnections by their average download 
	 * or upload speeds.  Higher speeds get preference.
	 */
	public static class SpeedComparator implements Comparator {
		
		private final boolean download;
		public SpeedComparator(boolean download) {
			this.download = download;
		}
		
		// requires both objects to be of type BTConnection
		public int compare(Object o1, Object o2) {
			if (o1 == o2)
				return 0;
			
			BTConnection c1 = (BTConnection) o1;
			BTConnection c2 = (BTConnection) o2;
			
			float bw1 = c1.getMeasuredBandwidth(download);
			float bw2 = c2.getMeasuredBandwidth(download);
			
			if (bw1 == bw2)
				return 0;
			else if (bw1 > bw2)
				return -1;
			else
				return 1;
		}
	}
	
	/**
	 * A comparator that compares BT connections by the number of
	 * unchoke round they were unchoked.  Connections with higher 
	 * round get preference.
	 */
	private static class UnchokeComparator implements Comparator {
		public int compare(Object a, Object b) {
			if (a == b)
				return 0;
			BTConnection con1 = (BTConnection) a;
			BTConnection con2 = (BTConnection) b;
			if (con1.getUnchokeRound() != con2.getUnchokeRound())
				return -1 * (con1.getUnchokeRound() - con2.getUnchokeRound());
			return UPLOAD_SPEED_COMPARATOR.compare(con1, con2);
		}
	}
}
