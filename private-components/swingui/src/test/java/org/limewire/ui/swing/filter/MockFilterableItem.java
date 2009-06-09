package org.limewire.ui.swing.filter;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.friend.Friend;

/**
 * Test implementation of FilterableItem.
 */
public class MockFilterableItem implements FilterableItem {

    private Category category;
    private String name;
    private HashMap<FilePropertyKey, Object> properties = new HashMap<FilePropertyKey, Object>();
    
    public MockFilterableItem(Category category) {
        this.category = category;
    }
    
    public MockFilterableItem(String name) {
        this.name = name;
    }
    
    @Override
    public boolean isAnonymous() {
        return true;
    }
    
    @Override
    public String getFileExtension() {
        return null;
    }
    
    @Override
    public Collection<Friend> getFriends() {
        return Collections.emptySet();
    }

    @Override
    public Map<FilePropertyKey, Object> getProperties() {
        return properties;
    }

    @Override
    public long getSize() {
        return 0;
    }

    @Override
    public boolean isSpam() {
        return false;
    }

    @Override
    public Category getCategory() {
        return category;
    }

    @Override
    public String getFileName() {
        return null;
    }

    @Override
    public Object getProperty(FilePropertyKey key) {
        if (key == FilePropertyKey.NAME) {
            return name;
        } else {
            return getProperties().get(key);
        }
    }

    @Override
    public String getPropertyString(FilePropertyKey filePropertyKey) {
        Object value = getProperties().get(filePropertyKey);
        return (value == null) ? null : value.toString();
    }

    @Override
    public URN getUrn() {
        return null;
    }
}
