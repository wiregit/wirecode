package com.limegroup.bittorrent.choking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.limegroup.bittorrent.Chokable;
import com.limegroup.bittorrent.settings.BittorrentSettings;
import com.limegroup.gnutella.util.SchedulingThreadPool;

/**
 * Choker that implements the choking logic during torrent downloading
 * (leeching) 
 */
class LeechChoker extends Choker {

	/**
	 * used to order BTConnections according to the average 
	 * download or upload speed.
	 */
	private static final Comparator<Chokable> DOWNLOAD_SPEED_COMPARATOR = 
		new SpeedComparator(true);
	
	LeechChoker(List<? extends Chokable> chokables, SchedulingThreadPool invoker) {
		super(chokables, invoker);
	}

	@Override
	protected void rechokeImpl(boolean force) {
		List<Chokable> fastest = new ArrayList<Chokable>(chokables.size());
		for (Chokable con : chokables) {
			if (con.isInterested() && con.shouldBeInterested() &&
					con.getMeasuredBandwidth(true, false) > 0.256)
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
		
		synchronized(chokables) {
			Collections.shuffle(chokables);
		}
		
		for (Chokable con : chokables) {
			if (fastest.remove(con)) 
				con.unchoke(round);
			else if (optimistic > 0 && con.shouldBeInterested()) {
				boolean wasChoked = con.isChoked();
				con.unchoke(round); // this is weird but that's how Bram does it
				if (con.isInterested() && wasChoked) 
					optimistic--;
			} else 
				con.choke();
		}
	}

}
