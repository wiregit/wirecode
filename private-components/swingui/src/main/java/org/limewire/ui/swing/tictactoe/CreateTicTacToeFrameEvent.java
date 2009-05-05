package org.limewire.ui.swing.tictactoe;

import org.limewire.ui.swing.event.AbstractEDTEvent;

/**
 * The Tic Tac Toe game has the friend's id (userName@host.com), a nickname and whether
 * this person is X or O.
 * <p>
 * <code>TicTacToeAction</code> creates and publishes this event when someone 
 * challenges another friend to a game of Tic Tac Toe.
 * Also, <code>ConversationPane</code>'s <code>handleChallengeToPlayTicTacToeAcceptedEvent</code> 
 * creates and publishes to show that someone accepted to play a game.
 * <p>
 * Handled in <code>ConversationPane</code>'s <code>handleCreatePane</code> 
 * where the Tic Tac Toe frame is created.
 */
public class CreateTicTacToeFrameEvent extends AbstractEDTEvent {

    private String friendID;
    private String friendNickName;
    private boolean isX;

    /**
     * Event to create the Tic Tac Toe board, includes your friend's name (added 
     * to the title bar and status messages) and your role.
     * 
     * @param friendID full name of the friend, for example 'userName@host.com'
     * @param friendNickName short name of the person, like 'user'
     * @param isX if true, you are X and get to make the first move
     */
    public CreateTicTacToeFrameEvent(String friendID, String friendNickName, boolean isX) {
        this.friendID = friendID;
        this.friendNickName = friendNickName;
        
        this.isX = isX;
    }
    
    public static String buildTopic(String topic) {
        return TicTacToeMessages.TICTACTOE + topic;
    }
    public String getFriendNickName() {
        return friendNickName;
    }
    public String getFriendID() {
        return friendID;
    }
    public boolean isX() {
        return isX;
    }
}
