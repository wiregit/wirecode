package com.limegroup.bittorrent.handshaking;

import java.util.concurrent.ScheduledExecutorService;

import com.google.inject.Singleton;
import com.limegroup.bittorrent.ManagedTorrent;

@Singleton
public class BTConnectionFetcherFactory {
	
	public BTConnectionFetcher getBTConnectionFetcher(ManagedTorrent torrent, ScheduledExecutorService scheduler) {
		return new BTConnectionFetcher(torrent, scheduler);
	}
}
