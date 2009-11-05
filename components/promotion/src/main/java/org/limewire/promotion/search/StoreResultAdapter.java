package org.limewire.promotion.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.Icon;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.store.StoreConnection;
import org.limewire.core.api.search.store.StoreResult;
import org.limewire.core.api.search.store.StoreResultListener;
import org.limewire.core.api.search.store.TrackResult;
import org.limewire.friend.api.FriendPresence;
import org.limewire.io.GUID;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.FileUtils;
import org.limewire.util.StringUtils;

/**
 * Implementation of StoreResult for the live core.
 */
public class StoreResultAdapter implements StoreResult {
    private static final Log LOG = LogFactory.getLog(StoreResultAdapter.class);
    
    private final StoreConnection storeConnection;
    private final List<StoreResultListener> listenerList;
    private final Map<FilePropertyKey, Object> propertyMap;
    private final List<TrackResult> trackList;
    
    private final Type type;
    private final String albumIconUri;
    private final String albumId;
    private final Category category;
    private final String fileExtension;
    private final String fileName;
    private final String infoUri;
    private final String price;
    private final RemoteHost remoteHost;
    private final long size;
    private final SortPriority sortPriority;
    private final String streamUri;
    private final long trackCount;
    private final URN urn;
    
    private Icon albumIcon;
    private String cashPrice;
    private String creditPrice;
    
    private boolean albumIconRequested;
    private boolean tracksRequested;
    
    /**
     * Constructs a StoreResultAdapter using the specified JSON object and
     * store connection.
     */
    public StoreResultAdapter(JSONObject jsonObj, StoreConnection storeConnection) 
        throws IOException, JSONException {
        
        this.storeConnection = storeConnection;
        listenerList = new CopyOnWriteArrayList<StoreResultListener>();
        propertyMap = new EnumMap<FilePropertyKey, Object>(FilePropertyKey.class);
        trackList = new ArrayList<TrackResult>();
        
        type = Type.valueOf(jsonObj.optString("objectType", "").toUpperCase(Locale.US));
        albumIconUri = jsonObj.optString("albumIcon", "");
        albumId = jsonObj.optString("albumId", "");
        category = getCategory(jsonObj);
        fileName = jsonObj.optString("sortableTrackTitle", "") + ".mp3";
        fileExtension = FileUtils.getFileExtension(fileName);
        infoUri = jsonObj.optString("infoPage", "");
        price = jsonObj.optString("price", "");
        remoteHost = new RemostHostImpl(new StorePresence("Store"));
        size = jsonObj.optLong("fileSize");
        sortPriority = getSortPriority(jsonObj);
        streamUri = jsonObj.optString("file", "");
        trackCount = jsonObj.optLong("trackCount");
        String urnString = jsonObj.optString("urn", ""); // TODO fix urns values coming from API
        if(urnString != null && urnString.trim().length() > 0) {
            urn = com.limegroup.gnutella.URN.createUrnFromString("urn:" + urnString);
        } else {
            // TODO horribly broken
            urn = com.limegroup.gnutella.URN.createUrnFromString("urn:guid:" + new GUID());
        }
        cashPrice = jsonObj.optString("cashPrice", "");
        creditPrice = jsonObj.optString("creditPrice", "");
        
        initProperties(jsonObj);
    }
    
