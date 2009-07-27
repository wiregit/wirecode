package org.limewire.core.impl.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.StoreResult;

/**
 * Implementation of StoreResult for the mock core.
 */
public class MockStoreResult implements StoreResult {

    private final Category category;
    private final URN urn;
    private final Map<FilePropertyKey, Object> propertyMap = 
        new EnumMap<FilePropertyKey, Object>(FilePropertyKey.class);
    private final List<SearchResult> fileList;
    
    private String fileExtension;
    private long size;
    
    /**
     * Constructs a MockStoreResult with the specified URN and category.
     */
    public MockStoreResult(URN urn, Category category) {
        this.urn = urn;
        this.category = category;
        this.fileList = new ArrayList<SearchResult>();
    }
    
    @Override
    public Category getCategory() {
        return category;
    }
    
    @Override
    public boolean isCollection() {
        return (fileList.size() > 1);
    }

    @Override
    public String getFileExtension() {
        return fileExtension;
    }
    
    @Override
    public List<SearchResult> getFileList() {
        return fileList;
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
    
    void addFile(SearchResult searchResult) {
        fileList.add(searchResult);
    }
    
    void setFileExtension(String extension) {
        this.fileExtension = extension;
    }
    
    void setProperty(FilePropertyKey key, Object value) {
        propertyMap.put(key, value);
    }
    
    void setSize(long size) {
        this.size = size;
    }
}
