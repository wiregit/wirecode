package org.limewire.ui.swing.friends;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;

import org.jdesktop.swingx.JXButton;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.action.CopyAction;
import org.limewire.ui.swing.action.CopyAllAction;
import org.limewire.ui.swing.action.PopupUtil;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.event.RuntimeTopicEventSubscriber;
import org.limewire.ui.swing.friends.Message.Type;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.xmpp.api.client.ChatState;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.XMPPException;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * @author Mario Aquino, Object Computing, Inc.
 * 
 */
public class ConversationPane extends JPanel implements Displayable {
    private static final String DISABLED_LIBRARY_TOOLTIP = tr(" isn't using LimeWire. Tell them about it to see their Library");
    private static final Log LOG = LogFactory.getLog(ConversationPane.class);
    private static final Color DEFAULT_BACKGROUND = new Color(224, 224, 224);
    private final ArrayList<Message> messages = new ArrayList<Message>();
    private final JEditorPane editor;
    private final String conversationName;
    private final String friendId;
    private final MessageWriter writer;
    private ResizingInputPanel inputPanel;
    private ChatState currentChatState;
    private static final Color BACKGROUND_COLOR = Color.WHITE;

    @AssistedInject
    public ConversationPane(@Assisted MessageWriter writer, @Assisted Friend friend) {
        this.conversationName = friend.getName();
        this.friendId = friend.getID();
        this.writer = writer;
        
        setLayout(new BorderLayout());
        
        JScrollPane scroll = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        
        //Needed to add margin to the left side of the scrolling chat pane
        JPanel chatWrapper = new JPanel(new GridBagLayout());
        chatWrapper.setBackground(BACKGROUND_COLOR);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(0, 8, 0, 0);
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        chatWrapper.add(scroll,constraints);
        
        add(chatWrapper, BorderLayout.CENTER);

        editor = new JEditorPane();
        editor.setEditable(false);
        editor.setContentType("text/html");
        editor.addHyperlinkListener(new HyperlinkListener());
        
        PopupUtil.addPopupMenus(editor, new CopyAction(editor), new CopyAllAction());
        
        scroll.getViewport().add(editor);
        
        add(footerPanel(writer, friend), BorderLayout.SOUTH);

        setBackground(DEFAULT_BACKGROUND);
        
        EventAnnotationProcessor.subscribe(this);
    }
    
    @RuntimeTopicEventSubscriber(methodName="getMessageReceivedTopicName")
    public void handleConversationMessage(String topic, MessageReceivedEvent event) {
        LOG.debugf("Message: from {0} text: {1} topic: {2}", event.getMessage().getSenderName(), event.getMessage().getMessageText(), topic);
        messages.add(event.getMessage());
        if (event.getMessage().getType() == Type.Received) {
            currentChatState = ChatState.active;
        }
        displayMessages();
    }
    
    @RuntimeTopicEventSubscriber(methodName="getChatStateTopicName")
    public void handleChatStateUpdate(String topic, ChatStateEvent event) {
        LOG.debugf("Chat state update for {0} to {1}", event.getFriend().getName(), event.getState());
        if (currentChatState != event.getState()) {
            currentChatState = event.getState();
            displayMessages();
        }
    }
    
    @RuntimeTopicEventSubscriber(methodName="getPresenceUpdateTopicName")
    public void handlePresenceUpdate(String topic, PresenceUpdateEvent event) {
        org.limewire.xmpp.api.client.Presence.Type type = event.getPresence().getType();
        if (type == Presence.Type.unavailable) {
            displayMessages(true);
            inputPanel.getInputComponent().setEnabled(false);
        } else if (type == Presence.Type.available) {
            displayMessages(false);
            inputPanel.getInputComponent().setEnabled(true);
        }
    }
    
    public void closeChat() {
        try {
            writer.setChatState(ChatState.gone);
        } catch (XMPPException e) {
            LOG.error("Could not set chat state while closing the conversation", e);
        }
        
        EventAnnotationProcessor.unsubscribe(this);
    }
    
    public String getMessageReceivedTopicName() {
        return MessageReceivedEvent.buildTopic(conversationName);
    }

    public String getChatStateTopicName() {
        return ChatStateEvent.buildTopic(conversationName);
    }
    
    public String getPresenceUpdateTopicName() {
        return PresenceUpdateEvent.buildTopic(friendId);
    }
    
    private void displayMessages() {
        displayMessages(false);
    }

    private void displayMessages(boolean friendSignedOff) {
        String chatDoc = ChatDocumentBuilder.buildChatText(messages, currentChatState, conversationName, friendSignedOff);
        LOG.debugf("Chat doc: {0}", chatDoc);
        editor.setText(chatDoc);
    }
    
    private JPanel footerPanel(MessageWriter writer, Friend friend) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND_COLOR);
        JXButton libraryButton = new JXButton(tr("Library"));
        if (!friend.isSignedInToLimewire()) {
            libraryButton.setEnabled(false);
            libraryButton.setToolTipText(friend.getName() + DISABLED_LIBRARY_TOOLTIP);
        }
        panel.add(libraryButton, BorderLayout.NORTH);
        inputPanel = new ResizingInputPanel(writer);
        panel.add(inputPanel, BorderLayout.CENTER);

        return panel;
    }
    
    @Override
    public void handleDisplay() {
        editor.repaint();
        inputPanel.handleDisplay();
    }

    private class HyperlinkListener implements javax.swing.event.HyperlinkListener {

        @Override
        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (EventType.ACTIVATED == e.getEventType()) {
                LOG.debugf("Hyperlink clicked: {0}", e.getURL());
                NativeLaunchUtils.openURL(e.getURL().toString());
            }
        }
    }
}
