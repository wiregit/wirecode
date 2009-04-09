package org.limewire.ui.swing.friends.chat;

import org.limewire.ui.swing.event.AbstractEDTEvent;

public class TicTacToeEvent extends AbstractEDTEvent {
    private final Message message;

    TicTacToeEvent(Message message) {
        this.message = message;
    }
    public static String buildTopic(String gameName) {
        return TicTacToeMessages.TICTACTOE + gameName;
    }
    public Message getMessage() {
        return message;
    }
}

