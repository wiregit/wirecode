package org.limewire.ui.swing.filter;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    private Set<Friend> friends = new HashSet<Friend>();
    
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
        return friends;
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
    
    public void addFriend(Friend friend) {
        friends.add(friend);
    }
}
