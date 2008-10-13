/**
 * 
 */
package org.limewire.core.impl.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.limewire.core.api.Category;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.impl.search.MockSearch.MockRemoteHost;

public class MockSearchResult implements Cloneable, SearchResult {

    private List<RemoteHost> sources = new ArrayList<RemoteHost>();

    private Map<PropertyKey, Object> properties =
        new HashMap<PropertyKey, Object>();

    private String extension;
    private String urn;
    private Category resultType;
    private long size;
    
    private boolean spam = false;

    public void addSource(String host) {
        sources.add(new MockRemoteHost(host));
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        MockSearchResult copy = (MockSearchResult) super.clone();

        // Copy contents of all the collection fields so they aren't shared.

        copy.sources = new ArrayList<RemoteHost>();
        for (RemoteHost rh : sources) copy.sources.add(rh);

        copy.properties = new HashMap<PropertyKey, Object>();
        for (PropertyKey key : properties.keySet()) {
            copy.properties.put(key, properties.get(key));
        }

        return copy;
    }

    @Override
    public String getFileExtension() {
        return extension;
    }

    @Override
    public Map<PropertyKey, Object> getProperties() {
        return properties;
    }

    @Override
    public Object getProperty(PropertyKey key) {
         return getProperties().get(key);
    }

    @Override
    public Category getCategory() {
        return resultType;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public List<RemoteHost> getSources() {
        return sources;
    }

    @Override
    public String getUrn() {
        return urn;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public void setProperty(PropertyKey key, Object value) {
        properties.put(key, value);
    }

    public void setResultType(Category resultType) {
        this.resultType = resultType;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setUrn(String urn) {
        this.urn = urn;
    }

    @Override
    public String toString() {
        return getClass().getName() + ": " + getProperty(PropertyKey.NAME) + "spam: " + spam;
    }

    @Override
    public boolean isSpam() {
        return spam;
    }

    public void setSpam(boolean spam) {
        this.spam = spam;
    }

    @Override
    public String getFileName() {
        String fileName="";
        if(getProperty(PropertyKey.NAME) != null) {
            fileName = getProperty(PropertyKey.NAME).toString();
        }
        
        if(getFileExtension() != null) {
            fileName += "." + getFileExtension();
        }
        return fileName;
    }
}