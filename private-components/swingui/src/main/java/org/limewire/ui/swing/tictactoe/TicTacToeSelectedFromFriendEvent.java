package org.limewire.ui.swing.tictactoe;

import org.limewire.ui.swing.event.AbstractEDTEvent;
import org.limewire.ui.swing.friends.chat.Message;

/**
 * Created and published in <code>ConversationPane</code>'s Tic Tac Toe (ttt) interceptor when a 
 * ttt message is sent.
 * <p>
 * Handled in <code>TicTacToePane</code> to mark a friend's move in the UI 
 * and, or to cause progress the game (like play another game).
 */
public class TicTacToeSelectedFromFriendEvent extends AbstractEDTEvent {
    private final Message message;

    public TicTacToeSelectedFromFriendEvent(Message message) {
        this.message = message;
    }
    public static String buildTopic(String gameName) {
        return TicTacToeMessages.TICTACTOE + gameName;
    }
    public Message getMessage() {
        return message;
    }

}
