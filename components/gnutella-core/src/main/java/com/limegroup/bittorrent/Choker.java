package com.limegroup.bittorrent;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.bittorrent.settings.BittorrentSettings;
import com.limegroup.gnutella.UploadManager;
import com.limegroup.gnutella.util.SchedulingThreadPool;

abstract class Choker implements Runnable {
	
	private static final Log LOG = LogFactory.getLog(Choker.class);
	/*
	 * time in milliseconds between choking/unchoking of connections
	 */
	private static final int RECHOKE_TIMEOUT = 10 * 1000;
	
	protected final SchedulingThreadPool invoker;
	protected final List<? extends Chokable> chokables;
	protected int round;
	
	private volatile Future periodic;
	private final Runnable immediateChoker = new ImmediateChoker();
	
	public Choker(List<? extends Chokable>chokables, SchedulingThreadPool invoker) {
		this.invoker = invoker;
		this.chokables = chokables;
	}
	
	/**
	 * Stops this choker, cancelling any scheduled choking.
	 */
	public void stop() {
		if (periodic != null)
			periodic.cancel(false);
	}

	/**
	 * Starts this choker
	 */
	public void start() {
		stop();
		periodic = invoker.invokeLater(this, RECHOKE_TIMEOUT);
	}

	/**
	 * Triggers an immediate rechoke
	 */
	public final void rechoke() {
		// final to make sure immediate rechokes happen through
		// the invoker.
		invoker.invokeLater(immediateChoker);
	}
	
	/**
	 * the actual choking logic
	 * @param force whether to force an unchoke.  May be ignored
	 * by actual implementation.
	 */
	protected abstract void rechokeImpl(boolean force);

	public void run() {
		
		round++;
		
		if (LOG.isDebugEnabled())
			LOG.debug("scheduling rechoke");
		
		rechokeImpl(true);
		
		periodic = invoker.invokeLater(this, RECHOKE_TIMEOUT);
	}
	
	
	/**
	 * @return the number of uploads that should be unchoked
	 * Note: Copied verbatim from mainline BT
	 */
	protected static int getNumUploads() {
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
	
	private class ImmediateChoker implements Runnable {
		public void run() {
			rechokeImpl(false);
		}
	}
	
	/*
	 * Compares two BTConnections by their average download 
	 * or upload speeds.  Higher speeds get preference.
	 */
	protected static class SpeedComparator implements Comparator<Chokable> {
		
		private final boolean download;
		public SpeedComparator(boolean download) {
			this.download = download;
		}
		
		public int compare(Chokable c1, Chokable c2) {
			if (c1 == c2)
				return 0;
			
			float bw1 = c1.getMeasuredBandwidth(download, false);
			float bw2 = c2.getMeasuredBandwidth(download, false);
			
			if (bw1 == bw2)
				return 0;
			else if (bw1 > bw2)
				return -1;
			else
				return 1;
		}
	}
}