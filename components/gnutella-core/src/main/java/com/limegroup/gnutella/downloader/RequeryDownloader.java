package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import java.io.*;

/** 
 * A wishlist ManagedDownloader.  This is initially with only a list of search
 * keywords (e.g., from the user)--with no RemoteFileDesc's.  It will requery
 * the network with the given keywords.  When it gets a result that matches the
 * query, it will start downloading from that location.  Subsequently this will
 * behave like a standard ManagedDownloader, only accepting RemoteFileDesc's
 * matching the current set of RemoteFileDesc's.
 */
public class RequeryDownloader extends ManagedDownloader 
	implements Serializable {
    /** Ensure backwards downloads.dat compatibility. */
    static final long serialVersionUID = 8241301635419840924L;

    /** Contains the specifics of the search that spawned me.  Important for
     *  requerying....
     */
    protected AutoDownloadDetails _add;

    /** Switch that is set when _add.addDownload returns true, meaning that the
     *  RequeryDownloader now has a file to download.
     */
    private boolean _hasFile = false;

    /** The time to wait for results after we are freshly started.
     */
    static int MAX_WAIT_TIME = 5*60*1000; // 5 minutes


    /**
     * Creates a new RequeryDownloader - a RequeryDownloader has no files
     * initially associated with it, but it may have them later (via calls to
     * addDownload().  Non-blocking.     
     *     @param incompleteFileManager the repository of incomplete files for
     *      resuming
     *     @param add the keywords to requery with
     */
    public RequeryDownloader(IncompleteFileManager incompleteFileManager,
                             AutoDownloadDetails add, GUID queryGUID) {
        super(new RemoteFileDesc[0], incompleteFileManager, queryGUID);
        Assert.that(add != null, 
                    "Instantiated with a null AutoDownloadDetail!");
        _add = add;
    }

    /** Returns the query that spawned this Downloader. */
    private String getQuery() {
        return _add.getQuery();
    }

    /** Returns the rich query that spawned this Downloader. */
    private String getRichQuery() {
        return _add.getRichQuery();
    }

    /** Returns the MediaType associated with this Downloader.
     */
    private final MediaType getMediaType() {
        return _add.getMediaType();
    }

    /** Returns true if the parameters of the add are sufficiently similar such
     *  that spawning a new RequeryDownloader would be redundant.
     */
    public boolean conflicts(AutoDownloadDetails add) {
        // currently, if the query is equal and the mediatype is the same.  this
        // may not be the most comprehensive test, but i'm trying to stop
        // AddWishList calls for the same search mainly....
        return (getQuery().equals(add.getQuery()) &&
               getMediaType().toString().equals(add.getMediaType().toString()));
    }

    /* We need special handling of the initial failed state so this overrides
     * the super class when necessary.
     */
    protected long[] getFailedState(boolean deserialized, 
                                   long timeSpentWaiting) {
        if (!deserialized && (timeSpentWaiting < MAX_WAIT_TIME)) {
            long retLongs[] = new long[2];
            retLongs[0] = Downloader.WAITING_FOR_RESULTS;
            retLongs[1] = MAX_WAIT_TIME - timeSpentWaiting;
            return retLongs;
        }
        return super.getFailedState(deserialized, timeSpentWaiting);
    }

    protected int getQueryCount(boolean deserializedFromDisk) {
        // RequeryDownloaders started from scratch have already had a search
        // done for them.
        if (deserializedFromDisk)
            return 0;
        else
            return 1;
    }

    /** Overrides ManagedDownloader to use the original search keywords. */
    protected QueryRequest newRequery(int numRequeries) 
		throws CantResumeException {
        //If this already started downloading, specifically ask for matches to
        //that file.
        if (_hasFile)
            return super.newRequery(numRequeries);
        //Otherwise just spit out the original search keywords.
		return QueryRequest.createQuery(getQuery());
    }

    /**
     * Overrides ManagedDownloader to allow any RemoteFileDesc that matches
     * this' keywords.  If a match has already been found and a download has
     * been started, only allows those RemoteFileDesc's that actually match 
     * the download.
     */
    protected boolean allowAddition(RemoteFileDesc other) {
        if (_hasFile)
            return super.allowAddition(other);
        else {
            // See if this RFD matches.  If so, then record the information.
            // Yes, we should really be modifying this in addDownloader, not
            // here, but that's ok since a return value of true implies that the
            // download will actually be added.  Note that from now on you'll
            // execute the above branch.
            synchronized (_add) {
                if (_add.addDownload(other)) {
                    _add.commitDownload(other);
                    _hasFile = true;
                    return true;
                } else {
                    return false;
                }
            }
        }
    }


    /** Need to override this until ManagedDownloader has a allFiles of non-zero
     * length. 
     */
    public synchronized String getFileName() {        
        if (_hasFile)
            return super.getFileName();
        else
            return "\"" + getQuery() + "\"";
    }

    /** Need to override this until ManagedDownloader has a allFiles of non-zero
     * length. 
     */
    public synchronized int getContentLength() {
        if (_hasFile)
            return super.getContentLength();
        else
            return -1;
    }

    /**
     * package access accessor needed for unit test in junit.
     */
    boolean hasFile() {
        //We dont want to expose the variable
        return _hasFile;
    }
}
