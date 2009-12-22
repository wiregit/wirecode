package org.limewire.promotion.search;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.URNFactory;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.core.api.search.store.TrackResult;
import org.limewire.util.FileUtils;

/**
 * Implementation of TrackResult for the live core.
 */
class TrackResultAdapter implements TrackResult, PropertiableFile {

    private final Map<FilePropertyKey, Object> propertyMap;
    
    private final String albumId;
    private final String fileExtension;
    private final String fileName;
    private final String price;
    private final long size;
    private final String streamUri;
    private final URN urn;
    private final String cashPrice;
    private final String creditPrice;
    
    /**
     * Constructs a TrackResultAdapter using the specified JSON object.
     */
    TrackResultAdapter(JSONObject jsonObj,
                       URNFactory urnFactory) throws IOException, JSONException {
        propertyMap = new EnumMap<FilePropertyKey, Object>(FilePropertyKey.class);
        
        albumId = jsonObj.optString("albumId");
        fileName = jsonObj.optString("sortableTrackTitle", "") + ".mp3";
        fileExtension = FileUtils.getFileExtension(fileName);
        price = jsonObj.optString("price");
        size = jsonObj.optLong("fileSize");
        streamUri = jsonObj.optString("file");
        urn = urnFactory.create("urn:" + jsonObj.getString("urn"));
        cashPrice = jsonObj.optString("cashPrice", "");
        creditPrice = jsonObj.optString("creditPrice", "");
        
        initProperties(jsonObj);
    }
    
    /**
     * Populates the properties using the specified JSON object.
     */
    private void initProperties(JSONObject jsonObj) throws JSONException {
        propertyMap.put(FilePropertyKey.TITLE, jsonObj.getString("sortableTrackTitle"));
        propertyMap.put(FilePropertyKey.BITRATE, jsonObj.getLong("bitrate"));
        propertyMap.put(FilePropertyKey.LENGTH, jsonObj.getLong("duration"));
        propertyMap.put(FilePropertyKey.QUALITY, jsonObj.getLong("quality"));
        propertyMap.put(FilePropertyKey.TRACK_NUMBER, jsonObj.getString("trackNumber"));
        
        String artist = jsonObj.optString("artist");
        if (artist.length() > 0) propertyMap.put(FilePropertyKey.AUTHOR, artist);
        
        String album = jsonObj.optString("albumTitle");
        if (album.length() > 0) propertyMap.put(FilePropertyKey.ALBUM, album);
        
        long year = jsonObj.optLong("year");
        if (year > 0) propertyMap.put(FilePropertyKey.YEAR, year);
    }

    @Override
    public String getAlbumId() {
        return albumId;
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
        return getCashPrice();
    }

    @Override
    public Object getProperty(FilePropertyKey key) {
        return propertyMap.get(key);
    }

    @Override
    public Category getCategory() {
        return Category.AUDIO;
    }

    @Override
    public String getPropertyString(FilePropertyKey filePropertyKey) {
        Object property = getProperty(filePropertyKey);
        return property != null ? property.toString() : null;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public URI getStreamURI() throws URISyntaxException {
        return new URI(streamUri);
    }

    @Override
    public URN getUrn() {
        return urn;
    }
    
    public String getCashPrice() {
        return cashPrice;
    }

    public String getCreditPrice() {
        return creditPrice;
    }
}
