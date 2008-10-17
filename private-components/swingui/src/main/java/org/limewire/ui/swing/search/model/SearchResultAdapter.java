package org.limewire.ui.swing.search.model;

import static org.limewire.ui.swing.util.I18n.tr;

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
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.SearchResult.PropertyKey;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.CommonUtils;
import org.limewire.util.StringUtils;

class SearchResultAdapter extends AbstractBean implements VisualSearchResult {
    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("M/d/yyyy");
    
    private final Log LOG = LogFactory.getLog(getClass());

    private final List<SearchResult> coreResults;

    private Map<SearchResult.PropertyKey, Object> properties;

    private final Set<RemoteHost> remoteHosts;

    private BasicDownloadState downloadState = BasicDownloadState.NOT_STARTED;

    private final Set<VisualSearchResult> similarResults = new HashSet<VisualSearchResult>();

    private VisualSearchResult similarityParent;

    private boolean visible;

    private boolean childrenVisible;

    public SearchResultAdapter(List<SearchResult> sourceValue) {
        this.coreResults = sourceValue;

        this.remoteHosts = new TreeSet<RemoteHost>(new Comparator<RemoteHost>() {
            @Override
            public int compare(RemoteHost o1, RemoteHost o2) {
                return o1.getRenderName().compareToIgnoreCase(o2.getRenderName());
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
        boolean oldValue = this.visible;
        this.visible = visible;
        firePropertyChange("visible", oldValue, visible);
        LOG.debugf("Updating visible to {0} for urn: {1}", visible, getUrn());
    }

    private URN getUrn() {
        List<SearchResult> coreSearchResults = getCoreSearchResults();
        SearchResult searchResult = coreSearchResults.get(0);
        return searchResult.getUrn();
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
        return coreResults.get(0).isSpam();
    }

    @Override
    public void setSpam(boolean spam) {
        firePropertyChange("spam", isSpam(), spam);
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
    public URN getURN() {
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
        String name = getProperty(PropertyKey.NAME).toString();
        String renderName = "";
        switch (getCategory()) {
        case AUDIO:
            String artist = getPropertyString(PropertyKey.ARTIST_NAME);
            String title = getPropertyString(PropertyKey.TRACK_NAME);
            if (!StringUtils.isEmpty(artist) && !StringUtils.isEmpty(title)) {
                renderName = artist + " - " + title;
            } else {
                renderName = name;
            }
            break;
        case VIDEO:
        case IMAGE:
            renderName = name;
            break;
        case DOCUMENT:
        case PROGRAM:
        case OTHER:
        default:
            renderName = name + "." + getFileExtension();
        }
        return renderName.trim();
    }

    @Override
    public String getSubHeading() {
        String subheading = "";

        switch (getCategory()) {
        case AUDIO: {
            String albumTitle = getPropertyString(PropertyKey.ALBUM_TITLE);
            String quality = getPropertyString(PropertyKey.QUALITY);
            String length = getPropertyString(PropertyKey.LENGTH);

            boolean changed = false;
            if (!StringUtils.isEmpty(albumTitle)) {
                subheading += albumTitle;
                changed = true;
            }

            if (!StringUtils.isEmpty(quality)) {
                if (changed) {
                    subheading += " - ";
                }
                subheading += quality;
                changed = true;
            }

            if (!StringUtils.isEmpty(length)) {
                try {
                    int len = Integer.parseInt(length);
                    length = CommonUtils.seconds2time(len);
                    if (changed) {
                        subheading += " - ";
                    }
                    subheading += length;
                } catch (NumberFormatException e) {
                    // continue
                }
            }
        }
            break;
        case VIDEO: {
            String quality = getPropertyString(PropertyKey.QUALITY);
            String length = getPropertyString(PropertyKey.LENGTH);
            boolean changed = false;
            if (!StringUtils.isEmpty(quality)) {
                subheading += quality;
                changed = true;
            }

            if (!StringUtils.isEmpty(length)) {
                if (changed) {
                    subheading += " - ";
                }
                subheading += length;
            }
        }
            break;
        case IMAGE: {
            Object time = getProperty(PropertyKey.DATE_CREATED);
            if (time != null) {
                subheading = DATE_FORMAT.format(new java.util.Date((Long) time));
            }
        }
            break;
        case PROGRAM: {
            String fileSize = getPropertyString(PropertyKey.FILE_SIZE);
            if (!StringUtils.isEmpty(fileSize)) {
                subheading = fileSize + tr("MB");
            }
        }
            break;
        case DOCUMENT:
        case OTHER:
        default: {
            subheading = "{application name}";
            String fileSize = getPropertyString(PropertyKey.FILE_SIZE);
            if (!StringUtils.isEmpty(fileSize)) {
                subheading = " - " + fileSize + tr("MB");
            }
        }
        }
        return subheading;
    }
}