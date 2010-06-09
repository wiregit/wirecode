package com.limegroup.gnutella.downloader.serial;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.limewire.collection.Range;
import org.limewire.io.URNImpl;


public interface GnutellaDownloadMemento extends DownloadMemento {

    void setContentLength(long contentLength);

    void setSha1Urn(URNImpl sha1Urn);

    void setSavedBlocks(List<Range> serializableBlocks);

    void setIncompleteFile(File incompleteFile);

    void setRemoteHosts(Set<RemoteHostMemento> remoteHostMementos);

    long getContentLength();

    URNImpl getSha1Urn();

    File getIncompleteFile();

    Set<RemoteHostMemento> getRemoteHosts();

    List<Range> getSavedBlocks();

}