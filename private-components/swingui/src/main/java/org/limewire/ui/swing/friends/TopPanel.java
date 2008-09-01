package org.limewire.ui.swing.friends;

import static org.limewire.ui.swing.friends.FriendsUtil.getIcon;
import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.xmpp.api.client.Presence.Mode;

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
        
        JMenu options = new JMenu(tr("Options"));
        FontUtils.changeSize(options, -3.0f);
        options.setForeground(getForeground());
        options.setBackground(getBackground());
        options.setBorderPainted(false);
        options.add(new AddBuddyOption());
        options.add(new RemoveBuddyOption());
        options.add(new MoreChatOptionsOption());
        options.addSeparator();
        JCheckBoxMenuItem availableItem = new JCheckBoxMenuItem(new AvailableOption());
        JCheckBoxMenuItem awayItem = new JCheckBoxMenuItem(new AwayOption());
        ButtonGroup selectables = new ButtonGroup();
        selectables.add(availableItem);
        selectables.add(awayItem);
        //Available by default
        selectables.setSelected(availableItem.getModel(), true);
        options.add(availableItem);
        options.add(awayItem);
        options.addSeparator();
        options.add(new SignoffAction());
        JMenuBar menuBar = new JMenuBar();
        menuBar.setForeground(getForeground());
        menuBar.setBackground(getBackground());
        menuBar.setBorderPainted(false);
        menuBar.add(options);
        add(menuBar);
        
        JButton closeChat = new JButton(new CloseAction(icons.getCloseChat()));
        closeChat.setBorderPainted(false);
        closeChat.setForeground(getForeground());
        closeChat.setBackground(getBackground());
        add(closeChat);
        
        EventAnnotationProcessor.subscribe(this);
    }
    
    @EventSubscriber
    public void handleConversationStarted(ConversationStartedEvent event) {
        Friend friend = event.getFriend();
        friendNameLabel.setText(friend.getName());
        friendNameLabel.setIcon(getIcon(friend, icons));
        String status = friend.getStatus();
        if (status != null && status.length() > 0) {
            friendStatusLabel.setText("- " + status);
        }
    }
    
    @EventSubscriber
    public void handleConversationEnded(CloseChatEvent event) {
        friendNameLabel.setText("");
        friendNameLabel.setIcon(null);
        friendStatusLabel.setText("");
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
    
    private static class SignoffAction extends AbstractAction {
        public SignoffAction() {
            super(tr("Sign off"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            new SignoffEvent().publish();
        }
    }
    
    private static class AddBuddyOption extends AbstractAction {
        public AddBuddyOption() {
            super(tr("Add buddy"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO Auto-generated method stub
        }
    }
    
    private static class RemoveBuddyOption extends AbstractAction {
        public RemoveBuddyOption() {
            super(tr("Remove buddy"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO Auto-generated method stub
        }
    }
    
    private static class MoreChatOptionsOption extends AbstractAction {
        public MoreChatOptionsOption() {
            super(tr("More chat options"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO Auto-generated method stub
        }
    }
    
    private static class AvailableOption extends AbstractAction {
        public AvailableOption() {
            super(tr("Available (checkable)"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            new PresenceChangeEvent(Mode.available).publish();
        }
    }
    
    private static class AwayOption extends AbstractAction {
        public AwayOption() {
            super(tr("Away (checkable)"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            new PresenceChangeEvent(Mode.away).publish();
        }
    }
}
