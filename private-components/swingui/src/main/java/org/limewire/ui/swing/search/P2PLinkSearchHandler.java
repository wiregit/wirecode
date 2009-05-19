package org.limewire.ui.swing.search;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.Network;
import org.limewire.core.api.friend.client.IncomingChatListener;
import org.limewire.core.api.friend.client.MessageReader;
import org.limewire.core.api.friend.client.MessageWriter;
import org.limewire.core.api.friend.feature.features.AddressFeature;
import org.limewire.core.api.friend.impl.AbstractFriendPresence;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.io.Address;
import org.limewire.io.ConnectableImpl;
import org.limewire.listener.EventListener;
import org.limewire.net.address.AddressFactory;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.xmpp.api.client.PresenceEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class P2PLinkSearchHandler implements SearchHandler {
    
    private final RemoteLibraryManager remoteLibraryManager;
    private final LibraryNavigator libraryNavigator;
    private final AddressFactory addressFactory;

    @Inject
    P2PLinkSearchHandler(RemoteLibraryManager remoteLibraryManager, LibraryNavigator libraryNavigator,
                         AddressFactory addressFactory) {
        this.remoteLibraryManager = remoteLibraryManager;
        this.libraryNavigator = libraryNavigator;
        this.addressFactory = addressFactory;
    }
    
    @Override
    public boolean doSearch(SearchInfo info) {
        String q = info.getSearchQuery().substring("p2p://".length());
        final FriendPresence presence = createFriendPresence(q);
        remoteLibraryManager.addPresenceLibrary(presence);
        // Run this later, to allow the library a bit of time to render the friend.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                libraryNavigator.selectFriendLibrary(presence.getFriend());
            }
        });
        return true;
    }
    
    private FriendPresence createFriendPresence(String info) {
        return new SimpleFriendPresence(info, addressFactory);
    }
    
    private static class SimpleFriendPresence extends AbstractFriendPresence implements FriendPresence {
        private final String id;
        private final String name;
        
        public SimpleFriendPresence(String info, AddressFactory addressFactory) {
            Address address;
            try {
                 address = addressFactory.deserialize(info);
            } catch(IOException ioe) {
                address = ConnectableImpl.INVALID_CONNECTABLE;
            }

            addFeature(new AddressFeature(address));
            this.id = info;
            this.name = address.getAddressDescription();
        }

        @Override
        public Friend getFriend() {
            return new SimpleFriend(this, name);
        }

        @Override
        public String getPresenceId() {
            return id;
        }

        @Override
        public Type getType() {
            return Type.available;
        }

        @Override
        public String getStatus() {
            return "";
        }

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public Mode getMode() {
            return Mode.available;
        }
    }
    
    private static class SimpleFriend implements Friend {
        private final String name;
        private final FriendPresence presence;
        
        public SimpleFriend(FriendPresence presence, String name) {
            this.presence = presence;
            this.name = name;
        }

        @Override
        public Map<String, FriendPresence> getPresences() {
            Map<String, FriendPresence> map = new HashMap<String, FriendPresence>();
            map.put(presence.getPresenceId(), presence);
            return map;
        }

        @Override
        public String getId() {
            return presence.getPresenceId();
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
        public String getRenderName() {
            return name;
        }

        @Override
        public String getFirstName() {
            return name;
        }

        @Override
        public boolean isAnonymous() {
            return true;
        }

        @Override
        public void setName(String name) {
        }

        @Override
        public void addPresenceListener(EventListener<PresenceEvent> presenceListener) {
        }

        @Override
        public MessageWriter createChat(MessageReader reader) {
            return null;
        }

        @Override
        public void setChatListenerIfNecessary(IncomingChatListener listener) {
        }

        @Override
        public void removeChatListener() {
        }

        @Override
        public FriendPresence getActivePresence() {
            return null;
        }

        @Override
        public boolean hasActivePresence() {
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
    }

}
