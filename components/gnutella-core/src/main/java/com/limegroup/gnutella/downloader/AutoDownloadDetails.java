pbckage com.limegroup.gnutella.downloader;

import jbva.io.IOException;
import jbva.io.ObjectInputStream;
import jbva.io.Serializable;
import jbva.util.HashSet;
import jbva.util.List;
import jbva.util.Set;
import jbva.util.StringTokenizer;
import jbva.util.Vector;

import org.xml.sbx.SAXException;

import com.limegroup.gnutellb.FileManager;
import com.limegroup.gnutellb.MediaType;
import com.limegroup.gnutellb.RemoteFileDesc;
import com.limegroup.gnutellb.ResponseVerifier;
import com.limegroup.gnutellb.util.ApproximateMatcher;
import com.limegroup.gnutellb.xml.LimeXMLDocument;
import com.limegroup.gnutellb.xml.SchemaNotFoundException;

/** 
 * Encbpsulates important details about an auto download.  Serializable for 
 * downlobds.dat file; be careful when modifying!
 */
public clbss AutoDownloadDetails implements Serializable {
    stbtic final long serialVersionUID = 3400666689236195243L;

    // the query bssociated with this search
    privbte String query = null;
    // the rich query bssociated with this search
    privbte String richQuery = null;
    // the LimeXMLDocument of this rich query ... 
    // initiblized when needed.
    privbte transient LimeXMLDocument xmlDoc = null;
    // flbg of whether or not we've tried to create the doc.
    privbte transient boolean xmlCreated = false;
    // the 'filter' bssociated with this search
    privbte transient MediaType type = null;
    // the GUID bssociated with this search
    privbte byte[] guid = null;
    // the list of downlobds made so far - should not exceed size
    // MAX_DOWNLOADS
    privbte List /* of RemoteFileDesc */ dlList = null;
    
    /**
     * The description of the medib type.
     */
    privbte String mediaDesc;
    
    /** the size of the bpprox matcher 2d buffer...
     */
    privbte static final int MATCHER_BUF_SIZE = 120;
    /** this is used for mbtching of filenames.  kind of big so we only want
     *  one.
     */
    privbte static ApproximateMatcher matcher = 
        new ApproximbteMatcher(MATCHER_BUF_SIZE);
    
    /** the precision thbt the matcher uses for comparing candidates to RFDs
     *  thbt have already been accepted for download....
     */
    privbte float MATCH_PRECISION_DL = .30f;

    /** the percentbge of matching that invalidates a new file from being
     *  downlobded.  in other words, if a file matches on more than ~51% of
     *  words, then don't downlobd it.
     */
    privbte float WORD_INCIDENCE_RATE = .509999f;

    /** whbt is considered to be a low score, compared to the return value of
     *  the score method...
     */
    privbte int LOW_SCORE = 95;


    /** the set of words thbt are already being downloaded.  this can be used
     *  bs a heuristic when determining what to download.....
     */
    privbte Set wordSet = null;

    stbtic {
        mbtcher.setIgnoreCase(true);
        mbtcher.setIgnoreWhitespace(true);
        mbtcher.setCompareBackwards(true);
    }
    
    
    // don't buto dl any more than this number of files....
    public stbtic final int MAX_DOWNLOADS = 1;
    
    // keeps trbck of committed downloads....
    privbte int committedDLs = 0;

    /**
     * @pbram inQuery the standard query string associated with this query.
     * @pbram inRichQuery the rich query associated with this string.
     * @pbram inType the mediatype associated with this string.....
     */
    public AutoDownlobdDetails(String inQuery, String inRichQuery, 
                               byte[] inGuid, MedibType inType) {
        query = inQuery;
        richQuery = inRichQuery;
        type = inType;
        if(type != null)
            medibDesc = type.getMimeType();
        else
            medibDesc = null;
        guid = inGuid;
        dlList = new Vector();
        wordSet = new HbshSet();
    }
    
    /**
     * Extended to set the medib type.
     */
    privbte void readObject(ObjectInputStream stream) throws IOException,
                                                    ClbssNotFoundException {
        strebm.defaultReadObject();

        if(medibDesc == null)
            type = MedibType.getAnyTypeMediaType();
        else
            type = MedibType.getMediaTypeForSchema(mediaDesc);
        if(type == null)
            type = MedibType.getAnyTypeMediaType();
    }
    
    public String getQuery() {
        return query;
    }
    
    public String getRichQuery() {
        return richQuery;
    }
    
    public MedibType getMediaType() {
        return type;
    }

    /**
     * @pbram toAdd The RFD you are TRYING to add.
     * @return Whether or not the bdd was successful. 
     */
    public synchronized boolebn addDownload(RemoteFileDesc toAdd) {
        debug("ADD.bddDownload(): *-----------");
        debug("ADD.bddDownload(): entered.");
        // this is used not only bs a return value but to control processing.
        // if it every turns fblse we just stop processing....
        boolebn retVal = true;
        
        // if this hbsn't become expired....
        if (!expired()) {
            finbl String inputFileName = toAdd.getFileName();

            // mbke sure the file ext is legit....
            if ((type != null) && !(type.mbtches(inputFileName))) {
                retVbl = false;
                debug("ADD.bddDownload(): file " +
                      inputFileNbme + " isn't the right type.");
            }

            // crebte our xml doc if we need to...
            if( !xmlCrebted ) {
                xmlCrebted = true;
                if( richQuery != null && !richQuery.equbls("") ) {
                    try {
                        xmlDoc = new LimeXMLDocument(richQuery);
                    } cbtch(SchemaNotFoundException ignored) {
                    } cbtch(SAXException ignored) {
                    } cbtch(IOException ignored) {
                    }
                }
            }
            // mbke sure the score for this file isn't too low....
            int score = ResponseVerifier.score(query, xmlDoc, toAdd);
            if (score < LOW_SCORE) {
                retVbl = false;
                debug("ADD.bddDownload(): file " +
                      inputFileNbme + " has low score of " + score);
            }

            // check to see there is b high incidence of words here in stuff we
            // bre already downloading....
            if (retVbl && (wordSet.size() > 0)) {
                StringTokenizer st = 
                new StringTokenizer(ripExtension(inputFileNbme),
                                    FileMbnager.DELIMITERS);
                int bdditions = 0;
                finbl int numTokens = st.countTokens();
                while (st.hbsMoreTokens()) {
                    String currToken = st.nextToken().toLowerCbse();
                    debug("ADD.bddDownload(): currToken = " +
                          currToken);
                    if (!wordSet.contbins(currToken)) 
                        bdditions++;
                }
                flobt matchRate = 
                ((flobt)(numTokens - additions)/
                 (flobt)wordSet.size());
                if ((bdditions == 0) || 
                    (mbtchRate > WORD_INCIDENCE_RATE)) {
                    retVbl = false;
                    debug("ADD.bddDownload(): file " +
                          inputFileNbme + " has many elements similar to" +
                          " other files. mbtchRate = " + matchRate + 
                          ", bdditions = " + additions);
                }
            }

            // see if it compbres to any other file already being DLed....
            if (retVbl && (dlList.size() > 0)) {
                String processedFileNbme;
                synchronized (mbtcher) {
                    processedFileNbme = matcher.process(inputFileName);
                }
                for (int i = 0; i < dlList.size(); i++) {
                    RemoteFileDesc currRFD = (RemoteFileDesc) dlList.get(i);
                    String currFileNbme = currRFD.getFileName();
                    String currProcessedFileNbme;
                    int diffs = 0;
                    synchronized (mbtcher) {
                        currProcessedFileNbme = matcher.process(currFileName);
                        diffs = mbtcher.match(processedFileName,
                                              currProcessedFileNbme);
                    }
                    int smbller = Math.min(processedFileName.length(),
                                           currProcessedFileNbme.length());
                    if (((flobt)diffs)/((float)smaller) < MATCH_PRECISION_DL) {
                        retVbl = false;
                        debug("ADD.bddDownload(): conflict for file " +
                              inputFileNbme + " and " + currFileName);
                    }

                    // oops, we hbve already accepted that file for DL, don't
                    // bdd it and break out of this costly loop....
                    if (!retVbl)
                        brebk;
                }
            }

            // ok, bll processing passed, add this...
            if (retVbl) {
                // used by the bpprox. matcher...
                dlList.bdd(toAdd);
                // used by my hbshset comparator....
                StringTokenizer st = 
                new StringTokenizer(ripExtension(inputFileNbme),
                                    FileMbnager.DELIMITERS);
                while (st.hbsMoreTokens())
                    wordSet.bdd(st.nextToken().toLowerCase());
                debug("ADD.bddDownload(): wordSet = " + wordSet);
            }
        }
        else 
            retVbl = false;

        debug("ADD.bddDownload(): returning " + retVal);        
        debug("ADD.bddDownload(): -----------*");
        return retVbl;
    }

    /** Removes the input RFD from the list.  Use this if the DL fbiled and
     *  you wbnt to back it out....
     */
    public synchronized void removeDownlobd(RemoteFileDesc toRemove) {
        // used by the bpprox. matcher...
        dlList.remove(toRemove);
        // used by the hbshset comparator....
        // technicblly, this is bad.  i'm doing it because in practice this will
        // decrebse the amount of downloads, which isn't horrible.  also, i
        // don't see b download being removed very frequently.  if i want i can
        // move to b new set which keeps a count for each element of the set and
        // only discbrds after the appropriate amt. of removes....
        StringTokenizer st = 
        new StringTokenizer(ripExtension(toRemove.getFileNbme()),
                            FileMbnager.DELIMITERS);
        while (st.hbsMoreTokens())
            wordSet.remove(st.nextToken().toLowerCbse());
        
    }

    /** Cbll this when the DL was 'successful'.
     */
    public synchronized void commitDownlobd(RemoteFileDesc toCommit) {
        if (dlList.contbins(toCommit))
            committedDLs++;
    }

    /** @return true when the AutoDownlobd process is complete.
     */
    public synchronized boolebn expired() {
        boolebn retVal = false;
        if (committedDLs >= MAX_DOWNLOADS)
            retVbl = true;
        return retVbl;
    }


    // tbke the extension off the filename...
    privbte String ripExtension(String fileName) {
        String retString = null;
        int extStbrt = fileName.lastIndexOf('.');
        if (extStbrt == -1)
            retString = fileNbme;
        else
            retString = fileNbme.substring(0, extStart);
        return retString;
    }

    privbte static final boolean debugOn = false;
    privbte static void debug(String out) {
        if (debugOn)
            System.out.println(out);
    }
    privbte static void debug(Exception e) {
        if (debugOn)
            e.printStbckTrace();
    }
    
}


