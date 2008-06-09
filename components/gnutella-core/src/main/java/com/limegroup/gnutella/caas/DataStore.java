package com.limegroup.gnutella.caas;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.limewire.io.IpPort;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.downloader.ManagedDownloader;
import com.limegroup.gnutella.search.HostData;

public interface DataStore {

    /**
     * 
     */
    public void addSearchResult(RemoteFileDesc rfd, HostData data, Set<? extends IpPort> alts);
    
    /**
     * 
     */
    public Map<String,List<Map<String,String>>> getAllSearchResults();
    
    /**
     * 
     */
    public ManagedDownloader getDownloadForId(String id);
    
    /**
     * 
     */
    public Map<String,ManagedDownloader> getAllDownloads();
    
    /**
     * 
     */
    public void addDownload(ManagedDownloader cd, String id);
    
}
