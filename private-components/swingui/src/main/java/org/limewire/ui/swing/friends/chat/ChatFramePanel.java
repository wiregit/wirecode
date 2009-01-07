package org.limewire.ui.swing.friends.chat;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;

import net.miginfocom.swing.MigLayout;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.application.Application;
import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.core.settings.UISettings;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.components.Resizable;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.event.PanelDisplayedEvent;
import org.limewire.ui.swing.event.RuntimeTopicPatternEventSubscriber;
import org.limewire.ui.swing.mainframe.UnseenMessageListener;
import org.limewire.ui.swing.sound.WavSoundPlayer;
import org.limewire.ui.swing.tray.Notification;
import org.limewire.ui.swing.tray.TrayNotifier;
import org.limewire.ui.swing.util.EnabledListener;
import org.limewire.ui.swing.util.EnabledListenerList;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.VisibilityListener;
import org.limewire.ui.swing.util.VisibilityListenerList;
import org.limewire.ui.swing.util.VisibleComponent;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The main frame of the chat panel.
 * 
 * All visible aspects of chat are rendered in this panel.
 */
@Singleton
public class ChatFramePanel extends JXPanel implements Resizable, VisibleComponent {
    private static final String ALL_CHAT_MESSAGES_TOPIC_PATTERN = MessageReceivedEvent.buildTopic(".*");
    private static final Log LOG = LogFactory.getLog(ChatFramePanel.class);
    private static final String MESSAGE_SOUND_PATH = "/org/limewire/ui/swing/mainframe/resources/sounds/friends/message.wav";
    private final ChatPanel chatPanel;
    private final ChatFriendListPane chatFriendListPane;
    private final TrayNotifier notifier;
    //Heavy-weight component so that it can appear above other heavy-weight components
    private final java.awt.Panel mainPanel;
    
    private final VisibilityListenerList visibilityListenerList = new VisibilityListenerList();
    private final EnabledListenerList enabledListenerList = new EnabledListenerList();
    @Resource(key="ChatFramePanel.primaryframeBorderColor") private Color primaryFrameBorderColor;
    @Resource(key="ChatFramePanel.secondaryframeBorderColor") private Color secondaryFrameBorderColor;
    private boolean actionEnabled = false;
    
    private UnseenMessageListener unseenMessageListener;
    private String lastSelectedConversationFriendId;
    private JXPanel borderPanel;
    private ChatFramePainter chatFramePainter;
    
