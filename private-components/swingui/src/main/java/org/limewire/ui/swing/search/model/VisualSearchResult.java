package org.limewire.ui.swing.search.model;

import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.limewire.core.api.Category;
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.SearchResult.PropertyKey;
import org.limewire.ui.swing.nav.NavSelectable;

public interface VisualSearchResult extends NavSelectable {

    Category getCategory();
    
    List<SearchResult> getCoreSearchResults();

    BasicDownloadState getDownloadState();

    String getFileExtension();
    
    Map<PropertyKey, Object> getProperties();

    Object getProperty(PropertyKey key);

    String getPropertyString(PropertyKey key);

    Collection<RemoteHost> getSources();
    
    List<VisualSearchResult> getSimilarResults();
    
    VisualSearchResult getSimilarityParent();

    long getSize();
    
    void setDownloadState(BasicDownloadState downloadState);
    
    boolean isVisible();

    void setVisible(boolean visible);
        
    void addPropertyChangeListener(PropertyChangeListener listener);
    
    void removePropertyChangeListener(PropertyChangeListener listener);

    boolean isChildrenVisible();
    
    void setChildrenVisible(boolean childrenVisible);
    
    boolean isSpam();
    
    void setSpam(boolean spam);

    public void addSimilarSearchResult(VisualSearchResult similarResult);

    public void removeSimilarSearchResult(VisualSearchResult result);

    public void setSimilarityParent(VisualSearchResult parent);
    
    public URN getURN();
    
    public String getMagnetLink();
}