package com.limegroup.gnutella.filters;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.settings.FilterSettings;

/**
 * A filter to eliminate unwanted files ("Reponses") from QueryResults
 * based on files' URN.
 */

public final class URNResponseFilter extends ResponseFilter {    
    private Set _blockedURNs = new HashSet();

    /**
     * Constructor only called by ReponseFilter
     */
    URNResponseFilter(){        
        refresh();
    }

    /**
     * Refresh this filter based on the files specified for blocking
     * in FilterSettings.
     */
    public synchronized void refresh() {
        _blockedURNs = new HashSet();
        String[] urns = FilterSettings.BLOCKED_URNS.getValue();
        for (int i=0; i<urns.length; i++)
            _blockedURNs.add(urns[i]);
    }

    /**
     * Returns whether the given URN string should be allowed, 
     * public for testing purposes
     */
    public boolean allow(String urn) {
        return !_blockedURNs.contains(urn);
    }

    /**
     * Returns whether the given Response should be allowed
     */
    public synchronized boolean allow(Response m) {
        Collection urns = m.getUrns();
        Iterator i = urns.iterator();
        while(i.hasNext()) {
            String urn = ((URN)i.next()).toString();
            if(!allow(urn)) return false;
        }
        return true;
    }
}