    /**
     * Populates the result properties using the specified JSON object.
     */
    private void initProperties(JSONObject jsonObj) throws JSONException {
        propertyMap.put(FilePropertyKey.AUTHOR, jsonObj.optString("artist"));
        propertyMap.put(FilePropertyKey.ALBUM, jsonObj.optString("albumTitle"));
        if(type == Type.TRACK) {
            propertyMap.put(FilePropertyKey.TITLE, jsonObj.optString("sortableTrackTitle"));  // TODO is thie right?
        } else if(type == Type.ALBUM){ 
            propertyMap.put(FilePropertyKey.TITLE, jsonObj.optString("albumTitle")); // TODO is thie right?
        }
        propertyMap.put(FilePropertyKey.BITRATE, 256l);
        propertyMap.put(FilePropertyKey.GENRE, jsonObj.optString("primaryGenre"));
        propertyMap.put(FilePropertyKey.LENGTH, jsonObj.optLong("duration"));
        propertyMap.put(FilePropertyKey.QUALITY, 3l);
        
        String trackNumber = jsonObj.optString("trackNumber");
        if (trackNumber.length() > 0) propertyMap.put(FilePropertyKey.TRACK_NUMBER, trackNumber);
        
        String year = jsonObj.optString("origRelDate");
        if (!StringUtils.isEmpty(year)) propertyMap.put(FilePropertyKey.YEAR, year.substring(0, 4));
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
    public Icon getAlbumIcon() {
        if (isAlbum() && (albumIconUri.length() > 0) && !albumIconRequested) {
            albumIconRequested = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // Use store connection to load album icon.
                    albumIcon = storeConnection.loadIcon(albumIconUri);

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
        return getCashPrice();
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
    public String getStreamURI() {
        return streamUri;
    }

    @Override
    public long getTrackCount() {
        return trackCount;
    }

    public String getCashPrice() {
        return cashPrice;
    }

    public String getCreditPrice() {
        return creditPrice;
    }

    @Override
    public List<TrackResult> getTracks() {
        if (isAlbum() && (trackList.size() == 0) && !tracksRequested) {
            tracksRequested = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int i = 0;
                        while(i < trackCount){
                            // Use store connection to load tracks.
                            String jsonStr = storeConnection.loadTracks(albumId, i);                    
                        
                            // Parse JSON and add tracks.
                            JSONObject jsonObj = new JSONObject(jsonStr);
                            List<TrackResult> newTracks = parseTracks(jsonObj);
                            trackList.addAll(newTracks); 
                            i += newTracks.size();
                        }
                        // Fire event to update UI.
                        fireTracksUpdated();
                        
                    } catch (Exception ex) {
                        LOG.warnf(ex, ex.getMessage());
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

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public boolean isAlbum() {
        return type == Type.ALBUM;
    }
    
    /**
     * Notifies listeners when the album icon is updated.
     */
    private void fireAlbumIconUpdated() {
        for (StoreResultListener listener : listenerList) {
            listener.albumIconUpdated(albumIcon);
        }
    }
    
    /**
     * Notifies listeners when the album tracks are updated.
     */
    private void fireTracksUpdated() {
        for (StoreResultListener listener : listenerList) {
            listener.tracksUpdated(trackList);
        }
    }
    
    /**
     * Returns the Category from the specified JSON object.
     */
    private Category getCategory(JSONObject jsonObj) throws JSONException {
        return Category.AUDIO;
        /*String value = jsonObj.optString("category");
        for (Category category : Category.values()) {
            if (category.toString().equalsIgnoreCase(value)) {
                return category;
            }
        }
        return null;*/
    }
    
    /**
     * Returns the SortPriority from the specified JSON object.
     */
    private SortPriority getSortPriority(JSONObject jsonObj) throws JSONException {
        String value = jsonObj.optString("sortPriority");
        if (value != null && value.length() > 0) {
            for (SortPriority priority : SortPriority.values()) {
                if (priority.toString().equalsIgnoreCase(value)) {
                    return priority;
                }
            }
        }
        return SortPriority.TOP;
    }
    
    /**
     * Returns a list of track results by parsing the specified JSON object.
     */
    private List<TrackResult> parseTracks(JSONObject jsonObj) throws IOException, JSONException {
        List<TrackResult> trackList = new ArrayList<TrackResult>();
        
        JSONArray trackArr = jsonObj.optJSONArray("tracks");
        if ((trackArr != null) && (trackArr.length() > 0)) {
            for (int i = 0, len = trackArr.length(); i < len; i++) {
                JSONObject trackObj = trackArr.getJSONObject(i);
                trackList.add(new TrackResultAdapter(trackObj));
            }
        }
        
        return trackList;
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
