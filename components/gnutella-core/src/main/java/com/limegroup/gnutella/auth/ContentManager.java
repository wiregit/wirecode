/**
 * 
 */
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
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.vendor.ContentRequest;
import com.limegroup.gnutella.messages.vendor.ContentResponse;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.ManagedThread;

/**
 * Keeps track of content requests & responses.
 */
public class ContentManager {
    
    private static final Log LOG = LogFactory.getLog(ContentManager.class);
    
    
    /** Map of SHA1 to Observers listening for responses to the SHA1. */
    private final Map /* URN -> List (of Responder) */ OBSERVERS = Collections.synchronizedMap(new HashMap());
    
    /** Map of SHA1 to Responses we already know about for this file. */
    private final Map /* Urn -> Response */ RESPONSES = Collections.synchronizedMap(new HashMap());
    
    /** List of Responder's that are currently waiting, in order of timeout. */
    private final List RESPONDERS = new ArrayList();
    
    /** Wehther or not we're shutting down. */
    private volatile boolean shutdown = false;
    
    /**
     * Initializes this content manager.
     */
    public void initialize() {
        buildResponses(RESPONSES);
        startTimeoutThread();
    }
    
    /**
     * Shuts down this ContentManager.
     */
    public void shutdown() {
        shutdown = true;
        writeResponses(RESPONSES);
    }
    
    /**
     * Determines if the given URN is valid.
     * 
     * @param urn
     * @param observer
     * @param timeout
     */
    public void request(URN urn, ResponseObserver observer, long timeout) {
        Response response = (Response)RESPONSES.get(urn);
        if(response != null) {
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
     * Gets a response if one exists.
     */
    public Response getResponse(URN urn) {
        return (Response)RESPONSES.get(urn);
    }
    
    /**
     * Schedules a request for the given URN, timing out in the given timeout.
     * 
     * @param urn
     * @param observer
     * @param timeout
     */
    protected void scheduleRequest(URN urn, ResponseObserver observer, long timeout) {
        IpPort authority = getContentAuthority();
        long now = System.currentTimeMillis();
        addResponder(new Responder(now, timeout, observer, urn));
        if (authority != null ) {
            UDPService.instance().send(new ContentRequest(urn), authority);
        }
    }
    
    /**
     * Notification that a ContentResponse was given.
     */
    public void handleContentResponse(ContentResponse responseMsg) {
        URN urn = responseMsg.getURN();
        if(urn != null) {
            Response response = new Response(responseMsg);
            RESPONSES.put(urn, response);
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
        } else if(LOG.isWarnEnabled())
            LOG.warn("No URN in response: " + responseMsg);
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
            } else {
                if (responder.dead >= ((Responder)RESPONDERS.get(RESPONDERS.size() - 1)).dead) {
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
    }
    
    /** Times out old responders. */
    protected void timeout(long now) {
        synchronized(RESPONDERS) {
            Responder next = null;
            for(int i = RESPONDERS.size() - 1; i >= 0; i--) {
                next = (Responder)RESPONDERS.get(i);
                if(next.dead <= now) {
                    if(LOG.isDebugEnabled())
                        LOG.debug("Timing out responder: " + next + " for URN: " + next.urn);
                    try {
                        next.observer.handleResponse(next.urn, null);
                    } catch(Throwable t) {
                        ErrorService.error(t, "Content Response Error");
                    }
                    RESPONDERS.remove(i);
                    next = null;
                } else {
                    break;
                }
            }
        }
    }
    
    /** Starst the thread that does the timeout stuff. */
    protected void startTimeoutThread() {
        Thread timeouter = new ManagedThread(new Runnable() {
            public void run() {
                while(true) {
                    if(shutdown)
                        return;
                    try {
                        try {
                            Thread.sleep(2000);
                        } catch(InterruptedException ix) {}
                        if(!shutdown)
                            timeout(System.currentTimeMillis());
                    } catch(Throwable t) {
                        ErrorService.error(t);
                    }
                }
            }
        }, "ContentTimeout");
        timeouter.setDaemon(true);
        timeouter.start();
    }    
    
    /**
     * Gets the content authority.
     */
    private IpPort getContentAuthority() {
        return null; // INSERT CONTENT AUTHORITY HERE
    }
    
    /**
     * Builds a map of responses from data cached on disk. 
     * 
     * @return
     */
    private void buildResponses(Map responses) {
        // TODO: read data from disk.
    }
    
    /**
     * Writes the given map out to disk.
     * 
     * @param responses
     */
    private void writeResponses(Map responses) {
        // TODO: write responses to disk.
    }
    
    /**
     * A simple struct to allow ResponseObservers to be timed out.
     */
    private static class Responder {
        private final long dead;
        private final ResponseObserver observer;
        private final URN urn;
        
        Responder(long now, long timeout, ResponseObserver observer, URN urn) {
            if(timeout != 0)
                this.dead = now + timeout;
            else
                this.dead = 0;
            this.observer = observer;
            this.urn = urn;
        }

        public int compareTo(Object a) {
            Responder o = (Responder)a;
            return dead > o.dead ? 1 : dead < o.dead ? -1 : 0;
        }
    }
    
}
