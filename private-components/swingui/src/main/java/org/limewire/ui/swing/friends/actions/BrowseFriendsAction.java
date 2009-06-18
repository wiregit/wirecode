package org.limewire.ui.swing.friends.actions;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.search.RemoteHostActions;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class BrowseFriendsAction extends AbstractAction {


    private RemoteHostActions remoteHostActions;

    @Inject
    public BrowseFriendsAction(RemoteHostActions remoteHostActions) {
        super(I18n.tr("Browse Friends' Files"));
        this.remoteHostActions = remoteHostActions;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {        
        remoteHostActions.browseAllFriends();
    }
}
