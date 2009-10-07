package org.limewire.core.impl.search.store;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.store.StoreConnection;
import org.limewire.core.api.search.store.StoreConnectionFactory;
import org.limewire.core.api.search.store.StoreResult;
import org.limewire.core.api.search.store.StoreResultListener;
import org.limewire.core.api.search.store.TrackResult;
import org.limewire.core.impl.MockURN;
import org.limewire.core.impl.friend.MockFriend;
import org.limewire.core.impl.friend.MockFriendPresence;
import org.limewire.friend.api.FriendPresence;
import org.limewire.util.FileUtils;

/**
 * Implementation of StoreResult for the mock core.
 */
public class MockStoreResult implements StoreResult {

    private final StoreConnectionFactory storeConnectionFactory;
    private final List<StoreResultListener> listenerList;
    private final Map<FilePropertyKey, Object> propertyMap;
    private final List<TrackResult> trackList;
    
    private final String albumIconUri;
    private final String albumId;
    private final Category category;
    private final RemoteHost remoteHost;
    private final String fileExtension;
    private final String fileName;
    private final String infoUri;
    private final String price;
    private final long size;
    private final String streamUri;
    private final long trackCount;
    private final URN urn;
    
    private Icon albumIcon;
    
    private boolean albumIconRequested;
    private boolean tracksRequested;
    
    /**
     * Constructs a MockStoreResult using the specified JSON object.
     */
    public MockStoreResult(JSONObject jsonObj, StoreConnectionFactory storeConnectionFactory) throws JSONException {
        this.storeConnectionFactory = storeConnectionFactory;
        listenerList = new CopyOnWriteArrayList<StoreResultListener>();
        propertyMap = new EnumMap<FilePropertyKey, Object>(FilePropertyKey.class);
        trackList = new ArrayList<TrackResult>();
        
        albumIconUri = jsonObj.optString("albumIcon");
        albumId = jsonObj.optString("albumId");
        category = getCategory(jsonObj);
        fileName = jsonObj.getString("fileName");
        fileExtension = FileUtils.getFileExtension(fileName);
        infoUri = jsonObj.getString("infoPage");
        price = jsonObj.optString("price");
        remoteHost = new MockStoreHost();
        size = jsonObj.getLong("fileSize");
        streamUri = jsonObj.optString("streamUrl");
        trackCount = jsonObj.optLong("trackCount");
        urn = new MockURN(jsonObj.getString("URN"));
        
        initProperties(jsonObj);
    }
    
    /**
     * Sets result values using the specified JSON object.
     */
    private void initProperties(JSONObject jsonObj) throws JSONException {
        // Get required attributes.
        propertyMap.put(FilePropertyKey.AUTHOR, jsonObj.getString("artist"));
        propertyMap.put(FilePropertyKey.ALBUM, jsonObj.getString("album"));
        propertyMap.put(FilePropertyKey.TITLE, jsonObj.getString("title"));
        propertyMap.put(FilePropertyKey.BITRATE, jsonObj.getLong("bitRate"));
        propertyMap.put(FilePropertyKey.GENRE, jsonObj.getString("genre"));
        propertyMap.put(FilePropertyKey.LENGTH, jsonObj.getLong("length"));
        propertyMap.put(FilePropertyKey.QUALITY, jsonObj.getLong("quality"));
        
        // Get optional attributes.
        String trackNumber = jsonObj.optString("trackNumber");
        if (trackNumber.length() > 0) propertyMap.put(FilePropertyKey.TRACK_NUMBER, trackNumber);
        
        long year = jsonObj.optLong("year");
        if (year > 0) propertyMap.put(FilePropertyKey.YEAR, year);
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
    public boolean isAlbum() {
        return (albumId != null) && (albumId.length() > 0) && (trackCount > 0);
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

                    // TODO use StoreConnection to load icon
                    albumIcon = new ImageIcon(getClass().getResource(albumIconUri));

                    // Fire event to update UI.
                    fireAlbumIconUpdated();
                }
            }).start();
        }
        
        return albumIcon;
    }
    
    @Override
    public String getAlbumId() {
        return albumId;
    }
    
    @Override
    public Category getCategory() {
        return category;
    }

    @Override
    public String getFileExtension() {
        return fileExtension;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public String getInfoURI() {
        return infoUri;
    }
    
    @Override
    public String getPrice() {
        return price;
    }
    
    @Override
    public Object getProperty(FilePropertyKey key) {
        return propertyMap.get(key);
    }
    
    @Override
    public long getSize() {
        return size;
    }
    
    @Override
    public SortPriority getSortPriority() {
        return SortPriority.TOP;
    }

    @Override
    public RemoteHost getSource() {
        return remoteHost;
    }

    @Override
    public String getStreamURI() {
        return streamUri;
    }
    
    @Override
    public long getTrackCount() {
        return trackCount;
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
                    StoreConnection storeConnection = storeConnectionFactory.create();
                    String jsonStr = storeConnection.loadTracks(albumId);
                    
                    try {
                        // Parse JSON and add tracks.
                        JSONObject jsonObj = new JSONObject(jsonStr);
                        List<TrackResult> newTracks = parseTracks(jsonObj);
                        trackList.addAll(newTracks);
                        
                        // Fire event to update UI.
                        fireTracksUpdated();
                        
                    } catch (JSONException ex) {
                        ex.printStackTrace();
                    }
                }
            }).start();
        }
        
        return trackList;
    }
    
    @Override
    public URN getUrn() {
        return urn;
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
     * Returns the Category from the specified JSON object.
     */
    private Category getCategory(JSONObject jsonObj) throws JSONException {
        String value = jsonObj.getString("category");
        for (Category category : Category.values()) {
            if (category.toString().equalsIgnoreCase(value)) {
                return category;
            }
        }
        throw new JSONException("Invalid result category");
    }
    
    /**
     * Returns a list of track results by parsing the specified JSON object.
     */
    private List<TrackResult> parseTracks(JSONObject jsonObj) throws JSONException {
        List<TrackResult> trackList = new ArrayList<TrackResult>();
        
        JSONArray trackArr = jsonObj.optJSONArray("tracks");
        if ((trackArr != null) && (trackArr.length() > 0)) {
            for (int i = 0, len = trackArr.length(); i < len; i++) {
                JSONObject trackObj = trackArr.getJSONObject(i);
                trackList.add(new MockTrackResult(trackObj));
            }
        }
        
        return trackList;
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
