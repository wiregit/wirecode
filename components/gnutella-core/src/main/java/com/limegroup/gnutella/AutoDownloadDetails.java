package com.limegroup.gnutella;

import com.limegroup.gnutella.util.*;
import java.util.*;

/** Encapsulates important details about a auto download....
 */
class AutoDownloadDetails {
    // the query associated with this search
    private String query = null;
    // the rich query associated with this search
    private String richQuery = null;
    // the 'filter' associated with this search
    private MediaType type = null;
    // the list of downloads made so far - should not exceed size
    // MAX_DOWNLOADS
    private List dlList = null;
    
    /** the size of the approx matcher 2d buffer...
     */
    private static final int MATCHER_BUF_SIZE = 120;
    /** this is used for matching of filenames.  kind of big so we only want
     *  one.
     */
    private static ApproximateMatcher matcher = 
        new ApproximateMatcher(MATCHER_BUF_SIZE);
    
    static {
        matcher.setIgnoreCase(true);
        matcher.setIgnoreWhitespace(true);
        matcher.setCompareBackwards(true);
    }
    
    
    // don't auto dl any more than this number of files....
    public static final int MAX_DOWNLOADS = 2;
    
    /**
     * @param inQuery the standard query string associated with this query.
     * @param inRichQuery the rich query associated with this string.
     * @param inType the mediatype associated with this string.....
     */
    public AutoDownloadDetails(String inQuery, String inRichQuery, 
                               MediaType inType) {
        query = inQuery;
        richQuery = inRichQuery;
        type = inType;
        dlList = new Vector();
    }
    
    public String getQuery() {
        return query;
    }
    
    public String getRichQuery() {
        return richQuery;
    }
    
    public MediaType getMediaType() {
        return type;
    }
    
    /**
     * @param toAdd The RFD you are TRYING to add.
     * @return Whether or not the add was successful. 
     */
    public synchronized boolean addDownload(RemoteFileDesc toAdd) {
        boolean retVal = true;
        
        if (!expired())
            dlList.add(toAdd);
        else 
            retVal = false;
        
        return retVal;
    }
    
    /** @return true when the AutoDownload process is complete.
     */
    public boolean expired() {
        boolean retVal = false;
        if (dlList.size() >= MAX_DOWNLOADS)
            retVal = true;
        return retVal;
    }
    
    
}


