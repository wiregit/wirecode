package org.limewire.core.api.search;

import java.util.List;
import java.util.Map;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;

public interface SearchResult {

    String getFileExtension();
    
    Map<FilePropertyKey, Object> getProperties();

    Object getProperty(FilePropertyKey key);
    
    Category getCategory();

    long getSize();
    
    List<RemoteHost> getSources();

    URN getUrn();
    
    public boolean isSpam();
    
    String getFileName();
    
    String getMagnetURL();
}