package com.limegroup.gnutella.caas;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.limewire.io.IpPort;

import com.limegroup.gnutella.RemoteFileDesc;
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
    
}
