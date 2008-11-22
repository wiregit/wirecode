package org.limewire.ui.swing.mainframe;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.border.Border;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.swingx.JXPanel;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.core.settings.UISettings;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.components.Resizable;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.event.PanelDisplayedEvent;
import org.limewire.ui.swing.event.RuntimeTopicPatternEventSubscriber;
import org.limewire.ui.swing.friends.ChatPanel;
import org.limewire.ui.swing.friends.DisplayFriendsEvent;
import org.limewire.ui.swing.friends.DisplayFriendsToggleEvent;
import org.limewire.ui.swing.friends.Displayable;
import org.limewire.ui.swing.friends.Message;
import org.limewire.ui.swing.friends.MessageReceivedEvent;
import org.limewire.ui.swing.friends.SignoffEvent;
import org.limewire.ui.swing.friends.XMPPConnectionEstablishedEvent;
import org.limewire.ui.swing.friends.Message.Type;
import org.limewire.ui.swing.sound.WavSoundPlayer;
import org.limewire.ui.swing.tray.Notification;
import org.limewire.ui.swing.tray.TrayNotifier;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.VisibilityListener;
import org.limewire.ui.swing.util.VisibilityListenerList;
import org.limewire.ui.swing.util.VisibleComponent;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author Mario Aquino, Object Computing, Inc. 
 * TODO: Add Javadocs
 */
@Singleton
public class FriendsPanel extends JXPanel implements Resizable, ApplicationLifecycleListener, VisibleComponent {
    private static final String ALL_CHAT_MESSAGES_TOPIC_PATTERN = MessageReceivedEvent.buildTopic(".*");
    private static final Log LOG = LogFactory.getLog(FriendsPanel.class);
    private static final String MESSAGE_SOUND_PATH = "/org/limewire/ui/swing/mainframe/resources/sounds/friends/message.wav";
    private final ChatPanel chatPanel;
    private final UnseenMessageListener unseenMessageListener;
    private final TrayNotifier notifier;
    private final WindowStateListener windowStateListener;
    //Heavy-weight component so that it can appear above other heavy-weight components
    private final java.awt.Panel mainPanel;
    
    private final VisibilityListenerList visibilityListenerList = new VisibilityListenerList();
    
    private final XMPPService xmppService;
    
    @Inject
    public FriendsPanel(ChatPanel chatPanel, UnseenMessageListener unseenMessageListener, 
            TrayNotifier notifier, XMPPService xmppService) {
        super(new BorderLayout());
        this.xmppService = xmppService;
        this.chatPanel = chatPanel;
        this.notifier = notifier;
        this.unseenMessageListener = unseenMessageListener;
        this.mainPanel = new java.awt.Panel();
        this.windowStateListener = new WindowStateListener();
        
        mainPanel.setVisible(false);
        mainPanel.setBackground(getBackground());

        Border lineBorder = BorderFactory.createLineBorder(Color.BLACK);
        chatPanel.setBorder(lineBorder);
        add(mainPanel);
        setVisible(false);
        
        AppFrame.addApplicationLifecycleListener(this);
          
        EventAnnotationProcessor.subscribe(this);
    }

    @Override
    public void startupComplete() {
        JFrame frame = getApplicationFrame();
        if (frame != null) {
            frame.addWindowFocusListener(windowStateListener);
        }
    }

    @EventSubscriber
    public void handleAppear(DisplayFriendsEvent event) {
        displayFriendsPanel(event.shouldShow());
    }

    @EventSubscriber
    public void handleAppear(DisplayFriendsToggleEvent event) {       
        if(event.getVisible() != null) {
           boolean shouldDisplay = event.getVisible().booleanValue();
           displayFriendsPanel(shouldDisplay);
        } else {
            toggleVisibility();
        }
        
    }

    private void displayFriendsPanel(boolean shouldDisplay) {
        if(shouldDisplay) {
            // If we're not logged in -- signin is handled elsewhere.
            if(!xmppService.isLoggedIn() && !xmppService.isLoggingIn()) {
                return;
            } else {
                resetBounds();
            }
        }

        mainPanel.setVisible(shouldDisplay);
        setVisible(shouldDisplay);
        if (shouldDisplay) {
            unseenMessageListener.clearUnseenMessages();
            ((Displayable)mainPanel.getComponent(0)).handleDisplay();
            new PanelDisplayedEvent(this).publish();
        }
        visibilityListenerList.visibilityChanged(shouldDisplay);
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
        if (!isVisible()) {
            LOG.debug("Got an unseen message...");
            unseenMessageListener.unseenMessagesReceived();
            JFrame frame = getApplicationFrame();
            if (frame != null) {
                frame.setTitle(getNoticeForMessage(event));
            }
        }
        
        if (event.getMessage().getType() != Type.Sent && !windowStateListener.isWindowMainFocus()) {
            LOG.debug("Sending a message to the tray notifier");
            notifier.showMessage(new Notification(getNoticeForMessage(event)));
            
            URL soundURL = FriendsPanel.class.getResource(MESSAGE_SOUND_PATH);
            if (soundURL != null && UISettings.PLAY_NOTIFICATION_SOUND.getValue()) {
                ThreadExecutor.startThread(new WavSoundPlayer(soundURL.getFile()), "newmessage-sound");
            }
        } 
    }

    private String getNoticeForMessage(MessageReceivedEvent event) {
        Message message = event.getMessage();
        return tr("Chat from {0} - LimeWire 5", message.getSenderName());
    }
    
    private JFrame getApplicationFrame() {
        Window mainFrame = GuiUtils.getMainFrame();
        if (mainFrame instanceof JFrame) {
            return (JFrame)mainFrame;
        }
        return null;
    }
    
    @EventSubscriber
    public void handleConnectionEstablished(XMPPConnectionEstablishedEvent event) {
        mainPanel.add(chatPanel);
        chatPanel.setLoggedInID(event.getID());
        resetBounds();
    }
    
    @EventSubscriber
    public void handleLogoffEvent(SignoffEvent event) {
        mainPanel.remove(chatPanel);
        resetBounds();
        displayFriendsPanel(false);
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
    
    private class WindowStateListener extends WindowAdapter {
        private WindowEvent lastWindowState;
        private String originalTitlebarText;

        @Override
        public void windowGainedFocus(WindowEvent e) {
            lastWindowState = e;
            JFrame frame = (JFrame)e.getComponent();
            if (frame != null) {
                if (originalTitlebarText != null) {
                    frame.setTitle(originalTitlebarText);
                    originalTitlebarText = null;
                }
            }
        }

        @Override
        public void windowLostFocus(WindowEvent e) {
            lastWindowState = e;
            JFrame frame = (JFrame)e.getComponent();
            if (frame != null) {
                String title = frame.getTitle();
                if (title != null) {
                    originalTitlebarText = title;
                }
            }
        }

        public boolean isWindowMainFocus() {
            if (lastWindowState != null) {
                int newState = lastWindowState.getNewState();
                return newState == WindowEvent.WINDOW_GAINED_FOCUS;
            }
            return true;
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
    public void toggleVisibility() {
        boolean shouldDisplay = !isVisible();
        displayFriendsPanel(shouldDisplay);
    }
}
