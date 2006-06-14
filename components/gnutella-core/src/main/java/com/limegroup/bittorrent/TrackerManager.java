package com.limegroup.bittorrent;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.bittorrent.settings.BittorrentSettings;
import com.limegroup.gnutella.MessageService;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.util.CoWList;
import com.limegroup.gnutella.util.ProcessingQueue;

public class TrackerManager {
	
	private static final Log LOG = LogFactory.getLog(TrackerManager.class);
	
	/**
	 * the number of failures after which we consider giving up
	 */
	private static final int MAX_TRACKER_FAILURES = 5;
	
	/**
	 * The trackers we know that track this torrent download.
	 * Note: It will be read much more often than updated. 
	 * Note2: This is not proper multi-tracker support. 
	 */
	private final Collection<Tracker> trackers = new CoWList<Tracker>(CoWList.ARRAY_LIST);
	
	/**
	 * the next time we'll contact the tracker.
	 */
	private volatile long _nextTrackerRequestTime;
	
	private final ProcessingQueue trackerQueue = 
		new ProcessingQueue("Tracker Request Queue");
	
	private final ManagedTorrent torrent;
	
	public TrackerManager(ManagedTorrent torrent) {
		this.torrent = torrent;
		BTMetaInfo info = torrent.getMetaInfo();
		for (int i = 0; i < info.getTrackers().length;i++) 
			trackers.add(new Tracker(info.getTrackers()[i],info, torrent));
	}
	
	public void add(Tracker t) {
		trackers.add(t);
	}
	
	/**
	 * Announce ourselve to a tracker
	 * 
	 * @param url
	 *            the <tt>URL</tt> for the tracker
	 * @param event
	 *            the event to send to the tracker, see TrackerRequester class
	 */
	private void announceBlocking(Tracker t, final int event) {
		if (LOG.isDebugEnabled())
			LOG.debug("connecting to tracker " + t.toString()+" for event "+event);
		TrackerResponse response = t.request(event);
		handleTrackerResponse(response, t);
	}
	
	public void announceStart() {
		_nextTrackerRequestTime = 0;
		
		
		Runnable announcer = new Runnable() {
			public void run() {
				// announce ourselves to the trackers
				for (Tracker t : trackers) {
					announceBlocking(t,Tracker.EVENT_START);
				}
			}
		};
		
		trackerQueue.invokeLater(announcer);
	}
	
	public void announceStop() {
		Runnable announcer = new Runnable() {
			public void run() {
				// tell all trackers we're stopping.
				for (Tracker t: trackers)
					announceBlocking(t,Tracker.EVENT_STOP);
			}
		};
		
		trackerQueue.invokeLater(announcer);
	}
	
	public void announceComplete() {
		//TODO: should we announce how much we've downloaded if we just resumed
		// the torrent?  Its not mentioned in the spec...
		Runnable announcer = new Runnable() {
			public void run() {
				for (Tracker t: trackers) {
					announceBlocking(t,Tracker.EVENT_COMPLETE);
				}
			}
		};
		trackerQueue.invokeLater(announcer);
	}
	
	/**
	 * Announces ourselves to a tracker
	 * 
	 * @param url the URL of the tracker
	 */
	public void announce(final Tracker t) {
		
		// offload tracker requests, - it simply takes too long even to execute
		// it in our timer thread
		Runnable trackerRequest = new Runnable() {
			public void run() {
				if (LOG.isDebugEnabled())
					LOG.debug("announce thread for " + t.toString());
				torrent.setScraping();
				announceBlocking(t, Tracker.EVENT_NONE);
			}
		};
		trackerQueue.invokeLater(trackerRequest);
	}
	
	/**
	 * @return whether we've failed to connect to any tracker
	 * too many times and should give up.
	 */
	public boolean isHopeless() {
		if (trackers.isEmpty())
			return true;
		
		int least = Integer.MAX_VALUE;
		for(Tracker t: trackers) {
			
			// shortcut
			if (t.getFailures() == 0)
				return false;
			
			if (t.getFailures() < least)
				least = t.getFailures();
		}
		return least >= MAX_TRACKER_FAILURES;
	}
	
	public void scheduleTrackerRequest(long minDelay, final Tracker t) {
		Runnable announcer = new Runnable() {
			public void run() {
				if (!torrent.isActive())
					return;
				
				if (LOG.isDebugEnabled())
					LOG.debug("announcing to " + t.toString());
				announce(t);
			}
		};
		RouterService.schedule(announcer, minDelay, 0);
		_nextTrackerRequestTime = System.currentTimeMillis() + minDelay;
	}
	
	long getNextTrackerRequestTime() {
		return _nextTrackerRequestTime;
	}
	
	/**
	 * This method handles the response from a tracker
	 */
	private void handleTrackerResponse(TrackerResponse response, Tracker t) {
		LOG.debug("handling tracker response " + t.toString());

		long minWaitTime = BittorrentSettings.TRACKER_MIN_REASK_INTERVAL
				.getValue() * 1000;
		
		if (response != null) {
				for (TorrentLocation next : response.PEERS) 
					torrent.addEndpoint(next);
				
				minWaitTime = response.INTERVAL * 1000;
				
				if (response.FAILURE_REASON != null) {
					t.recordFailure();
					// if we have only one tracker and it gave a reason,
					// inform the user on first failure.
					if (trackers.size() == 1 && t.getFailures() == 0) {
						MessageService.showError("TORRENTS_TRACKER_FAILURE", 
								torrent.getMetaInfo().getName() + "\n" +
								response.FAILURE_REASON);
					}
				} else
					t.recordSuccess();
		} else 
			t.recordFailure();

		if (torrent.isActive()) {
			if (isHopeless() && torrent.shouldStop()) {
				torrent.stopVoluntarily();
			} else
				scheduleTrackerRequest(minWaitTime, t);
		}
	}
}
