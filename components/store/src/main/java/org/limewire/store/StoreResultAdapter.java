package org.limewire.store;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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
import org.limewire.friend.api.FriendPresence;
import org.limewire.util.FileUtils;

/**
 * Implementation of StoreResult for the live core.
 */
public class StoreResultAdapter implements StoreResult {

    private final Map<FilePropertyKey, Object> propertyMap;
    private final List<TrackResult> trackList;
    
    private final Icon albumIcon;
    private final Category category;
    private final String fileExtension;
    private final String fileName;
    private final String infoUri;
    private final String price;
    private final RemoteHost remoteHost;
    private final long size;
    private final SortPriority sortPriority;
    private final URN urn;
    
    /**
     * Constructs a StoreResultAdapter using the specified JSON object.
     */
    public StoreResultAdapter(JSONObject jsonObj) throws IOException, JSONException {
        propertyMap = new EnumMap<FilePropertyKey, Object>(FilePropertyKey.class);
        trackList = new ArrayList<TrackResult>();
        
        albumIcon = getIcon(jsonObj, "albumIcon");
        category = getCategory(jsonObj);
        fileName = jsonObj.getString("fileName");
        fileExtension = FileUtils.getFileExtension(fileName);
        infoUri = jsonObj.getString("infoPage");
        price = jsonObj.optString("price");
        remoteHost = new RemostHostImpl(new StorePresence("Store"));
        size = jsonObj.getLong("fileSize");
        sortPriority = getSortPriority(jsonObj);
        urn = com.limegroup.gnutella.URN.createUrnFromString(jsonObj.getString("URN"));
        
        initProperties(jsonObj);
        initTracks(jsonObj);
    }
    
    /**
     * Populates the result properties using the specified JSON object.
     */
    private void initProperties(JSONObject jsonObj) throws JSONException {
        propertyMap.put(FilePropertyKey.AUTHOR, jsonObj.getString("artist"));
        propertyMap.put(FilePropertyKey.ALBUM, jsonObj.getString("album"));
        propertyMap.put(FilePropertyKey.TITLE, jsonObj.getString("title"));
        propertyMap.put(FilePropertyKey.BITRATE, jsonObj.getLong("bitRate"));
        propertyMap.put(FilePropertyKey.GENRE, jsonObj.getString("genre"));
        propertyMap.put(FilePropertyKey.LENGTH, jsonObj.getLong("length"));
        propertyMap.put(FilePropertyKey.QUALITY, jsonObj.getLong("quality"));
        
        String trackNumber = jsonObj.optString("trackNumber");
        if (trackNumber.length() > 0) propertyMap.put(FilePropertyKey.TRACK_NUMBER, trackNumber);
        
        long year = jsonObj.optLong("year");
        if (year > 0) propertyMap.put(FilePropertyKey.YEAR, year);
    }
    
    /**
     * Populates the tracks using the specified JSON object.
     */
    private void initTracks(JSONObject jsonObj) throws IOException, JSONException {
        JSONArray trackArr = jsonObj.optJSONArray("tracks");
        if ((trackArr != null) && (trackArr.length() > 0)) {
            for (int i = 0, len = trackArr.length(); i < len; i++) {
                JSONObject trackObj = trackArr.getJSONObject(i);
                trackList.add(new TrackResultAdapter(trackObj));
            }
        }
    }
    
    @Override
    public Icon getAlbumIcon() {
        return albumIcon;
    }

    @Override
    public List<TrackResult> getAlbumResults() {
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
        return sortPriority;
    }

    @Override
    public RemoteHost getSource() {
        return remoteHost;
    }

    @Override
    public URN getUrn() {
        return urn;
    }

    @Override
    public boolean isAlbum() {
        return (trackList.size() > 0);
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
     * Returns the icon from the specified JSON object and name.
     */
    private Icon getIcon(JSONObject jsonObj, String name) throws MalformedURLException {
        String value = jsonObj.optString(name);
        if (value.length() > 0) {
            return new ImageIcon(new URL(value));
        }
        return null;
    }
    
    /**
     * Returns the SortPriority from the specified JSON object.
     */
    private SortPriority getSortPriority(JSONObject jsonObj) throws JSONException {
        String value = jsonObj.optString("sortPriority");
        if (value.length() > 0) {
            for (SortPriority priority : SortPriority.values()) {
                if (priority.toString().equalsIgnoreCase(value)) {
                    return priority;
                }
            }
        }
        return SortPriority.TOP;
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
