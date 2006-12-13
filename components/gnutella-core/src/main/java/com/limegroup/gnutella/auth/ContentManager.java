package com.limegroup.gnutella.auth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.FileDetails;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.vendor.ContentResponse;
import com.limegroup.gnutella.settings.ContentSettings;
import com.limegroup.gnutella.util.ManagedThread;

/**
 * Keeps track of content requests & responses.
 */
public class ContentManager {
    
    private static final Log LOG = LogFactory.getLog(ContentManager.class);
    
    /** Map of SHA1 to Observers listening for responses to the SHA1. */
    private final Map<URN, Collection<Responder>> OBSERVERS =
        Collections.synchronizedMap(new HashMap<URN, Collection<Responder>>());
    
    /** List of Responder's that are currently waiting, in order of timeout. */
    private final List<Responder> RESPONDERS = new ArrayList<Responder>();
    
    /** Set or URNs that we've already requested. */
    private final Map<URN, FileDetails> REQUESTED = Collections.synchronizedMap(new HashMap<URN, FileDetails>());
    
    /** Set of URNs that have failed requesting. */
    private final Set<URN> TIMEOUTS = Collections.synchronizedSet(new HashSet<URN>());
    
    /* 
     * LOCKING OF THE ABOVE:
     * 
     * OBSERVERS may NOT be locked if RESPONDERS, TIMEOUTS or REQUESTED is locked.
     * RESPONDERS may NOT be locked if TIMEOUTS or REQUESTED is locked.
     * TIMEOUTS may be locked at any time.
     * REQUESTED may be locked at any time.
     * 
     * In other words, locking order goes:
     *  synchronized(OBSERVERS) {
     *      ...
     *      synchronized(RESPONDERS) {
     *          ...
     *          synchronized(TIMEOUTS) { ... }
     *          synchronized(REQUESTED) { ... }
     *      }
     *  }
     */
    
    /** The ContentCache. */
    private final ContentCache CACHE = new ContentCache();
    
    /** The content authority. */
    private volatile ContentAuthority authority = null;
    
    /** Wehther or not we're shutting down. */
    private volatile boolean shutdown = false;
    
    private ContentResponseObserver responseObserver = new ContentResponseHandler();
    
    /**
     * Initializes this content manager.
     */
    public void initialize() {
        CACHE.initialize();
        startProcessingThread();
    }
    
    /**
     * Shuts down this ContentManager.
     */
    public void shutdown() {
        shutdown = true;
        CACHE.writeToDisk();
    }
    
    /** Gets the number of items in the cache. */
    public int getCacheSize() {
        return CACHE.getSize();
    }
    
