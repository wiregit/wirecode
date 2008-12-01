package org.limewire.ui.swing.menu;

import javax.swing.JMenu;

import org.limewire.ui.swing.menu.actions.SignInOutAction;
import org.limewire.ui.swing.menu.actions.SwitchUserAction;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class FriendMenu extends JMenu {
    @Inject
    public FriendMenu(SwitchUserAction switchUserAction, SignInOutAction signInOutAction) {
        super(I18n.tr("Friend"));

        add(switchUserAction);
        add(signInOutAction);
    }
}