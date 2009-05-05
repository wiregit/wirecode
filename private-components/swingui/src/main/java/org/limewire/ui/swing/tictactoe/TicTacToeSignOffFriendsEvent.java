package org.limewire.ui.swing.tictactoe;

import org.limewire.ui.swing.event.AbstractEDTEvent;

/**
 * Created and published in <code>ChatFramePanel</code> upon you signing out of Friends.
 * Alternatively, if your friend signed out of Friends, this event is created and 
 * published <code>ChatDocumentBuilder</code>'s <code>appendIsTypingMessage</code>.
 * <p>
 * Handled in TicTacToeMiglayout.
 *
 */
public class TicTacToeSignOffFriendsEvent extends AbstractEDTEvent {

    private boolean friendSignedOff = true;
    public TicTacToeSignOffFriendsEvent(boolean friendSignedOff) {
        this.friendSignedOff = friendSignedOff;
    }
    /**
     * 
     * @return true if your friend signed off, false if you signed off
     */
    public boolean getFriendSignedOff() {
        return friendSignedOff;
    }

}
