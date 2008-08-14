package org.limewire.ui.swing.friends;

import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.limewire.xmpp.api.client.MessageReader;
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

                MessageWriter writer = new MessageWriter() {
                    @Override
                    public void writeMessage(String message) throws XMPPException {
                        //do nothing - simulates sending message on XMPP
                    }
                };
                MockFriend friend = new MockFriend("foo@gmail.com", "Will Benedict", "Just listening to some jams", Mode.available);
                friend.writer = writer;
                ConversationPane pane = new ConversationPane(friend, new IconLibraryImpl());
                frame.add(pane);
                
                MessageReader reader = friend.reader;
                for(int i = 0; i < 10; i++) {
                    reader.readMessage("This is a message This is a message This is a message This is a message This is a message This is a message ");
                }

                frame.setPreferredSize(new Dimension(470, 400));

                frame.pack();
                frame.setVisible(true);
            }
        });
    }
}
