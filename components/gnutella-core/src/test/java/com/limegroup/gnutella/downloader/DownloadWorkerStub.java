package com.limegroup.gnutella.downloader;

import java.net.Socket;

import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.RemoteFileDesc;

class DownloadWorkerStub extends DownloadWorker {

    public DownloadWorkerStub(ManagedDownloader manager, RemoteFileDesc rfd) {
        super(manager, rfd, null, ProviderHacks.getHTTPDownloaderFactory());
    }

    HTTPDownloader getDownloader() {
        return null;
    }

    public RemoteFileDesc getRFD() {
        return null;
    }

    void interrupt() {
    }

    public void run() {
    }

    synchronized void setPushSocket(Socket s) {
    }

}
