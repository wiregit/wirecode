package org.limewire.ui.swing.friends;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.swingx.JXPanel;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
@Singleton
public class ChatPanel extends JXPanel {
    private final ConversationPaneFactory conversationFactory;
    private JPanel conversationPanel;
    
    @Inject
    public ChatPanel(ConversationPaneFactory conversationFactory, IconLibrary icons) {
        super(new BorderLayout());
        this.conversationFactory = conversationFactory;
        
        //Dimensions according to the spec
        setPreferredSize(new Dimension(400, 235));
        add(new FriendsPane(icons), BorderLayout.WEST);
        add(new TopPanel(icons), BorderLayout.NORTH);
        conversationPanel = new JPanel(new BorderLayout());
        conversationPanel.add(new JLabel("This is a placeholder"));
        add(conversationPanel, BorderLayout.CENTER);
        
        AnnotationProcessor.process(this);
    }
    
    @EventSubscriber
    public void handleAddConversation(ConversationStartedEvent event) {
        conversationPanel.removeAll();
        conversationPanel.add(conversationFactory.create(event.getFriend()), BorderLayout.CENTER);
        //FIXME: Why doesn't add() trigger the repaint that revalidate() does?
        conversationPanel.revalidate();
    }
}