    /** Sets the content authority. */
    public void setContentAuthority(ContentAuthority authority) {
    	ContentAuthority old = this.authority;
    	if (old != null) {
    		old.setContentResponseObserver(null);
    		old.shutdown();
    	}
        this.authority = authority;
        if (authority != null) { 
        	authority.setContentResponseObserver(responseObserver);
        	// TODO fberger also initialize here?
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
    public void request(FileDetails details, ContentResponseObserver observer, long timeout) {
        ContentResponseData response = getResponse(details);
        if(response != null || !ContentSettings.isManagementActive()) {
            if(LOG.isDebugEnabled())
                LOG.debug("Immediate response for URN: " + details.getSHA1Urn());
            observer.handleResponse(details.getSHA1Urn(), response);
        } else {
            if(LOG.isDebugEnabled())
            	LOG.debug("Scheduling request for URN: " + details.getSHA1Urn());
            scheduleRequest(details, observer, timeout);
        }
    }
    
    /**
     * Does a request, blocking until a response is given or the request times out.
     */
    public ContentResponseData request(FileDetails details, long timeout) {
        Validator validator = new Validator();
        synchronized(validator) {
            request(details, validator, timeout);
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
    protected void scheduleRequest(FileDetails details, ContentResponseObserver observer, long timeout) {
        long now = System.currentTimeMillis();
        URN urn = details.getSHA1Urn();
        addResponder(new Responder(now, timeout, observer, urn));

        // only send if we haven't already requested, check authority
        // afterwards so request is stored in any case
        if (REQUESTED.put(urn, details) == null && authority != null) {
            if(LOG.isDebugEnabled())
                LOG.debug("Sending request for URN: " + details.getSHA1Urn() + " to authority: " + authority);
            authority.sendAuthorizationRequest(details, timeout);
        } else if(LOG.isDebugEnabled())
            LOG.debug("Not sending request. Already requested or no authority.");
    }
    
    /**
     * Notification that a ContentResponse was given.
     */
    public void handleContentResponse(ContentResponse responseMsg) {
        URN urn = responseMsg.getURN();
        // Only process if we requested this msg.
        // (Don't allow arbitrary responses to be processed)
        if (REQUESTED.remove(urn) != null) {
            ContentResponseData response = new ContentResponseData(responseMsg);
            CACHE.addResponse(urn, response);
            if(LOG.isDebugEnabled())
                LOG.debug("Adding response (" + response + ") for URN: " + urn);
    
            Collection<Responder> responders = OBSERVERS.remove(urn);
            if(responders != null) {
                removeResponders(responders);
                for(Responder next : responders)
                    next.observer.handleResponse(next.urn, response);
            }
        } else if(LOG.isWarnEnabled()) {
            if(urn == null) {
                LOG.debug("No URN in response: " + responseMsg);
            } else {
                LOG.debug("Didn't request URN: " + urn + ", msg: " + responseMsg);
            }
        }
    }
    
    /**
     * Removes all responders from RESPONDERS.
     */
    protected void removeResponders(Collection<Responder> responders) {
        int size = responders.size();
        int removed = 0;
        synchronized(RESPONDERS) {
            for(int i = RESPONDERS.size() - 1; i >= 0; i--) {
                Responder next = RESPONDERS.get(i);
                if(responders.contains(next)) {
                    RESPONDERS.remove(i);
                    removed++;
                }
                
                // optimization: stop early.
                if(removed == size)
                    break;
            }
        }
        
        if(removed != size)
            LOG.warn("unable to remove all responders");
    }
    
    /**
     * Adds a given responder into the list of responders that need to be told
     * when a response comes in.
     * 
     * @param responder
     */
    protected void addResponder(Responder responder) {
        synchronized(OBSERVERS) {
        	URN urn = responder.urn;
            Collection<Responder> observers = OBSERVERS.get(urn);
            if(observers == null)
                observers = new HashSet<Responder>();
            observers.add(responder);
            OBSERVERS.put(urn, observers);
            
            if(responder.dead != 0)
                addForTimeout(responder);
        }
    }
    
    /** Adds a responder into the correct place in the sorted Responders list. */
    protected void addForTimeout(Responder responder) {
        synchronized (RESPONDERS) {
            if (RESPONDERS.isEmpty()) {
                RESPONDERS.add(responder);
            } else if (responder.dead <= RESPONDERS.get(RESPONDERS.size() - 1).dead) {
                RESPONDERS.add(responder);
            } else {
                // Quick lookup.
                int insertion = Collections.binarySearch(RESPONDERS, responder);
                if (insertion < 0)
                    insertion = (insertion + 1) * -1;
                RESPONDERS.add(insertion, responder);
            }
        }
    }
    
    /** Times out old responders. */
    protected void timeout(long now) {
        List<Responder> responders = null;
        synchronized(RESPONDERS) {
            Responder next = null;
            for(int i = RESPONDERS.size() - 1; i >= 0; i--) {
                next = RESPONDERS.get(i);
                if(next.dead <= now) {
                    REQUESTED.remove(next.urn);
                    TIMEOUTS.add(next.urn);
                    if(responders == null)
                        responders = new ArrayList<Responder>(2);
                    responders.add(next);
                    RESPONDERS.remove(i);
                    next = null;
                } else {
                    break;
                }
            }
        }
        
        // Now call outside of lock.
        if (responders != null) {
            for (int i = 0; i < responders.size(); i++) {
                Responder next = responders.get(i);
                if (LOG.isDebugEnabled())
                    LOG.debug("Timing out responder: " + next + " for URN: " + next.urn);
                try {
                    next.observer.handleResponse(next.urn, null);
                } catch (Throwable t) {
                    ErrorService.error(t, "Content ContentResponseData Error");
                }
            }
        }
    }
    
    /**
     * Starts the thread that does the timeout stuff & sets the content authority.
     * The content authority is attempted to be set here instead of outside this
     * thread because looking up the DNS name can block.
     */
    protected void startProcessingThread() {
        Thread timeouter = new ManagedThread(new Runnable() {
            public void run() {
                // if no existing authority, try and make one.
                if(authority == null)
                    setDefaultContentAuthority();
                
                while(true) {
                    if(shutdown)
                        return;
                    try {
                        try {
                            Thread.sleep(1000);
                        } catch(InterruptedException ix) {}
                        if(!shutdown)
                            timeout(System.currentTimeMillis());
                    } catch(Throwable t) {
                        ErrorService.error(t);
                    }
                }
            }
        }, "ContentProcessor");
        timeouter.setDaemon(true);
        timeouter.start();
    }    
    
    /**
     * Gets the default content authority.
     */
    protected ContentAuthority getDefaultContentAuthority() {
        return new SettingsBasedContentAuthority();
    }
    
    /** Sets the content authority with the default & process all pre-requested items. */
    private void setDefaultContentAuthority() {
        ContentAuthority auth = getDefaultContentAuthority();
        if(auth.initialize()) {
            // if we have an authority to set, grab all pre-requested items,
            // set the authority (so newly requested ones will immediately send to it),
            // and then send off those requested.
            // note that the timeouts on processing older requests will be lagging slightly.
        	if (!REQUESTED.isEmpty()) {
        		List<FileDetails> alreadyReq = new ArrayList<FileDetails>(REQUESTED.size());
        		synchronized(REQUESTED) {
        			alreadyReq.addAll(REQUESTED.values());
        			setContentAuthority(auth);
        		}

        		// process outside of lock
        		for (FileDetails details : alreadyReq) {
        			if(LOG.isDebugEnabled())
        				LOG.debug("Sending delayed request for URN: " + details + " to: " + auth);
        			// TODO fberger timeout... check
        			auth.sendAuthorizationRequest(details, 0);
        		}
            } else {
        		synchronized(REQUESTED) {
        			setContentAuthority(auth);
        		}
            }
        } else {
        	// FIXME
        	System.err.println("Initialization of content authority failed!");
        }
    }
    
    /**
     * A simple struct to allow ResponseObservers to be timed out.
     */
    private static class Responder implements Comparable<Responder> {
        private final long dead;
        private final ContentResponseObserver observer;
        
        private final URN urn;
                
        Responder(long now, long timeout, ContentResponseObserver observer, 
                URN urn) {
            
            if (timeout != 0) {
                this.dead = now + timeout;
            } else {
                this.dead = 0;
            }
            
            this.observer = observer;
            this.urn = urn;
        }

        public int compareTo(Responder o) {
            return dead < o.dead ? 1 : dead > o.dead ? -1 : 0;
        }
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

    private class ContentResponseHandler implements ContentResponseObserver {

		public void handleResponse(URN urn, ContentResponseData response) {
	        // Only process if we requested this msg.
	        // (Don't allow arbitrary responses to be processed)
	        if(REQUESTED.remove(urn) != null) {
	        	CACHE.addResponse(urn, response);
	        	if(LOG.isDebugEnabled())
	        		LOG.debug("Adding response (" + response + ") for URN: " + urn);
	    
	            Collection<Responder> responders = OBSERVERS.remove(urn);
	            if(responders != null) {
	                removeResponders(responders);
	                for(Responder next : responders)
	                    next.observer.handleResponse(next.urn, response);
	            }
	        } else if(LOG.isWarnEnabled()) {
	        	// TODO fberger update warnings
	            if(urn == null) {
	                LOG.debug("No URN in response: " + response);
	            } else {
	                LOG.debug("Didn't request URN: " + urn + ", msg: " + response);
	            }
	        }
		}
    	
    }
}
