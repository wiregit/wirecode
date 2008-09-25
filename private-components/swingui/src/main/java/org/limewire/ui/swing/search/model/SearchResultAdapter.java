package org.limewire.ui.swing.search.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jdesktop.beans.AbstractBean;
import org.limewire.core.api.Category;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.SearchResult.PropertyKey;
import org.limewire.util.MediaType;

class SearchResultAdapter extends AbstractBean implements VisualSearchResult {
    
    private final List<SearchResult> coreResults;
    private Map<SearchResult.PropertyKey, Object> properties;
    private final Set<RemoteHost> remoteHosts;
    private BasicDownloadState downloadState = BasicDownloadState.NOT_STARTED;
    private final Set<VisualSearchResult> similarResults = new HashSet<VisualSearchResult>();
    private VisualSearchResult similarityParent;
    private boolean childrenVisible;
    private boolean visible;

    public SearchResultAdapter(List<SearchResult> sourceValue) {
        this.coreResults = sourceValue;
        
        this.remoteHosts =
            new TreeSet<RemoteHost>(new Comparator<RemoteHost>() {
            @Override
            public int compare(RemoteHost o1, RemoteHost o2) {
                return o1.getHostDescription().compareToIgnoreCase(
                    o2.getHostDescription());
            }
        });
        this.visible = true;
        this.childrenVisible = false;
        
        update();
    }

    @Override
    public Category getCategory() {
        return coreResults.get(0).getCategory();
    }
    
    @Override
    public List<SearchResult> getCoreSearchResults() {
        return coreResults;
    }
    
    @Override
    public String getDescription() {
        return coreResults.get(0).getDescription();
    }
    
    @Override
    public String getFileExtension() {
        return coreResults.get(0).getFileExtension();
    }
    
    @Override
    public String getMediaType() {
        String ext = getFileExtension();
        MediaType mediaType = MediaType.getMediaTypeForExtension(ext);
        // TODO: RMV improve the text returned
        return mediaType == null ? ext : mediaType.toString();
    }

    @Override
    public Map<SearchResult.PropertyKey, Object> getProperties() {
        if (properties == null) {
            properties = new HashMap<SearchResult.PropertyKey, Object>();
            for (SearchResult result : coreResults) {
                Map<PropertyKey, Object> props = result.getProperties();
                properties.putAll(props);
            }
        }

        return properties;
    }

    public Object getProperty(SearchResult.PropertyKey key) {
        return getProperties().get(key);
    }

    public String getPropertyString(SearchResult.PropertyKey key) {
        Object value = getProperty(key);
        if(value != null) {
            String stringValue = value.toString();
            
            if (value instanceof Calendar) {
                Calendar calendar = (Calendar) value;
                Date date = calendar.getTime();
                DateFormat df = SimpleDateFormat.getDateTimeInstance(
                    DateFormat.LONG, DateFormat.LONG);
                stringValue = df.format(date);
            }
    
            return stringValue;
        } else {
            return null;
        }
    }
    
    public void addSimilarSearchResult(VisualSearchResult similarResult) {
        similarResults.add(similarResult);
    }
    
    @Override
    public List<VisualSearchResult> getSimilarResults() {
        return new ArrayList<VisualSearchResult>(similarResults);
    }
    
    public void setSimilarityParent(VisualSearchResult parent) {
//        VisualSearchResult oldParent = this.similarityParent;
        this.similarityParent = parent;
//        firePropertyChange("similarityParent", oldParent, parent);
    }
    
    @Override
    public VisualSearchResult getSimilarityParent() {
        return similarityParent;
    }

    @Override
    public long getSize() {
        return coreResults.get(0).getSize();
    }
    
    @Override
    public Collection<RemoteHost> getSources() {
        return remoteHosts;
    }
    
    @Override
    public BasicDownloadState getDownloadState() {
        return downloadState;
    }

    @Override
    public void setDownloadState(BasicDownloadState downloadState) {
        BasicDownloadState oldDownloadState = this.downloadState;
        this.downloadState = downloadState;
        firePropertyChange("downloadState", oldDownloadState, downloadState);
    }

    @Override
    public String toString() {
        return getCoreSearchResults().toString();
    }

    void update() {
        for (SearchResult result : coreResults) {
            remoteHosts.addAll(result.getSources());
        }
    }

    @Override
    public boolean isVisible() {
//        if(similarityParent == null) {
//           return true;   
//        }
//        return similarityParent.isChildrenVisible();
        return visible;
    }
    
    @Override
    public void setVisible(boolean visible) {
        boolean oldVisible = this.visible;
        this.visible = visible;
        firePropertyChange("visible", oldVisible, visible);
    }
    
    private String getUrn() {
        List<SearchResult> coreSearchResults = getCoreSearchResults();
        if (coreSearchResults == null || coreSearchResults.size() == 0) return "";
        SearchResult searchResult = coreSearchResults.get(0);
        return searchResult.getUrn();
    }

    @Override
    public boolean isChildrenVisible() {
        return childrenVisible;
    }
    
    @Override
    public void setChildrenVisible(boolean childrenVisible) {
        boolean oldVisible = this.childrenVisible;
        this.childrenVisible = childrenVisible;
        for(VisualSearchResult child : getSimilarResults()) {
            child.setVisible(childrenVisible);
        }
        firePropertyChange("childrenVisible", oldVisible, childrenVisible);
    }

    public void removeSimilarSearchResult(VisualSearchResult result) {
        similarResults.remove(result);       
    }

    @Override
    public boolean isSpam() {
        return coreResults.get(0).isSpam();
    }

    @Override
    public void setSpam(boolean spam) {
        firePropertyChange("spam", isSpam(), spam);
    }
}