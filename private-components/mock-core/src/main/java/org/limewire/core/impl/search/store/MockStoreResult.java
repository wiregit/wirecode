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
import org.limewire.core.api.search.store.StoreTrackResult;
import org.limewire.core.impl.MockURN;
import org.limewire.core.impl.friend.MockFriend;
import org.limewire.core.impl.friend.MockFriendPresence;
import org.limewire.friend.api.FriendPresence;

/**
 * Implementation of StoreResult for the mock core.
 */
public class MockStoreResult implements StoreResult {

    private final Category category;
    private final URN urn;
    private final RemoteHost remoteHost;
    private final Map<FilePropertyKey, Object> propertyMap;
    private final List<StoreTrackResult> trackList;
    
    private Icon albumIcon;
    private String fileExtension;
    private String fileName;
    private String infoUri;
    private String price;
    private long size;
    
    /**
     * Constructs a MockStoreResult using the specified JSON object.
     */
    public MockStoreResult(JSONObject jsonObj) throws JSONException {
        this.category = getCategory(jsonObj);
        this.urn = new MockURN(jsonObj.getString("URN"));
        this.remoteHost = new MockStoreHost();
        this.propertyMap = new EnumMap<FilePropertyKey, Object>(FilePropertyKey.class);
        this.trackList = new ArrayList<StoreTrackResult>();
        
        buildResult(jsonObj);
        buildTracks(jsonObj);
    }
    
    /**
     * Sets result values using the specified JSON object.
     */
    private void buildResult(JSONObject jsonObj) throws JSONException {
        // Get required attributes.
        propertyMap.put(FilePropertyKey.AUTHOR, jsonObj.getString("artist"));
        propertyMap.put(FilePropertyKey.ALBUM, jsonObj.getString("album"));
        propertyMap.put(FilePropertyKey.TITLE, jsonObj.getString("title"));
        fileName = jsonObj.getString("fileName");
        fileExtension = getFileExtension(fileName);
        infoUri = jsonObj.getString("infoPage");
        
        // Get optional attributes.
        albumIcon = getAlbumIcon(jsonObj);
        price = jsonObj.optString("price");
        size = jsonObj.optLong("fileSize");
        
        long length = jsonObj.optLong("length");
        if (length > 0) propertyMap.put(FilePropertyKey.LENGTH, length);
        
        long quality = jsonObj.optLong("quality");
        if (quality > 0) propertyMap.put(FilePropertyKey.QUALITY, quality);
        
        String trackNumber = jsonObj.optString("trackNumber");
        if (trackNumber.length() > 0) propertyMap.put(FilePropertyKey.TRACK_NUMBER, trackNumber);
    }
    
    /**
     * Sets album track values using the specified JSON object.
     */
    private void buildTracks(JSONObject jsonObj) throws JSONException {
        JSONArray trackArr = jsonObj.optJSONArray("tracks");
        if ((trackArr != null) && (trackArr.length() > 0)) {
            for (int i = 0, len = trackArr.length(); i < len; i++) {
                JSONObject trackObj = trackArr.getJSONObject(i);
                trackList.add(new MockStoreTrackResult(trackObj));
            }
        }
    }
    
    @Override
    public boolean isAlbum() {
        return (trackList.size() > 0);
    }
    
    @Override
    public Icon getAlbumIcon() {
        return albumIcon;
    }
    
    @Override
    public List<StoreTrackResult> getAlbumResults() {
        return trackList;
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
    
    private String getFileExtension(String fileName) {
        int pos = fileName.lastIndexOf('.');
        if (pos > -1) {
            return fileName.substring(pos + 1);
        }
        return "";
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
