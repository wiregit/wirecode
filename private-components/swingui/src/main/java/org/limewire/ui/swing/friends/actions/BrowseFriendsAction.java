package org.limewire.ui.swing.friends.actions;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.search.FriendPresenceActions;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class BrowseFriendsAction extends AbstractAction {


    private FriendPresenceActions remoteHostActions;

    @Inject
    public BrowseFriendsAction(FriendPresenceActions remoteHostActions) {
        super(I18n.tr("Browse Friends' Files"));
        this.remoteHostActions = remoteHostActions;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {        
        remoteHostActions.browseAllFriends();
    }
}
