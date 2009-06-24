package org.limewire.ui.swing.friends.actions;

import java.awt.event.ActionEvent;

import org.limewire.friend.api.FriendConnection;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.NotImplementedException;

class RemoveFriendAction extends AbstractAction {

    private final FriendConnection friendConnection;

    /**
     * Creates remove friend action.
     * <p>
     * Action is disabled if <code>friendConnection</code> is null or does
     * not support removing friends, see {@link FriendConnection#supportsAddRemoveFriend()}.
     * 
     * @param friendConnection can be null, action will be constructed in a
     * disabled state then
     */
    public RemoveFriendAction(FriendConnection friendConnection) {
        super(I18n.tr("Remove Friend..."));
        this.friendConnection = friendConnection;
        setEnabled(friendConnection != null && friendConnection.supportsAddRemoveFriend());
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        assert friendConnection != null;
        //???
        throw new NotImplementedException("Not implemented");
    }
}
