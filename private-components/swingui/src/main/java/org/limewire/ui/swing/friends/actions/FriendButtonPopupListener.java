package org.limewire.ui.swing.friends.actions;

import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class FriendButtonPopupListener implements PopupMenuListener {

    private final Provider<XMPPService> xmppService;
    private final Provider<LoginAction> loginAction;
    private final Provider<LogoutAction> logoutAction;
    private final Provider<AddFriendAction> addFriendAction;
    private final Provider<RemoveFriendAction> removeFriendAction;
    private final Provider<BrowseFriendsAction> browseFriendAction;
    
    @Inject
    public FriendButtonPopupListener(Provider<XMPPService> xmppService, Provider<LoginAction> loginAction,
        Provider<LogoutAction> logoutAction, Provider<AddFriendAction> addFriendAction,
        Provider<RemoveFriendAction> removeFriendAction, Provider<BrowseFriendsAction> browseFriendAction) {
        this.xmppService = xmppService;
        this.loginAction = loginAction;
        this.logoutAction = logoutAction;
        this.addFriendAction = addFriendAction;
        this.removeFriendAction = removeFriendAction;
        this.browseFriendAction = browseFriendAction;
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
            
            boolean isLoggedIn = xmppService.get().isLoggedIn() || xmppService.get().isLoggingIn();
            
            menu.add(browseFriendAction.get()).setEnabled(isLoggedIn);
            menu.addSeparator();
            menu.add(addFriendAction.get()).setEnabled(isLoggedIn);
            menu.add(removeFriendAction.get()).setEnabled(isLoggedIn);
            menu.addSeparator();
            if(isLoggedIn)
                menu.add(logoutAction.get());
            else
                menu.add(loginAction.get());
        }
    }
}
