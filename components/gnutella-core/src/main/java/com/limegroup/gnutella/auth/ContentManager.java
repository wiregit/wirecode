package com.limegroup.gnutella.auth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.text.AbstractDocument.Content;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.FileDetails;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.auth.ContentResponseData.Authorization;
import com.limegroup.gnutella.settings.ContentSettings;

/**
 * Keeps track of content requests & responses.
 */
public class ContentManager {
    
    private static final Log LOG = LogFactory.getLog(ContentManager.class);
    
    /**
     * Map of SHA1 to TimeoutTasks.
     * 
     * Invariant: A task is in the map iff it is not yet run and not cancelled.
     */
    private final Map<URN, TimeoutTask> OBSERVERS =
        Collections.synchronizedMap(new HashMap<URN, TimeoutTask>());
    
    /** Set of URNs that have failed requesting. */
    private final Set<URN> TIMEOUTS = Collections.synchronizedSet(new HashSet<URN>());
    
    /** The ContentCache. */
    private final ContentCache CACHE = new ContentCache();
    
    /** The content authority. */
    private volatile ContentAuthority[] authorities = null;
    
    private final ContentAuthorityResponseObserver responseObserver = new ContentResponseHandler();
    
    private volatile Timer timeoutTimer;
    
    private static final long DEFAULT_TIMEOUT = 5 * 1000;
    
    /**
     * Initializes this content manager.
     */
    public void initialize() {
        CACHE.initialize();
        if (timeoutTimer != null) {
        	throw new IllegalStateException("manager already initialized");
        }
        timeoutTimer = new Timer("ContentProcessor " + this, true);
        timeoutTimer.schedule(new InitializerTask(), 0);
    }
    
    /**
     * Shuts down this ContentManager.
     */
    public void shutdown() {
    	synchronized (OBSERVERS) {
    		Timer timer = timeoutTimer;
    		if (timer != null) {
    			timer.cancel();
    		}
    		timeoutTimer = null;
    		if (authorities != null) {
    			for (ContentAuthority auth : authorities) {
    				auth.shutdown();
    			}
    		}
    		authorities = null;
    	}
        CACHE.writeToDisk();
    }
    
    /** Gets the number of items in the cache. */
    public int getCacheSize() {
        return CACHE.getSize();
    }
    
    /**
     *  Sets the chain of content authorities. This call initializes the
     *  authorities and can block therefore. 
     */
    public void setContentAuthorities(ContentAuthority... auths) {
    	if (authorities != null) {
    		throw new IllegalStateException("authorities can only be set once");
    	}
    	List<ContentAuthority> list = new ArrayList<ContentAuthority>(auths.length);
    	for (ContentAuthority auth : auths) {
    		try {
    			auth.initialize();
    			auth.setContentResponseObserver(responseObserver);
    			list.add(auth);
    		}
    		catch (Exception e) {
    			LOG.error("Could not initialize content authority", e);
    		}
    	}
    	if (list.isEmpty()) {
    		throw new IllegalArgumentException("No authority could be initialized");
    	}
    	synchronized (OBSERVERS) {
    		authorities = list.toArray(new ContentAuthority[list.size()]);
    	}
    }
    
    /**
     *  Determines if we've already tried sending a request & waited the time
     *  for a response for the given URN.
     */
    public boolean isVerified(URN urn) {
        return !ContentSettings.isManagementActive() ||
               CACHE.hasResponseFor(urn) || TIMEOUTS.contains(urn);
    }
    
    
    /**
     * Determines if the given FileDetails is valid.
     * 
     * @param details
     * @param observer
     * @param timeout
     */
    public void request(FileDetails details, ContentResponseObserver observer) {
        ContentResponseData response = getResponse(details);
        if(response != null || !ContentSettings.isManagementActive()) {
            if(LOG.isDebugEnabled())
                LOG.debug("Immediate response for URN: " + details.getSHA1Urn());
            observer.handleResponse(details.getSHA1Urn(), response);
        } else {
            if(LOG.isDebugEnabled())
            	LOG.debug("Scheduling request for URN: " + details.getSHA1Urn());
            scheduleRequest(details, observer);
        }
    }
    
