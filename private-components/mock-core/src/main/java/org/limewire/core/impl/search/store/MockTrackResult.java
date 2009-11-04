package org.limewire.core.impl.search.store;

import java.util.Map;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.search.store.TrackResult;
import org.limewire.core.impl.MockURN;
import org.limewire.util.FileUtils;

/**
 * Implementation of TrackResult for the mock core.
 */
public class MockTrackResult implements TrackResult {

    private Map<FilePropertyKey, Object> propertyMap;

    private String albumId;
    private String fileExtension;
    private String fileName;
    private String price;
    private long size;
    private String streamUri;
    private MockURN urn;
    
    @Override
    public String getAlbumId() {
        return albumId;
    }

    public void setAlbumId(String albumId) {
        this.albumId = albumId;
    }

    @Override
    public String getFileExtension() {
        return fileExtension;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
        fileExtension = FileUtils.getFileExtension(fileName);
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
    public String getStreamURI() {
        return streamUri;
    }

    public void setStreamUri(String streamUri) {
        this.streamUri = streamUri;
    }

    @Override
    public URN getUrn() {
        return urn;
    }

    public void setUrn(MockURN urn) {
        this.urn = urn;
    }
}
