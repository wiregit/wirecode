package org.limewire.ui.swing.friends.actions;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.NotImplementedException;

import com.google.inject.Inject;

public class AddFriendAction extends AbstractAction {

    @Inject
    public AddFriendAction() {
        super(I18n.tr("Add Friend..."));
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        //??
        throw new NotImplementedException("Not implemented");
    }
}
