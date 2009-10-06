package org.limewire.core.impl.search.store;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.store.StoreResult;
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

    private final Map<FilePropertyKey, Object> propertyMap;
    private final List<TrackResult> trackList;
    
    private final Icon albumIcon;
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
    
    /**
     * Constructs a MockStoreResult using the specified JSON object.
     */
    public MockStoreResult(JSONObject jsonObj) throws JSONException {
        propertyMap = new EnumMap<FilePropertyKey, Object>(FilePropertyKey.class);
        trackList = new ArrayList<TrackResult>();
        
        albumIcon = getAlbumIcon(jsonObj);
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
        initTracks(jsonObj);
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
    
    /**
     * Sets album track values using the specified JSON object.
     */
    private void initTracks(JSONObject jsonObj) throws JSONException {
        JSONArray trackArr = jsonObj.optJSONArray("tracks");
        if ((trackArr != null) && (trackArr.length() > 0)) {
            for (int i = 0, len = trackArr.length(); i < len; i++) {
                JSONObject trackObj = trackArr.getJSONObject(i);
                trackList.add(new MockTrackResult(trackObj));
            }
        }
    }
    
    @Override
    public boolean isAlbum() {
        return (albumId != null) && (albumId.length() > 0) && (trackCount > 0);
    }
    
    @Override
    public Icon getAlbumIcon() {
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
        return trackList;
    }
    
    @Override
    public URN getUrn() {
        return urn;
    }
    
    private Icon getAlbumIcon(JSONObject jsonObj) {
        String url = jsonObj.optString("albumIcon");
        if (url.length() > 0) {
            return new ImageIcon(getClass().getResource(url));
        }
        return null;
    }
    
    private Category getCategory(JSONObject jsonObj) throws JSONException {
        String value = jsonObj.getString("category");
        for (Category category : Category.values()) {
            if (category.toString().equalsIgnoreCase(value)) {
                return category;
            }
        }
        throw new JSONException("Invalid result category");
    }
    
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
