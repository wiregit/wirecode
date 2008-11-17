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
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.SearchResult;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.util.PropertiableHeadings;

class SearchResultAdapter extends AbstractBean implements VisualSearchResult {

    private final Log LOG = LogFactory.getLog(getClass());

    private final List<SearchResult> coreResults;

    private Map<FilePropertyKey, Object> properties;

    private final Set<RemoteHost> remoteHosts;
    
    private final PropertiableHeadings propertiableHeadings;

    private BasicDownloadState downloadState = BasicDownloadState.NOT_STARTED;

    private final Set<VisualSearchResult> similarResults = new HashSet<VisualSearchResult>();

    private VisualSearchResult similarityParent;

    private boolean visible;

    private boolean childrenVisible;
    
    private Boolean spamCache;
    
    private boolean preExistingDownload = false;
    
    private Double relevance = null;

    public SearchResultAdapter(List<SearchResult> sourceValue, PropertiableHeadings propertiableHeadings) {
        this.coreResults = sourceValue;
        this.propertiableHeadings = propertiableHeadings;

        this.remoteHosts = new TreeSet<RemoteHost>(new Comparator<RemoteHost>() {
            @Override
            public int compare(RemoteHost o1, RemoteHost o2) {
                int compare = 0;
                boolean anonymous1 = o1.isAnonymous();
                boolean anonymous2 = o2.isAnonymous();

                if (anonymous1 == anonymous2) {
                    compare = o1.getRenderName().compareToIgnoreCase(o2.getRenderName());
                } else if (anonymous1) {
                    compare = 1;
                } else if (anonymous2) {
                    compare = -1;
                }
                return compare;
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
    public String getFileExtension() {
        return coreResults.get(0).getFileExtension();
    }
    
    @Override
    public String getFileName() {
        return coreResults.get(0).getFileName();
    }

    @Override
    public Map<FilePropertyKey, Object> getProperties() {
        if (properties == null) {
            properties = new HashMap<FilePropertyKey, Object>();
            for (SearchResult result : coreResults) {
                Map<FilePropertyKey, Object> props = result.getProperties();
                properties.putAll(props);
            }
        }

        return properties;
    }

    public Object getProperty(FilePropertyKey key) {
        return getProperties().get(key);
    }

    public String getPropertyString(FilePropertyKey key) {
        Object value = getProperty(key);
        if (value != null) {
            String stringValue = value.toString();

            if (value instanceof Calendar) {
                Calendar calendar = (Calendar) value;
                Date date = calendar.getTime();
                DateFormat df = SimpleDateFormat.getDateTimeInstance(DateFormat.LONG,
                        DateFormat.LONG);
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
        VisualSearchResult oldParent = this.similarityParent;
        this.similarityParent = parent;
        firePropertyChange("similarityParent", oldParent, parent);
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

    /**
     * Readds the sources from the core search results.
     * Only adding the filteredSources list to try and cut back on spam related results.
     */
    void update() {
        relevance = null;
        remoteHosts.clear();
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
        boolean oldValue = this.visible;
        this.visible = visible;
        firePropertyChange("visible", oldValue, visible);
        LOG.debugf("Updating visible to {0} for urn: {1}", visible, getUrn());
    }

    @Override
    public boolean isChildrenVisible() {
        return childrenVisible;
    }

    public void removeSimilarSearchResult(VisualSearchResult result) {
        similarResults.remove(result);
    }

    @Override
    public boolean isSpam() {
        if (spamCache == null) {
            spamCache = getSpamBoolean(coreResults.get(0).isSpam());
        }
        return spamCache.booleanValue();
    }

    private Boolean getSpamBoolean(boolean spam) {
        return spam ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    public void setSpam(boolean spam) {
        boolean oldSpam = isSpam();
        spamCache = getSpamBoolean(spam);
        firePropertyChange("spam", oldSpam, spam);
    }

    @Override
    public void setChildrenVisible(boolean childrenVisible) {
        this.childrenVisible = childrenVisible;
        for (VisualSearchResult similarResult : getSimilarResults()) {
            similarResult.setVisible(childrenVisible);
            similarResult.setChildrenVisible(false);
        }
    }

    @Override
    public URN getUrn() {
        return coreResults.get(0).getUrn();
    }

    @Override
    public URN getNavSelectionId() {
        return getUrn();
    }

    @Override
    public String getMagnetLink() {

        String sep = System.getProperty("line.separator");
        StringBuilder bldr = new StringBuilder();
        for (SearchResult result : getCoreSearchResults()) {
            bldr.append(result.getMagnetURL()).append(sep);
        }

        if (bldr.length() > sep.length()) {
            return bldr.substring(0, bldr.length() - sep.length());
        }

        return null;
    }

    @Override
    public String getHeading() {
        return propertiableHeadings.getHeading(this);
    }

    @Override
    public String getSubHeading() {
        return propertiableHeadings.getSubHeading(this);
    }

    @Override
    public double getRelevance() {
        
        if(this.relevance == null) {
            double sum = 0;
            for(SearchResult searchResult : coreResults) {
                sum += searchResult.getRelevance();
            }
            this.relevance = sum;
        } 
        return relevance;
    }

    @Override
    public boolean isPreExistingDownload() {
        return preExistingDownload;
    }

    @Override
    public void setPreExistingDownload(boolean preExistingDownload) {
        this.preExistingDownload = preExistingDownload;
    }
}