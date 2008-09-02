package org.limewire.ui.swing.friends;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
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
        Friend friend = event.getFriend();
        LOG.debugf("ConversationStartedEvent with friend: {0}", friend.getName());
        ConversationPane chatPane = chats.get(friend.getName());
        if (chatPane == null) {
            chatPane = conversationFactory.create(event.getWriter(), friend);
            chats.put(friend.getName(), chatPane);
        }
        
        if (conversationPanel.getComponent(0) != chatPane && event.isLocallyInitiated()) {
            setConversationPanel(chatPane);
        }
        
        chatPane.handleDisplay();
        
        event.unlock();
        LOG.debug("unlocked");
    }

    private void setConversationPanel(JComponent comp) {
        conversationPanel.removeAll();
        conversationPanel.add(comp, BorderLayout.CENTER);
        //FIXME: Why doesn't add() trigger the repaint that revalidate() does?
        conversationPanel.revalidate();
    }
    
    @EventSubscriber
    public void handleSignoff(SignoffEvent event) {
        //close all open chats when we sign-off
        for(String key : chats.keySet()) {
            closeChat(key);
        }
        setConversationPanel(new JPanel());
    }

    @EventSubscriber
    public void handleCloseChat(CloseChatEvent event) {
        closeChat(event.getFriend().getName());
        
        setConversationPanel(new JPanel());
    }

    private void closeChat(String chatKey) {
        LOG.debugf("Closing clat panel for {0}", chatKey);
        chats.remove(chatKey).closeChat();
    }

    @Override
    public void handleDisplay() {
        // No-op
    }
    
    public void setLoggedInID(String id) {
        friendsPanel.setLoggedInID(id);    
    }
}
