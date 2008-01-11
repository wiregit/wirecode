package com.limegroup.gnutella.downloader.serial;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.limewire.collection.Range;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.CoreDownloader;
import com.limegroup.gnutella.downloader.DownloaderType;
import com.limegroup.gnutella.downloader.ManagedDownloader;

public class InNetworkMemento extends GnutellaDownloadMemento {

    /** The TigerTree root for this download. */
    private final String ttRoot;
    
    /** The number of times we have attempted this download */
    private final int downloadAttempts;
    
    /** The time we created this download */
    private final long startTime;
    
    
    public InNetworkMemento(Map<String, Serializable> properties,
            List<Range> ranges, File incompleteFile, Set<RemoteHostMemento> remoteHosts,
            String ttRoot, int downloadAttempts, long startTime) {
        super(DownloaderType.INNETWORK, properties, ranges, incompleteFile, remoteHosts);
        this.ttRoot = ttRoot;
        this.downloadAttempts = downloadAttempts;
        this.startTime = startTime;
    }

    public String getTigerTreeRoot() {
        return ttRoot;
    }

    public int getDownloadAttempts() {
        return downloadAttempts;
    }

    public long getStartTime() {
        return startTime;
    }
    
    public long getSize() {
        return (Long)getProperties().get(CoreDownloader.FILE_SIZE);
    }
    
    public URN getUrn() {
        return (URN)getProperties().get(ManagedDownloader.SHA1_URN);
    }
    
}
