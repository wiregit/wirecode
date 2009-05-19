package org.limewire.core.impl.xmpp;

import java.util.ArrayList;
import java.util.Collection;

import org.limewire.concurrent.ListeningFuture;
import org.limewire.concurrent.SimpleFuture;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.client.FriendConnection;
import org.limewire.core.api.friend.client.FriendConnectionConfiguration;

public class MockFriendConnection implements FriendConnection {
    private FriendConnectionConfiguration config;
    
    public MockFriendConnection(FriendConnectionConfiguration config) {
        this.config = config;
    }

    @Override
    public FriendConnectionConfiguration getConfiguration() {
        return config;
    }


    @Override
    public boolean isLoggedIn() {
        return true;
    }

    @Override
    public boolean isLoggingIn() {
        return false;
    }

    @Override
    public ListeningFuture<Void> login() {
        return new SimpleFuture<Void>((Void)null);
    }

    @Override
    public ListeningFuture<Void> logout() {
        return new SimpleFuture<Void>((Void)null);
    }

    @Override
    public ListeningFuture<Void> setMode(FriendPresence.Mode mode) {
        return new SimpleFuture<Void>((Void)null);
    }

    @Override
    public ListeningFuture<Void> addFriend(String id, String name) {
        return new SimpleFuture<Void>((Void)null);
    }

    @Override
    public ListeningFuture<Void> removeFriend(String id) {
        return new SimpleFuture<Void>((Void)null);
    }

    @Override
    public Friend getFriend(String id) {
        return null;
    }

    @Override
    public Collection<Friend> getFriends() {
        return new ArrayList<Friend>();
    }

}
