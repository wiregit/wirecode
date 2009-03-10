package org.limewire.ui.swing.menu.actions;

import java.awt.event.ActionEvent;

import org.limewire.core.api.friend.Friend;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.friends.chat.TicTacToeFramePanel;
import org.limewire.ui.swing.friends.chat.TicTacToeFriendListPane;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class TicTacToeAction extends AbstractAction {

    private Friend friend;
    private final TicTacToeFriendListPane friendsPane;
    private final TicTacToeFramePanel friendsPanel;

    @Inject
    TicTacToeAction(TicTacToeFriendListPane friendsPane, TicTacToeFramePanel friendsPanel) {
        super(I18n.tr("&Challenge to a Tic-Tac-Toe game"));
        this.friendsPane = friendsPane;
        this.friendsPanel = friendsPanel;
        setEnabled(false);

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Friend friend = getFriend();
        if (friend != null) {
            friendsPanel.setTicTacToePanelVisible(true);
            friendsPane.fireConversationStarted(friend.getId());
        }
    }

    /** Sets a new friend for this action and updates the enabledness. */
    public void setFriend(Friend friend) {
        this.friend = friend;
        setEnabled(friend != null && !friend.isAnonymous() && !friend.getFriendPresences().isEmpty());
    }

    protected Friend getFriend() {
        return friend;
    }

}