    @Inject
    public ChatFramePanel(ChatPanel chatPanel, ChatFriendListPane chatFriendListPane, TrayNotifier notifier) {
        super(new BorderLayout());
        this.chatPanel = chatPanel;
        this.chatFriendListPane = chatFriendListPane;
        this.notifier = notifier;
        this.mainPanel = new java.awt.Panel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        
        GuiUtils.assignResources(this);
        borderPanel = new JXPanel(new MigLayout("insets 1 1 1 1"));
        chatFramePainter = new ChatFramePainter(primaryFrameBorderColor, Color.white);
        borderPanel.setBackgroundPainter(chatFramePainter);
        borderPanel.add(chatPanel);
        
        mainPanel.setVisible(false);        
        add(mainPanel);
        setVisible(false);
        
        chatPanel.setMinimizeAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setChatPanelVisible(false);
            }
        });
          
        EventAnnotationProcessor.subscribe(this);
    }
    
    @Inject void register(ListenerSupport<XMPPConnectionEvent> connectionSupport) {
        connectionSupport.addListener(new EventListener<XMPPConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(XMPPConnectionEvent event) {
                switch(event.getType()) {
                case CONNECTED:
                    handleConnectionEstablished(event);
                    break;
                case DISCONNECTED:
                    handleLogoffEvent();
                    break;
                }
            }
        });
    }
    
    /**
     * The width of the portion of the bottom right frame border that 
     * intersects with the 'chat' button.
     * @param width
     */
    public void setAdjacentEdgeWidth(int width) {
        this.chatFramePainter.setSecondaryColorWidth(width);
    }
    
    public void setUnseenMessageListener(UnseenMessageListener unseenMessageListener) {
        this.unseenMessageListener = unseenMessageListener;
    }
    
    @Override
    public void toggleVisibility() {
        boolean shouldDisplay = !isVisible();
        setVisibility(shouldDisplay);
    }
    
    public void setChatPanelVisible(boolean shouldDisplay) {
        if(shouldDisplay) {
           resetBounds();
        }

        mainPanel.setVisible(shouldDisplay);
        setVisible(shouldDisplay);
        if (shouldDisplay) {
            unseenMessageListener.clearUnseenMessages();
            getDisplayable().handleDisplay();
            new PanelDisplayedEvent(this).publish();
        }
        visibilityListenerList.visibilityChanged(shouldDisplay);
    }
    
    private Displayable getDisplayable() {
        return chatPanel;
    }
    
    /**
     * Hides FriendsPanel when another panel is shown in the same layer.
     */
    @EventSubscriber
    public void handleOtherPanelDisplayed(PanelDisplayedEvent event){
        if(event.getDisplayedPanel() != this){
            setVisible(false);
            mainPanel.setVisible(false);
        }
    }
    
    @RuntimeTopicPatternEventSubscriber(methodName="getMessagingTopicPatternName")
    public void handleMessageReceived(String topic, MessageReceivedEvent event) {
        if (event.getMessage().getType() != Message.Type.Sent && !GuiUtils.getMainFrame().isActive()) {
            notifyUnseenMessageListener(event);
            LOG.debug("Sending a message to the tray notifier");
            notifier.showMessage(getNoticeForMessage(event));
            
            URL soundURL = ChatFramePanel.class.getResource(MESSAGE_SOUND_PATH);
            if (soundURL != null && UISettings.PLAY_NOTIFICATION_SOUND.getValue()) {
                ThreadExecutor.startThread(new WavSoundPlayer(soundURL.getFile()), "newmessage-sound");
            }
        } 
    }
    
    private void notifyUnseenMessageListener(MessageReceivedEvent event) {
        String messageFriendID = event.getMessage().getFriendID();
        if (!messageFriendID.equals(lastSelectedConversationFriendId)) {
            unseenMessageListener.messageReceivedFrom(messageFriendID, isVisible());
        }
    }
    
    @EventSubscriber
    public void handleConversationSelected(ConversationSelectedEvent event) {
        if (event.isLocallyInitiated()) {
            lastSelectedConversationFriendId = event.getFriend().getID();
            unseenMessageListener.conversationSelected(lastSelectedConversationFriendId);
            chatFramePainter.setSecondaryBorderPaint(secondaryFrameBorderColor);
            borderPanel.invalidate();
            borderPanel.repaint();
        }
    }
    
    @EventSubscriber
    public void handleChatClosed(CloseChatEvent event) {
        if (event.getFriend().getID().equals(lastSelectedConversationFriendId)) {
            lastSelectedConversationFriendId = null;
        }
        chatFramePainter.setSecondaryBorderPaint(Color.white);
        borderPanel.invalidate();
        borderPanel.repaint();
    }

    private Notification getNoticeForMessage(MessageReceivedEvent event) {
        final Message message = event.getMessage();
        String title = tr("Chat from {0}", message.getSenderName());
        String messageString = message.toString();
        Notification notification = new Notification(title, messageString, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ActionMap map = Application.getInstance().getContext().getActionManager()
                .getActionMap();
                map.get("restoreView").actionPerformed(e);
                setChatPanelVisible(true);
                chatFriendListPane.fireConversationStarted(message.getFriendID());
            }
        });
        return notification;
    }
    
    private void handleConnectionEstablished(XMPPConnectionEvent event) {
        addChatPanel();
        chatPanel.setLoggedInID(event.getSource().getConfiguration().getCanonicalizedLocalID());
        resetBounds();
        setActionEnabled(true);
    }

    private void addChatPanel() {
        mainPanel.add(borderPanel);
    }
    
    private void handleLogoffEvent() {
        removeChatPanel();
        resetBounds();
        setChatPanelVisible(false);
        setActionEnabled(false);
        lastSelectedConversationFriendId = null;
    }

    private void removeChatPanel() {
        mainPanel.remove(borderPanel);
    }
    
    public String getMessagingTopicPatternName() {
        return ALL_CHAT_MESSAGES_TOPIC_PATTERN;
    }
    
    private void resetBounds() {
        Rectangle parentBounds = getParent().getBounds();
        Dimension childPreferredSize = mainPanel.getPreferredSize();
        int w = (int) childPreferredSize.getWidth();
        int h = (int) childPreferredSize.getHeight();
        setBounds(parentBounds.width - w, parentBounds.height - h, w, h);
    }

    @Override
    public void resize() {
        if (isVisible()) {
            resetBounds();
        }
    }

    @Override
    public void addVisibilityListener(VisibilityListener listener) {
        visibilityListenerList.addVisibilityListener(listener);
    }

    @Override
    public void removeVisibilityListener(VisibilityListener listener) {
        visibilityListenerList.removeVisibilityListener(listener);
    }

    @Override
    public void setVisibility(boolean visible) {
        setChatPanelVisible(visible);
        visibilityListenerList.visibilityChanged(visible);
    }

    @Override
    public void addEnabledListener(EnabledListener listener) {
        enabledListenerList.addEnabledListener(listener);
    }

    @Override
    public void removeEnabledListener(EnabledListener listener) {
        enabledListenerList.removeEnabledListener(listener);
    }

    /**
     * Returns true if the component is enabled for use. 
     */
    @Override
    public boolean isActionEnabled() {
        return actionEnabled;
    }

    /**
     * Sets an indicator to determine whether the component is enabled for use,
     * and notifies all registered EnabledListener instances. 
     */
    private void setActionEnabled(boolean enabled) {
        // Get old value, and save new value.
        boolean oldValue = actionEnabled;
        actionEnabled = enabled;
        
        // Notify listeners if value changed.
        if (enabled != oldValue) {
            enabledListenerList.fireEnabledChanged(enabled);
        }
    }
    
}
