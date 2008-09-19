package org.limewire.ui.swing.search.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.ResultType;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.SearchResult.PropertyKey;

public interface VisualSearchResult {

    ResultType getCategory();
    
    List<SearchResult> getCoreSearchResults();

    String getDescription();

    BasicDownloadState getDownloadState();

    String getFileExtension();

    String getMediaType();
    
    Map<PropertyKey, Object> getProperties();

    Object getProperty(PropertyKey key);

    String getPropertyString(PropertyKey key);

    Collection<RemoteHost> getSources();
    
    List<VisualSearchResult> getSimilarResults();
    
    VisualSearchResult getSimilarityParent();

    long getSize();
    
    boolean isMarkedAsJunk();

    void setDownloadState(BasicDownloadState downloadState);

    void setJunk(boolean junk);
    
    boolean isVisible();
    
    void setVisible(boolean visible);
}