package org.limewire.ui.swing.friends.chat;

import org.limewire.ui.swing.event.AbstractEDTEvent;

public class CreateTicTacToeFrameEvent extends AbstractEDTEvent {

    private String friendID;
    private String friendNickName;
    private boolean isX;
//    private final TicTacToeMigLayout tictactoePane;

    public CreateTicTacToeFrameEvent(String friendID, String friendNickName, boolean isX) {
        this.friendID = friendID;
        this.friendNickName = friendNickName;
        
        this.isX = isX;
    }
//    public CreateTicTacToeFrameEvent(String friend, TicTacToeMigLayout tictactoePane) {
//        this.friend = friend;
//        this.tictactoePane = tictactoePane;
//    }
    
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
//    public TicTacToeMigLayout getPane() {
//        return tictactoePane;
//    }
}
