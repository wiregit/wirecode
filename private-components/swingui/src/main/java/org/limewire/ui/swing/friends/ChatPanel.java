package org.limewire.ui.swing.friends;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent.EventType;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.limewire.core.settings.FriendSettings;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.sharing.FriendSharingDisplay;

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
    private final FriendSharingDisplay friendSharing;
    private final Map<String, ConversationPane> chats;
    
    @Inject
    public ChatPanel(ConversationPaneFactory conversationFactory, IconLibrary icons, FriendsPane friendsPanel,
            TopPanel topPanel, FriendSharingDisplay friendSharing) {
        super(new BorderLayout());
        this.conversationFactory = conversationFactory;
        this.friendsPanel = friendsPanel;
        this.chats = new HashMap<String, ConversationPane>();
        this.friendSharing = friendSharing;

        //Dimensions according to the spec
        setPreferredSize(new Dimension(400, 235));
        add(friendsPanel, BorderLayout.WEST);
        add(topPanel, BorderLayout.NORTH);
        conversationPanel = new JPanel(new BorderLayout());
        setConversationPanel(buildMessagesPane());
        add(conversationPanel, BorderLayout.CENTER);
        
        EventAnnotationProcessor.subscribe(this);
    }

    private JPanel buildMessagesPane() {
        JPanel panel = new JPanel(new BorderLayout());
        JEditorPane pane = new JEditorPane();
        pane.setEditable(false);
        pane.setContentType("text/html");
        pane.setText(getMessagesPaneText());
        pane.addHyperlinkListener(new HyperlinkHandler(friendSharing));
        panel.add(pane, BorderLayout.CENTER);
        return panel;
    }

    private String getMessagesPaneText() {
        boolean isSharingWithFriends = friendsPanel.isSharingFilesWithFriends();
        boolean hasFriendsOnLimeWire = friendsPanel.hasFriendsOnLimeWire();
        boolean hasLoggedInMoreThan3Times = FriendSettings.NUM_LOGINS.getValue() > 3;
        LOG.debugf("isSharingWithFriends: {0} hasFriendsOnLimeWire: {1} hasLoggedInMoreThan3Times: {2} totalNumberOfLogins: {3}",
                isSharingWithFriends, hasFriendsOnLimeWire, hasLoggedInMoreThan3Times, FriendSettings.NUM_LOGINS.getValue());
        if (isSharingWithFriends && hasFriendsOnLimeWire && hasLoggedInMoreThan3Times) {
            return getRecentUpdatesText();
        } else {
            StringBuilder bldr = new StringBuilder();
            bldr.append("<html>")
            .append("<head>")
            .append("<style>")
            .append("body { margin-left: 10px;}")
            .append("h2 { margin-bottom: 25px;}")
            .append("ul { margin-left: 10px;}")
            .append("li { margin-bottom: 15px;}")
            .append("</style>")
            .append("</head>")
            .append("<body>")
            .append("<h2>").append(tr("Now What?")).append("</h2>")
            .append("<ul>");
            if (!isSharingWithFriends) {
                // {0} and {1} are surrounding html tags to make 'here' a  link
                bldr.append("<li>").append(tr("Share files in your Library with your friends")).append("</li>");
            }
            
            if (!hasFriendsOnLimeWire) {
                bldr.append("<li>").append(tr("Chat with your friends about getting the new LimeWire")).append("</li>");
            }
            if (!hasLoggedInMoreThan3Times) {
                bldr.append("<li>").append(tr("Search and download from your friends")).append("</li>");
            }
            bldr.append("</ul>")
            .append("</body>")
            .append("</html>");
            return bldr.toString();
        }
    }
    
    private String getRecentUpdatesText() {
        // TODO
        return "Replace me with an HTML doc describing recent updates to LimeWire 5";
    }

    private static class HyperlinkHandler implements HyperlinkListener {
        private final FriendSharingDisplay friendSharing;
        
        public HyperlinkHandler(FriendSharingDisplay friendSharing) {
            this.friendSharing = friendSharing;
        }
        
        @Override
        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (EventType.ACTIVATED == e.getEventType()) {
                LOG.debugf("Hyperlink clicked: {0}", e.getDescription());
                if (e.getDescription().equals("all_friends_share_list")) {
                    friendSharing.displaySharing();
                }
            }
        }
    }
    
    @EventSubscriber
    public void handleSelectedConversation(ConversationSelectedEvent event) {
        ChatFriend chatFriend = event.getFriend();
        LOG.debugf("ConversationSelectedEvent with friend: {0}", chatFriend.getName());
        ConversationPane chatPane = chats.get(chatFriend.getID());
        if (chatPane == null) {
            chatPane = conversationFactory.create(event.getWriter(), chatFriend, getLoggedInID());
            chats.put(chatFriend.getID(), chatPane);
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
    }
    
    @EventSubscriber
    public void handleSignon(XMPPConnectionEstablishedEvent event) {
        setConversationPanel(buildMessagesPane());
    }
    
    @EventSubscriber
    public void handleSignoff(SignoffEvent event) {
        //close all open chats when we sign-off
        String[] chatKeys = chats.keySet().toArray(new String[chats.size()]);
        for(String key : chatKeys) {
            closeChat(key);
        }
        setConversationPanel(new JPanel());
    }

    @EventSubscriber
    public void handleCloseChat(CloseChatEvent event) {
        chats.get(event.getFriend().getID()).closeChat();
        
        setConversationPanel(buildMessagesPane());
    }
    
    @EventSubscriber
    public void handleRemoveFriend(RemoveFriendEvent event) {
        final String friendID = event.getFriend().getID();
        if (chats.containsKey(friendID)) {
            closeChat(friendID);
        }
    }

    private void closeChat(String chatKey) {
        LOG.debugf("Closing chat panel for {0}", chatKey);
        ConversationPane conversation = chats.remove(chatKey);
        conversation.closeChat();
        conversation.destroy();
    }

    @Override
    public void handleDisplay() {
        Component component = conversationPanel.getComponent(0);
        if (component instanceof Displayable) {
            ((Displayable)component).handleDisplay();
        }
    }
    
    public void setLoggedInID(String id) {
        friendsPanel.setLoggedInID(id);    
    }
    
    private String getLoggedInID() {
        return friendsPanel.getLoggedInID();
    }
}
