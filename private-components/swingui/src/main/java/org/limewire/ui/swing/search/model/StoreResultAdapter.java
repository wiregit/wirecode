package org.limewire.ui.swing.search.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.store.StoreResult;
import org.limewire.core.api.search.store.StoreResult.SortPriority;
import org.limewire.friend.api.Friend;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule.RowDisplayResult;
import org.limewire.ui.swing.util.PropertiableFileUtils;
import org.limewire.ui.swing.util.PropertiableHeadings;

import com.google.inject.Provider;

/**
 * An implementation of VisualSearchResult for results from the Lime Store.
 */
public class StoreResultAdapter implements VisualStoreResult, Comparable {

    private final StoreResult storeResult;
    private final Provider<PropertiableHeadings> propertiableHeadings;
    private final Set<RemoteHost> remoteHosts;
    private final Set<Friend> friends;
    
    private BasicDownloadState downloadState = BasicDownloadState.NOT_STARTED;
    private int relevance;
    private String heading;
    private String subHeading;
    private boolean showTracks = true;
    private RowDisplayResult rowDisplayResult;
    
    public StoreResultAdapter(StoreResult storeResult,
            Provider<PropertiableHeadings> propertiableHeadings) {
        this.storeResult = storeResult;
        this.propertiableHeadings = propertiableHeadings;
        this.relevance = storeResult.getAlbumResults().size();
        this.remoteHosts = new HashSet<RemoteHost>();
        this.friends = new HashSet<Friend>();
        
        // Add source for store result.
        this.remoteHosts.add(storeResult.getSource());
        this.friends.add(storeResult.getSource().getFriendPresence().getFriend());
    }
    
    @Override
    public void addSimilarSearchResult(VisualSearchResult similarResult) {
    }

    @Override
    public void removeSimilarSearchResult(VisualSearchResult result) {
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
    public int getRelevance() {
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
            subHeading = propertiableHeadings.get().getSubHeading(this);
        }
        return subHeading;
    }

    @Override
    public boolean isChildrenVisible() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isLicensed() {
        return true;
    }

    @Override
    public boolean isPreExistingDownload() {
        // TODO Auto-generated method stub
        return false;
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
        // TODO Auto-generated method stub
    }

    @Override
    public void setDownloadState(BasicDownloadState downloadState) {
        this.downloadState = downloadState;
    }

    @Override
    public void setPreExistingDownload(boolean preExistingDownload) {
        // TODO Auto-generated method stub
    }
    
    @Override
    public void setRowDisplayResult(RowDisplayResult rowDisplayResult) {
        this.rowDisplayResult = rowDisplayResult;
    }

    @Override
    public void setSimilarityParent(VisualSearchResult parent) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setSpam(boolean spam) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setVisible(boolean visible) {
        // TODO Auto-generated method stub
    }

    @Override
    public void toggleChildrenVisibility() {
        // TODO Auto-generated method stub
    }

    @Override
    public Object getNavSelectionId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Category getCategory() {
        return storeResult.getCategory();
    }

    @Override
    public String getFileName() {
        // TODO Auto-generated method stub
        return null;
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
    public int compareTo(Object o) {
        if(!(o instanceof VisualSearchResult)) {
            return -1;
        }
        
        VisualSearchResult vsr = (VisualSearchResult) o;
        return getHeading().compareTo(vsr.getHeading());
    }
}