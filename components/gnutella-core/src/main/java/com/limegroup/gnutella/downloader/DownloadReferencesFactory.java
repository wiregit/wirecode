package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.Downloader;

// DPINJ: get rid of this!  see: CORE-306
public interface DownloadReferencesFactory {
    
    DownloadReferences create(Downloader downloader);

}
