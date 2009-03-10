package org.limewire.ui.swing.friends.chat;

import org.limewire.ui.swing.event.AbstractEDTEvent;

public class CloseTicTacToeEvent extends AbstractEDTEvent {
    private final TicTacToeFriend tictactoeFriend;
    
    public CloseTicTacToeEvent(TicTacToeFriend tictactoeFriend) {
        this.tictactoeFriend = tictactoeFriend;
    }
    
    public TicTacToeFriend getFriend() {
        return tictactoeFriend;
    }

}
