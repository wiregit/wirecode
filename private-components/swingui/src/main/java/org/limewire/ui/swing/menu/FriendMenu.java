package org.limewire.ui.swing.menu;

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
        this.autoLoginService = autoLoginService;

        add(friendService);
        add(browseFriendMenuItem);
        add(addFriendSeperator);
        add(addFriendMenuItem);
        add(statusSeperator);
        add(statusActions.getAvailableMenuItem());
        add(statusActions.getDnDMenuItem());
        addSeparator();
        add(loginMenuItem);
        add(logoutMenuItem);

        updateSignedInStatus();
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
        boolean supportsAddRemoveFriend = signedIn && friendConnection != null && friendConnection.supportsAddRemoveFriend();
        boolean supportModeChanges = signedIn && friendConnection != null && friendConnection.supportsMode();
        boolean loggingIn = autoLoginService.isAttemptingLogin() || (friendConnection != null && friendConnection.isLoggingIn());
        
        browseFriendMenuItem.setEnabled(signedIn);
        addFriendMenuItem.setVisible(supportsAddRemoveFriend);
        addFriendSeperator.setVisible(supportsAddRemoveFriend);
        statusActions.updateSignedInStatus();
        statusActions.getAvailableMenuItem().setVisible(supportModeChanges);
        statusActions.getDnDMenuItem().setVisible(supportModeChanges);
        statusSeperator.setVisible(supportModeChanges);
        loginMenuItem.setVisible(!signedIn);
        loginMenuItem.setEnabled(!loggingIn);
        logoutMenuItem.setVisible(signedIn);
        friendService.updateSignedInStatus();
        friendService.setVisible(signedIn);
    }

}
