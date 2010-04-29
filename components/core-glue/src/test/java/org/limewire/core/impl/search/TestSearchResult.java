package org.limewire.core.impl.search;

import java.net.URI;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.SearchResult;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.Feature;

/**
 * Mock SearchResult for unit tests.
 */
class TestSearchResult implements SearchResult {

    /** The URN value is real. */
    private final URN urn;
    private final RemoteHost remoteHost;
    
    public TestSearchResult(URN urn) {
        this.urn = urn;
        this.remoteHost = new TestRemoteHost("test");
    }
    
    public TestSearchResult(URN urn, String sourceName) {
        this.urn = urn;
        this.remoteHost = new TestRemoteHost(sourceName);
    }
    
    @Override
    public Category getCategory() {
        return null;
    }

    @Override
    public String getFileExtension() {
        return null;
    }

    @Override
    public String getFileName() {
        return null;
    }

    @Override
    public String getFileNameWithoutExtension() {
        return null;
    }

    @Override
    public String getMagnetURL() {
        return null;
    }

    @Override
    public Object getProperty(FilePropertyKey key) {
        return null;
    }

    @Override
    public float getRelevance(String query) {
        return 0;
    }

    @Override
    public long getSize() {
        return 0;
    }

    @Override
    public RemoteHost getSource() {
        return remoteHost;
    }

    @Override
    public URN getUrn() {
        return urn;
    }

    @Override
    public boolean isLicensed() {
        return false;
    }

    @Override
    public boolean isSpam() {
        return false;
    }

    private static class TestRemoteHost implements RemoteHost {
        private final String description;
        
        public TestRemoteHost(String description) {
            this.description = description;
        }
        
        @Override
        public FriendPresence getFriendPresence() {
            return new TestFriendPresence(new TestFriend(description));
        }

        @Override
        public boolean isBrowseHostEnabled() {
            return false;
        }

        @Override
        public boolean isSharingEnabled() {
            return false;
        }
    }
    
    private static class TestFriendPresence implements FriendPresence {
        private final Friend friend;

        public TestFriendPresence(Friend friend) {
            this.friend = friend;
        }
        
        @Override
        public Feature getFeature(URI id) {
            return null;
        }

        @Override
        public Friend getFriend() {
            return friend;
        }

        @Override
        public String getPresenceId() {
            return null;
        }
        @Override
        public boolean hasFeatures(URI... id) {
            return false;
        }
    }
    
    private static class TestFriend implements Friend {
        private final String name;
        
        public TestFriend(String name) {
            this.name = name;
        }
        
        @Override
        public String getId() {
            return name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getRenderName() {
            return name;
        }
    }
}
