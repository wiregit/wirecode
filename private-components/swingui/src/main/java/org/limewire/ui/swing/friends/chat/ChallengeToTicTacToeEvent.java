package org.limewire.ui.swing.friends.chat;

import org.limewire.ui.swing.event.AbstractEDTEvent;
import org.limewire.xmpp.api.client.MessageWriter;

public class ChallengeToTicTacToeEvent extends AbstractEDTEvent {
    private final Message message;
    private TicTacToeFriend tttFriend;
    private MessageWriter writer;

    public ChallengeToTicTacToeEvent(Message message, TicTacToeFriend tictactoeFriend, MessageWriter writer) {
        System.out.println("ChallengeToTicTacToe 11: " + message.toString());
        this.message = message;
        tttFriend = tictactoeFriend;
        this.writer = writer;
    }

    public ChallengeToTicTacToeEvent(Message message) {
        System.out.println("ChallengeToTicTacToe 11: " + message.toString());
        this.message = message;
        tttFriend = null;
        this.writer = null;
    }

    public Message getMessage() {
        return message;
    }

    public TicTacToeFriend getFriend() {
        return tttFriend;
    }
    public MessageWriter getWriter() {
        return writer;
    }

    public static String buildTopic(String gameName) {
        return TicTacToeMessages.TICTACTOE + gameName;
    }
    
    @Override
    public void publish() {
        System.out.println("ChallengeToTicTacToe publish 11: " + message.toString());
        super.publish(buildTopic(message.getFriendID()));
    }

}
