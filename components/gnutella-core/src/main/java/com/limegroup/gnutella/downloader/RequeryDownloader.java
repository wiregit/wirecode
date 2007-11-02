package com.limegroup.gnutella.downloader;



/** 
 * DEPRECATED.  Used to be a 'wishlist' downloader, however now it only exists
 * so that we can deserialize old downloads.dat files that contained a
 * RequeryDownloader.
 */
public class RequeryDownloader extends ManagedDownloader {
    /** Ensure backwards downloads.dat compatibility. */
    private static final long serialVersionUID = 8241301635419840924L;

     @Deprecated
     private RequeryDownloader() {
        super(null, null, null, null);
        throw new UnsupportedOperationException("deprecated");
    }
}
