package com.limegroup.gnutella.uploader;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.settings.UploadSettings;


/**
 * This class implements the logic of managing BT uploads and HTTP Uploads.
 * More information available here:  
 * http://limewire.org/wiki/index.php?title=UploadSlotsAndBT
 */
public class UploadSlotManager {
	
	/**
	 * The three priority levels
	 */
	private static final int BT_SEED = 0; // low priority
	private static final int HTTP = 1; // medium periority
	private static final int BT_DOWNLOAD = 2; // high priority
	
    /** 
     * The desired minimum quality of service to provide for uploads, in
     *  KB/s
     */
    private static final float MINIMUM_UPLOAD_SPEED = 3.0f;
    
	/**
	 * The list of active upload slot requests
	 * INVARIANT: sorted by priority and contains only 
	 * requests of the highest priority or non-preemptible requests
	 */
	private final List /* <? extends UploadSlotRequest> */ active;
	
	/**
	 * The list of queued non-resumable requests
	 */
	private final List /* <HTTPSlotRequest> */ queued;
	
	/**
	 * The list of queued resumable requests
	 * (currently only Seeding BT Uploaders)
	 */
	private final List /* <BTSlotRequest> */ queuedResumable;
	
	public UploadSlotManager() {
		active = new ArrayList(UploadSettings.HARD_MAX_UPLOADS.getValue());
		queued = new ArrayList(UploadSettings.UPLOAD_QUEUE_SIZE.getValue());
		queuedResumable = new ArrayList(UploadSettings.UPLOAD_QUEUE_SIZE.getValue());
	}

	/**
	 * Polls for an available upload slot. (HTTP-style)
	 * 
	 * @param user the user that will use the upload slot
	 * @queue if the user supports queueing
	 * @return the position in the queue if queued, -1 if rejected,
	 * 0 if it can proceed immediately
	 */
	public int pollForSlot(UploadSlotUser user, boolean queue) {
		return requestSlot(new HTTPSlotRequest(user, queue));
	}
	
	/**
	 * Requests an upload slot. (BT-style)
	 * 
	 * @param listener the listener that should be notified when a slot
	 * becomes available
	 * @param highPriority if the user needs an upload slot now or never
	 * @return the position of the upload if queued, -1 if rejected, 0 if 
	 * it can proceed immediately.
	 */
	public int requestSlot(UploadSlotListener listener, boolean highPriority) {
		return requestSlot(new BTSlotRequest(listener, highPriority));
	}

	private int requestSlot(UploadSlotRequest request) {
		
		// see if there exists an uploader with higher priority
		boolean existHigherPriority = existActiveHigherPriority(request.getPriority());
		
		// see if this is already in the queue
		int positionInQueue = positionInQueue(request);
		
		// see if there are any uploaders with lower priority
		int freeableSlots = getPreemptible(request.getPriority());
		
		// if there is a higher priority upload or not enough free slots, queue.
		if (existHigherPriority || 
				!hasFreeSlot(active.size() + positionInQueue - freeableSlots)) {
			
			if (!request.isQueuable())
				return -1;
			
			if (positionInQueue > 0)
				return positionInQueue;
			else
				return queueRequest(request);
		}
		
		// free any freeable slots
		if (freeableSlots > 0)
			killPreemptible(request.getPriority());

		// remove from queue if it was there
		if (positionInQueue > 0)
			removeIfQueued(request.getUser());
		
		addActiveRequest(request);
		return 0;
	}
	
	/**
	 * @return the position in the appropriate queue of the request
	 *    0 if not in the queue
	 */
	private int positionInQueue(UploadSlotRequest request) {
		if (request.isQueuable()) {
			List queue = getQueue(request.getUser());
			int i = queue.indexOf(request);
			if (i > -1)
				return ++i;
		} 
		return 0;
	}
	
	/**
	 * @return the queue where requests from the user would be found.
	 */
	private List getQueue(UploadSlotUser user) {
		return user instanceof UploadSlotListener ? queuedResumable : queued;
	}
	
	/**
	 * @return if there are any active users with higher priority
	 */
	private boolean existActiveHigherPriority(int priority) {
		if (priority == BT_DOWNLOAD)
			return false;
		
		if (!active.isEmpty()) {
			UploadSlotRequest max = (UploadSlotRequest) active.get(0);
			if (max.getPriority() > priority)
				return true;
		}
		return false;
	}
	
	/**
	 * @return the number of active uploaders with lower priority
	 * that can be preempted.
	 */
	private int getPreemptible(int priority) {
		if (priority == BT_SEED)
			return 0;
		
		// iterate backwards
		int ret = 0;
		for(int i = active.size() - 1; i >= 0; i--) {
			UploadSlotRequest request = (UploadSlotRequest) active.get(i);
			if (request.getPriority() < priority && request.isPreemptible())
				ret++;
		}
		return ret;
	}
	