    /**
     * Does a request, blocking until a response is given or the request times out.
     */
    public ContentResponseData request(FileDetails details) {
        Validator validator = new Validator();
        synchronized(validator) {
            request(details, validator);
            if (validator.hasResponse()) {
                return validator.getResponse();
            } else {
                try {
                    validator.wait(); // notified when response comes in.
                } catch(InterruptedException ix) {
                    LOG.warn("Interrupted while waiting for response", ix);
                }
                return validator.getResponse();
            }
        }
    }
    
    /**
     * Gets a response if one exists.
     */
    public ContentResponseData getResponse(URN urn) {
        return CACHE.getResponse(urn);
    }
    
    public ContentResponseData getResponse(FileDetails details) {
    	URN sha1 = details.getSHA1Urn();
    	return sha1 != null ? getResponse(sha1) : null;
    }
    
    /**
     * Schedules a request for the given URN, timing out in the given timeout.
     * 
     * @param urn
     * @param observer
     * @param timeout
     */
    protected void scheduleRequest(FileDetails details, ContentResponseObserver observer) {
        URN urn = details.getSHA1Urn();
        if (urn == null) {
        	throw new IllegalArgumentException("urn of details is null");
        }
        TimeoutTask task = null;
        synchronized (OBSERVERS) {
        	task = OBSERVERS.get(urn);
        	if (task != null) {
        		task.observers.add(observer);
        		return;
        	}
        	else {
        		task = new TimeoutTask(1, details, observer);
        		OBSERVERS.put(urn, task);
        		if (hasAuthorities()) {
                	long timeout = authorities[0].getTimeout();
                	timeoutTimer.schedule(task, timeout);
                	sendAuthorizationRequest(0, details);
        		}
        		else if (LOG.isDebugEnabled()) {
        			LOG.debug("Not sending request. No authority yet. " + urn);
        		}
        	}
        }
    }
    
    private boolean hasAuthorities() {
    	return authorities != null && authorities.length > 0;
    }
    
    private void sendAuthorizationRequest(int authIndex, FileDetails details) {
    	if(LOG.isDebugEnabled())
            LOG.debug("Sending request for URN: " + details.getSHA1Urn() 
            		+ " to authority: " + authorities[authIndex]);
    	authorities[authIndex].sendAuthorizationRequest(details);
    }
    
    /**
     * Gets the default content authority.
     */
    protected ContentAuthority[] getDefaultContentAuthorities() {
        return new ContentAuthority[] { new SettingsBasedContentAuthority() };
    }
    
    /** Sets the content authority with the default & process all pre-requested items. */

    private void setDefaultContentAuthorities() {
        ContentAuthority[] auths = getDefaultContentAuthorities();
        setContentAuthorities(auths);
    }
    
    private static long getTimeout(ContentAuthority authority) {
    	long timeout = authority.getTimeout();
    	return timeout > 0 ? timeout : DEFAULT_TIMEOUT;
    }
    
    /** A blocking ContentResponseObserver. */
    private static class Validator implements ContentResponseObserver {
        private boolean gotResponse = false;
        private ContentResponseData response = null;
        
        public void handleResponse(URN urn, ContentResponseData response) {
            synchronized(this) {
                gotResponse = true;
                this.response = response;
                notify();
            }
        }
        
        public boolean hasResponse() {
            return gotResponse;
        }
        
        public ContentResponseData getResponse() {
            return response;
        }
		
    }

    private class ContentResponseHandler implements ContentAuthorityResponseObserver {

		public void handleResponse(ContentAuthority authority, URN urn, ContentResponseData response) {
	        // Only process if we requested this msg.
	        // (Don't allow arbitrary responses to be processed)
			if (LOG.isDebugEnabled()) {
				LOG.debug("received response: " + urn + " " + response);
			}
			if (response.getAuthorization() == Authorization.UNKNOWN) {
				handleUnknownResponse(authority, urn, response);
			}
			else {
				handleKnownResponse(authority, urn, response);
			}
		}
		
