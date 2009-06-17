package org.limewire.ui.swing.library.sharing;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.friends.login.LoginPopupPanel;

import com.google.inject.Inject;

/** An action that will show the login popup dialog. */
class ShowLoginAction extends AbstractAction {
    
    private final LoginPopupPanel loginPopupPanel;

    @Inject
    public ShowLoginAction(LoginPopupPanel loginPopupPanel) {
        this.loginPopupPanel = loginPopupPanel;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        loginPopupPanel.setVisible(true);
    }
}
