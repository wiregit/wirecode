package org.limewire.ui.swing.friends.chat;

import org.limewire.concurrent.ThreadExecutor;
import org.limewire.xmpp.api.client.ChatState;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.XMPPException;

public class TicTacToeMessageWriter implements MessageWriter {
    private final MessageWriter writer;
    private final String localID;
    private final ChatFriend tictactoeFriend;


    TicTacToeMessageWriter(String localID, TicTacToeFriend tictactoeFriend, MessageWriter writer) {
        this.writer = writer;
        this.localID = localID;
        this.tictactoeFriend = tictactoeFriend;
    }

    @Override
    public void writeMessage(final String message) throws XMPPException {
        ThreadExecutor.startThread(new Runnable() {
            @Override
            public void run() {
                try {
                    writer.writeMessage(message);
                } catch (XMPPException e) {
                    e.printStackTrace();
                }
            }
        }, "send-message");
        System.out.println("TicTacToeMessageWriter new MessageReceivedEvent publish");

        new MessageReceivedEvent(newMessage(message, Message.Type.Sent)).publish();

    }

    public void setChatState(ChatState chatState) throws XMPPException {
        writer.setChatState(chatState);
    }
    private Message newMessage(String message, Message.Type type) {
        return new MessageTextImpl(localID, tictactoeFriend.getID(), type, message);
    }
}
