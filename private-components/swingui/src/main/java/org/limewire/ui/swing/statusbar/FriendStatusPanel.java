package org.limewire.ui.swing.statusbar;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.JPanel;
import javax.swing.Timer;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.friends.chat.ChatFramePanel;
import org.limewire.ui.swing.friends.chat.IconLibrary;
import org.limewire.ui.swing.mainframe.UnseenMessageListener;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class FriendStatusPanel {
    @Resource(key="FriendStatus.font") private Font chatButtonFont;
    @Resource(key="FriendStatus.foreground") private Color chatButtonForeground;
    @Resource(key="FriendStatus.background") private Color chatBackground;
    @Resource(key="FriendStatus.primaryTopBorderColor") private Color primaryTopBorderColor;
    @Resource(key="FriendStatus.primaryButtonBorderColor") private Color primaryButtonBorderColor;
    @Resource(key="FriendStatus.secondaryButtonBorderColor") private Color secondaryButtonBorderColor;

    private final JXButton chatButton;
    
    private JPanel mainComponent;
    private ChatButtonBorderPainter borderPainter;

    @Inject FriendStatusPanel(final ChatFramePanel friendsPanel, IconLibrary iconLibrary) {
        GuiUtils.assignResources(this);
        
        JXPanel chatPanel = new JXPanel(new MigLayout("insets 1 1 1 1"));
        
        borderPainter = new ChatButtonBorderPainter(primaryButtonBorderColor, primaryTopBorderColor);
        chatPanel.setBackgroundPainter(borderPainter);
        chatPanel.setPaintBorderInsets(true);
        chatPanel.setOpaque(true);
        chatPanel.setBackground(chatBackground);
        
        final RectanglePainter<JXButton> backgroundPainter = new RectanglePainter<JXButton>();
        
        chatButton = new JXButton(new AbstractAction(I18n.tr("Chat")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                friendsPanel.setAdjacentEdgeWidth(mainComponent.getWidth());
                friendsPanel.toggleVisibility();
                boolean friendsVisible = friendsPanel.isVisible();
                backgroundPainter.setBorderPaint(friendsVisible ? chatBackground : null);
                borderPainter.setBorderPaint(friendsVisible ? secondaryButtonBorderColor : primaryButtonBorderColor);
                borderPainter.setSecondaryBorderPaint(friendsVisible ? null : primaryTopBorderColor);
                mainComponent.invalidate();
                mainComponent.repaint();
            }
        });
        backgroundPainter.setFillPaint(chatBackground);
        backgroundPainter.setBorderPaint(null);
        backgroundPainter.setBorderWidth(0.0f);
        chatButton.setBackgroundPainter(backgroundPainter);
        chatButton.setIcon(iconLibrary.getChatting());
        chatButton.setFont(chatButtonFont);
        chatButton.setForeground(chatButtonForeground);
        chatButton.setFocusPainted(false);
        chatButton.setHorizontalAlignment(AbstractButton.LEFT);
        Insets insets = new Insets(1, 0, 0, 25);
        chatButton.setMargin(insets);
        chatButton.setPaintBorderInsets(true);
        
        chatPanel.add(chatButton, "hmax 17");
        
        friendsPanel.setUnseenMessageListener(new UnseenMessageFlasher(chatButton, iconLibrary));       
        
        mainComponent = chatPanel;
    }
    
    Component getComponent() {
        return mainComponent;
    }
    
    @Inject void register(ListenerSupport<XMPPConnectionEvent> connectionSupport) {
        connectionSupport.addListener(new EventListener<XMPPConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(XMPPConnectionEvent event) {
                switch(event.getType()) {
                case CONNECTED:
                    mainComponent.setVisible(true);
                    break;
                case DISCONNECTED:
                    mainComponent.setVisible(false);
                    break;
                }
            }
        });
    }
    
    private static class UnseenMessageFlasher implements UnseenMessageListener {
        private static Painter<JXButton> BLACK_BACKGROUND_PAINTER = new RectanglePainter<JXButton>(Color.BLACK, Color.BLACK);
        private boolean hasFlashed;
        private final JXButton flashingButton;
        private final IconLibrary iconLibrary;
        private final Color originalForeground;
        private final Painter<JXButton> originalBackgroundPainter;
        private final Set<String> unseenSenderIds = new HashSet<String>();
        private final String originalButtonText;
        
        public UnseenMessageFlasher(JXButton flashingButton, IconLibrary iconLibrary) {
            this.flashingButton = flashingButton;
            this.iconLibrary = iconLibrary;
            this.originalForeground = flashingButton.getForeground();
            this.originalBackgroundPainter = flashingButton.getBackgroundPainter();
            this.originalButtonText = flashingButton.getText();
        }
        
        @Override
        public void clearUnseenMessages() {
            reset();
        }

        @Override
        public void messageReceivedFrom(String senderId, boolean chatIsVisible) {
            unseenSenderIds.add(senderId);
            if (!chatIsVisible) {
                flash();
            }
            
            updateButtonText();
        }

        private void updateButtonText() {
            int unseenMessageSenderCount = unseenSenderIds.size();
            boolean hasUnseenMessages = unseenMessageSenderCount > 0;
            flashingButton.setText(originalButtonText + (hasUnseenMessages ? " (" + unseenMessageSenderCount + ")" : ""));
            flashingButton.setIcon(hasUnseenMessages ? iconLibrary.getUnviewedMessages() : iconLibrary.getChatting());
        }
            
        @Override
        public void conversationSelected(String chatId) {
            unseenSenderIds.remove(chatId);
            updateButtonText();
        }

        public void flash() {
            if (!hasFlashed) {
                new Timer(1500, new ActionListener() {
                    private int flashCount;
                    
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (flashCount++ < 5) {
                            flashingButton.setForeground(toggle(flashingButton.getForeground()));
                            flashingButton.setBackgroundPainter(toggle(flashingButton.getBackgroundPainter()));
                        } else {
                            Timer timer = (Timer)e.getSource();
                            timer.stop();
                        }
                    }
                    
                    private Color toggle(Color color) {
                        return color.equals(Color.WHITE) ? Color.BLACK : Color.WHITE;
                    }
                    
                    private Painter<JXButton> toggle(Painter<JXButton> painter) {
                        return painter.equals(originalBackgroundPainter) ? BLACK_BACKGROUND_PAINTER : originalBackgroundPainter;
                    }
                }).start();
                hasFlashed = true;
            }
        }
        
        public void reset() {
            flashingButton.setForeground(originalForeground);
            flashingButton.setBackgroundPainter(originalBackgroundPainter);
            hasFlashed = false;
        }
    }
}
