package org.limewire.ui.swing.friends;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

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
import javax.swing.ToolTipManager;
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
    
    private JLabel friendAvailabiltyIcon;
    private JLabel friendNameLabel;
    private JLabel friendStatusLabel;
    private final IconLibrary icons;
    private final FriendRemover friendRemover;
    private ButtonGroup availabilityButtonGroup;
    private JCheckBoxMenuItem availablePopupItem;
    private JCheckBoxMenuItem awayPopupItem;
    
    @Inject
    public TopPanel(final IconLibrary icons, FriendRemover friendRemover) {
        this.icons = icons;
        this.friendRemover = friendRemover;
        
        setBackground(Color.BLACK);
        setForeground(Color.WHITE);
        setLayout(new MigLayout("insets 0 0 0 0", "3[]3[shrinkprio 50][]3:push[shrinkprio 0]3[shrinkprio 0]3[shrinkprio 0]0", "0[]0"));
        
        friendAvailabiltyIcon = new JLabel();
        add(friendAvailabiltyIcon);
        friendNameLabel = new JLabel();
        friendNameLabel.setForeground(getForeground());
        add(friendNameLabel, "wmin 0");
        
        friendStatusLabel = new JLabel();
        friendStatusLabel.setForeground(getForeground());
        FontUtils.changeSize(friendStatusLabel, -1.8f);
        add(friendStatusLabel, "wmin 0");
        
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
        FontUtils.changeSize(options, -2.0f);
        options.setForeground(getForeground());
        options.setBackground(getBackground());
        options.setBorderPainted(false);
        options.add(new AddFriendOption());
        options.add(new RemoveFriendOption());
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
        
        RectanglePainter<JXButton> backgroundPainter = new RectanglePainter<JXButton>(getBackground(), getBackground());
        final JXButton minimizeChat = new JXButton(new MinimizeChat());
        minimizeChat.setMinimumSize(new Dimension(13, 14));
        minimizeChat.setPreferredSize(new Dimension(13, 14));
        minimizeChat.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                minimizeChat.setIcon(icons.getMinimizeOver());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                minimizeChat.setIcon(icons.getMinimizeNormal());
            }

            @Override
            public void mousePressed(MouseEvent e) {
                minimizeChat.setIcon(icons.getMinimizeDown());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                minimizeChat.setIcon(icons.getMinimizeNormal());
            }
        });
        minimizeChat.setBorderPainted(false);
        minimizeChat.setBackgroundPainter(backgroundPainter);
        add(minimizeChat);
        final JMenu signoffMenu = new JMenu();
        signoffMenu.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                signoffMenu.doClick();
            }
        });
        signoffMenu.setForeground(getForeground());
        signoffMenu.setBackground(getBackground());
        signoffMenu.setBorderPainted(false);
        signoffMenu.setIcon(icons.getCloseChat());
        signoffMenu.add(new SignoffAction());
        JMenuBar signoffMenuBar = new JMenuBar();
        signoffMenuBar.setForeground(getForeground());
        signoffMenuBar.setBackground(getBackground());
        signoffMenuBar.setBorderPainted(false);
        signoffMenuBar.add(signoffMenu);
        add(signoffMenuBar);
        
        ToolTipManager.sharedInstance().registerComponent(this);
        
        EventAnnotationProcessor.subscribe(this);
    }
    
    private String getAvailabilityHTML(Mode mode) {
        return "<html><img src=\"" + FriendsUtil.getIconURL(mode) + "\" /></html>";
    }
    
    @Override
    public String getToolTipText() {
        String name = friendNameLabel.getText();
        String label = friendStatusLabel.getText();
        String tooltip = name + label;
        return tooltip.length() == 0 ? null : friendAvailabiltyIcon.getText().replace("</html>", "&nbsp;" + tooltip + "</html>");
    }
    
    @EventSubscriber
    public void handleConversationStarted(ConversationStartedEvent event) {
        if (event.isLocallyInitiated()) {
            ChatFriend chatFriend = event.getFriend();
            friendAvailabiltyIcon.setText(getAvailabilityHTML(chatFriend.getMode()));
            friendNameLabel.setText(chatFriend.getName());
            String status = chatFriend.getStatus();
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
        friendAvailabiltyIcon.setText("");
        friendNameLabel.setText("");
        friendStatusLabel.setText("");
    }
    
    @EventSubscriber
    public void handleStatusChange(SelfAvailabilityUpdateEvent event) {
        Mode newMode = event.getNewMode();
        ButtonModel model = newMode == Mode.available ? availablePopupItem.getModel() :
                            (newMode == Mode.away  || newMode == Mode.xa) ? awayPopupItem.getModel() : null;
        if (model != null) {
            availabilityButtonGroup.setSelected(model, true);
        }
    }
    
    private class AddFriendOption extends AbstractAction {
        public AddFriendOption() {
            super(tr("Add friend"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(TopPanel.this), tr("Add Friend"));
            dialog.setModalityType(ModalityType.MODELESS);
            dialog.setLayout(new MigLayout("", "[right]2[]2[]", "[]2[]2[]"));
            dialog.add(new JLabel(tr("Friend ID:")));
            final JTextField idTextField = new JTextField(25);
            dialog.add(idTextField, "span, wrap");
            dialog.add(new JLabel(tr("Name:")));
            final JTextField nameField = new JTextField(25);
            dialog.add(nameField, "span, wrap");
            
            JButton ok = new JButton(tr("Add friend"));
            ok.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (idTextField.getText().length() > 0)
                    dialog.setVisible(false);
                    new AddFriendEvent(idTextField.getText(), nameField.getText()).publish();
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
    
    private class RemoveFriendOption extends AbstractAction {
        public RemoveFriendOption() {
            super(tr("Remove friend"));
        }

        @Override
        public boolean isEnabled() {
            return friendRemover.canRemoveSelectedFriend();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            friendRemover.removeSelectedFriend();
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
    
    private class MinimizeChat extends AbstractAction {
        public MinimizeChat() {
            super("", icons.getMinimizeNormal());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            new DisplayFriendsEvent(false).publish();
        }
    }
}
