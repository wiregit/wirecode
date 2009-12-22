package org.limewire.ui.swing.search.model;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.Icon;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.store.ReleaseResult;
import org.limewire.core.api.search.store.ReleaseResult.SortPriority;
import org.limewire.core.api.search.store.StoreResultListener;
import org.limewire.core.api.search.store.TrackResult;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.ui.swing.util.PropertiableHeadings;

import com.google.inject.Provider;

/**
 * An implementation of VisualStoreResult for displaying results from the 
 * Lime Store.
 */
class ReleaseResultAdapter extends AbstractResultAdapter implements VisualStoreResult {

    private final ReleaseResult releaseResult;
    private final Provider<PropertiableHeadings> propertiableHeadings;
    private final Set<RemoteHost> remoteHosts;
    private final Set<Friend> friends;
    
    private BasicDownloadState downloadState = BasicDownloadState.NOT_STARTED;
    private float relevance;
    private String heading;
    private String subHeading;   
    private boolean showTracks;

    /**
     * Constructs a StoreResultAdapter with the specified store result and
     * and heading service.
     */
    ReleaseResultAdapter(ReleaseResult releaseResult,
            Provider<PropertiableHeadings> propertiableHeadings,
            VisualSearchResultStatusListener changeListener) {
        super(changeListener);
        this.releaseResult = releaseResult;
        this.propertiableHeadings = propertiableHeadings;
        this.relevance = releaseResult.getTrackCount();
        
        // Add source from store result.  The friend name is displayed in the
        // From widget.
        final StorePresence presence = new StorePresence("Store");
        final RemostHostImpl remostHost = new RemostHostImpl(presence);
        this.remoteHosts = Collections.<RemoteHost>singleton(remostHost);
        this.friends = Collections.singleton(presence.getFriend());
        
        initResultListener();
    }

    /**
     * Installs a listener for store result updates.
     */
    private void initResultListener() {
        // Add listener for store result updates.
        releaseResult.addStoreResultListener(new StoreResultListener() {
            @Override
            public void albumIconUpdated(Icon icon) {
                firePropertyChange(ALBUM_ICON, null, icon);
            }

            @Override
            public void tracksUpdated(List<TrackResult> tracks) {
                firePropertyChange(TRACKS, Collections.emptyList(), tracks);
            }
        });
    }
    
    @Override
    public void addSimilarSearchResult(VisualSearchResult similarResult) {
        throw new IllegalStateException("Cannot add similar result to Store result");
    }

    @Override
    public void removeSimilarSearchResult(VisualSearchResult result) {
        throw new IllegalStateException("Cannot remove similar result from Store result");
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
        return releaseResult.getFileExtension();
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
    public float getRelevance() {
        return relevance;
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
        return releaseResult.getSize();
    }

    @Override
    public Collection<RemoteHost> getSources() {
        return remoteHosts;
    }

    @Override
    public String getSubHeading() {
        if (subHeading == null) {
            subHeading = propertiableHeadings.get().getSubHeading(this, releaseResult.isAlbum());
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
            setRowDisplayResult(null);
        }
    }
    
    @Override
    public SortPriority getSortPriority() {
        return releaseResult.getSortPriority();
    }
    
    @Override
    public ReleaseResult getStoreResult() {
        return releaseResult;
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
    public Category getCategory() {
        return releaseResult.getCategory();
    }

    @Override
    public String getFileName() {
        return releaseResult.getFileName();
    }

    @Override
    public Object getProperty(FilePropertyKey key) {
        return releaseResult.getProperty(key);
    }

    @Override
    public URN getUrn() {
        return releaseResult.getUrn();
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
    
    /**
     * An implementation of RemoteHost for the LimeStore.
     */
    private static class RemostHostImpl implements RemoteHost {
        private final FriendPresence friendPresence;
        
        public RemostHostImpl(FriendPresence friendPresence) {
            this.friendPresence = friendPresence; 
        }
        
        @Override
        public FriendPresence getFriendPresence() {
            return friendPresence;
        }

        @Override
        public boolean isBrowseHostEnabled() {
            return false;
        }

        @Override
        public boolean isChatEnabled() {
            return false;
        }

        @Override
        public boolean isSharingEnabled() {
            return false;
        }
    }
}
