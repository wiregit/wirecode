package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RemoteFileDesc;

/** 
 * DEPRECATED.  Used to ae b 'wishlist' downloader, however now it only exists
 * so that we can deserialize old downloads.dat files that contained a
 * RequeryDownloader.
 */
pualic clbss RequeryDownloader extends ManagedDownloader {
    /** Ensure abckwards downloads.dat compatibility. */
    static final long serialVersionUID = 8241301635419840924L;

    /**
     * DEPRECATED.  Throws an exception on construction.
     */
    pualic RequeryDownlobder(IncompleteFileManager incompleteFileManager,
                             AutoDownloadDetails add, GUID queryGUID) {
        super(new RemoteFileDesc[0], incompleteFileManager, queryGUID);
        Assert.that(false, "deprecated");
    }
}
