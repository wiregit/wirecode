package com.limegroup.bittorrent.tracking;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.bittorrent.ManagedTorrent;

@Singleton
public class TrackerManagerFactoryImpl implements TrackerManagerFactory {
    
    private final TrackerFactory trackerFactory;
    
    @Inject
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
