package org.limewire.ui.swing.friends;

import static org.limewire.ui.swing.util.I18n.tr;
import static org.limewire.ui.swing.friends.FriendsUtil.getIcon;

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.swingx.JXButton;
import org.limewire.ui.swing.util.FontUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class TopPanel extends JPanel {
    
    private JLabel friendNameLabel;
    private JLabel friendStatusLabel;
    private final IconLibrary icons;

    @Inject
    public TopPanel(IconLibrary icons) {
        this.icons = icons;
        setBackground(Color.BLACK);
        setForeground(Color.WHITE);
        setLayout(new MigLayout("insets 0 0 0 0", "3[][]0:push[]0[]0", "0[]0"));
        
        friendNameLabel = new JLabel();
        friendNameLabel.setForeground(getForeground());
        add(friendNameLabel);
        
        friendStatusLabel = new JLabel();
        friendStatusLabel.setForeground(getForeground());
        FontUtils.changeSize(friendStatusLabel, -1.8f);
        add(friendStatusLabel);
        
        JXButton options = new JXButton(tr("Options"));
        FontUtils.changeSize(options, -2.8f);
        options.setForeground(getForeground());
        options.setBackground(getBackground());
        options.setBorderPainted(false);
        add(options);
        
        JXButton closeChat = new JXButton(new CloseAction(icons.getCloseChat()));
        closeChat.setBorderPainted(false);
        add(closeChat);
        
        AnnotationProcessor.process(this);
    }
    
    @EventSubscriber
    public void handleConversationStarted(ConversationStartedEvent event) {
        Friend friend = event.getFriend();
        friendNameLabel.setText(friend.getName());
        friendNameLabel.setIcon(getIcon(friend, icons));
        friendStatusLabel.setText("- " + friend.getStatus());
    }
    
    private static class CloseAction extends AbstractAction {
        public CloseAction(Icon icon) {
            super("", icon);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            new DisplayFriendsEvent().publish();
        }
    }
}
