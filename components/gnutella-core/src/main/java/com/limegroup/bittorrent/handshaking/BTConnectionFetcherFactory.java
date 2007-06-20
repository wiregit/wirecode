package com.limegroup.bittorrent.handshaking;

import java.util.concurrent.ScheduledExecutorService;

import com.limegroup.bittorrent.ManagedTorrent;

public class BTConnectionFetcherFactory {
	private static BTConnectionFetcherFactory instance;
	public static BTConnectionFetcherFactory instance() {
		if (instance == null)
			instance = new BTConnectionFetcherFactory();
		return instance;
	}
	
	protected BTConnectionFetcherFactory(){}
	
	public BTConnectionFetcher getBTConnectionFetcher(ManagedTorrent torrent, ScheduledExecutorService scheduler) {
		return new BTConnectionFetcher(torrent, scheduler);
	}
}
