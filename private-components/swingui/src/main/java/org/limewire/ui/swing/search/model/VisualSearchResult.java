package org.limewire.ui.swing.search.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.ResultType;
import org.limewire.core.api.search.SearchResult;

public interface VisualSearchResult {

    ResultType getCategory();
    
    List<SearchResult> getCoreSearchResults();

    String getDescription();

    BasicDownloadState getDownloadState();

    String getFileExtension();

    String getMediaType();
    
    Map<Object, Object> getProperties();

    Object getProperty(Object key);

    String getPropertyString(Object key);

    Collection<RemoteHost> getSources();
    
    List<VisualSearchResult> getSimilarResults();

    long getSize();
    
    boolean isMarkedAsJunk();

    void setDownloadState(BasicDownloadState downloadState);

    void setJunk(boolean junk);
}