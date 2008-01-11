package com.limegroup.gnutella.downloader.serial;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.limewire.collection.Range;

import com.limegroup.gnutella.downloader.CoreDownloader;
import com.limegroup.gnutella.downloader.DownloaderType;

public class GnutellaDownloadMemento implements DownloadMemento {
    
    /** The kind of download this is. */
    private final DownloaderType downloadType;

    /** Any additional properties of this download. */
    private final Map<String, Serializable> properties;

    /** Any ranges that were written. */
    private final List<Range> ranges;

    /** The filename of the incomplete file. */
    private final File incompleteFile;

    /** All remote hosts that should be saved. */
    private final Set<RemoteHostMemento> remoteHosts;

    public GnutellaDownloadMemento(DownloaderType downloadType, Map<String, Serializable> properties,
            List<Range> ranges, File incompleteFile, Set<RemoteHostMemento> remoteHosts) {
        switch(downloadType) {
        case INNETWORK:
        case MAGNET:
        case MANAGED:
        case STORE:
            break;
        default:
            throw new IllegalArgumentException("invalid type: " + downloadType);
        }
        this.downloadType = downloadType;
        this.properties = properties;
        this.ranges = ranges;
        this.incompleteFile = incompleteFile;
        this.remoteHosts = remoteHosts;
    }

    public DownloaderType getDownloadType() {
        return downloadType;
    }

    public Map<String, Serializable> getProperties() {
        return properties;
    }

    public List<Range> getRanges() {
        return ranges;
    }

    public File getIncompleteFile() {
        return incompleteFile;
    }

    public Set<RemoteHostMemento> getRemoteHosts() {
        return remoteHosts;
    }
    
    public String getDefaultFileName() {
        return (String)properties.get(CoreDownloader.DEFAULT_FILENAME);
    }


}
