package org.limewire.ui.swing.friends.actions;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.NotImplementedException;

import com.google.inject.Inject;

public class BrowseFriendAction extends AbstractAction {

    @Inject
    public BrowseFriendAction() {
        super(I18n.tr("Browse Friend's Files"));
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        //??
        throw new NotImplementedException("not implemented");
    }
}
