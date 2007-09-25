package com.limegroup.gnutella.downloader;

import java.net.Socket;
import java.util.concurrent.ScheduledExecutorService;

import com.google.inject.Provider;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.util.SocketsManager;

class DownloadWorkerStub extends DownloadWorker {


    public DownloadWorkerStub(ManagedDownloader manager, RemoteFileDesc rfd, VerifyingFile vf,
            HTTPDownloaderFactory httpDownloaderFactory,
            ScheduledExecutorService backgroundExecutor, ScheduledExecutorService nioExecutor,
            Provider<PushDownloadManager> pushDownloadManager, SocketsManager socketsManager) {
        super(manager, rfd, vf, httpDownloaderFactory, backgroundExecutor, nioExecutor,
                pushDownloadManager, socketsManager);
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
