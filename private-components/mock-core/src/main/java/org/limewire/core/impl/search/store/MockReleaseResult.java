package org.limewire.core.impl.search.store;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.Icon;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.store.ReleaseResult;
import org.limewire.core.api.search.store.StoreResultListener;
import org.limewire.core.api.search.store.TrackResult;
import org.limewire.core.impl.friend.MockFriend;
import org.limewire.core.impl.friend.MockFriendPresence;
import org.limewire.friend.api.FriendPresence;
import org.limewire.util.FileUtils;

/**
 * Implementation of StoreResult for the mock core.
 */
public class MockReleaseResult implements ReleaseResult {

    private final List<StoreResultListener> listenerList;
    private Map<FilePropertyKey, Object> propertyMap;
    private List<TrackResult> trackList;
    
    private Type type;
    private String albumIconUri;
    private String albumId;
    private Category category;
    private RemoteHost remoteHost;
    private String fileExtension;
    private String fileName;
    private String infoUri;
    private String price;
    private long size;
    private String streamUri;
    private long trackCount;
    private URN urn;
    
    private Icon albumIcon;
    
    private boolean albumIconRequested;
    private boolean tracksRequested;
    private final MockStoreConnection storeConnection;

    /**
     * Constructs a MockStoreResult using the specified JSON object.
     */
    public MockReleaseResult(MockStoreConnection storeConnection) {
        this.storeConnection = storeConnection;
        listenerList = new CopyOnWriteArrayList<StoreResultListener>();
        propertyMap = new EnumMap<FilePropertyKey, Object>(FilePropertyKey.class);
        trackList = new ArrayList<TrackResult>();
        remoteHost = new MockStoreHost();
    }

    @Override
    public void addStoreResultListener(StoreResultListener listener) {
        listenerList.add(listener);
    }

    @Override
    public void removeStoreResultListener(StoreResultListener listener) {
        listenerList.remove(listener);
    }

    @Override
    public Type getType() {
        return type;
    }
    
    void setType(Type type) {
        this.type = type;
    }

    @Override
    public boolean isAlbum() {
        return type == Type.ALBUM;
    }
    
    @Override
    public Icon getAlbumIcon() {
        if (isAlbum() && (albumIconUri.length() > 0) && !albumIconRequested) {
            albumIconRequested = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {}

                    // Create connection and load album icon.
                    albumIcon = storeConnection.loadIcon(albumIconUri);

                    // Fire event to update UI.
                    fireAlbumIconUpdated();
                }
            }).start();
        }
        
        return albumIcon;
    }

    public void setAlbumIconUri(String albumIconUri) {
        this.albumIconUri = albumIconUri;
    }

    @Override
    public String getAlbumId() {
        return albumId;
    }

    public void setAlbumId(String albumId) {
        this.albumId = albumId;
    }

    @Override
    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    @Override
    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
        setFileExtension(FileUtils.getFileExtension(fileName));
    }

    @Override
    public String getInfoURI() {
        return infoUri;
    }

    public void setInfoUri(String infoUri) {
        this.infoUri = infoUri;
    }

    @Override
    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    @Override
    public Object getProperty(FilePropertyKey key) {
        return propertyMap.get(key);
    }

    public void setPropertyMap(Map<FilePropertyKey, Object> propertyMap) {
        this.propertyMap = propertyMap;
    }

    @Override
    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @Override
    public SortPriority getSortPriority() {
        return SortPriority.TOP;
    }

    public void setRemoteHost(RemoteHost remoteHost) {
        this.remoteHost = remoteHost;
    }

    @Override
    public String getStreamURI() {
        return streamUri;
    }

    public void setStreamUri(String streamUri) {
        this.streamUri = streamUri;
    }

    @Override
    public long getTrackCount() {
        return trackCount;
    }

    public void setTrackCount(long trackCount) {
        this.trackCount = trackCount;
    }

    @Override
    public List<TrackResult> getTracks() {
        if (isAlbum() && (trackList.size() == 0) && !tracksRequested) {
            tracksRequested = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {}
                    
                    // Create connection and load tracks.
                    List<TrackResult> newTracks = storeConnection.loadTracks(albumId);
                    trackList.addAll(newTracks);

                    // Fire event to update UI.
                    fireTracksUpdated();

                }
            }).start();
        }
        
        return trackList;
    }

    public void setTrackList(List<TrackResult> trackList) {
        this.trackList = trackList;
    }

    @Override
    public URN getUrn() {
        return urn;
    }

    public void setUrn(URN urn) {
        this.urn = urn;
    }

    private void fireAlbumIconUpdated() {
        for (StoreResultListener listener : listenerList) {
            listener.albumIconUpdated(albumIcon);
        }
    }
    
    private void fireTracksUpdated() {
        for (StoreResultListener listener : listenerList) {
            listener.tracksUpdated(trackList);
        }
    }
    
    /**
     * Implementation of RemoteHost for mock store data.
     */
    private class MockStoreHost implements RemoteHost {
        private final FriendPresence friendPresence;

        public MockStoreHost() {
            friendPresence = new MockFriendPresence(new MockFriend("Store", false), "Store");
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
