package org.limewire.ui.swing.friends.chat;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLayeredPane;

import org.jdesktop.application.Application;
import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.friend.api.MessageWriter;
import org.limewire.inject.LazySingleton;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.components.OverlayPopupPanel;
import org.limewire.ui.swing.components.decorators.ButtonDecorator;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.event.RuntimeTopicPatternEventSubscriber;
import org.limewire.ui.swing.mainframe.GlobalLayeredPane;
import org.limewire.ui.swing.tray.Notification;
import org.limewire.ui.swing.tray.TrayNotifier;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.PainterUtils;
import org.limewire.ui.swing.util.ResizeUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Mediator for the chat window and chat button in the status bar. Listens for
 * sign on/ sign off events and updates the chat button. Listens for incoming messages
 * when signed on and lazily creates the chat frame only when needed. 
 */
@LazySingleton
public class ChatMediator {

    private static final String ALL_CHAT_MESSAGES_TOPIC_PATTERN = MessageReceivedEvent.buildTopic(".*");
    
    @Resource private Font font;
    @Resource private Color foreground;

    @Resource private Icon unviewedChatIcon;
    @Resource private Icon normalChatIcon;
    
    private final JLayeredPane layeredPane;
    private final Provider<ChatModel> chatModel;
    private final Provider<ChatFrame> chatFrameProvider;
    private ChatFrame chatFrame;
    private Frame panel;
    private final JXButton chatButton;
    private final TrayNotifier trayNotifier;
    private IncomingListener incomingChatListener;
    
    private Set<String> unseenMessages = new HashSet<String>();
    
    @Inject
    public ChatMediator(Provider<ChatFrame> chatFrameProvider, ButtonDecorator buttonDecorator, TrayNotifier trayNotifier,
            Provider<ChatModel> chatModel, @GlobalLayeredPane JLayeredPane layeredPane) {
        this.chatFrameProvider = chatFrameProvider;
        this.layeredPane = layeredPane;
        this.trayNotifier = trayNotifier;
        this.chatModel = chatModel;
        chatButton = new JXButton();
        
        initChatButton();
    }
    
    /**
	 * Returns the ChatFrame. This is lazily created on the first call.
	 */
    private Panel getChatFrame() {
        if(panel == null) {
            chatFrame = chatFrameProvider.get();
            panel = new Frame(layeredPane);
            panel.add(chatFrame, BorderLayout.CENTER);
            chatFrame.revalidate();
        }
        return panel;
    }
    
    /**
     * Returns the chat button displayed in the status panel.
     */
    public JXButton getChatButton() {
        return chatButton;
    }
    
    /**
     * Sets the visibility of the ChatFrame.
     */
    public void setVisible(boolean value) {
        getChatFrame().setVisible(value);
        panel.resize();
        if(unseenMessages.size() > 0) {
            unseenMessages.clear();
            setUnseenMessageCount(unseenMessages.size());
        }
        getChatButton().repaint();
    }
    
    /**
     * Returns true if the ChatFrame is visible, false otherwise.
     */
    public boolean isVisible() {
        return panel != null && panel.isVisible();
    }
    
    /**
     * Selects this friend's conversation if one already exists, or
     * starts a new conversation with this friend and selects it.
     */
    public void startOrSelectConversation(String friendId) {
        setVisible(true);
        chatFrame.selectOrStartConversation(chatModel.get().getChatFriend(friendId));
    }
        
    /**
     * Initializes the chat button.
     */
    private void initChatButton() {
        GuiUtils.assignResources(this);
        
        chatButton.setFocusPainted(false);
        chatButton.setOpaque(false);        
        chatButton.setBorder(null);
        chatButton.setContentAreaFilled(false);
        chatButton.setFocusable(false);
        chatButton.setBorder(BorderFactory.createEmptyBorder(2, 10, 0, 10));
        chatButton.setPaintBorderInsets(true);
        
        chatButton.setFont(font);
        chatButton.setForeground(foreground);
        chatButton.setVisible(false);
        chatButton.setText(I18n.tr("Chat"));
        chatButton.setIcon(normalChatIcon);
        chatButton.setBackgroundPainter(new ChatButtonPainter());
    }
    
