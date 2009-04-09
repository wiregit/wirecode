package org.limewire.ui.swing.friends.chat;

import org.limewire.xmpp.api.client.ChatState;
import org.limewire.xmpp.api.client.MessageReader;

public class TicTacToeMessageReader implements MessageReader {

    private final TicTacToeFriend tictactoeFriend;

    TicTacToeMessageReader(TicTacToeFriend tictactoeFriend) {
        this.tictactoeFriend = tictactoeFriend;
    }
    
    @Override
    public void newChatState(ChatState chatState) {
        // TODO Auto-generated method stub
        System.out.println("in TicTacToeMessageReader#newChatState");
//        new ChatStateEvent(chatFriend, chatState).publish();      
    }

    @Override
    public void readMessage(String message) {
        System.out.println("in TicTacToeMessageReader#readMessage");
        if (message != null) {
            final Message msg = newMessage(message, Message.Type.Received);
            new MessageReceivedEvent(msg).publish();
        }               
    }
    
    private Message newMessage(String message, Message.Type type) {
        return new MessageTextImpl(tictactoeFriend.getName(), tictactoeFriend.getID(), type, message);
    }

}
