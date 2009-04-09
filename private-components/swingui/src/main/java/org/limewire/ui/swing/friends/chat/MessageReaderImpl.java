package org.limewire.ui.swing.friends.chat;

import org.limewire.xmpp.api.client.MessageReader;
import org.limewire.xmpp.api.client.ChatState;

class MessageReaderImpl implements MessageReader {
    private final ChatFriend chatFriend;

    MessageReaderImpl(ChatFriend chatFriend) {
        this.chatFriend = chatFriend;
    }

    @Override
    public void readMessage(final String message) {
        if (message != null) {
//            System.out.println("messagereaderimpl: " + message);
            final Message msg = newMessage(message, Message.Type.Received);
            //TODO send the message through the event bus so it lands in miglayout?            
            if(message.indexOf(TicTacToeMessages.TICTACTOE) > -1) {
                new TicTacToeEvent(msg).publish();
            }

            new MessageReceivedEvent(msg).publish();
        }
    }

    private Message newMessage(String message, Message.Type type) {
        return new MessageTextImpl(chatFriend.getName(), chatFriend.getID(), type, message);
    }

    @Override
    public void newChatState(ChatState chatState) {
        new ChatStateEvent(chatFriend, chatState).publish();
    }
}
