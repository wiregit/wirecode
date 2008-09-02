package org.limewire.ui.swing.friends;

import static org.limewire.ui.swing.friends.FriendsUtil.getIcon;
import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
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
    private ButtonGroup availabilityButtonGroup;
    private JCheckBoxMenuItem availablePopupItem;
    private JCheckBoxMenuItem awayPopupItem;

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
        availablePopupItem = new JCheckBoxMenuItem(new AvailableOption());
        awayPopupItem = new JCheckBoxMenuItem(new AwayOption());
        availabilityButtonGroup = new ButtonGroup();
        availabilityButtonGroup.add(availablePopupItem);
        availabilityButtonGroup.add(awayPopupItem);
        options.add(availablePopupItem);
        options.add(awayPopupItem);
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
        if (event.isLocallyInitiated()) {
            Friend friend = event.getFriend();
            friendNameLabel.setText(friend.getName());
            friendNameLabel.setIcon(getIcon(friend, icons));
            String status = friend.getStatus();
            friendStatusLabel.setText(status != null && status.length() > 0 ? " - " + status : "");
        }
    }
    
    @EventSubscriber
    public void handleConversationEnded(CloseChatEvent event) {
        clearFriendInfo();
    }
    
    @EventSubscriber
    public void handleSignoff(SignoffEvent event) {
        clearFriendInfo();
    }

    private void clearFriendInfo() {
        friendNameLabel.setText("");
        friendNameLabel.setIcon(null);
        friendStatusLabel.setText("");
    }
    
    @EventSubscriber
    public void handleStatusChange(PresenceChangeEvent event) {
        Mode newMode = event.getNewMode();
        ButtonModel model = newMode == Mode.available ? availablePopupItem.getModel() :
                            newMode == Mode.away ? awayPopupItem.getModel() : null;
        if (model != null) {
            availabilityButtonGroup.setSelected(model, true);
        }
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
}
