package org.limewire.core.impl.search.store;

import java.util.EnumMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.search.store.StoreTrackResult;
import org.limewire.core.impl.MockURN;

/**
 * Implementation of StoreTrackResult for the mock core.
 */
public class MockStoreTrackResult implements StoreTrackResult {

    private final Map<FilePropertyKey, Object> propertyMap = 
        new EnumMap<FilePropertyKey, Object>(FilePropertyKey.class);

    private String fileExtension;

    private String fileName;

    private String price;

    private long size;
    
    private MockURN urn;
    
    /**
     * Constructs a MockStoreTrackResult using the specified JSON object.
     */
    public MockStoreTrackResult(JSONObject jsonObj) throws JSONException {
        // Get required attributes.
        propertyMap.put(FilePropertyKey.TITLE, jsonObj.getString("title"));
        fileName = jsonObj.getString("fileName");
        fileExtension = getFileExtension(fileName);
        urn = new MockURN(jsonObj.getString("URN"));

        // Get optional attributes.
        price = jsonObj.optString("price");
        size = jsonObj.optLong("fileSize");

        long length = jsonObj.optLong("length");
        if (length > 0) propertyMap.put(FilePropertyKey.LENGTH, length);
        
        long quality = jsonObj.optLong("quality");
        if (quality > 0) propertyMap.put(FilePropertyKey.QUALITY, quality);
        
        String artist = jsonObj.optString("artist");
        if (artist.length() > 0) propertyMap.put(FilePropertyKey.AUTHOR, artist);
        
        String album = jsonObj.optString("album");
        if (album.length() > 0) propertyMap.put(FilePropertyKey.ALBUM, album);
        
        String trackNumber = jsonObj.optString("trackNumber");
        if (trackNumber.length() > 0) propertyMap.put(FilePropertyKey.TRACK_NUMBER, trackNumber);
    }
    
    @Override
    public String getFileExtension() {
        return fileExtension;
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
    public URN getUrn() {
        return urn;
    }
    
    private String getFileExtension(String fileName) {
        int pos = fileName.lastIndexOf('.');
        if (pos > -1) {
            return fileName.substring(pos + 1);
        }
        return "";
    }
}
