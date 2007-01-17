package com.limegroup.gnutella;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.filters.IP;
import com.limegroup.gnutella.io.Shutdownable;

/**
 * A sanity checker for many different in-network verification
 * requests.
 * 
 * If we cannot verify the message as received from a number
 * of hosts from different areas, message the user to hit
 * the website as the installation may possible be corrupted.
 */
public class NetworkUpdateSanityChecker {
    
    public static enum RequestType {
        SIMPP, VERSION;
    }
    private static final Set<RequestType> allTypes = EnumSet.allOf(RequestType.class);
    
    private static final Log LOG = LogFactory.getLog(NetworkUpdateSanityChecker.class);
    
    private static final int MAXIMUM_FAILURES = 20;
    private static final String MASK = "/255.255.0.0";
    
    private final Map<ReplyHandler, Boolean> requests = new WeakHashMap<ReplyHandler, Boolean>();
    private final List<IP> failures = new ArrayList<IP>(MAXIMUM_FAILURES);
    private boolean finished = false;
    private Set<RequestType> successes = EnumSet.noneOf(RequestType.class);
    
    
    /**
     * Stores knowledge that we've requested a network-updatable component
     * from the given source.
     * 
     * @param handler
     */
    public void handleNewRequest(ReplyHandler handler, RequestType type) {
        if(LOG.isDebugEnabled())
            LOG.debug("Adding request to handler: " + handler);
        
        synchronized(requests) {
            // We're done.
            if(finished) {
                LOG.debug("Already reached maximum failure point, ignoring.");
                return;
            }
            
            requests.put(handler, Boolean.TRUE);
        }
    }

    /**
     * Acknowledge we received a valid response from the source.
     * 
     * @param handler
     */
    public void handleValidResponse(ReplyHandler handler, RequestType type) {
        if(LOG.isDebugEnabled())
            LOG.debug("Received valid response from handler: " + handler + "of type: " + type);
        synchronized(requests) {
            if(!finished) {
                // If we've gotten all kinds of valid responses,
                // assume we're okay.
                successes.add(type);
                if(successes.containsAll(allTypes)) {
                    LOG.debug("Received every kind of success!");
                    finished = true;
                    requests.clear();
                    failures.clear();
                } else {
                    // Forget about this handler.
                    requests.remove(handler);
                }
            }
        }
    }
    
    /**
     * Acknowledge we received an invalid response from the source.
     * 
     * @param handler
     */
    public void handleInvalidResponse(ReplyHandler handler, RequestType type) {
        synchronized(requests) {
            // If we had a request for this handler...
            if(!finished && requests.remove(handler) != null) {
                if(LOG.isDebugEnabled())
                    LOG.debug("Had a request from handler, adding as failure... " + handler);
                
                IP ip = new IP(handler.getAddress() + MASK);
                boolean contained = false;
                // See if we already had a failure in this range, if so
                // then ignore it.
                for(IP current : failures) {
                    if(current.contains(ip)) {
                        if(LOG.isDebugEnabled())
                            LOG.debug("Already had a failure from range: " +    
                                      current + ", ignoring handler ip: " + ip);
                        contained = true;
                        break;
                    }
                }
                
                if(!contained) {
                    // If no failure already here, add it.
                    failures.add(ip);
                    if(LOG.isDebugEnabled())
                        LOG.debug("Current failure size: " + failures.size());
                    
                    if(failures.size() == MAXIMUM_FAILURES) {
                        LOG.debug("Reached failure threshold!");
                        finished = true;
                        requests.clear();
                        failures.clear();
                        RouterService.getCallback().installationCorrupted();
                    }
                }
            }
        }
        
        if(handler instanceof Shutdownable)
            ((Shutdownable)handler).shutdown();
    }
    

}
