package com.limegroup.bittorrent.tracking;

import java.util.Collection;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.bittorrent.BTMetaInfo;
import com.limegroup.bittorrent.ManagedTorrent;
import com.limegroup.bittorrent.TorrentLocation;
import com.limegroup.bittorrent.settings.BittorrentSettings;
import com.limegroup.gnutella.MessageService;
import com.limegroup.gnutella.RouterService;
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
	private final Collection<Tracker> trackers = new CopyOnWriteArrayList<Tracker>();
	
	private final ProcessingQueue requestQueue = new ProcessingQueue("tracker requester");
	
	/**
	 * the next time we'll contact the tracker.
	 */
	private volatile long _nextTrackerRequestTime;
	
	private final ManagedTorrent torrent;
	
	private volatile TimerTask scheduledAnnounce;
	
	TrackerManager(ManagedTorrent torrent) {
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
	private void announceBlocking(Tracker t, final Tracker.Event event) {
		if (LOG.isDebugEnabled())
			LOG.debug("connecting to tracker " + t.toString()+" for event "+event);
		TrackerResponse response = t.request(event);
		handleTrackerResponse(response, t);
	}
	
	private void announceToAll(final Tracker.Event event) {
		// announce ourselves to the trackers
		for (final Tracker t : trackers) {
			Runnable announcer = new Runnable(){
				public void run() {
					announceBlocking(t,event);
				}
			};
			requestQueue.invokeLater(announcer);
		}
	}
	
	public void announceStart() {
		_nextTrackerRequestTime = 0;
		announceToAll(Tracker.Event.START);
	}
	
	public void announceStop() {
		// stop any current or future announcements
		TimerTask t = scheduledAnnounce;
		if (t != null)
			t.cancel();
		
		announceToAll(Tracker.Event.STOP);	
	}
	
	public void announceComplete() {
		announceToAll(Tracker.Event.COMPLETE);
	}
	
	/**
	 * Announces ourselves to a tracker
	 * 
	 * @param url the URL of the tracker
	 */
	private void announce(final Tracker t) {
		if (LOG.isDebugEnabled())
			LOG.debug("announce thread for " + t.toString());
		torrent.setScraping();
		announceBlocking(t, Tracker.Event.NONE);
	}
	
	/**
	 * @return whether we've failed to connect to any tracker
	 * too many times and should give up.
	 */
	private boolean isHopeless() {
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
	
	private void scheduleTrackerRequest(long minDelay, final Tracker t) {
		final Runnable announcer = new Runnable() {
			public void run() {
				if (!torrent.isActive())
					return;
				
				if (LOG.isDebugEnabled())
					LOG.debug("announcing to " + t.toString());
				announce(t);
			}
		};
		Runnable scheduled = new Runnable() {
			public void run() {
				requestQueue.add(announcer);
			}
		};
		LOG.debug("scheduling new tracker request");
		if (scheduledAnnounce != null)
			scheduledAnnounce.cancel();
		scheduledAnnounce = RouterService.schedule(scheduled, minDelay, 0);
		_nextTrackerRequestTime = System.currentTimeMillis() + minDelay;
	}
	
	public long getNextTrackerRequestTime() {
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
