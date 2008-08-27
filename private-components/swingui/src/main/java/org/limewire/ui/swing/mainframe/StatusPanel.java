package org.limewire.ui.swing.mainframe;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Random;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.friends.DisplayFriendsEvent;
import org.limewire.ui.swing.friends.FriendsCountUpdater;
import org.limewire.ui.swing.friends.XMPPConnectionEstablishedEvent;
import org.limewire.ui.swing.player.AudioPlayer;
import org.limewire.ui.swing.player.MiniPlayerPanel;
import org.limewire.ui.swing.tray.Notification;
import org.limewire.ui.swing.tray.TrayNotifier;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.SwingUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class StatusPanel extends JPanel implements FriendsCountUpdater {
    private JButton friendsButton;

    @Inject
    public StatusPanel(final TrayNotifier trayNotifier, final IconManager iconManager, AudioPlayer player) {

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
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

        friendsButton = new JButton(new FriendsAction("Sign in"));
        add(friendsButton);
        setBackground(Color.GRAY);
        setMinimumSize(new Dimension(0, 20));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        setPreferredSize(new Dimension(1024, 20));
        
        EventAnnotationProcessor.subscribe(this);
    }
    
    @EventSubscriber
    public void handleSigninEvent(XMPPConnectionEstablishedEvent event) {
        friendsButton.setText("Friends");
    }
    
    @Override
    public void setFriendsCount(final int count) {
        SwingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                friendsButton.setText("Friends (" + count + ")");        
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