    @Inject void register(ListenerSupport<FriendConnectionEvent> connectionSupport) {
        // listen for login/logout events
        connectionSupport.addListener(new EventListener<FriendConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendConnectionEvent event) {
                switch(event.getType()) {
                // register listeners for incoming events with friends, make the 
                // chat button visible
                case CONNECTED:
                    chatModel.get().registerListeners();
                    if(incomingChatListener == null) {
                        incomingChatListener = new IncomingListener() {
                            @Override
                            public void incomingChat(ChatFriend chatFriend, MessageWriter messageWriter) {
                                getChatFrame();
                                chatFrame.startConversation(chatFriend, messageWriter);
                            }
                        };
                    }
                    chatModel.get().addIncomingListener(incomingChatListener);
                    getChatButton().setVisible(true);
                    EventAnnotationProcessor.subscribe(ChatMediator.this);
                    break;
                // unregister listeners and hide the chat window/chat button
                case DISCONNECTED:
                    getChatButton().setVisible(false);
                    if(panel != null) {
                        setVisible(false);
                        chatFrame.closeAllChats();
                    }
                    chatModel.get().unregisterListeners();
                    chatModel.get().removeIncomingListener(incomingChatListener);
                    EventAnnotationProcessor.unsubscribe(ChatMediator.this);
                    break;
                }
            }
        });
        
        // listen for mouse clicks on the chat button to show/hide window
        chatButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setVisible(!isVisible());
            }
        });
    }
    
    /**
     * Listen for incoming messages. This doesn't care what the message is, it simply
     * updates UI components that a new message has arrived. 
     */
    @RuntimeTopicPatternEventSubscriber(methodName="getMessagingTopicPatternName")
    public void handleMessageReceived(String topic, MessageReceivedEvent event) {
        if (event.getMessage().getType() != Message.Type.SENT) { 
            String messageFriendID = event.getMessage().getFriendID();
            ChatFriend chatFriend = chatModel.get().getChatFriend(messageFriendID);
            
            // if the chat frame not visible, update unseen message
            if(!isVisible()) {
                chatFriend.setHasUnviewedMessages(true);
                unseenMessages.add(event.getMessage().getFriendID());
                setUnseenMessageCount(unseenMessages.size());
            } // otherwise, if chatframe visible and the friend is not selected, update friend with unseen message. 
            else if(chatFriend != chatFrame.getSelectedConversation() && chatFrame.getSelectedConversation() != null) {
                chatFriend.setHasUnviewedMessages(true);
            }
        }

        // if chat panel not visible, notify in tray
        if (event.getMessage().getType() != Message.Type.SENT && 
             (!GuiUtils.getMainFrame().isActive() || !isVisible())) {
            trayNotifier.showMessage(getNoticeForMessage(event));
        } 
    }
    
    public String getMessagingTopicPatternName() {
        return ALL_CHAT_MESSAGES_TOPIC_PATTERN;
    }
    
    /**
     * Creates Notification to display in the TrayNotifier.
     */
    private Notification getNoticeForMessage(MessageReceivedEvent event) {
        final Message message = event.getMessage();

        // todo: each message type should know how to display itself as a notification
        String title = message.getType() == Message.Type.SERVER ? tr("Message from the chat server") : tr("Chat from {0}", message.getSenderName());
        Notification notification = new Notification(title, message.toString(), new AbstractAction(I18n.tr("Reply")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                ActionMap map = Application.getInstance().getContext().getActionManager().getActionMap();
                map.get("restoreView").actionPerformed(e);
            }
        });
        return notification;
    }
    
    /**
	 * Updates the text for the chat button.
     */
    private void setUnseenMessageCount(int count) {
        chatButton.setText(count > 0 ? I18n.tr("Chat ({0})", count) : I18n.tr("Chat"));
        chatButton.setIcon(count > 0 ? unviewedChatIcon : normalChatIcon);
    }
    
    /**
     * Heavy weight component so it displays over the browser.
     */
    private class Frame extends OverlayPopupPanel {

        public Frame(JLayeredPane layeredPane) {
            super(layeredPane);
            
            setLayout(new BorderLayout());
            
            ResizeUtils.forceSize(this, new Dimension(400, 240));
            setVisible(false);
            resize();
        }
        
        @Override
        public void resize() {
            Rectangle parentBounds = layeredPane.getBounds();
            int w = getPreferredSize().width;
            int h = getPreferredSize().height;
            setLocation(parentBounds.width - w, parentBounds.height - h);
        }
    }
    
    private class ChatButtonPainter extends AbstractPainter<JXButton> {

        @Resource private Color rolloverBackground = PainterUtils.TRASPARENT;
        @Resource private Color activeBackground = PainterUtils.TRASPARENT;
        @Resource private Color activeBorder = PainterUtils.TRASPARENT;
        @Resource private Color border = PainterUtils.TRASPARENT;
        
        public ChatButtonPainter() {
            GuiUtils.assignResources(this);
            
            setCacheable(false);
            setAntialiasing(true);
        }
        
        @Override
        protected void doPaint(Graphics2D g, JXButton object, int width, int height) {
            
            if (panel != null && panel.isVisible()) {
                g.setPaint(activeBackground);
                g.fillRect(0, 0, width, height);
                g.setPaint(border);
                g.drawLine(0, 0, 0, height-1);
                g.drawLine(0, height-1, width-1, height-1);
                g.drawLine(width-1, 0, width-1, height-1);
                
                if (chatFrame.getSelectedConversation() != null) {
                    g.setPaint(activeBorder);
                    g.drawLine(0,0,width-2,0);
                }
            } else if (object.getModel().isRollover()) {
                g.setPaint(rolloverBackground);
                g.fillRect(0, 2, width-1, height-2);
                g.setPaint(activeBorder);
                g.drawLine(0, 1, 0, height-1);
            }
        }
    }
}
