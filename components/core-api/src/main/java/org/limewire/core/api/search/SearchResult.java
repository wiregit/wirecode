package org.limewire.core.api.search;

import java.util.List;
import java.util.Map;

import org.limewire.core.api.endpoint.RemoteHost;

public interface SearchResult {

    String getUrn();

    Map<Object, Object> getProperties();
    
    ResultType getResultType();
    
    String getDescription();
    
    long getSize();
    
    String getFileExtension();
    
    List<RemoteHost> getSources();

}
