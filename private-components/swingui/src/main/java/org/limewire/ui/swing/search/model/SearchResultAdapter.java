package org.limewire.ui.swing.search.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdesktop.beans.AbstractBean;
import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.SearchResult;
import org.limewire.friend.api.Friend;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.util.PropertiableHeadings;
import org.limewire.util.Objects;
import org.limewire.util.StringUtils;

import com.google.inject.Provider;

/**
 * An implementation of VisualSearchResult for displaying actual search 
 * results. 
 */
class SearchResultAdapter extends AbstractBean implements VisualSearchResult, Comparable {

    private static final Log LOG = LogFactory.getLog(SearchResultAdapter.class);
    
    private static final Matcher FIND_HTML_MARKUP; 
    static {
        Pattern p = Pattern.compile("[<][/]?[\\w =\"\\./:#\\-\\!\\&\\?]*[>]");
        FIND_HTML_MARKUP = p.matcher("");
    }

    private final List<SearchResult> coreResults;
    private final Set<Friend> friends;
    private final Set<RemoteHost> remoteHosts;    
    private final Provider<PropertiableHeadings> propertiableHeadings;
    private Map<FilePropertyKey, Object> properties;
    private BasicDownloadState downloadState = BasicDownloadState.NOT_STARTED;
    private List<VisualSearchResult> similarResults = null;
    private VisualSearchResult similarityParent;
    private boolean anonymous;
    private boolean visible;
    private boolean childrenVisible;    
    private Boolean spamCache;    
    private boolean preExistingDownload = false;    
    private int relevance = -1;    
    private String cachedHeading;    
    private String cachedSubHeading;
    private String cachedAudioArtistName;
    private String cachedName;

