package org.limewire.ui.swing.friends.actions;

import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.limewire.ui.swing.friends.login.AutoLoginService;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class FriendButtonPopupListener implements PopupMenuListener {

    private final Provider<XMPPService> xmppServiceProvider;
    private final Provider<LoginAction> loginAction;
    private final Provider<LogoutAction> logoutAction;
    private final Provider<AddFriendAction> addFriendAction;
    private final Provider<RemoveFriendAction> removeFriendAction;
    private final Provider<BrowseFriendsAction> browseFriendAction;
    private final Provider<AutoLoginService> autoLoginServiceProvider;
    
    @Inject
    public FriendButtonPopupListener(Provider<XMPPService> xmppServiceProvider, Provider<LoginAction> loginAction,
        Provider<LogoutAction> logoutAction, Provider<AddFriendAction> addFriendAction,
        Provider<RemoveFriendAction> removeFriendAction, Provider<BrowseFriendsAction> browseFriendAction,
        Provider<AutoLoginService> autoLoginServiceProvider) {
        this.xmppServiceProvider = xmppServiceProvider;
        this.loginAction = loginAction;
        this.logoutAction = logoutAction;
        this.addFriendAction = addFriendAction;
        this.removeFriendAction = removeFriendAction;
        this.browseFriendAction = browseFriendAction;
        this.autoLoginServiceProvider = autoLoginServiceProvider;
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
            
            XMPPService xmppService = xmppServiceProvider.get();
            
            boolean canLogout = xmppService.isLoggedIn() || xmppService.isLoggingIn();
            boolean shouldAllowLogin = !canLogout && !autoLoginServiceProvider.get().isAttemptingLogin();

            menu.add(browseFriendAction.get()).setEnabled(shouldAllowLogin);
            menu.addSeparator();
            menu.add(addFriendAction.get()).setEnabled(shouldAllowLogin);
            menu.add(removeFriendAction.get()).setEnabled(shouldAllowLogin);
            menu.addSeparator();
            if(shouldAllowLogin)
                menu.add(loginAction.get());
            else if (canLogout)
                menu.add(logoutAction.get());
        }
    }
}
