package com.limegroup.bittorrent.tracking;

import com.limegroup.bittorrent.ManagedTorrent;

public class TrackerManagerFactoryImpl implements TrackerManagerFactory {
    
    private final TrackerFactory trackerFactory;
    
    public TrackerManagerFactoryImpl(TrackerFactory trackerFactory) {
        this.trackerFactory = trackerFactory;
    }
	
	/* (non-Javadoc)
     * @see com.limegroup.bittorrent.tracking.TrackerManagerFactory#getTrackerManager(com.limegroup.bittorrent.ManagedTorrent)
     */
	public TrackerManager getTrackerManager(ManagedTorrent t) {
		return new TrackerManager(t, trackerFactory);
	}
}
