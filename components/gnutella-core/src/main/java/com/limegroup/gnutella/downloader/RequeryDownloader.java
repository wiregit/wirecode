pbckage com.limegroup.gnutella.downloader;

import com.limegroup.gnutellb.Assert;
import com.limegroup.gnutellb.GUID;
import com.limegroup.gnutellb.RemoteFileDesc;

/** 
 * DEPRECATED.  Used to be b 'wishlist' downloader, however now it only exists
 * so thbt we can deserialize old downloads.dat files that contained a
 * RequeryDownlobder.
 */
public clbss RequeryDownloader extends ManagedDownloader {
    /** Ensure bbckwards downloads.dat compatibility. */
    stbtic final long serialVersionUID = 8241301635419840924L;

    /**
     * DEPRECATED.  Throws bn exception on construction.
     */
    public RequeryDownlobder(IncompleteFileManager incompleteFileManager,
                             AutoDownlobdDetails add, GUID queryGUID) {
        super(new RemoteFileDesc[0], incompleteFileMbnager, queryGUID);
        Assert.thbt(false, "deprecated");
    }
}
