package org.limewire.ui.swing.search;

import javax.swing.SwingUtilities;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.friends.FriendsPane;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.nav.Navigator;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class RemoteHostActionsImpl implements RemoteHostActions {
    private static final Log LOG = LogFactory.getLog(RemoteHostActionsImpl.class);

    private final RemoteLibraryManager remoteLibraryManager;

    private final ShareListManager shareListManager;

    private final FriendsPane friendsPane;

    private final LibraryNavigator libraryNavigator;

    @Inject
    public RemoteHostActionsImpl(RemoteLibraryManager remoteLibraryManager,
            ShareListManager shareListManager, FriendsPane friendsPane, LibraryNavigator libraryNavigator, Navigator navigator) {
        this.remoteLibraryManager = remoteLibraryManager;
        this.shareListManager = shareListManager;
        this.friendsPane = friendsPane;
        this.libraryNavigator = libraryNavigator;
    }

    @Override
    public void chatWith(RemoteHost person) {
        LOG.debugf("chatWith: {0}", person.getRenderName());
        Friend friend = person.getFriendPresence().getFriend();
        friendsPane.fireConversationStarted(friend);

        // TODO make sure the input box for chat gets focus, the code is
        // calling requestFocusInWindow, but I think it is gettting some
        // weirdness because the search window is currently the active one, not
        // the chat
    }

    @Override
    public void showFilesSharedBy(RemoteHost person) {
        LOG.debugf("showFilesSharedBy: {0}", person.getRenderName());
        Friend friend = person.getFriendPresence().getFriend();
        libraryNavigator.selectFriendLibrary(friend);
    }

    @Override
    public void viewLibraryOf(final RemoteHost person) {
        LOG.debugf("viewLibraryOf: {0}", person.getRenderName());
        remoteLibraryManager.addPresenceLibrary(person.getFriendPresence());
        // Run this later, to allow the library a bit of time to render the friend.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
				libraryNavigator.selectFriendLibrary(person.getFriendPresence().getFriend());
            }
        });
    }

    @Override
    public int getNumberOfSharedFiles(RemoteHost person) {
        Friend friend = person.getFriendPresence().getFriend();
        FriendFileList friendFileList = shareListManager.getFriendShareList(friend);

        if (friendFileList == null) {
            return 0;
        }
        return friendFileList.size();
    }
}
