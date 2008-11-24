package org.limewire.ui.swing.statusbar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.Timer;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.ui.swing.friends.chat.ChatFramePanel;
import org.limewire.ui.swing.friends.login.FriendActions;
import org.limewire.ui.swing.mainframe.UnseenMessageListener;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class FriendStatusPanel {

    private final JXButton friendsButton;
    
    private Component mainComponent;
    
    @Inject FriendStatusPanel(final FriendActions friendActions, final ChatFramePanel friendsPanel) {
        Color whiteBackground = Color.WHITE;
        JPanel menuPanel = new JPanel(new BorderLayout());
        menuPanel.setBorder(BorderFactory.createLineBorder(new Color(159, 159, 159)));
        menuPanel.setOpaque(true);
        menuPanel.setMinimumSize(new Dimension(0, 20));
        menuPanel.setMaximumSize(new Dimension(150, 20));
        menuPanel.setBackground(whiteBackground);
        
        friendsButton = new JXButton(new AbstractAction(I18n.tr("Chat")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!friendActions.isSignedIn()) {
                    friendActions.signIn();
                } else {
                    friendsPanel.toggleVisibility();
                }
            }
        });
        friendsButton.setBackgroundPainter(new RectanglePainter<JXButton>(whiteBackground, whiteBackground));
        menuPanel.add(friendsButton, BorderLayout.EAST);
        
        friendsPanel.setUnseenMessageListener(new UnseenMessageFlasher(friendsButton));       
        
        mainComponent = menuPanel;
    }
    
    Component getComponent() {
        return mainComponent;
    }
    
    private static class UnseenMessageFlasher implements UnseenMessageListener {
        private static Painter<JXButton> BLACK_BACKGROUND_PAINTER = new RectanglePainter<JXButton>(Color.BLACK, Color.BLACK);
        private boolean hasFlashed;
        private final JXButton flashingButton;
        private final Color originalForeground;
        private final Painter<JXButton> originalBackgroundPainter;
        
        public UnseenMessageFlasher(JXButton flashingButton) {
            this.flashingButton = flashingButton;
            this.originalForeground = flashingButton.getForeground();
            
            this.originalBackgroundPainter = flashingButton.getBackgroundPainter();
        }
        
        @Override
        public void clearUnseenMessages() {
            reset();
        }

        @Override
        public void unseenMessagesReceived() {
            flash();
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
