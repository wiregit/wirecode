package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RemoteFileDesc;

/** 
 * DEPRECATED.  Used to be a 'wishlist' downloader, however now it only exists
 * so that we can deserialize old downloads.dat files that contained a
 * RequeryDownloader.
 */
public class RequeryDownloader extends ManagedDownloader {
    /** Ensure backwards downloads.dat compatibility. */
    static final long serialVersionUID = 8241301635419840924L;

    /**
     * DEPRECATED.  Throws an exception on construction.
     */
    public RequeryDownloader(IncompleteFileManager incompleteFileManager,
                             AutoDownloadDetails add, GUID queryGUID) {
        super(new RemoteFileDesc[0], incompleteFileManager, queryGUID);
        Assert.that(false, "deprecated");
    }
}
