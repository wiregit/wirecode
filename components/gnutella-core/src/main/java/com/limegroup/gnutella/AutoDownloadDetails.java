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
    
    /** the precision that the matcher uses for comparing candidates to RFDs
     *  that have already been accepted for download....
     */
    private float MATCH_PRECISION_DL = 0.45f;

    static {
        matcher.setIgnoreCase(true);
        matcher.setIgnoreWhitespace(true);
        matcher.setCompareBackwards(true);
    }
    
    
    // don't auto dl any more than this number of files....
    public static final int MAX_DOWNLOADS = 5;
    
    // keeps track of committed downloads....
    private int committedDLs = 0;

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
        // this is used not only as a return value but to control processing.
        // if it every turns false we just stop processing....
        boolean retVal = true;
        
        // if this hasn't become expired....
        if (!expired()) {
            final String inputFileName = toAdd.getFileName();

            // make sure the file ext is legit....
            if ((type != null) && !(type.matches(inputFileName))) {
                retVal = false;
                debug("ADD.addDownload(): file " +
                      inputFileName + " isn't the right type.");
            }


            // see if it compares to any other file already being DLed....
            if (retVal && (dlList.size() > 0)) {
                for (int i = 0; i < dlList.size(); i++) {
                    RemoteFileDesc currRFD = (RemoteFileDesc) dlList.get(i);
                    String currFileName = currRFD.getFileName();
                    boolean doesMatch = false;
                    synchronized (matcher) {
                        doesMatch = matcher.matches(inputFileName, currFileName,
                                                    MATCH_PRECISION_DL);
                    }
                    if (doesMatch) {
                        retVal = false;
                        debug("ADD.addDownload(): conflict for file " +
                              inputFileName + " and " + currFileName);
                    }
                    // oops, we have already accepted that file for DL, don't
                    // add it and break out of this costly loop....
                    if (!retVal)
                        break;
                }
            }

            if (retVal)
                dlList.add(toAdd);
        }
        else 
            retVal = false;
        
        return retVal;
    }

    /** Removes the input RFD from the list.  Use this if the DL failed and
     *  you want to back it out....
     */
    public synchronized void removeDownload(RemoteFileDesc toRemove) {
        dlList.remove(toRemove);
    }

    /** Call this when the DL was 'successful'.
     */
    public synchronized void commitDownload(RemoteFileDesc toCommit) {
        if (dlList.contains(toCommit))
            committedDLs++;
    }

    /** @return true when the AutoDownload process is complete.
     */
    public boolean expired() {
        boolean retVal = false;
        if (committedDLs >= MAX_DOWNLOADS)
            retVal = true;
        return retVal;
    }


    private static final boolean debugOn = false;
    private static void debug(String out) {
        if (debugOn)
            System.out.println(out);
    }
    private static void debug(Exception e) {
        if (debugOn)
            e.printStackTrace();
    }

    

    
    
}


