package org.limewire.ui.swing.search.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Icon;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.store.StoreResult;
import org.limewire.core.api.search.store.StoreResultListener;
import org.limewire.core.api.search.store.TrackResult;
import org.limewire.core.api.search.store.StoreResult.SortPriority;
import org.limewire.friend.api.Friend;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule.RowDisplayResult;
import org.limewire.ui.swing.util.PropertiableFileUtils;
import org.limewire.ui.swing.util.PropertiableHeadings;
import org.limewire.util.Objects;

import com.google.inject.Provider;

/**
 * An implementation of VisualStoreResult for displaying results from the 
 * Lime Store.
 */
public class StoreResultAdapter implements VisualStoreResult, Comparable {

    private final StoreResult storeResult;
    private final Provider<PropertiableHeadings> propertiableHeadings;
    private final VisualSearchResultStatusListener changeListener;
    private final Set<RemoteHost> remoteHosts;
    private final Set<Friend> friends;
    
    private BasicDownloadState downloadState = BasicDownloadState.NOT_STARTED;
    private float relevance;
    private String heading;
    private String subHeading;
    private boolean preExistingDownload;    
    private boolean showTracks;
    private RowDisplayResult rowDisplayResult;
    
    /**
     * Constructs a StoreResultAdapter with the specified store result and
     * and heading service.
     */
    public StoreResultAdapter(StoreResult storeResult,
            Provider<PropertiableHeadings> propertiableHeadings,
            VisualSearchResultStatusListener changeListener) {
        this.storeResult = storeResult;
        this.propertiableHeadings = propertiableHeadings;
        this.changeListener = changeListener;
        this.relevance = storeResult.getTrackCount();
        this.remoteHosts = new HashSet<RemoteHost>();
        this.friends = new HashSet<Friend>();
        
        // Add source from store result.  The friend name is displayed in the
        // From widget.
        this.remoteHosts.add(storeResult.getSource());
        this.friends.add(storeResult.getSource().getFriendPresence().getFriend());
        
        initResultListener();
    }
    
    /**
     * Installs a listener for store result updates.
     */
    private void initResultListener() {
        // Add listener for store result updates.
        storeResult.addStoreResultListener(new StoreResultListener() {
            @Override
            public void albumIconUpdated(Icon icon) {
                firePropertyChange("albumIcon", null, icon);
            }

            @Override
            public void tracksUpdated(List<TrackResult> tracks) {
                firePropertyChange("tracks", Collections.emptyList(), tracks);
            }
        });
    }
    
    /**
     * Notifies change listener that the specified property name has changed 
     * values.
     */
    private void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (!Objects.equalOrNull(oldValue, newValue)) {
            changeListener.resultChanged(this, propertyName, oldValue, newValue);
        }
    }
    
    @Override
    public void addSimilarSearchResult(VisualSearchResult similarResult) {
        // Do nothing.
    }

    @Override
    public void removeSimilarSearchResult(VisualSearchResult result) {
        // Do nothing.
    }

    @Override
    public List<SearchResult> getCoreSearchResults() {
        return Collections.emptyList();
    }

    @Override
    public BasicDownloadState getDownloadState() {
        return downloadState;
    }

    @Override
    public String getFileExtension() {
        return storeResult.getFileExtension();
    }

    @Override
    public String getHeading() {
        if (heading == null) {
            heading = propertiableHeadings.get().getHeading(this);
        }
        return heading;
    }

    @Override
    public String getMagnetLink() {
        return null;
    }

    @Override
    public String getNameProperty(boolean useAudioArtist) {
        return PropertiableFileUtils.getNameProperty(this, useAudioArtist);
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
    public float getRelevance() {
        return relevance;
    }
    
    @Override
    public RowDisplayResult getRowDisplayResult() {
        return rowDisplayResult;
    }

    @Override
    public List<VisualSearchResult> getSimilarResults() {
        return Collections.emptyList();
    }

    @Override
    public VisualSearchResult getSimilarityParent() {
        return null;
    }

    @Override
    public long getSize() {
        return storeResult.getSize();
    }

    @Override
    public Collection<RemoteHost> getSources() {
        return remoteHosts;
    }

    @Override
    public String getSubHeading() {
        if (subHeading == null) {
            subHeading = propertiableHeadings.get().getSubHeading(this, storeResult.isAlbum());
        }
        return subHeading;
    }

    @Override
    public boolean isChildrenVisible() {
        return false;
    }

    @Override
    public boolean isLicensed() {
        return true;
    }

    @Override
    public boolean isPreExistingDownload() {
        return preExistingDownload;
    }

    @Override
    public boolean isSpam() {
        return false;
    }

    @Override
    public boolean isVisible() {
        return true;
    }
    
    @Override
    public boolean isShowTracks() {
        return showTracks;
    }
    
    @Override
    public void setShowTracks(boolean showTracks) {
        if (this.showTracks != showTracks) {
            this.showTracks = showTracks;
            this.rowDisplayResult = null;
        }
    }
    
    @Override
    public SortPriority getSortPriority() {
        return storeResult.getSortPriority();
    }
    
    @Override
    public StoreResult getStoreResult() {
        return storeResult;
    }

    @Override
    public void setChildrenVisible(boolean childrenVisible) {
        // Do nothing.
    }

    @Override
    public void setDownloadState(BasicDownloadState downloadState) {
        BasicDownloadState oldDownloadState = this.downloadState;
        this.downloadState = downloadState;
        firePropertyChange("downloadState", oldDownloadState, downloadState);
    }

    @Override
    public void setPreExistingDownload(boolean preExistingDownload) {
        this.preExistingDownload = preExistingDownload;
    }
    
    @Override
    public void setRowDisplayResult(RowDisplayResult rowDisplayResult) {
        this.rowDisplayResult = rowDisplayResult;
    }

    @Override
    public void setSimilarityParent(VisualSearchResult parent) {
        // Do nothing.
    }

    @Override
    public void setSpam(boolean spam) {
        // Do nothing.
    }

    @Override
    public void setVisible(boolean visible) {
        // Do nothing.
    }

    @Override
    public void toggleChildrenVisibility() {
        // Do nothing.
    }

    @Override
    public Object getNavSelectionId() {
        return getUrn();
    }

    @Override
    public Category getCategory() {
        return storeResult.getCategory();
    }

    @Override
    public String getFileName() {
        return storeResult.getFileName();
    }

    @Override
    public Object getProperty(FilePropertyKey key) {
        return storeResult.getProperty(key);
    }

    @Override
    public URN getUrn() {
        return storeResult.getUrn();
    }

    @Override
    public Collection<Friend> getFriends() {
        return friends;
    }

    @Override
    public boolean isAnonymous() {
        return false;
    }

    @Override
    public boolean isStore() {
        return true;
    }
    
    @Override
    public int compareTo(Object o) {
        if(!(o instanceof VisualSearchResult)) {
            return -1;
        }
        
        VisualSearchResult vsr = (VisualSearchResult) o;
        return getHeading().compareTo(vsr.getHeading());
    }
}
