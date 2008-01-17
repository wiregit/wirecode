package com.limegroup.gnutella.downloader;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.tigertree.HashTree;

/**
 * Defines the contract by which a download can be signaled & controlled from a
 * DownloadWorker.
 */
interface DownloadWorkerSupport extends ManagedDownloader {

    void addRFD(RemoteFileDesc _rfd);

    void forgetRFD(RemoteFileDesc _rfd);

    List<DownloadWorker> getActiveWorkers();

    List<DownloadWorker> getAllWorkers();

    Set<AlternateLocation> getInvalidAlts();

    Map<DownloadWorker, Integer> getQueuedWorkers();

    Set<AlternateLocation> getValidAlts();

    void hashTreeRead(HashTree newTree);

    void setState(DownloadStatus connecting);

    void incrementTriedHostsCount();

    void removeQueuedWorker(DownloadWorker downloadWorker);

    boolean killQueuedIfNecessary(DownloadWorker downloadWorker, int i);

    void promptAboutCorruptDownload();

    QueryRequest newRequery() throws CantResumeException;
    
    void registerPushObserver(HTTPConnectObserver observer, PushDetails details);

    boolean removeActiveWorker(DownloadWorker downloadWorker);

    void unregisterPushObserver(PushDetails details, boolean b);

    void workerFailed(DownloadWorker downloadWorker);

    void workerFinished(DownloadWorker downloadWorker);

    void workerStarted(DownloadWorker downloadWorker);

    
}
