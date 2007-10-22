package com.limegroup.gnutella.downloader;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import org.limewire.collection.ApproximateMatcher;
import org.xml.sax.SAXException;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.MediaType;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.ResponseVerifierImpl;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;
import com.limegroup.gnutella.xml.SchemaNotFoundException;

/** 
 * Encapsulates important details about an auto download.  Serializable for 
 * downloads.dat file; be careful when modifying!
 */
public class AutoDownloadDetails implements Serializable {
    static final long serialVersionUID = 3400666689236195243L;

    // the query associated with this search
    private String query = null;
    // the rich query associated with this search
    private String richQuery = null;
    // the LimeXMLDocument of this rich query ... 
    // initialized when needed.
    private transient LimeXMLDocument xmlDoc = null;
    // flag of whether or not we've tried to create the doc.
    private transient boolean xmlCreated = false;
    // the 'filter' associated with this search
    private transient MediaType type = null;
    // the list of downloads made so far - should not exceed size
    // MAX_DOWNLOADS
    private List<RemoteFileDesc> dlList = null;
    
    /**
     * The description of the media type.
     */
    private String mediaDesc;
    
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
    private float MATCH_PRECISION_DL = .30f;

    /** the percentage of matching that invalidates a new file from being
     *  downloaded.  in other words, if a file matches on more than ~51% of
     *  words, then don't download it.
     */
    private float WORD_INCIDENCE_RATE = .509999f;

    /** what is considered to be a low score, compared to the return value of
     *  the score method...
     */
    private int LOW_SCORE = 95;


    /** the set of words that are already being downloaded.  this can be used
     *  as a heuristic when determining what to download.....
     */
    private Set<String> wordSet = null;

    static {
        matcher.setIgnoreCase(true);
        matcher.setIgnoreWhitespace(true);
        matcher.setCompareBackwards(true);
    }
    
    
    // don't auto dl any more than this number of files....
    public static final int MAX_DOWNLOADS = 1;
    
    // keeps track of committed downloads....
    private int committedDLs = 0;

    @Inject
    private static Provider<LimeXMLDocumentFactory> globalLimeXMLDocumentFactory;
    
    private transient volatile LimeXMLDocumentFactory limeXMLDocumentFactory;
    
    /**
     * @param inQuery the standard query string associated with this query.
     * @param inRichQuery the rich query associated with this string.
     * @param inType the mediatype associated with this string.....
     */
    public AutoDownloadDetails(String inQuery, String inRichQuery, 
                               MediaType inType) {
        limeXMLDocumentFactory = globalLimeXMLDocumentFactory.get();
        query = inQuery;
        richQuery = inRichQuery;
        type = inType;
        if(type != null)
            mediaDesc = type.getMimeType();
        else
            mediaDesc = null;
        dlList = new Vector<RemoteFileDesc>();
        wordSet = new HashSet<String>();
    }
    
    /**
     * Extended to set the media type.
     */
    private void readObject(ObjectInputStream stream) throws IOException,
                                                    ClassNotFoundException {
        limeXMLDocumentFactory = globalLimeXMLDocumentFactory.get();
        stream.defaultReadObject();

        if(mediaDesc == null)
            type = MediaType.getAnyTypeMediaType();
        else
            type = MediaType.getMediaTypeForSchema(mediaDesc);
        if(type == null)
            type = MediaType.getAnyTypeMediaType();
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
            }

            // create our xml doc if we need to...
            if( !xmlCreated ) {
                xmlCreated = true;
                if( richQuery != null && !richQuery.equals("") ) {
                    try {
                        xmlDoc = limeXMLDocumentFactory
                        .createLimeXMLDocument(richQuery);
                    } catch(SchemaNotFoundException ignored) {
                    } catch(SAXException ignored) {
                    } catch(IOException ignored) {
                    }
                }
            }
            // make sure the score for this file isn't too low....
            // TODO: DPINJ: fix this 
            int score = ResponseVerifierImpl.score(query, xmlDoc, toAdd);
            if (score < LOW_SCORE) {
                retVal = false;
            }

