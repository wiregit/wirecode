package org.limewire.ui.swing.friends.chat;

import java.awt.Dimension;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.limewire.core.impl.library.MockLibraryManager;
import org.limewire.ui.swing.friends.chat.Message.Type;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.IconManagerStub;
import org.limewire.friend.api.ChatState;
import org.limewire.friend.api.FriendException;
import org.limewire.friend.api.MessageWriter;
import org.limewire.friend.api.FriendPresence.Mode;

/**
 *
 */
public class ConversationPaneHarness {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final JFrame frame = new JFrame();

                final MockChatFriend friend = new MockChatFriend("Will Benedict", "Just listening to some jams", Mode.available);
                final MessageWriter writer = new MessageWriter() {
                    @Override
                    public void writeMessage(String message) throws FriendException {
                        new MessageReceivedEvent(new MessageTextImpl("me", friend.getID(), Type.Sent, message)).publish();
                    }

                    @Override
                    public void setChatState(ChatState chatState) {
                        // TODO
                    }
                };
                friend.writer = writer;
                final IconManager iconManager = new IconManagerStub();
                try {
                    //Sleep to give the IconManagerStub a chance to load the local mime-type icons
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                final ChatHyperlinkListenerFactory dummyFactory = new ChatHyperlinkListenerFactory() {
                    @Override
                    public ChatHyperlinkListener create(Conversation chat) {
                        return null;
                    }
                };


                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        ConversationPane pane = new ConversationPane(writer, friend, "me", new MockLibraryManager(), iconManager, null,
                                new IconLibraryImpl(), dummyFactory, new ScheduledThreadPoolExecutor(1));
                        frame.add(pane);
                        
                        for(int i = 0; i < 10; i++) {
                            new MessageReceivedEvent(
                                    new MessageTextImpl(friend.getName(), friend.getID(), Type.Received,
                                            "This is a message This is a message This is a message This is a message This is a message This is a message "
                                            )).publish();
                        }
                        
                        frame.setPreferredSize(new Dimension(470, 400));
                        
                        frame.pack();
                        frame.setVisible(true);
                    }
                });
            }
        });
    }
}