		private void handleUnknownResponse(ContentAuthority authority, URN urn, ContentResponseData response) {
			synchronized (OBSERVERS) {
				TimeoutTask task = OBSERVERS.get(urn);
				if (task == null) {
					// unknown response that came too late or was never requested
					// do nothing
					return;
				}
				else {
					// check if the response's originating authority is the one
					// from the current request and not an earlier one that 
					// responed after its timeout
					if (authority == authorities[task.index - 1]) {
						// the answer unknown is like an early timeout event
						// so run the task to query the next authority if possible
						task.timeoutAndScheduleNext();
					}
				}
			}
		}
		
		private void handleKnownResponse(ContentAuthority authority, URN urn, ContentResponseData response) {
			TimeoutTask task = OBSERVERS.remove(urn);
			if (task != null) {
				CACHE.addResponse(urn, response);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Notifying observers");
				}
				task.handleResponse(response);
			}
			else {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Could not find task for urn: " + urn);
				}
				// if there is a timed out request for this response
				// cache the response for future lookups
				if (TIMEOUTS.remove(urn)) {
					CACHE.addResponse(urn, response);
				}
			}
		}
    }
    	
    /**
     * Task that initiaalizes and sets the default content authorities. 
     */
    private class InitializerTask extends TimerTask {
		@Override
		public void run() {
			synchronized (OBSERVERS) {
				Assert.that(authorities == null);
				setDefaultContentAuthorities();
				Assert.that(authorities != null);
				Assert.that(authorities.length > 0);
				Assert.that(authorities[0] != null);
				if (LOG.isDebugEnabled()) {
					LOG.debug("authorities set");
				}
				long timeout = getTimeout(authorities[0]);
				// send queued up requests
				for (TimeoutTask task : OBSERVERS.values()) {
					timeoutTimer.schedule(task, timeout);
					sendAuthorizationRequest(0, task.details);
				}
			}
		}
    }
    
    private class TimeoutTask extends TimerTask {

    	List<ContentResponseObserver> observers;
        
        final FileDetails details;
        final int index;
        
        final URN urn; 
        
        public TimeoutTask(int index, FileDetails details, ContentResponseObserver observer) {
        	this(index, details, createList(observer));
        }
        
        public TimeoutTask(int index, FileDetails details, List<ContentResponseObserver> observers) {
        	this.index = index;
        	this.details = details;
        	this.observers = observers;
        	this.urn = details.getSHA1Urn();
        }
        
		@Override
		public void run() {
			timeoutAndScheduleNext();
		}
		
		public void timeoutAndScheduleNext() {
			synchronized (OBSERVERS) {
				if (OBSERVERS.remove(urn) != this) {
					// task not in map, means it has been cancelled
					return;
				}
				if (canRequestFromOtherAuthority()) {
					long timeout = getTimeout(authorities[index]);
					TimeoutTask task = new TimeoutTask(index + 1, details, observers);
					OBSERVERS.put(details.getSHA1Urn(), task);
					timeoutTimer.schedule(task, timeout);
					sendAuthorizationRequest(index, details);
					return;
				}
				else {
					TIMEOUTS.add(urn);
				}
			}
			// timeout and no more authorities left, notify observers
			if (LOG.isDebugEnabled()) {
				LOG.debug("timeout for urn: " + details.getSHA1Urn());
			}
			handleResponse(new ContentResponseData(System.currentTimeMillis(), Authorization.UNKNOWN, "No authority knows this file"));
		}
		
		public void handleResponse(ContentResponseData data) {
			for (ContentResponseObserver observer : observers) {
				observer.handleResponse(urn, data);
			}
		}
    	
		public boolean canRequestFromOtherAuthority() {
			return index < authorities.length;
		}
    }
    
	private static List<ContentResponseObserver> createList(ContentResponseObserver observer) {
		ArrayList<ContentResponseObserver> list = new ArrayList<ContentResponseObserver>(2);
		list.add(observer);
		return list;
	}
}
