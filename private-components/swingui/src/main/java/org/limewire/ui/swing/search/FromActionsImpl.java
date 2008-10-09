package org.limewire.ui.swing.search;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.friends.FriendsPane;

import com.google.inject.Inject;
import com.google.inject.Singleton;


@Singleton
public class FromActionsImpl implements FromActions {
    private static final Log LOG = LogFactory.getLog(FromActionsImpl.class);

    private final RemoteLibraryManager remoteLibraryManager;

    private final ShareListManager shareListManager;

    private final FriendsPane friendsPane;

    @Inject
    public FromActionsImpl(RemoteLibraryManager remoteLibraryManager,
            ShareListManager shareListManager, FriendsPane friendsPane) {
        this.remoteLibraryManager = remoteLibraryManager;
        this.shareListManager = shareListManager;
        this.friendsPane = friendsPane;
    }

    @Override
    public void chatWith(RemoteHost person) {
        LOG.debugf("chatWith: {0}", person.getRenderName());
        Friend friend = person.getFriendPresence().getFriend();
        friendsPane.fireConversationStarted(friend);
    }

    @Override
    public void showFilesSharedBy(RemoteHost person) {
        LOG.debugf("showFilesSharedBy: {0}", person.getRenderName());
        Friend friend = person.getFriendPresence().getFriend();
        FriendFileList friendFileList = shareListManager.getFriendShareList(friend);
        for (LocalFileItem localFileItem : friendFileList.getModel()) {
            System.out.println(localFileItem.getName());
        }

    }

    @Override
    public void viewLibraryOf(final RemoteHost person) {
        // TODO: Make this work so that friend libraries are jumped to
        // instead of browsed!
        LOG.debugf("viewLibraryOf: {0}", person.getRenderName());
        remoteLibraryManager.addPresenceLibrary(person.getFriendPresence());
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
