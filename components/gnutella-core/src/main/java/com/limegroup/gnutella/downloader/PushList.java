package com.limegroup.gnutella.downloader;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.util.NetworkUtils;

/** Keeps track of who needs a push, and who should be notified of of success or failure. */
public class PushList {
    
    private static final Log LOG = LogFactory.getLog(PushList.class);

    /** Map of clientGUID -> List of potential pushes with that GUID. */
    private final TreeMap /* GUID -> List (of Push) */ pushers = new TreeMap(GUID.GUID_BYTE_COMPARATOR);
    
    /** Adds a host that wants to be notified of a push. */
    public void addPushHost(PushDetails details, HTTPConnectObserver observer) {
        if(LOG.isDebugEnabled())
            LOG.debug("Adding observer for details: " + details);
        synchronized(pushers) {
            byte[] clientGUID = details.getClientGUID();
            List perGUID = (List)pushers.get(clientGUID);
            if(perGUID == null) {
                perGUID = new LinkedList();
                pushers.put(clientGUID, perGUID);
            }
            perGUID.add(new Push(details, observer));
        }
    }

    /** Returns the exact observer that was added with this PushDetails. */
    public HTTPConnectObserver getExactHostFor(PushDetails details) {
        if(LOG.isDebugEnabled())
            LOG.debug("Retrieving exact match for details: " + details);
        
        synchronized(pushers) {
            byte[] clientGUID = details.getClientGUID();
            List perGUID = (List)pushers.get(clientGUID);
            if(perGUID == null) {
                LOG.debug("No pushes waiting on those exact details.");
                return null;
            } else {
                Push best = getExactHost(perGUID, details);
                if(perGUID.isEmpty())
                    pushers.remove(clientGUID);
                if(best != null)
                    return best.observer;
                else
                    return null;
            }
        }
    }
    
    /** Returns an HTTPConnectObserver for the given clientGUID & Socket. */
    public HTTPConnectObserver getHostFor(byte[] clientGUID, String address) {
        if(LOG.isDebugEnabled())
            LOG.debug("Retrieving best match for address: " + address + ", guid: " + new GUID(clientGUID));
        
        synchronized(pushers) {
            List perGUID = (List)pushers.get(clientGUID);
            if(perGUID == null) {
                LOG.debug("No pushes waiting on that GUID.");
                return null;
            } else {
                Push best = getBestHost(perGUID, address);
                if(perGUID.isEmpty())
                    pushers.remove(clientGUID);
                if(best != null)
                    return best.observer;
                else
                    return null;
            }
        }
    }
    
    /** Returns all existing HTTPConnectObservers and clears the list. */
    public List /* of HTTPConnectObserver */ getAllAndClear() {
        List allConnectors = new LinkedList();
        synchronized(pushers) {
            for(Iterator i = pushers.values().iterator(); i.hasNext(); ) {
                List list = (List)i.next();
                if(list != null) {
                    for(Iterator j = list.iterator(); j.hasNext(); ) {
                        Push next = (Push)j.next();
                        allConnectors.add(next.observer);
                    }
                }
            }
            pushers.clear();
        }
        return allConnectors;
    }
    
    /** Returns the first matching Push in the list, or a random one if none match. */
    private Push getBestHost(List hosts, String address) {
        if(hosts.isEmpty())
            return null;
        
        // First try to find one that exactly matches the IP address.
        for(Iterator i = hosts.iterator(); i.hasNext(); ) {
            Push next = (Push)i.next();
            if(next.details.getAddress().equals(address)) {
                LOG.debug("Found an exact match!");
                i.remove();
                return next;
            }
        }
        
        // Then try and find the first private address.
        LOG.debug("No exact match, using first private address.");
        for(Iterator i = hosts.iterator(); i.hasNext();) {
            Push next = (Push)i.next();
            if(NetworkUtils.isPrivateAddress(next.details.getAddress())) {
                i.remove();
                return next;
            }   
        }
        
        LOG.debug("No private address to use!");
        return null;
    }
    
    /** Returns the exact Push in the list. */
    private Push getExactHost(List hosts, PushDetails details) {
        if(hosts.isEmpty())
            return null;
        
        // First try to find one that exactly matches the IP address.
        for(Iterator i = hosts.iterator(); i.hasNext(); ) {
            Push next = (Push)i.next();
            if(next.details.equals(details)) {
                i.remove();
                return next;
            }
        }
        
        LOG.debug("No exact match!");
        return null;
    }    
    
    /** A push-type struct. */
    private static class Push {
        private final PushDetails details;
        private final HTTPConnectObserver observer;
        
        Push(PushDetails details, HTTPConnectObserver observer) {
            this.details = details;
            this.observer = observer;
        }
        
    }
}
