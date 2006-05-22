package com.limegroup.gnutella.uploader;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.limegroup.gnutella.BandwidthTracker;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.settings.UploadSettings;


/**
 * This class implements the logic of managing BT uploads and HTTP Uploads.
 * More information available here:  
 * http://limewire.org/wiki/index.php?title=UploadSlotsAndBT
 */
public class UploadSlotManager {
    /** 
     * The desired minimum quality of service to provide for uploads, in
     *  KB/s
     */
    private static final float MINIMUM_UPLOAD_SPEED=3.0f;
    
    /**
     * The BandwidthTracker we'll query when we want to know 
     * the maximum upload speed.
     */
	private final BandwidthTracker tracker;
	
	/**
	 * The list of active and queued uploaders.
	 * INVARIANT: 
	 * active is sorted and contains only uploaders of the highest priority or 
	 * non-killable uploads
	 * queued is sorted and queued[0].priority <= active[last].priority
	 * 	
	 */
	private final List active, queued;
	
	public UploadSlotManager(BandwidthTracker tracker) {
		this.tracker = tracker;
		active = new ArrayList(UploadSettings.HARD_MAX_UPLOADS.getValue());
		queued = new ArrayList(UploadSettings.UPLOAD_QUEUE_SIZE.getValue());
	}
	
	/**
	 * Requests an upload slot
	 * @param listener the listener that should be notified when a slot
	 * becomes available
	 * @param queue whether this requestor can be queued
	 * @param preempt whether this requestor can be preempted
	 * @param priority the priority of this requestor
	 * @return the position of the upload if queued, -1 if rejected, 0 if 
	 * it can proceed immediately.
	 */
	public int requestSlot(UploadSlotListener listener,
			boolean queue,
			boolean preempt,
			int priority) {
		if (active.isEmpty())
			return 0;
		
		UploadSlotRequest request = new UploadSlotRequest(listener, preempt, priority);
		
		// do not allow it if there are active uploads with higher priority
		UploadSlotRequest max = (UploadSlotRequest) active.get(0);
		if (max.priority > priority) {
			if (queue) 
				return queueRequest(request);
			else
				return -1; // reject.
		}
		
		// kill all killable uploads with lower priority
		for (int i = active.size() - 1; i >= 0; i--) {
			UploadSlotRequest current = (UploadSlotRequest) active.get(i);
			if (current.priority < priority && current.preempt) {
				request.listener.releaseSlot();
				active.remove(i);
			} else
				break;
		}
		
		if (hasFreeSlot(active.size())) {
			addActiveRequest(request);
			return 0;
		}
		else if (queue)
			return queueRequest(request);
		else
			return -1;
	}
	
	private boolean hasFreeSlot(int current) {
        //Allow another upload if (a) we currently have fewer than
        //SOFT_MAX_UPLOADS uploads or (b) some upload has more than
        //MINIMUM_UPLOAD_SPEED KB/s.  But never allow more than MAX_UPLOADS.
        //
        //In other words, we continue to allow uploads until everyone's
        //bandwidth is diluted.  The assumption is that with MAX_UPLOADS
        //uploads, the probability that all just happen to have low capacity
        //(e.g., modems) is small.  This reduces "Try Again Later"'s at the
        //expensive of quality, making swarmed downloads work better.
        
		if (current >= UploadSettings.HARD_MAX_UPLOADS.getValue()) 
			return false;
		else if (current < UploadSettings.SOFT_MAX_UPLOADS.getValue()) 
			return true;
		else {
			try {
				return tracker.getMeasuredBandwidth() > MINIMUM_UPLOAD_SPEED;
			} catch (InsufficientDataException ide) {
				// this can happen when we haven't had many uploads,
				// so we optimistically allow the upload.
				return true;
			}
		}
	}
	
	/**
	 * adds a request to the queue
	 * @return the position in the queue >= 1.
	 */
	private int queueRequest(UploadSlotRequest request) {
		if (queued.isEmpty()) {
			queued.add(request);
			return 1;
		}
		
		int i = 0;
		for(; i < queued.size(); i++) {
			UploadSlotRequest current = (UploadSlotRequest) queued.get(i);
			if (current.priority < request.priority) 
				break;
		}
		queued.add(i,request);
		return i++;
	}
	
	/**
	 * adds an active request.  
	 */
	private void addActiveRequest(UploadSlotRequest request) {
		int i = 0;
		for(; i < active.size(); i++) {
			UploadSlotRequest current = (UploadSlotRequest) active.get(i);
			if (current.priority < request.priority) 
				break;
		}
		active.add(i,request);
	}
	
	/**
	 * Cancels the request issued by this UploadSlotListener
	 */
	public void cancelRequest(UploadSlotListener listener) {
		requestDone(listener);
		
		for (Iterator iter = queued.iterator(); iter.hasNext();) {
			UploadSlotRequest request = (UploadSlotRequest) iter.next();
			if (request.listener == listener) {
				iter.remove();
				return;
			}
		}
	}

	/**
	 * Notification that the UploadSlotListener is done with its request.
	 */
	public void requestDone(UploadSlotListener listener) {
		for (Iterator iter = active.iterator(); iter.hasNext();) {
			UploadSlotRequest request = (UploadSlotRequest) iter.next();
			if (request.listener == listener) {
				iter.remove();
				startQueuedIfPossible();
				break;
			}
		}
	}
	
	private void startQueuedIfPossible() {
		UploadSlotRequest started = null;
		if (hasFreeSlot(active.size()) && !queued.isEmpty()) {
			UploadSlotRequest queuedRequest = (UploadSlotRequest) queued.get(0);
			if (active.isEmpty()) {
				queued.remove(0);
				active.add(queuedRequest);
				started = queuedRequest;
			} else {
				
				// if we already have active uploads, start a queued one only
				// if the active ones are with the same or lesser priority.
				UploadSlotRequest activeRequest = 
					(UploadSlotRequest) active.get(active.size() - 1);
				
				if (activeRequest.priority <= queuedRequest.priority) {
					// could be less than if the last active is non-preemptible
					queued.remove(0);
					addActiveRequest(queuedRequest);
					started = queuedRequest;
				}
			}
		}
		
		if (started != null)
			started.listener.slotAvailable();
	}
	
	private class UploadSlotRequest implements Comparable {
		final UploadSlotListener listener;
		final boolean preempt;
		final int priority;
		UploadSlotRequest(UploadSlotListener listener,
				boolean preempt,
				int priority) {
			this.listener = listener;
			this.preempt = preempt;
			this.priority = priority;
		}
		
		public int compareTo(Object o) {
			if (this == o)
				return 0;
			UploadSlotRequest other = (UploadSlotRequest)o;
			return this.priority - other.priority;
		}
	}
}
