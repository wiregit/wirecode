package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.*;

/** This is essentially a ManagedDownloader with a few hitches.  First of all,
 *  the conflictsLAX method does different things depending on the state.
 *  Secondly, this downloader will requery the network until it has a file to
 *  download.
 */
public class RequeryDownloader extends ManagedDownloader {

    /** Contains the specifics of the search that spawned me.  Important for
     *  requerying....
     */
    protected AutoDownloadDetails _add;

    /** Switch that is set when _add.addDownload returns true, meaning that the
     *  RequeryDownloader now has a file to download.
     */
    private boolean _hasFile = false;

    /**
     * Creates a new RequeryDownloader - a RequeryDownloader has no files
     * initially associated with it, but it may have them later (via calls to
     * addDownload().
     * Non-blocking.
     *     @param manager the delegate for queueing purposes.  Also the callback
     *      for changes in state.
     *     @param incompleteFileManager the repository of incomplete files for
     *      resuming
     */
    public RequeryDownloader(DownloadManager manager,
                             FileManager fileManager,
                             IncompleteFileManager incompleteFileManager,
                             AutoDownloadDetails add) {
        super(manager, new RemoteFileDesc[0], fileManager,
              incompleteFileManager);
        Assert.that(add != null, 
                    "Instantiated with a null AutoDownloadDetail!");
        _add = add;
    }

    /** Returns the query that spawned this Downloader.
     */
    public String getQuery() {
        return _add.getQuery();
    }

    /** Returns the rich query that spawned this Downloader.
     */
    public String getRichQuery() {
        return _add.getRichQuery();
    }


    /**
     * Returns true if 'other' could conflict with one of the files in this. 
     * This is a much less strict version compared to conflicts().
     * WARNING - THIS SHOULD NOT BE USED WHEN THE Downloader IS IN A DOWNLOADING
     * STATE!!!  Ideally used when WAITING_FOR_RESULTS....
     */
    public boolean conflictsLAX(RemoteFileDesc other) {        
        boolean retVal = false;
        if (_hasFile)
            retVal = super.conflictsLAX(other);
        else {
            // see if this RFD is kosher.  if so, then add the download to the
            // superclass and from now on you'll execute the above branch....
            synchronized (_add) {
                if (_add.addDownload(other)) {
                    super.addDownload(other);
                    _add.commitDownload(other);
                    _hasFile = true;
                }
            }
        }    
        return retVal;
    }

}
