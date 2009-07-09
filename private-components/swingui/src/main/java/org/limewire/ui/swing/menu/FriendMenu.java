package org.limewire.ui.swing.menu;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventUtils;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.action.MnemonicMenu;
import org.limewire.ui.swing.components.PlainCheckBoxMenuItemUI;
import org.limewire.ui.swing.components.PlainMenuItemUI;
import org.limewire.ui.swing.friends.actions.AddFriendAction;
import org.limewire.ui.swing.friends.actions.BrowseFriendsAction;
import org.limewire.ui.swing.friends.actions.FriendService;
import org.limewire.ui.swing.friends.actions.LoginAction;
import org.limewire.ui.swing.friends.actions.LogoutAction;
import org.limewire.ui.swing.friends.actions.StatusActions;
import org.limewire.ui.swing.friends.login.AutoLoginService;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

class FriendMenu extends MnemonicMenu {

    private final JMenuItem browseFriendMenuItem;

    private final StatusActions statusActions;
    private final JMenuItem loginMenuItem;
    private final JMenuItem logoutMenuItem;
    private final JMenuItem addFriendMenuItem;
    private final FriendService friendService;
    private final EventBean<FriendConnectionEvent> friendConnectionEventBean;
    private final JSeparator addFriendSeperator;
    private final JSeparator statusSeperator;
    private final JSeparator loginSeperator;
    private final AutoLoginService autoLoginService;

    @Inject
    public FriendMenu(EventBean<FriendConnectionEvent> friendConnectionEventBean,
            BrowseFriendsAction browseFriendAction, StatusActions statusActions,
            AddFriendAction addFriendAction, LoginAction loginAction, LogoutAction logoutAction,
            FriendService friendService, AutoLoginService autoLoginService) {
        super(I18n.tr("&Friends"));
        this.friendConnectionEventBean = friendConnectionEventBean;
        this.browseFriendMenuItem = new JMenuItem(browseFriendAction);
        this.statusActions = statusActions;
        this.loginMenuItem = new JMenuItem(loginAction);
        this.logoutMenuItem = new JMenuItem(logoutAction);
        this.addFriendMenuItem = new JMenuItem(addFriendAction);
        this.friendService = friendService;
        this.addFriendSeperator = new JSeparator();
        this.statusSeperator = new JSeparator();
        this.loginSeperator = new JSeparator();
        this.autoLoginService = autoLoginService;
        updateSignedInStatus();
    }

    @Override
    public JMenuItem add(JMenuItem item) {
        if (item instanceof JCheckBoxMenuItem) {
            item.setUI(new PlainCheckBoxMenuItemUI());
        } else {
            // done here instead of super class because this else statement
            // can effect a wide range of components poorly.
            item.setUI(new PlainMenuItemUI());
        }

        JMenuItem itemReturned = super.add(item);
        return itemReturned;
    }

    @Inject
    void register(ListenerSupport<FriendConnectionEvent> event) {
        event.addListener(new EventListener<FriendConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendConnectionEvent event) {
                switch (event.getType()) {
                case CONNECTED:
                case CONNECTING:
                case CONNECT_FAILED:
                case DISCONNECTED:
                    updateSignedInStatus();
                    break;
                }
            }
        });
    }

    private void updateSignedInStatus() {
        FriendConnection friendConnection = EventUtils.getSource(friendConnectionEventBean);
        boolean signedIn = friendConnection != null && friendConnection.isLoggedIn();
        boolean supportsAddRemoveFriend = signedIn && friendConnection != null
                && friendConnection.supportsAddRemoveFriend();
        boolean supportModeChanges = signedIn && friendConnection != null
                && friendConnection.supportsMode();
        boolean loggingIn = autoLoginService.isAttemptingLogin()
                || (friendConnection != null && friendConnection.isLoggingIn());

        boolean popUpMenuVisible = isPopupMenuVisible();
        setPopupMenuVisible(false);

        removeAll();
        friendService.updateSignedInStatus();
        if (signedIn) {
            add(friendService);
        }
        add(browseFriendMenuItem);
        browseFriendMenuItem.setEnabled(signedIn);
        if (supportsAddRemoveFriend) {
            add(addFriendSeperator);
            add(addFriendMenuItem);
        }
        statusActions.updateSignedInStatus();

        if (supportModeChanges) {
            add(statusSeperator);
            add(statusActions.getAvailableMenuItem());
            add(statusActions.getDnDMenuItem());
        }

        loginMenuItem.setEnabled(!loggingIn);
        add(loginSeperator);
        if (!signedIn) {
            add(loginMenuItem);
        } else {
            add(logoutMenuItem);
        }
        // needed so that the menu does not stay squished after we add in all
        // the new items.
        setPopupMenuVisible(popUpMenuVisible);
    }

}
