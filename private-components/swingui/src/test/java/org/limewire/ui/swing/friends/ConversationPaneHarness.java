package org.limewire.ui.swing.friends;

import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.limewire.ui.swing.friends.Message.Type;
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

                ConversationPane pane = new ConversationPane(new MockFriend("foo@gmail.com", "Will Benedict", "Just listening to some jams", Mode.available),
                        new IconLibraryImpl());
                frame.add(pane);
                
                for(int i = 0; i < 10; i++) {
                    pane.handleMessage("foo", new MockMessage("Foo Bar", 
                            "This is a message This is a message This is a message This is a message This is a message This is a message ",
                            Math.random() < 0.5 ? Type.Received : Type.Sent));
                }

                frame.setPreferredSize(new Dimension(470, 400));

                frame.pack();
                frame.setVisible(true);
            }
        });
    }
}
