package org.limewire.ui.swing.search.model;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.limewire.core.api.Category;
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.SearchResult.PropertyKey;

public class MockVisualSearchResult implements VisualSearchResult {
    private List<VisualSearchResult> similarResults = new ArrayList<VisualSearchResult>();
    private String name;
    private VisualSearchResult similarityParent;
    
    public MockVisualSearchResult(String name) {
        this.name = name;
    }

    public MockVisualSearchResult(String name, VisualSearchResult parent) {
        this(name);
        this.similarityParent = parent;
        parent.getSimilarResults().add(this);
    }

    @Override
    public Category getCategory() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<SearchResult> getCoreSearchResults() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BasicDownloadState getDownloadState() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getFileExtension() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<PropertyKey, Object> getProperties() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getProperty(PropertyKey key) {
        if(key == PropertyKey.NAME) {
            return name; 
        } else {
            return null;
        }
    }

    @Override
    public String getPropertyString(PropertyKey key) {
        // TODO Auto-generated method stub
        return null;
    }
    
    public void setSimilarResults(List<VisualSearchResult> similarResults) {
        this.similarResults = similarResults;
    }

    @Override
    public List<VisualSearchResult> getSimilarResults() {
        return similarResults;
    }
    
    @Override
    public VisualSearchResult getSimilarityParent() {
        return similarityParent;
    }
    
    @Override
    public long getSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Collection<RemoteHost> getSources() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isVisible() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setDownloadState(BasicDownloadState downloadState) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setVisible(boolean visible) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isChildrenVisible() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isSpam() {
        return false;
    }

    @Override
    public void setSpam(boolean spam) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setChildrenVisible(boolean childrenVisible) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String toString() {
        return name;
    }

    public void removeSimilarSearchResult(VisualSearchResult result) {
    }

    public void addSimilarSearchResult(VisualSearchResult similarResult) {
    }

    public void setSimilarityParent(VisualSearchResult parent) {
    }

    @Override
    public URN getURN() {
        return null;
    }

    @Override
    public String getNavSelectionId() {
        return null;
    }

    @Override
    public String getMagnetLink() {
        return null;
    }
}
