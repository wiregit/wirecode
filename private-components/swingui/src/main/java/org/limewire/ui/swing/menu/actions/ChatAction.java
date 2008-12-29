package org.limewire.ui.swing.menu.actions;

import java.awt.event.ActionEvent;

import javax.swing.JComponent;

import org.limewire.core.api.friend.Friend;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.friends.chat.ChatFramePanel;
import org.limewire.ui.swing.friends.chat.ChatFriendListPane;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavSelectable;
import org.limewire.ui.swing.nav.NavigationListener;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Starts a chat with the selected user in the left panel if possible.
 */
@Singleton
public class ChatAction extends AbstractAction {

    private Friend friend;
    private final ChatFriendListPane friendsPane;
    private final ChatFramePanel friendsPanel;

    @Inject
    ChatAction(ChatFriendListPane friendsPane, ChatFramePanel friendsPanel) {
        super(I18n.tr("Chat"));
        this.friendsPane = friendsPane;
        this.friendsPanel = friendsPanel;
        setEnabled(false);

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Friend friend = getFriend();
        if (friend != null) {
            friendsPanel.setChatPanelVisible(true);
            friendsPane.fireConversationStarted(friend.getId());
        }
    }

    @Inject
    void register(Navigator navigator, final LibraryNavigator libraryNavigator) {
        // listen for changes in the selected friend
        navigator.addNavigationListener(new NavigationListener() {
            @Override
            public void itemSelected(NavCategory category, NavItem navItem,
                    NavSelectable selectable, JComponent panel) {
                setFriend(libraryNavigator.getSelectedFriend());
            }
            
            @Override public void itemAdded(NavCategory category, NavItem navItem, JComponent panel) {}
            @Override public void itemRemoved(NavCategory category, NavItem navItem, JComponent panel) {}
            @Override public void categoryAdded(NavCategory category) {}
            @Override public void categoryRemoved(NavCategory category) {}
        });
    }

    /** Sets a new friend for this action and calls updates the enabledness. */
    protected void setFriend(Friend friend) {
        this.friend = friend;
        setEnabled(friend != null && !friend.isAnonymous());
    }

    protected Friend getFriend() {
        return friend;
    }
}