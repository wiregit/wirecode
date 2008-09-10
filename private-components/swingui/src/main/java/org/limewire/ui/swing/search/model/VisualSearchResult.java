package org.limewire.ui.swing.search.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.ResultType;
import org.limewire.core.api.search.SearchResult;

// TODO: RMV Why not use SearchResult instead?
public interface VisualSearchResult {
    
    void download();

    List<SearchResult> getCoreSearchResults();

    List<VisualSearchResult> getSimilarResults();

    Map<Object, Object> getProperties();

    Object getProperty(Object key);

    Collection<RemoteHost> getSources();
    
    ResultType getCategory();
    
    String getDescription();

    String getMediaType();
    
    long getSize();
    
    String getFileExtension();

    /**
     * @return true if currently being downloaded; false otherwise
     */
    boolean isDownloading();
}
