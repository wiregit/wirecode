package com.limegroup.gnutella.filters;

import java.io.IOException;
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
        String[] urnStrings = FilterSettings.BLOCKED_URNS.getValue();
        for (int i=0; i<urnStrings.length; i++) {
            URN urn;
            try {
                urn = URN.createSHA1Urn(urnStrings[i]);
                _blockedURNs.add(urn);
            } catch(IOException exception) {
                //ignore
            } 
        }
    }

    /**
     * Returns whether the given URN string should be allowed, 
     * public for testing purposes
     */
    public boolean allow(URN urn) {
        return !_blockedURNs.contains(urn);
    }

    /**
     * Returns whether the given Response should be allowed
     */
    public synchronized boolean allow(Response m) {
        Collection urns = m.getUrns();
        Iterator i = urns.iterator();
        while(i.hasNext()) {
            URN urn = ((URN)i.next());
            if(!allow(urn)) return false;
        }
        return true;
    }
}



