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
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.ResultType;
import org.limewire.core.api.search.SearchResult;
import org.limewire.util.MediaType;

class SearchResultAdapter extends AbstractBean implements VisualSearchResult {
    
    private final List<SearchResult> coreResults;
    private Map<SearchResult.PropertyKey, Object> properties;
    private final Set<RemoteHost> remoteHosts;
    private BasicDownloadState downloadState = BasicDownloadState.NOT_STARTED;
    private final Set<VisualSearchResult> similarResults = new HashSet<VisualSearchResult>();
    private VisualSearchResult similarityParent;
    private boolean junk;
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
        
        update();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof SearchResultAdapter)) return false;
        
        final SearchResultAdapter other = (SearchResultAdapter) obj;
        
        // TODO: RMV Should the comparison be only on the description?
        return getDescription().equals(other.getDescription());
        /*
        if (coreResults == null) {
            if (other.coreResults != null) return false;
        } else if (!coreResults.equals(other.coreResults)) return false;
        
        if (remoteHosts == null) {
            if (other.remoteHosts != null) return false;
        } else if (!remoteHosts.equals(other.remoteHosts)) return false;
        
        return true;
        */
    }

    @Override
    public ResultType getCategory() {
        return coreResults.get(0).getResultType();
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
                properties.putAll(result.getProperties());
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
        this.similarityParent = parent;
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
    public int hashCode() {
        /*
        final int prime = 31;
        int result = 1;
        result = prime * result +
            ((coreResults == null) ? 0 : coreResults.hashCode());
        result = prime * result +
            ((remoteHosts == null) ? 0 : remoteHosts.hashCode());
        return result;
        */
        return getDescription().hashCode(); // TODO: RMV Changed to match equal.
    }

    @Override
    public BasicDownloadState getDownloadState() {
        return downloadState;
    }

    @Override
    public boolean isMarkedAsJunk() {
        return junk;
    }

    @Override
    public void setDownloadState(BasicDownloadState downloadState) {
        BasicDownloadState oldDownloadState = this.downloadState;
        this.downloadState = downloadState;
        firePropertyChange("downloadState", oldDownloadState, downloadState);
    }

    @Override
    public void setMarkedAsJunk(boolean junk) {
        boolean oldJunk = this.junk;
        this.junk = junk;
        firePropertyChange("markedAsJunk", oldJunk, junk);
    }

    @Override
    public String toString() {
        return getDescription() +
            " with " + getSources().size() + " sources, " +
            "in category: " + getCategory() +
            ", with size: " + getSize() +
            ", and extension: " + getFileExtension();
    }

    void update() {
        for (SearchResult result : coreResults) {
            remoteHosts.addAll(result.getSources());
        }
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setVisible(boolean visible) {
        boolean oldVisible = this.visible;
        this.visible = visible;
        firePropertyChange("visible", oldVisible, visible);
    }
}