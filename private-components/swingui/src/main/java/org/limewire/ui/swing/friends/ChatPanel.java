package org.limewire.ui.swing.friends;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.event.EventAnnotationProcessor;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
@Singleton
public class ChatPanel extends JPanel implements Displayable {
    private static final Log LOG = LogFactory.getLog(ChatPanel.class);
    private final ConversationPaneFactory conversationFactory;
    private final JPanel conversationPanel;
    private final FriendsPane friendsPanel;
    private final Map<String, ConversationPane> chats;
    
    @Inject
    public ChatPanel(ConversationPaneFactory conversationFactory, IconLibrary icons, FriendsPane friendsPanel,
            TopPanel topPanel) {
        super(new BorderLayout());
        this.conversationFactory = conversationFactory;
        this.friendsPanel = friendsPanel;
        this.chats = new HashMap<String, ConversationPane>();

        //Dimensions according to the spec
        setPreferredSize(new Dimension(400, 235));
        add(friendsPanel, BorderLayout.WEST);
        add(topPanel, BorderLayout.NORTH);
        conversationPanel = new JPanel(new BorderLayout());
        conversationPanel.add(new JLabel("This is a placeholder"), BorderLayout.CENTER);
        add(conversationPanel, BorderLayout.CENTER);
        
        EventAnnotationProcessor.subscribe(this);
    }
    
    @EventSubscriber
    public void handleAddConversation(ConversationStartedEvent event) {
        LOG.debugf("ConversationStartedEvent with friend: {0}", event.getFriend().getName());
        ConversationPane chatPane = chats.get(event.getFriend().getName());
        if (chatPane == null) {
            chatPane = conversationFactory.create(event.getWriter(), "chat-" + event.getFriend().getName());
            chats.put(event.getFriend().getName(), chatPane);
        }
        
        if (conversationPanel.getComponent(0) != chatPane) {
            conversationPanel.removeAll();
            conversationPanel.add(chatPane, BorderLayout.CENTER);
            //FIXME: Why doesn't add() trigger the repaint that revalidate() does?
            conversationPanel.revalidate();
        }
        event.unlock();
        LOG.debug("unlocked");
    }

    @Override
    public void handleDisplay() {
        // No-op
    }
    
    public void setLoggedInID(String id) {
        friendsPanel.setLoggedInID(id);    
    }
}
