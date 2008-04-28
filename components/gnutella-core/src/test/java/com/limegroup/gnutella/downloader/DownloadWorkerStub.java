package com.limegroup.gnutella.downloader;

import java.net.Socket;
import java.util.concurrent.ScheduledExecutorService;

import org.limewire.net.SocketsManager;

import com.google.inject.Provider;
import com.limegroup.gnutella.RemoteFileDesc;

class DownloadWorkerStub extends DownloadWorker {


    public DownloadWorkerStub(DownloadWorkerSupport manager, RemoteFileDesc rfd, VerifyingFile vf,
            HTTPDownloaderFactory httpDownloaderFactory,
            ScheduledExecutorService backgroundExecutor, ScheduledExecutorService nioExecutor,
            Provider<PushDownloadManager> pushDownloadManager, SocketsManager socketsManager) {
        super(manager, rfd, vf, httpDownloaderFactory, backgroundExecutor, nioExecutor,
                pushDownloadManager, socketsManager, new DownloadStatsTrackerImpl());
    }

    @Override
    HTTPDownloader getDownloader() {
        return null;
    }

    @Override
    public RemoteFileDesc getRFD() {
        return null;
    }

    @Override
    void interrupt() {
    }

    public void run() {
    }

    synchronized void setPushSocket(Socket s) {
    }

}
