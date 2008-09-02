package org.limewire.ui.swing.mainframe;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Random;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.limewire.player.api.AudioPlayer;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.friends.AvailableOption;
import org.limewire.ui.swing.friends.AwayOption;
import org.limewire.ui.swing.friends.DisplayFriendsEvent;
import org.limewire.ui.swing.friends.FriendsCountUpdater;
import org.limewire.ui.swing.friends.FriendsUtil;
import org.limewire.ui.swing.friends.IconLibrary;
import org.limewire.ui.swing.friends.PresenceChangeEvent;
import org.limewire.ui.swing.friends.SignoffAction;
import org.limewire.ui.swing.friends.SignoffEvent;
import org.limewire.ui.swing.friends.XMPPConnectionEstablishedEvent;
import org.limewire.ui.swing.player.MiniPlayerPanel;
import org.limewire.ui.swing.tray.Notification;
import org.limewire.ui.swing.tray.TrayNotifier;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.xmpp.api.client.Presence.Mode;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class StatusPanel extends JPanel implements FriendsCountUpdater {
    private static final String OFFLINE = "offline";
    private final IconLibrary icons;
    private JButton friendsButton;
    private JMenu statusMenu;
    private String SIGN_IN = tr("Sign in");
    private String FRIENDS = tr("Friends");
    private boolean loggedIn;
    private ButtonGroup availabilityButtonGroup;
    private JCheckBoxMenuItem availablePopupItem;
    private JCheckBoxMenuItem awayPopupItem;

    @Inject
    public StatusPanel(final TrayNotifier trayNotifier, final IconManager iconManager, IconLibrary icons, AudioPlayer player) {
        this.icons = icons;
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBackground(Color.GRAY);
        add(new JLabel("status"));
        add(Box.createHorizontalGlue());
        add(new JButton(new AbstractAction("Error Test") {
            @Override
            public void actionPerformed(ActionEvent e) {
                throw new RuntimeException("Test Error");
            }
        }));

        add(new JButton(new AbstractAction("Tray Test") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (new Random().nextBoolean()) {
                    Icon icon = iconManager.getIconForFile(new File("limewire.exe"));
                    Notification notification = new Notification(
                            "This is a very looooooooooooooooooooooooooooooooong message.",
                            icon, this);
                    trayNotifier.showMessage(notification);
                } else if (new Random().nextBoolean()) {
                    Icon icon = iconManager.getIconForFile(new File("limewire.html"));
                    Notification notification = new Notification(
                            "This is a another very loooong  loooong loooong loooong loooong loooong loooong loooong loooong message.",
                            icon, this);
                    trayNotifier.showMessage(notification);               
                } else {
                    Icon icon = iconManager.getIconForFile(new File("limewire.html"));
                    Notification notification = new Notification(
                            "Short message.",
                            icon, this);
                    trayNotifier.showMessage(notification);               
                }
            }
        }));
        MiniPlayerPanel miniPlayerPanel = new MiniPlayerPanel(player);
        miniPlayerPanel.setVisible(false);
        add(miniPlayerPanel);

        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(Color.WHITE);
        menuBar.setOpaque(true);
        menuBar.setBorderPainted(false);
        JPanel menuPanel = new JPanel(new BorderLayout());
        menuPanel.setBorder(BorderFactory.createLineBorder(new Color(159, 159, 159)));
        menuPanel.setOpaque(true);
        menuPanel.setMinimumSize(new Dimension(0, 20));
        menuPanel.setMaximumSize(new Dimension(150, 20));
        menuPanel.add(menuBar, BorderLayout.WEST);
        menuPanel.setBackground(menuBar.getBackground());
        
        add(menuPanel);
        
        statusMenu = new JMenu();
        statusMenu.setOpaque(true);
        statusMenu.setEnabled(false);
        statusMenu.setIcon(icons.getEndChat());
        statusMenu.setBackground(menuBar.getBackground());
        statusMenu.setBorderPainted(false);
        menuBar.add(statusMenu);
        friendsButton = new JButton(new FriendsAction(SIGN_IN));
        friendsButton.setBackground(statusMenu.getBackground());
        friendsButton.setBorderPainted(false);
        menuPanel.add(friendsButton, BorderLayout.EAST);
        
        statusMenu.add(new SignoffAction());
        statusMenu.addSeparator();
        availablePopupItem = new JCheckBoxMenuItem(new AvailableOption());
        statusMenu.add(availablePopupItem);
        awayPopupItem = new JCheckBoxMenuItem(new AwayOption());
        statusMenu.add(awayPopupItem);
        //Set the menu location so that the popup will appear up instead of the default down for menus
        statusMenu.setMenuLocation(0, (statusMenu.getPopupMenu().getPreferredSize().height * -1));
        availabilityButtonGroup = new ButtonGroup();
        availabilityButtonGroup.add(availablePopupItem);
        availabilityButtonGroup.add(awayPopupItem);
        
        updateStatus(SIGN_IN, icons.getEndChat(), OFFLINE);

        setMinimumSize(new Dimension(0, 20));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        setPreferredSize(new Dimension(1024, 20));
        
        EventAnnotationProcessor.subscribe(this);
    }
    
    @EventSubscriber
    public void handleSigninEvent(XMPPConnectionEstablishedEvent event) {
        loggedIn = true;
        updateStatus(FRIENDS, FriendsUtil.getIcon(Mode.available, icons), Mode.available.toString());
        statusMenu.setEnabled(true);
    }

    private void updateStatus(String buttonText, Icon icon, String status) {
        friendsButton.setText(buttonText);
        statusMenu.setIcon(icon);
        statusMenu.setToolTipText(tr(status + " - click to set status"));
    }
    
    @EventSubscriber
    public void handleSignoffEvent(SignoffEvent event) {
        loggedIn = false;
        updateStatus(SIGN_IN, icons.getEndChat(), OFFLINE);
        statusMenu.setEnabled(false);
    }
    
    @EventSubscriber
    public void handleStatusChange(PresenceChangeEvent event) {
        Mode newMode = event.getNewMode();
        updateStatus(friendsButton.getText(), FriendsUtil.getIcon(newMode, icons), newMode.toString());
        ButtonModel model = newMode == Mode.available ? availablePopupItem.getModel() :
                            newMode == Mode.away ? awayPopupItem.getModel() : null;
        if (model != null) {
            availabilityButtonGroup.setSelected(model, true);
        }
    }
    
    @Override
    public void setFriendsCount(final int count) {
        SwingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (loggedIn) {
                    friendsButton.setText(FRIENDS + " (" + count + ")");        
                }
            }
        });
    }

    private class FriendsAction extends AbstractAction {
        public FriendsAction(String title) {
            super(title);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            new DisplayFriendsEvent().publish();
        }
    }
}
