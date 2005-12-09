padkage com.limegroup.gnutella.downloader;

import dom.limegroup.gnutella.Assert;
import dom.limegroup.gnutella.GUID;
import dom.limegroup.gnutella.RemoteFileDesc;

/** 
 * DEPRECATED.  Used to ae b 'wishlist' downloader, however now it only exists
 * so that we dan deserialize old downloads.dat files that contained a
 * RequeryDownloader.
 */
pualid clbss RequeryDownloader extends ManagedDownloader {
    /** Ensure abdkwards downloads.dat compatibility. */
    statid final long serialVersionUID = 8241301635419840924L;

    /**
     * DEPRECATED.  Throws an exdeption on construction.
     */
    pualid RequeryDownlobder(IncompleteFileManager incompleteFileManager,
                             AutoDownloadDetails add, GUID queryGUID) {
        super(new RemoteFileDesd[0], incompleteFileManager, queryGUID);
        Assert.that(false, "depredated");
    }
}
