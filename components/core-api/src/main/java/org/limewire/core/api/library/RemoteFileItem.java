package org.limewire.core.api.library;

import java.util.List;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.SearchResult;

public interface RemoteFileItem extends FileItem {
    /**
     * Returns a subset of sources identified for a file. 
     * Limiting the number of sources to friends plus 2 other sources.
     */
    List<RemoteHost> getSources();
    
    /**
     * @return the underlying SearchResult
     */
    SearchResult getSearchResult();
}
