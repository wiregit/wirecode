package org.limewire.ui.swing.friends.chat;

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.RectanglePainter;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The top border of the chat panel, for minimizing the chat window
 * & other controls.
 */
@Singleton
public class ChatTopPanel extends JPanel {
    
    private Action minimizeAction;
    
    @Inject
    public ChatTopPanel(IconLibrary icons) {        
        setBackground(Color.BLACK);
        setForeground(Color.WHITE);
        setLayout(new MigLayout("insets 0, fill"));
        
        RectanglePainter<JXButton> backgroundPainter = new RectanglePainter<JXButton>(getBackground(), getBackground());
        final JXButton minimizeChat = new JXButton(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                minimizeAction.actionPerformed(e);
            }
        });        
        minimizeChat.setIcon(icons.getMinimizeNormal());
        minimizeChat.setRolloverIcon(icons.getMinimizeOver());
        minimizeChat.setPressedIcon(icons.getMinimizeDown());
        minimizeChat.setBorderPainted(false);
        minimizeChat.setBackgroundPainter(backgroundPainter);
        add(minimizeChat, "alignx right");
    }
    
    void setMinimizeAction(Action minimizeAction) {
        this.minimizeAction = minimizeAction;
    }
    
    // Commenting out till we know we'll keep it.  Can fix then.
//    @EventSubscriber
//    public void handleConversationStarted(ConversationSelectedEvent event) {
//        if (event.isLocallyInitiated()) {
//            ChatFriend chatFriend = event.getFriend();
//            friendAvailabiltyIcon.setText(getAvailabilityHTML(chatFriend.getMode()));
//            friendNameLabel.setText(chatFriend.getName());
//            String status = chatFriend.getStatus();
//            friendStatusLabel.setText(status != null && status.length() > 0 ? " - " + status : "");
//        }
//    }
//    
//    @EventSubscriber
//    public void handleConversationEnded(CloseChatEvent event) {
//        clearFriendInfo();
//    }
//    
//    @EventSubscriber
//    public void handleSignoff(SignoffEvent event) {
//        clearFriendInfo();
//    }
//
//    private void clearFriendInfo() {
//        friendAvailabiltyIcon.setText("");
//        friendNameLabel.setText("");
//        friendStatusLabel.setText("");
//    }
}
