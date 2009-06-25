package org.limewire.ui.swing.friends.actions;

import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventUtils;
import org.limewire.ui.swing.friends.login.AutoLoginService;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class FriendButtonPopupListener implements PopupMenuListener {

    private final Provider<LoginAction> loginAction;
    private final Provider<BrowseFriendsAction> browseFriendAction;
    private final Provider<AutoLoginService> autoLoginServiceProvider;
    private final EventBean<FriendConnectionEvent> connectionEventBean;
    private final Provider<StatusActions> statusActions;
    
    @Inject
    public FriendButtonPopupListener(EventBean<FriendConnectionEvent> connectionEventBean, Provider<LoginAction> loginAction,
        Provider<BrowseFriendsAction> browseFriendAction,
        Provider<AutoLoginService> autoLoginServiceProvider,
        Provider<StatusActions> statusActions) {
        this.connectionEventBean = connectionEventBean;
        this.loginAction = loginAction;
        this.browseFriendAction = browseFriendAction;
        this.autoLoginServiceProvider = autoLoginServiceProvider;
        this.statusActions = statusActions;
    }
    
    @Override
    public void popupMenuCanceled(PopupMenuEvent e) {}

    @Override
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

    @Override
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        if(e.getSource() instanceof JPopupMenu) {
            JPopupMenu menu = (JPopupMenu) e.getSource();
            
            menu.removeAll();
            
            FriendConnection friendConnection = EventUtils.getSource(connectionEventBean);
            
            boolean canLogout = friendConnection != null && (friendConnection.isLoggedIn() || friendConnection.isLoggingIn());
            boolean shouldAllowLogin = !canLogout && !autoLoginServiceProvider.get().isAttemptingLogin();

            menu.add(browseFriendAction.get()).setEnabled(!shouldAllowLogin);
            menu.addSeparator();
            menu.add(new AddFriendAction(friendConnection)).setEnabled(!shouldAllowLogin);
            menu.add(new RemoveFriendAction(friendConnection)).setEnabled(!shouldAllowLogin);
            menu.addSeparator();
            menu.add(statusActions.get().getAvailableMenuItem());
            menu.add(statusActions.get().getDnDMenuItem());
            menu.addSeparator();
            if(shouldAllowLogin)
                menu.add(loginAction.get());
            else if (canLogout)
                menu.add(new LogoutAction(friendConnection));
        }
    }
}