            // check to see there is a high incidence of words here in stuff we
            // are already downloading....
            if (retVal && (wordSet.size() > 0)) {
                StringTokenizer st = 
                new StringTokenizer(ripExtension(inputFileName),
                                    FileManager.DELIMITERS);
                int additions = 0;
                final int numTokens = st.countTokens();
                while (st.hasMoreTokens()) {
                    String currToken = st.nextToken().toLowerCase();
                    if (!wordSet.contains(currToken)) 
                        additions++;
                }
                float matchRate = 
                ((float)(numTokens - additions)/
                 (float)wordSet.size());
                if ((additions == 0) || 
                    (matchRate > WORD_INCIDENCE_RATE)) {
                    retVal = false;
                }
            }

            // see if it compares to any other file already being DLed....
            if (retVal && (dlList.size() > 0)) {
                String processedFileName;
                synchronized (matcher) {
                    processedFileName = matcher.process(inputFileName);
                }
                for (int i = 0; i < dlList.size(); i++) {
                    RemoteFileDesc currRFD = dlList.get(i);
                    String currFileName = currRFD.getFileName();
                    String currProcessedFileName;
                    int diffs = 0;
                    synchronized (matcher) {
                        currProcessedFileName = matcher.process(currFileName);
                        diffs = matcher.match(processedFileName,
                                              currProcessedFileName);
                    }
                    int smaller = Math.min(processedFileName.length(),
                                           currProcessedFileName.length());
                    if (((float)diffs)/((float)smaller) < MATCH_PRECISION_DL) {
                        retVal = false;
                    }

                    // oops, we have already accepted that file for DL, don't
                    // add it and break out of this costly loop....
                    if (!retVal)
                        break;
                }
            }

            // ok, all processing passed, add this...
            if (retVal) {
                // used by the approx. matcher...
                dlList.add(toAdd);
                // used by my hashset comparator....
                StringTokenizer st = 
                new StringTokenizer(ripExtension(inputFileName),
                                    FileManager.DELIMITERS);
                while (st.hasMoreTokens())
                    wordSet.add(st.nextToken().toLowerCase());
            }
        }
        else 
            retVal = false;
        return retVal;
    }

    /** Removes the input RFD from the list.  Use this if the DL failed and
     *  you want to back it out....
     */
    public synchronized void removeDownload(RemoteFileDesc toRemove) {
        // used by the approx. matcher...
        dlList.remove(toRemove);
        // used by the hashset comparator....
        // technically, this is bad.  i'm doing it because in practice this will
        // decrease the amount of downloads, which isn't horrible.  also, i
        // don't see a download being removed very frequently.  if i want i can
        // move to a new set which keeps a count for each element of the set and
        // only discards after the appropriate amt. of removes....
        StringTokenizer st = 
        new StringTokenizer(ripExtension(toRemove.getFileName()),
                            FileManager.DELIMITERS);
        while (st.hasMoreTokens())
            wordSet.remove(st.nextToken().toLowerCase());
        
    }

    /** Call this when the DL was 'successful'.
     */
    public synchronized void commitDownload(RemoteFileDesc toCommit) {
        if (dlList.contains(toCommit))
            committedDLs++;
    }

    /** @return true when the AutoDownload process is complete.
     */
    public synchronized boolean expired() {
        boolean retVal = false;
        if (committedDLs >= MAX_DOWNLOADS)
            retVal = true;
        return retVal;
    }


    // take the extension off the filename...
    private String ripExtension(String fileName) {
        String retString = null;
        int extStart = fileName.lastIndexOf('.');
        if (extStart == -1)
            retString = fileName;
        else
            retString = fileName.substring(0, extStart);
        return retString;
    }
    
}


