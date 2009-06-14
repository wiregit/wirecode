package org.limewire.ui.swing.friends.login;

import java.awt.event.ActionEvent;

import javax.swing.JDialog;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

//TODO: temporary class to show sign in panel
public class SignInAction extends AbstractAction {
    
    private final Provider<FriendsSignInPanel> friendsSignInPanel;
    private JDialog dialog;
    
    @Inject
    public SignInAction(Provider<FriendsSignInPanel> friendsSignInPanel) {
        this.friendsSignInPanel = friendsSignInPanel;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if(dialog == null) {
            dialog = FocusJOptionPane.createDialog(I18n.tr("Create List"), null, friendsSignInPanel.get());
            dialog.setResizable(true);
        }
        if(!dialog.isVisible()) {
//            dialog.setLocationRelativeTo(libraryPanel.get());
            dialog.setVisible(true);
        }
    }

}
