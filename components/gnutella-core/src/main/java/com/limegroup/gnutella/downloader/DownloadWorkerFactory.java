package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.RemoteFileDesc;

interface DownloadWorkerFactory {

    DownloadWorker create(DownloadWorkerSupport manager, RemoteFileDesc rfd,
            VerifyingFile vf);

}