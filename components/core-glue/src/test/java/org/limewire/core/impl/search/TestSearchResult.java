package org.limewire.core.impl.search;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.SearchResult;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.IncomingChatListener;
import org.limewire.friend.api.MessageReader;
import org.limewire.friend.api.MessageWriter;
import org.limewire.friend.api.Network;
import org.limewire.friend.api.PresenceEvent;
import org.limewire.friend.api.feature.Feature;
import org.limewire.friend.api.feature.FeatureTransport;
import org.limewire.listener.EventListener;

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
    public List<RemoteHost> getSources() {
        return Collections.<RemoteHost>singletonList(remoteHost);
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
        public boolean isChatEnabled() {
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
        public void addFeature(Feature feature) {
        }

        @Override
        public <D, F extends Feature<D>> void addTransport(Class<F> clazz,
                FeatureTransport<D> transport) {
        }

        @Override
        public Feature getFeature(URI id) {
            return null;
        }

        @Override
        public Collection<Feature> getFeatures() {
            return null;
        }

        @Override
        public Friend getFriend() {
            return friend;
        }

        @Override
        public Mode getMode() {
            return null;
        }

        @Override
        public String getPresenceId() {
            return null;
        }

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public String getStatus() {
            return null;
        }

        @Override
        public <F extends Feature<D>, D> FeatureTransport<D> getTransport(Class<F> feature) {
            return null;
        }

        @Override
        public Type getType() {
            return null;
        }

        @Override
        public boolean hasFeatures(URI... id) {
            return false;
        }

        @Override
        public void removeFeature(URI id) {
        }
    }
    
    private static class TestFriend implements Friend {
        private final String name;
        
        public TestFriend(String name) {
            this.name = name;
        }
        
        @Override
        public void addPresenceListener(EventListener<PresenceEvent> presenceListener) {
        }

        @Override
        public MessageWriter createChat(MessageReader reader) {
            return null;
        }

        @Override
        public FriendPresence getActivePresence() {
            return null;
        }

        @Override
        public String getFirstName() {
            return null;
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
        public Network getNetwork() {
            return null;
        }

        @Override
        public Map<String, FriendPresence> getPresences() {
            return null;
        }

        @Override
        public String getRenderName() {
            return name;
        }

        @Override
        public boolean hasActivePresence() {
            return false;
        }

        @Override
        public boolean isAnonymous() {
            return false;
        }

        @Override
        public boolean isSignedIn() {
            return false;
        }

        @Override
        public boolean isSubscribed() {
            return false;
        }

        @Override
        public void removeChatListener() {
        }

        @Override
        public void setChatListenerIfNecessary(IncomingChatListener listener) {
        }

        @Override
        public void setName(String name) {
        }
    }
}
