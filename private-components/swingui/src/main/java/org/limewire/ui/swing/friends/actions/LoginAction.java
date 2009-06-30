package org.limewire.ui.swing.friends.actions;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.friends.login.LoginPopupPanel;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class LoginAction extends AbstractAction {

    private final Provider<LoginPopupPanel> friendsSignInPanel;
    
    @Inject
    public LoginAction(Provider<LoginPopupPanel> friendsSignInPanel) {
        super(I18n.tr("Sign in"));

        this.friendsSignInPanel = friendsSignInPanel;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        friendsSignInPanel.get().setVisible(true);
    }
}
