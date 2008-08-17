package org.limewire.ui.swing.friends;

import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.limewire.ui.swing.friends.Message.Type;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.XMPPException;
import org.limewire.xmpp.api.client.Presence.Mode;

/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
public class ConversationPaneHarness {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame();

                final MockFriend friend = new MockFriend("Will Benedict", "Just listening to some jams", Mode.available);
                MessageWriter writer = new MessageWriter() {
                    @Override
                    public void writeMessage(String message) throws XMPPException {
                        new MessageReceivedEvent(new MessageImpl("me", friend, message, Type.Sent)).publish();
                    }
                };
                friend.writer = writer;
                ConversationPane pane = new ConversationPane(writer, friend.getName(), new IconLibraryImpl());
                frame.add(pane);
                
                for(int i = 0; i < 10; i++) {
                    new MessageReceivedEvent(
                            new MessageImpl(friend.getName(), friend, 
                                    "This is a message This is a message This is a message This is a message This is a message This is a message ",
                                    Type.Received)).publish();
                }

                frame.setPreferredSize(new Dimension(470, 400));

                frame.pack();
                frame.setVisible(true);
            }
        });
    }
}
