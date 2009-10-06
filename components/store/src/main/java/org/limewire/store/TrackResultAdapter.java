package org.limewire.store;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.search.store.TrackResult;
import org.limewire.util.FileUtils;

/**
 * Implementation of TrackResult for the live core.
 */
public class TrackResultAdapter implements TrackResult {

    private final Map<FilePropertyKey, Object> propertyMap;
    
    private final String fileExtension;
    private final String fileName;
    private final String price;
    private final long size;
    private final URN urn;
    
    /**
     * Constructs a TrackResultAdapter using the specified JSON object.
     */
    public TrackResultAdapter(JSONObject jsonObj) throws IOException, JSONException {
        propertyMap = new EnumMap<FilePropertyKey, Object>(FilePropertyKey.class);
        
        fileName = jsonObj.getString("fileName");
        fileExtension = FileUtils.getFileExtension(fileName);
        price = jsonObj.optString("price");
        size = jsonObj.getLong("fileSize");
        urn = com.limegroup.gnutella.URN.createUrnFromString(jsonObj.getString("URN"));
        
        initProperties(jsonObj);
    }
    
    /**
     * Populates the properties using the specified JSON object.
     */
    private void initProperties(JSONObject jsonObj) throws JSONException {
        propertyMap.put(FilePropertyKey.TITLE, jsonObj.getString("title"));
        propertyMap.put(FilePropertyKey.BITRATE, jsonObj.getLong("bitRate"));
        propertyMap.put(FilePropertyKey.LENGTH, jsonObj.getLong("length"));
        propertyMap.put(FilePropertyKey.QUALITY, jsonObj.getLong("quality"));
        propertyMap.put(FilePropertyKey.TRACK_NUMBER, jsonObj.getString("trackNumber"));
        
        String artist = jsonObj.optString("artist");
        if (artist.length() > 0) propertyMap.put(FilePropertyKey.AUTHOR, artist);
        
        String album = jsonObj.optString("album");
        if (album.length() > 0) propertyMap.put(FilePropertyKey.ALBUM, album);
        
        long year = jsonObj.optLong("year");
        if (year > 0) propertyMap.put(FilePropertyKey.YEAR, year);
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
}
