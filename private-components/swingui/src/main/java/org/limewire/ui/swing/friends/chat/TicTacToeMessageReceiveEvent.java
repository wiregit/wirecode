package org.limewire.ui.swing.friends.chat;

public class TicTacToeMessageReceiveEvent extends MessageReceivedEvent {

    private static final String TOPIC_PREFIX_2 = "tictactoe-";
    
    TicTacToeMessageReceiveEvent(Message message) {
        super(message);
        System.out.println("TicTacToemessagereceivedEvent 1: " + message.toString());
    }
    public static String buildTopic(String conversationName) {
        return TOPIC_PREFIX_2 + conversationName;
    }
}