	/**
	 * kills any active uploaders that can be preempted and have lower priority
	 */
	private void killPreemptible(int priority) {
		for(int i = active.size() - 1; i >= 0; i--) {
			UploadSlotRequest request = (UploadSlotRequest) active.get(i);
			if (request.getPriority() < priority && request.isPreemptible())
				request.getUser().releaseSlot();
		}
	}

	/**
	 * @return whether there would be a free slot if current many were taken.
	 */
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
			float fastest = 0f;
			for (Iterator iter = active.iterator(); iter.hasNext();) {
				UploadSlotRequest request = (UploadSlotRequest) iter.next();
				UploadSlotUser user = request.getUser();
				float speed = 0;
				try {
					speed = user.getMeasuredBandwidth();
				} catch (InsufficientDataException ide) {}
				fastest = Math.max(fastest,speed);
			}
			
			return fastest > MINIMUM_UPLOAD_SPEED;
		}
	}
	
	/**
	 * adds a request to the appropriate queue if not already there
	 * @return the position in the queue (>= 1)
	 */
	private int queueRequest(UploadSlotRequest request) {
		List queue = getQueue(request.user);
		if (queue.size() == UploadSettings.UPLOAD_QUEUE_SIZE.getValue())
			return -1;
		queue.add(request);
		return queue.size();
	}
	
	/**
	 * adds an active request.  
	 */
	private void addActiveRequest(UploadSlotRequest request) {
		int i = 0;
		for(; i < active.size(); i++) {
			UploadSlotRequest current = (UploadSlotRequest) active.get(i);
			if (current.getPriority() < request.getPriority()) 
				break;
		}
		active.add(i,request);
	}
	
	/**
	 * Cancels the request issued by this UploadSlotListener
	 */
	public void cancelRequest(UploadSlotUser listener) {
		if (!removeIfQueued(listener))
			requestDone(listener);
	}

	/**
	 * Removes an UploadSlotUser from the queue. 
	 * @return if the user was in the queue.
	 */
	private boolean removeIfQueued(UploadSlotUser user) {
		List queue = getQueue(user);
		for (Iterator iter = queue.iterator(); iter.hasNext();) {
			UploadSlotRequest request = (UploadSlotRequest) iter.next();
			if (request.getUser() == user) {
				iter.remove();
				return true;
			}
		}
		return false;
	}

	/**
	 * Notification that the UploadSlotUser is done with its request.
	 */
	public void requestDone(UploadSlotUser listener) {
		for (Iterator iter = active.iterator(); iter.hasNext();) {
			UploadSlotRequest request = (UploadSlotRequest) iter.next();
			if (request.getUser() == listener) {
				iter.remove();
				resumeQueued();
				return;
			}
		}
	}
	
	/**
	 * resumes an uploader from the resumable queue
	 * (in this specific case a Seeding BT uploader)
	 */
	private void resumeQueued() {
		// can't resume if someone is still active
		if (existActiveHigherPriority(BT_SEED))
			return;
		
		for(Iterator iter = queuedResumable.iterator();
		iter.hasNext() && hasFreeSlot(active.size());) {
			BTSlotRequest queuedRequest = (BTSlotRequest) iter.next();
			iter.remove();
			active.add(queuedRequest);
			queuedRequest.getListener().slotAvailable();
		}
	}
	
	/**
	 * A request for an upload slot.
	 */
	private abstract class UploadSlotRequest {
		private final UploadSlotUser user;
		private final boolean preempt;
		private final int priority;
		
		boolean isPreemptible() {
			return preempt;
		}
		
		int getPriority() {
			return priority;
		}
		
		UploadSlotUser getUser() {
			return user;
		}
		
		abstract boolean isQueuable();
		
		protected UploadSlotRequest(UploadSlotUser listener,
				boolean preempt,
				int priority) {
			this.user = listener;
			this.preempt = preempt;
			this.priority = priority;
		}
		
		public boolean equals(Object o) {
			if (! (o instanceof UploadSlotRequest))
				return false;
			UploadSlotRequest other = (UploadSlotRequest) o;
			
			// one request per user at a time.
			return getUser() == other.getUser();
		}
	}

	/**
	 * An HTTP request for an upload slot.
	 */
	private class HTTPSlotRequest extends UploadSlotRequest {
		
		private final boolean queuable;
		
		HTTPSlotRequest (UploadSlotUser user, boolean queuable) {
			super(user, false, HTTP);
			this.queuable = queuable;
		}
		
		boolean isQueuable() {
			return queuable;
		}
	}

	/**
	 * A BT request for an upload slot.
	 */
	private class BTSlotRequest extends UploadSlotRequest {
		BTSlotRequest(UploadSlotListener listener, boolean highPriority) {
			super(listener, !highPriority, highPriority ? BT_DOWNLOAD : BT_SEED);
		}
		
		UploadSlotListener getListener() {
			return (UploadSlotListener) getUser();
		}
		
		boolean isQueuable() {
			return getPriority() == BT_SEED;
		}
	}
}
