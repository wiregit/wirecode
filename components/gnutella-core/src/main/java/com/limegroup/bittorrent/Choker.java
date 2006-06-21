package com.limegroup.bittorrent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.bittorrent.settings.BittorrentSettings;
import com.limegroup.gnutella.UploadManager;
import com.limegroup.gnutella.util.SchedulingThreadPool;

public class Choker {
	
	private static final Log LOG = LogFactory.getLog(Choker.class);
	
	/*
	 * used to order BTConnections according to the average 
	 * download or upload speed.
	 */
	private static final Comparator<BTConnection> DOWNLOAD_SPEED_COMPARATOR = 
		new SpeedComparator(true);
	private static final Comparator<BTConnection> UPLOAD_SPEED_COMPARATOR = 
		new SpeedComparator(false);
	
	/*
	 * orders BTConnections by the round they were unchoked.
	 */
	private static final Comparator<BTConnection> UNCHOKE_COMPARATOR =
		new UnchokeComparator();
	
	/*
	 * time in milliseconds between choking/unchoking of connections
	 */
	private static final int RECHOKE_TIMEOUT = 10 * 1000;
	
	private final ManagedTorrent torrent;
	private final SchedulingThreadPool invoker;
	
	
	private int unchokesSinceLast;
	
	private int round;
	
	private volatile Future periodic;
	
	public Choker(ManagedTorrent torrent, SchedulingThreadPool invoker) {
		this.torrent = torrent;
		this.invoker = invoker;
	}
	
	public void stop() {
		if (periodic != null)
			periodic.cancel(false);
	}
	
	public void scheduleRechoke() {
		stop();
		periodic = invoker.invokeLater(new PeriodicChoker(), RECHOKE_TIMEOUT);
	}
	
	public void rechoke() {
		invoker.invokeLater(new Rechoker(false));
	}
	
	private class PeriodicChoker implements Runnable {
		public void run() {
			
			round++;
			
			if (LOG.isDebugEnabled())
				LOG.debug("scheduling rechoke");
			
			rechokeImpl(true);
			
			periodic = invoker.invokeLater(this, RECHOKE_TIMEOUT);
		}
	}
	
	private class Rechoker implements Runnable {
		private final boolean forceUnchokes;
		
		Rechoker(boolean forceUnchokes) {
			this.forceUnchokes = forceUnchokes;
		}
		
		public void run() {
			rechokeImpl(forceUnchokes);
		}
	}
	
	private void rechokeImpl(boolean force) {
		if (torrent.getState() == ManagedTorrent.SEEDING)
			seedRechoke(force);
		else if (torrent.getState() == ManagedTorrent.DOWNLOADING)
			leechRechoke();
	}
	
	private void leechRechoke() {
		List<BTConnection> connections = torrent.getConnections();
		List<BTConnection> fastest = new ArrayList<BTConnection>(connections.size());
		for (BTConnection con : connections) {
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
		
		
		for (BTConnection con : connections) {
			if (fastest.remove(con)) 
				con.sendUnchoke(round);
			else if (optimistic > 0 && con.shouldBeInterested()) {
				boolean wasChoked = con.isChoked();
				con.sendUnchoke(round); // this is weird but that's how Bram does it
				if (con.isInterested() && wasChoked) 
					optimistic--;
			} else 
				con.sendChoke();
		}
	}
	
	private void seedRechoke(boolean forceUnchokes) {
		List<BTConnection> connections = torrent.getConnections();
		int numForceUnchokes = 0;
		if (forceUnchokes) {
			int x = (getNumUploads() + 2) / 3;
			numForceUnchokes = Math.max(0, x + round % 3) / 3 -
			unchokesSinceLast;
		}
		
		List<BTConnection> preferred = new ArrayList<BTConnection>();
		int newLimit = round - 3;
		for (BTConnection con : connections) {
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
		
		for (BTConnection con : connections) {
			if (preferred.contains(con))
				continue;
			if (!con.isInterested())
				con.sendChoke();
			else if (con.isChoked() && numNonPref > 0 && 
					con.shouldBeInterested()) {
				con.sendUnchoke(round);
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
	public static class SpeedComparator implements Comparator<BTConnection> {
		
		private final boolean download;
		public SpeedComparator(boolean download) {
			this.download = download;
		}
		
		// requires both objects to be of type BTConnection
		public int compare(BTConnection c1, BTConnection c2) {
			if (c1 == c2)
				return 0;
			
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
	private static class UnchokeComparator implements Comparator<BTConnection> {
		public int compare(BTConnection con1, BTConnection con2) {
			if (con1 == con2)
				return 0;
			if (con1.getUnchokeRound() != con2.getUnchokeRound())
				return -1 * (con1.getUnchokeRound() - con2.getUnchokeRound());
			return UPLOAD_SPEED_COMPARATOR.compare(con1, con2);
		}
	}
}
