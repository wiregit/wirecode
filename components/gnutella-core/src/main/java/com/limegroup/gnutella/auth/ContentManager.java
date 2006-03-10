package com.limegroup.gnutella.auth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.vendor.ContentRequest;
import com.limegroup.gnutella.messages.vendor.ContentResponse;
import com.limegroup.gnutella.settings.ContentSettings;
import com.limegroup.gnutella.util.ManagedThread;

/**
 * Keeps track of content requests & responses.
 */
public class ContentManager {
    
    private static final Log LOG = LogFactory.getLog(ContentManager.class);
    
    /** Map of SHA1 to Observers listening for responses to the SHA1. */
    private final Map /* URN -> List (of Responder) */ OBSERVERS = Collections.synchronizedMap(new HashMap());
    
    /** List of Responder's that are currently waiting, in order of timeout. */
    private final List RESPONDERS = new ArrayList();
    
    /** Set or URNs that we've already requested. */
    private final Set REQUESTED = Collections.synchronizedSet(new HashSet());
    
    /** Set of URNs that have failed requesting. */
    private final Set TIMEOUTS = Collections.synchronizedSet(new HashSet());
    
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
    
    /** Sets the content authority. */
    public void setContentAuthority(ContentAuthority authority) {
        this.authority = authority;
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
     * Determines if the given URN is valid.
     * 
     * @param urn
     * @param observer
     * @param timeout
     */
    public void request(URN urn, ContentResponseObserver observer, long timeout) {
        ContentResponseData response = CACHE.getResponse(urn);
        if(response != null || !ContentSettings.isManagementActive()) {
            if(LOG.isDebugEnabled())
                LOG.debug("Immediate response for URN: " + urn);
            observer.handleResponse(urn, response);
        } else {
            if(LOG.isDebugEnabled())
                LOG.debug("Scheduling request for URN: " + urn);
            scheduleRequest(urn, observer, timeout);
        }
    }
    
    /**
     * Does a request, blocking until a response is given or the request times out.
     */
    public ContentResponseData request(URN urn, long timeout) {
        Validator validator = new Validator();
        synchronized(validator) {
            request(urn, validator, timeout);
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
    
    /**
     * Schedules a request for the given URN, timing out in the given timeout.
     * 
     * @param urn
     * @param observer
     * @param timeout
     */
    protected void scheduleRequest(URN urn, ContentResponseObserver observer, long timeout) {
        long now = System.currentTimeMillis();
        addResponder(new Responder(now, timeout, observer, urn));

        // only send if we haven't already requested.
        if (REQUESTED.add(urn) && authority != null) {
            if(LOG.isDebugEnabled())
                LOG.debug("Sending request for URN: " + urn + " to authority: " + authority);
            authority.send(new ContentRequest(urn));
        } else if(LOG.isDebugEnabled())
            LOG.debug("Not sending request.  No authority or already requested.");
    }
    
    /**
     * Notification that a ContentResponse was given.
     */
    public void handleContentResponse(ContentResponse responseMsg) {
        URN urn = responseMsg.getURN();
        // Only process if we requested this msg.
        // (Don't allow arbitrary responses to be processed)
        if(urn != null && REQUESTED.remove(urn)) {
            ContentResponseData response = new ContentResponseData(responseMsg);
            CACHE.addResponse(urn, response);
            if(LOG.isDebugEnabled())
                LOG.debug("Adding response (" + response + ") for URN: " + urn);
    
            Collection responders = (Collection)OBSERVERS.remove(urn);
            if(responders != null) {
                removeResponders(responders);
                for(Iterator i = responders.iterator(); i.hasNext(); ) {
                    Responder next = (Responder)i.next();
                    next.observer.handleResponse(next.urn, response);
                }
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
    protected void removeResponders(Collection responders) {
        int size = responders.size();
        int removed = 0;
        synchronized(RESPONDERS) {
            for(int i = RESPONDERS.size() - 1; i >= 0; i--) {
                Responder next = (Responder)RESPONDERS.get(i);
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
            Set observers = (Set)OBSERVERS.get(responder.urn);
            if(observers == null)
                observers = new HashSet();
            observers.add(responder);
            OBSERVERS.put(responder.urn, observers);
            
            if(responder.dead != 0)
                addForTimeout(responder);
        }
    }
    
    /** Adds a responder into the correct place in the sorted Responders list. */
    protected void addForTimeout(Responder responder) {
        synchronized (RESPONDERS) {
            if (RESPONDERS.isEmpty()) {
                RESPONDERS.add(responder);
            } else if (responder.dead <= ((Responder) RESPONDERS.get(RESPONDERS.size() - 1)).dead) {
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
        List responders = null;
        synchronized(RESPONDERS) {
            Responder next = null;
            for(int i = RESPONDERS.size() - 1; i >= 0; i--) {
                next = (Responder)RESPONDERS.get(i);
                if(next.dead <= now) {
                    REQUESTED.remove(next.urn);
                    TIMEOUTS.add(next.urn);
                    if(responders == null)
                        responders = new ArrayList(2);
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
                Responder next = (Responder) responders.get(i);
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
            if (auth != null) {
                Set alreadyReq = new HashSet();
                synchronized(REQUESTED) {
                    alreadyReq.addAll(REQUESTED);
                    setContentAuthority(auth);
                }
                
                for(Iterator i = alreadyReq.iterator(); i.hasNext(); ) {
                    URN urn = (URN)i.next();
                    if(LOG.isDebugEnabled())
                        LOG.debug("Sending delayed request for URN: " + urn + " to: " + auth);
                    auth.send(new ContentRequest(urn));
                }
            }
        }
    }
    
    /**
     * A simple struct to allow ResponseObservers to be timed out.
     */
    private static class Responder implements Comparable {
        private final long dead;
        private final ContentResponseObserver observer;
        private final URN urn;
        
        Responder(long now, long timeout, ContentResponseObserver observer, URN urn) {
            if(timeout != 0)
                this.dead = now + timeout;
            else
                this.dead = 0;
            this.observer = observer;
            this.urn = urn;
        }

        public int compareTo(Object a) {
            Responder o = (Responder)a;
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
    
}
