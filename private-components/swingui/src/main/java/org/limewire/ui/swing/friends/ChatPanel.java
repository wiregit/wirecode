package org.limewire.ui.swing.friends;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jdesktop.swingx.JXPanel;

import com.google.inject.Inject;

/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
public class ChatPanel extends JXPanel {
    
    @Inject
    public ChatPanel(ConversationPaneFactory conversationFactory, IconLibrary icons) {
        super(new BorderLayout());
        //Dimensions according to the spec
        setPreferredSize(new Dimension(400, 235));
        add(new FriendsPane(icons), BorderLayout.WEST);
        add(new TopPanel(icons), BorderLayout.NORTH);
        JPanel placeholder = new JPanel(new BorderLayout());
        placeholder.add(new JLabel("This is a placeholder"));
        add(placeholder, BorderLayout.CENTER);
    }
}
