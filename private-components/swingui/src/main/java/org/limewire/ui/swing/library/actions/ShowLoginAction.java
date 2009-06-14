package org.limewire.ui.swing.library.actions;

import java.awt.event.ActionEvent;

import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.action.AbstractAction;

import com.google.inject.Inject;

@LazySingleton
public class ShowLoginAction extends AbstractAction {

    @Inject
    public ShowLoginAction() {
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        //TODO: this should show the login panel
    }
}