    /**
     * Constructs a SearchResultAdapter with the specified List of core results
     * and property values.
     */
    public SearchResultAdapter(List<SearchResult> sourceValue, Provider<PropertiableHeadings> propertiableHeadings) {
        this.coreResults = sourceValue;
        this.propertiableHeadings = propertiableHeadings;
        
        this.friends = new TreeSet<Friend>(new Comparator<Friend>() {
            @Override
            public int compare(Friend o1, Friend o2) {
                String id1 = o1.getId();
                String id2 = o2.getId();
                return Objects.compareToNullIgnoreCase(id1, id2, false);
            }
        });
        
        this.remoteHosts = new TreeSet<RemoteHost>(new Comparator<RemoteHost>() {
            @Override
            public int compare(RemoteHost o1, RemoteHost o2) {
                int compare = 0;
                boolean anonymous1 = o1.getFriendPresence().getFriend().isAnonymous();
                boolean anonymous2 = o2.getFriendPresence().getFriend().isAnonymous();

                if (anonymous1 == anonymous2) {
                    compare = o1.getFriendPresence().getFriend().getRenderName().compareToIgnoreCase(o2.getFriendPresence().getFriend().getRenderName());
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
    public boolean isAnonymous() {
        return anonymous;
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
    public Collection<Friend> getFriends() {
        return friends;
    }

    @Override
    public Map<FilePropertyKey, Object> getProperties() {
        if (properties == null) {
            properties = new EnumMap<FilePropertyKey, Object>(FilePropertyKey.class);
            for (SearchResult result : coreResults) {
                Map<FilePropertyKey, Object> props = result.getProperties();
                properties.putAll(props);
            }
        }

        return properties;
    }

    @Override
    public Object getProperty(FilePropertyKey key) {
        return getProperties().get(key);
    }

    @Override
    public String getPropertyString(FilePropertyKey key) {
        Object value = getProperty(key);
        if (value != null) {
            return value.toString();
        } else {
            return null;
        }
    }

    @Override
    public String getNameProperty(boolean useAudioArtist) {
        String name = getPropertyString(FilePropertyKey.NAME);
        
        // For audio files, use non-blank title, prefixed by non-blank artist.
        if (getCategory().equals(Category.AUDIO)) {
            if(useAudioArtist && cachedAudioArtistName != null) {
                return cachedAudioArtistName;
            } else if(!useAudioArtist && cachedName != null) {
                return cachedName;
            }
            
            String title = getPropertyString(FilePropertyKey.TITLE);
            if(!StringUtils.isEmpty(title)) {
                String artist = getPropertyString(FilePropertyKey.AUTHOR);
                if (useAudioArtist && !StringUtils.isEmpty(artist)) {
                    name = artist + " - " + title;
                } else {
                    name = title;
                }
            }
            
            if(useAudioArtist) {
                cachedAudioArtistName = name;
            } else {
                cachedName = name;
            }
        }
        
        // Return result.
        return name;
    }
    
    @Override
    public void addSimilarSearchResult(VisualSearchResult similarResult) {
        assert similarResult != this;
        if(similarResults == null) {
            similarResults = new CopyOnWriteArrayList<VisualSearchResult>();
        }
        similarResults.add(similarResult);
    }

    @Override
    public void removeSimilarSearchResult(VisualSearchResult result) {
        if(similarResults == null) {
            similarResults = new CopyOnWriteArrayList<VisualSearchResult>();
        }
        similarResults.remove(result);
    }

    @Override
    public List<VisualSearchResult> getSimilarResults() {
        return similarResults == null ? Collections.<VisualSearchResult>emptyList() : similarResults;
    }

    @Override
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
        // If the download was aborted, recalculate the spam score
        if(downloadState == BasicDownloadState.NOT_STARTED) {
            boolean oldSpam = isSpam();
            spamCache = null;
            boolean newSpam = isSpam();
            firePropertyChange("spam-core", oldSpam, newSpam);
        }
        BasicDownloadState oldDownloadState = this.downloadState;
        this.downloadState = downloadState;
        firePropertyChange("downloadState", oldDownloadState, downloadState);
    }

    @Override
    public String toString() {
        return getCoreSearchResults().toString();
    }

    /**
     * Reloads the sources from the core search results. The number of alt-locs
     * is limited to avoid giving high relevance to spam results.
     */
    void update() {
        relevance = -1;
        remoteHosts.clear();
        friends.clear();
        anonymous = false;
        
        for (SearchResult result : coreResults) {
            List<RemoteHost> sources = result.getSources();
            remoteHosts.addAll(sources);
            
            // Build collection of non-anonymous friends for filtering.
            for (RemoteHost source : sources) {
                Friend friend = source.getFriendPresence().getFriend();
                if (friend.isAnonymous()) {
                    anonymous = true;
                } else {
                    friends.add(friend);
                }
            }
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

    @Override
    public boolean isSpam() {
        if (spamCache == null) {
            boolean spam = false;
            for (SearchResult result : coreResults)
                spam |= result.isSpam();
            spamCache = spam;
        }
        return spamCache.booleanValue();
    }

    @Override
    public void setSpam(boolean spam) {
        boolean oldSpam = isSpam();
        spamCache = spam;
        firePropertyChange("spam-ui", oldSpam, spam);
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
    public void toggleChildrenVisibility() {
        setChildrenVisible(!isShowingSimilarResults());
    }

    private boolean isShowingSimilarResults() {
        return getSimilarResults().size() > 0 && isChildrenVisible();
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
        if (cachedHeading == null) {
            cachedHeading =  sanitize(propertiableHeadings.get().getHeading(this));
        }
        return cachedHeading;
    }
    
    /**
     * This method checks for HTML encoding in the content of the supplied string.
     * If found, the encoding is stripped from the string and this result is marked as
     * spam.  
     * @param input
     * @return The stripped string is returned (or the same string if no HTML encoding
     * is found).
     */
    private String sanitize(String input) {
        Matcher matcher = FIND_HTML_MARKUP.reset(input);
        if (matcher.find()) {
            setSpam(true);
            return matcher.replaceAll("");
        }
        return input;
    }

    @Override
    public String getSubHeading() {
        if (cachedSubHeading == null) {
            cachedSubHeading = sanitize(propertiableHeadings.get().getSubHeading(this));
        }
        return cachedSubHeading;
    }

    @Override
    public int getRelevance() {
        if(relevance == -1) {        
            int sum = 0;
            for(SearchResult searchResult : coreResults) {
                sum += searchResult.getRelevance();
            }
            relevance = sum;
        }
        return relevance;
    }


    /**
     * If any of the search results' lime xml docs contains a license string
     * the entire VisualSearchResult is considered "licensed".
     *
     */
    @Override
    public boolean isLicensed() {
        for (SearchResult searchResult : coreResults) {
             if (searchResult.isLicensed()) {
                return true;
             }
        }
        return false;
    }

    @Override
    public boolean isPreExistingDownload() {
        return preExistingDownload;
    }

    @Override
    public void setPreExistingDownload(boolean preExistingDownload) {
        this.preExistingDownload = preExistingDownload;
    }

    @Override
    public int compareTo(Object o) {
        if(!(o instanceof SearchResultAdapter)) 
            return -1;
        
        SearchResultAdapter sra = (SearchResultAdapter) o;
        return getHeading().compareTo(sra.getHeading());
    }
}
