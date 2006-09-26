package com.limegroup.bittorrent.tracking;

import com.limegroup.bittorrent.ManagedTorrent;

public class TrackerManagerFactory {
	private static TrackerManagerFactory instance;
	public static TrackerManagerFactory instance() {
		if (instance == null)
			instance = new TrackerManagerFactory();
		return instance;
	}
	
	protected TrackerManagerFactory(){}
	
	public TrackerManager getTrackerManager(ManagedTorrent t) {
		return new TrackerManager(t);
	}
}
