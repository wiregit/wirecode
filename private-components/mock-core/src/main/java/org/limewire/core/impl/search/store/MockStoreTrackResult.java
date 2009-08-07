package org.limewire.core.impl.search.store;

import java.util.HashMap;
import java.util.Map;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.search.store.StoreTrackResult;
import org.limewire.core.impl.MockURN;

/**
 * Implementation of StoreTrackResult for the mock core.
 */
public class MockStoreTrackResult implements StoreTrackResult {

    private String extension;

    private String price;

    private Map<FilePropertyKey, Object> properties = new HashMap<FilePropertyKey, Object>();

    private long size;
    
    private MockURN urn;
    
    @Override
    public String getFileExtension() {
        return extension;
    }

    @Override
    public String getPrice() {
        return price;
    }
    
    @Override
    public Object getProperty(FilePropertyKey key) {
        return properties.get(key);
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public URN getUrn() {
        return urn;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }
    
    public void setPrice(String price) {
        this.price = price;
    }

    public void setProperty(FilePropertyKey key, Object value) {
        properties.put(key, value);
    }

    public void setSize(long size) {
        this.size = size;
    }
    
    public void setUrn(String string) {
        urn = new MockURN(string);
    }
}
