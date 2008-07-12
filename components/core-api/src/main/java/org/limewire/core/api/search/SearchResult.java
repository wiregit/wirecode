package org.limewire.core.api.search;

import java.util.Map;

public interface SearchResult {

    String getUrn();

    Map<Object, Object> getProperties();
    
    ResultType getResultType();
    
    String getDescription();
    
    long getSize();
    
    String getFileExtension();

}
