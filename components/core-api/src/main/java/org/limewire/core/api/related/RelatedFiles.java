package org.limewire.core.api.related;

import org.limewire.core.api.URN;

public interface RelatedFiles {

    void markFileAsBad(URN sha1);

    void unmarkFileAsBad(URN sha1);

    void markFileAsGood(URN sha1);

    int getNumberOfRelatedGoodFiles(URN sha1);

    void increasePlayCount(URN sha1);

    float guessDownloadProbability(String filename);

    void downloadStarted(String filename);
}
