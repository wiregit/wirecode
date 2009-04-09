package org.limewire.ui.swing.friends.chat;

import org.limewire.concurrent.ThreadExecutor;
import org.limewire.xmpp.api.client.ChatState;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.XMPPException;

public class TicTacToeMessageWriter implements MessageWriter {
//    private final String localID;
//    private final TicTacToeFriend tictactoeFriend;
    private final MessageWriter writer;

    TicTacToeMessageWriter(String localID, TicTacToeFriend tictactoeFriend, MessageWriter writer) {
//        this.localID = localID;
//        this.tictactoeFriend = tictactoeFriend;
        this.writer = writer;
    }

    @Override
    public void writeMessage(final String message) throws XMPPException {
//        System.out.println("in TicTacToeMessageWriter#writeMessage");
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
//        new MessageReceivedEvent(newMessage(message, Message.Type.Sent)).publish();

    }

    /**
     * If necessary, sends a message indicating the new
     * chat state
     *
     * @param chatState
     * @throws XMPPException
     */
    public void setChatState(ChatState chatState) throws XMPPException {
//        System.out.println("in TicTacToeMessageWriter#setChatState");
//        writer.setChatState(chatState);
    }
//    private Message newMessage(String message, Message.Type type) {
////        System.out.println("in writer newMessage");
//        return new MessageTextImpl(localID, tictactoeFriend.getID(), type, message);
//    }

}
