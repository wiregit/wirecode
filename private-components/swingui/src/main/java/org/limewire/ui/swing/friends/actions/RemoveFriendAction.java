package org.limewire.ui.swing.friends.actions;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.NotImplementedException;

import com.google.inject.Inject;

class RemoveFriendAction extends AbstractAction {

    @Inject
    public RemoveFriendAction() {
        super(I18n.tr("Remove Friend..."));
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        //???
        throw new NotImplementedException("Not implemented");
    }
}
