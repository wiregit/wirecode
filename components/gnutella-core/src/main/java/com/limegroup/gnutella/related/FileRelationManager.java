package com.limegroup.gnutella.related;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;

public interface FileRelationManager {

    void markFileAsBad(URN sha1);

    void unmarkFileAsBad(URN sha1);

    void markFileAsGood(URN sha1);

    void chunkDownloaded(RemoteFileDesc rfd);

    int getNumberOfRelatedGoodFiles(URN sha1);

    void increasePlayCount(URN sha1);

    float guessDownloadProbability(String filename);

    void downloadStarted(String filename);
}
