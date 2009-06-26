package org.limewire.ui.swing.friends.actions;

import java.awt.event.ActionEvent;

import org.limewire.friend.api.FriendConnection;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.friends.AddFriendDialog;
import org.limewire.ui.swing.util.I18n;

class AddFriendAction extends AbstractAction {

    private final FriendConnection friendConnection;

    /**
     * Creates add friend action.
     * <p>
     * Action is disabled if <code>friendConnection</code> is null or does
     * not support adding friends, see {@link FriendConnection#supportsAddRemoveFriend()}
     * or is not logged in.
     * 
     * @param friendConnection can be null, action will be constructed in a
     * disabled state then
     */
    public AddFriendAction(FriendConnection friendConnection) {
        super(I18n.tr("Add Friend..."));
        this.friendConnection = friendConnection;
        setEnabled(friendConnection != null && friendConnection.supportsAddRemoveFriend() && friendConnection.isLoggedIn());
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        assert friendConnection != null;
        new AddFriendDialog(friendConnection);
    }
}
