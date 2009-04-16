package org.limewire.ui.swing.friends.chat;

import org.limewire.core.api.friend.Friend;
import org.limewire.ui.swing.event.AbstractEDTEvent;

public class TicTacToeCreatePanelEvent extends AbstractEDTEvent {
    Friend friend;
    public TicTacToeCreatePanelEvent(Friend friend) {
        this.friend = friend;
    }
    public TicTacToeCreatePanelEvent() {
        this.friend = null;
    }
    public Friend getFriend() {
        return friend;
    }

}
