package org.limewire.ui.swing.friends.actions;

import java.awt.event.ActionEvent;

import javax.swing.JDialog;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.friends.login.FriendsSignInPanel;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class LoginAction extends AbstractAction {

    private final Provider<FriendsSignInPanel> friendsSignInPanel;
    private JDialog dialog;
    
    @Inject
    public LoginAction(Provider<FriendsSignInPanel> friendsSignInPanel) {
        super(I18n.tr("Login"));

        this.friendsSignInPanel = friendsSignInPanel;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        //TODO: display the login overlay
        if(dialog == null) {
            dialog = FocusJOptionPane.createDialog(I18n.tr("Login"), null, friendsSignInPanel.get());
            dialog.setResizable(true);
            dialog.setModal(false);
        }
        if(!dialog.isVisible()) {
//            dialog.setLocationRelativeTo(libraryPanel.get());
            dialog.setVisible(true);
        }
    }
}
