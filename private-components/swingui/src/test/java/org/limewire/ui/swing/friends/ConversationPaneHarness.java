package org.limewire.ui.swing.friends;

import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.limewire.ui.swing.friends.Message.Type;

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

                ConversationPane pane = new ConversationPane("Will Benedict");
                frame.add(pane);
                
                for(int i = 0; i < 10; i++) {
                    pane.handleMessage("foo", new MessageImpl("Foo Bar", 
                            "This is a message This is a message This is a message This is a message This is a message This is a message ",
                            Math.random() < 0.5 ? Type.Received : Type.Sent));
                }

                frame.setPreferredSize(new Dimension(470, 400));

                frame.pack();
                frame.setVisible(true);
            }
        });
    }
    
    private static class MessageImpl implements Message {
        private final String message, sender;
        private final Type type;

        public MessageImpl(String sender, String message, Type type) {
            this.message = message;
            this.sender = sender;
            this.type = type;
        }

        @Override
        public String getMessageText() {
            return message;
        }

        @Override
        public String getSenderName() {
            return sender;
        }

        @Override
        public Type getType() {
            return type;
        }
        
    }
}
