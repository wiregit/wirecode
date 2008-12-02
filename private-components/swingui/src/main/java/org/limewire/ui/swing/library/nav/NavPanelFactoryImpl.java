package org.limewire.ui.swing.library.nav;

import javax.swing.Action;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.listener.EventBroadcaster;
import org.limewire.ui.swing.library.FriendLibraryMediator;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class NavPanelFactoryImpl implements NavPanelFactory {
    private final RemoteLibraryManager remoteLibraryManager;
    private final EventBroadcaster<FriendSelectEvent> friendSelectBroadcaster;
    
    @Inject
    public NavPanelFactoryImpl(RemoteLibraryManager remoteLibraryManager,
            @Named("friendSelection") EventBroadcaster<FriendSelectEvent> friendSelectBroadcaster) {
        this.remoteLibraryManager = remoteLibraryManager;
        this.friendSelectBroadcaster = friendSelectBroadcaster;
    }

    @Override
    public NavPanel createNavPanel(Action action, Friend friend, FriendLibraryMediator libraryPanel) {
        NavPanel navPanel = new NavPanel(action, friend, libraryPanel, remoteLibraryManager, friendSelectBroadcaster);
        return navPanel;
    }

}
