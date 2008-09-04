package org.limewire.ui.swing.friends;

import static org.limewire.ui.swing.friends.FriendsUtil.getIcon;
import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import net.miginfocom.swing.MigLayout;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.RectanglePainter;
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
    private final BuddyRemover buddyRemover;
    private ButtonGroup availabilityButtonGroup;
    private JCheckBoxMenuItem availablePopupItem;
    private JCheckBoxMenuItem awayPopupItem;

    @Inject
    public TopPanel(IconLibrary icons, BuddyRemover buddyRemover) {
        this.icons = icons;
        this.buddyRemover = buddyRemover;
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
        final JPopupMenu popup = options.getPopupMenu();
        popup.addPopupMenuListener(new PopupMenuListener() {

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                //no-op
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                //no-op
            }

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                for(Component comp : popup.getComponents()) {
                    if (comp instanceof JMenuItem) {
                        JMenuItem item = (JMenuItem)comp;
                        Action action = item.getAction();
                        if (action != null) {
                            item.setEnabled(action.isEnabled());
                        }
                    }
                }
            }
        });
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
        
        JXButton closeChat = new JXButton(new SignoffAction(icons.getCloseChat()));
        closeChat.setBorderPainted(false);
        closeChat.setBackgroundPainter(new RectanglePainter<JXButton>(getBackground(), getBackground()));
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
    
    private class AddBuddyOption extends AbstractAction {
        public AddBuddyOption() {
            super(tr("Add buddy"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(TopPanel.this), tr("Add Buddy"));
            dialog.setModalityType(ModalityType.MODELESS);
            dialog.setLayout(new MigLayout("", "[right]2[]2[]", "[]2[]2[]"));
            dialog.add(new JLabel(tr("Buddy ID:")));
            final JTextField idTextField = new JTextField(25);
            dialog.add(idTextField, "span, wrap");
            dialog.add(new JLabel(tr("Name:")));
            final JTextField nameField = new JTextField(25);
            dialog.add(nameField, "span, wrap");
            
            JButton ok = new JButton(tr("Add buddy"));
            ok.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (idTextField.getText().length() > 0)
                    dialog.setVisible(false);
                    new AddBuddyEvent(idTextField.getText(), nameField.getText()).publish();
                }
            });
            
            JButton cancel = new JButton(tr("Cancel"));
            cancel.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.setVisible(false);
                }
            });
            
            dialog.add(ok, "cell 1 2");
            dialog.add(cancel, "cell 2 2");
            dialog.pack();
            dialog.setVisible(true);
        }
    }
    
    private class RemoveBuddyOption extends AbstractAction {
        public RemoveBuddyOption() {
            super(tr("Remove buddy"));
        }

        @Override
        public boolean isEnabled() {
            return buddyRemover.canRemoveSelectedBuddy();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            buddyRemover.removeSelectedBuddy();
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
