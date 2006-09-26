package com.limegroup.bittorrent.handshaking;

import com.limegroup.bittorrent.ManagedTorrent;
import com.limegroup.gnutella.util.SchedulingThreadPool;

public class BTConnectionFetcherFactory {
	private static BTConnectionFetcherFactory instance;
	public static BTConnectionFetcherFactory instance() {
		if (instance == null)
			instance = new BTConnectionFetcherFactory();
		return instance;
	}
	
	protected BTConnectionFetcherFactory(){}
	
	public BTConnectionFetcher getBTConnectionFetcher(ManagedTorrent torrent, SchedulingThreadPool scheduler) {
		return new BTConnectionFetcher(torrent, scheduler);
	}
}
