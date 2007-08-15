package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.RemoteFileDesc;

public interface DownloadWorkerFactory {

    public DownloadWorker create(ManagedDownloader manager, RemoteFileDesc rfd,
            VerifyingFile vf);

}