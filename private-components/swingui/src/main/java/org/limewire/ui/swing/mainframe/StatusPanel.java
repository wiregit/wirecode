package org.limewire.ui.swing.mainframe;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.Random;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.Timer;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.MattePainter;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.player.api.AudioPlayer;
import org.limewire.ui.swing.components.Line;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.friends.AvailableOption;
import org.limewire.ui.swing.friends.AwayOption;
import org.limewire.ui.swing.friends.DisplayFriendsToggleEvent;
import org.limewire.ui.swing.friends.FriendsCountUpdater;
import org.limewire.ui.swing.friends.FriendsUtil;
import org.limewire.ui.swing.friends.IconLibrary;
import org.limewire.ui.swing.friends.SelfAvailabilityUpdateEvent;
import org.limewire.ui.swing.friends.SignoffAction;
import org.limewire.ui.swing.friends.SignoffEvent;
import org.limewire.ui.swing.friends.XMPPConnectionEstablishedEvent;
import org.limewire.ui.swing.player.MiniPlayerPanel;
import org.limewire.ui.swing.tray.Notification;
import org.limewire.ui.swing.tray.TrayNotifier;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import static org.limewire.ui.swing.util.I18n.tr;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.xmpp.api.client.Presence.Mode;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import net.miginfocom.swing.MigLayout;

@Singleton
public class StatusPanel extends JXPanel implements FriendsCountUpdater, UnseenMessageListener {
    
    private final String OFFLINE = tr("Offline");
    private final IconLibrary icons;
    private JXButton friendsButton;
    private JMenu statusMenu;
    private String SIGN_IN = tr("Sign in");
    private String FRIENDS = tr("Friends");
    private boolean loggedIn;
    private ButtonGroup availabilityButtonGroup;
    private JCheckBoxMenuItem availablePopupItem;
    private JCheckBoxMenuItem awayPopupItem;
    private final UnseenMessageFlasher flasher;
    
    @Resource private Color topGradient;
    @Resource private Color bottomGradient;
    
    @Resource private Color topBorderColor;
    @Resource private Color belowTopBorderColor;
    @Resource private int height;

    @Inject
    public StatusPanel(final TrayNotifier trayNotifier, final IconManager iconManager, IconLibrary icons, AudioPlayer player) {
        GuiUtils.assignResources(this);
        this.icons = icons;
        
        setLayout(new MigLayout("insets 0, gap 0, hidemode 3, fill"));
        add(Line.createHorizontalLine(topBorderColor, 1), "span, grow, wrap");
        add(Line.createHorizontalLine(belowTopBorderColor, 1), "span, grow, wrap");
        
        setMinimumSize(new Dimension(0, height + 2));
        setMaximumSize(new Dimension(Short.MAX_VALUE, height + 2));
        setPreferredSize(new Dimension(Short.MAX_VALUE, height + 2));
        
        setBackgroundPainter(new MattePainter<JXButton>(
                new GradientPaint(new Point2D.Double(0, 0), topGradient, 
                        new Point2D.Double(0, 1), bottomGradient,
                        false), true));
        
        add(new JButton(new AbstractAction("Error Test") {
            @Override
            public void actionPerformed(ActionEvent e) {
                throw new RuntimeException("Test Error");
            }
        }), "gapbefore push");

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
        }), "gapbefore push");
        MiniPlayerPanel miniPlayerPanel = new MiniPlayerPanel(player);
        miniPlayerPanel.setVisible(false);
        add(miniPlayerPanel, "gapbefore push");

        JMenuBar menuBar = new JMenuBar();
        Color whiteBackground = Color.WHITE;
        menuBar.setBackground(whiteBackground);
        menuBar.setOpaque(true);
        menuBar.setBorderPainted(false);
        JPanel menuPanel = new JPanel(new BorderLayout());
        menuPanel.setBorder(BorderFactory.createLineBorder(new Color(159, 159, 159)));
        menuPanel.setOpaque(true);
        menuPanel.setMinimumSize(new Dimension(0, 20));
        menuPanel.setMaximumSize(new Dimension(150, 20));
        menuPanel.add(menuBar, BorderLayout.WEST);
        menuPanel.setBackground(whiteBackground);
        
        add(menuPanel, "gapbefore push");
        
        statusMenu = new JMenu();
        statusMenu.setCursor(new Cursor(Cursor.HAND_CURSOR));
        statusMenu.setOpaque(true);
        statusMenu.setEnabled(false);
        statusMenu.setIcon(icons.getEndChat());
        statusMenu.setBackground(whiteBackground);
        statusMenu.setBorderPainted(false);
        menuBar.add(statusMenu);
        friendsButton = new JXButton(new FriendsAction(SIGN_IN));
        friendsButton.setBackgroundPainter(new RectanglePainter<JXButton>(whiteBackground, whiteBackground));
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
        
        this.flasher = new UnseenMessageFlasher(friendsButton);
        
        EventAnnotationProcessor.subscribe(this);
    }
    
    @EventSubscriber
    public void handleSigninEvent(XMPPConnectionEstablishedEvent event) {
        loggedIn = true;
        updateStatus(FRIENDS, FriendsUtil.getIcon(Mode.available, icons), I18n.tr(Mode.available.toString()));
        statusMenu.setEnabled(true);
    }
    
    
    private void updateStatus(String buttonText, Icon icon, String status) {
        friendsButton.setText(buttonText);
        statusMenu.setIcon(icon);
        statusMenu.setToolTipText(tr("{0} - click to set status", status));
    }
    
    @EventSubscriber
    public void handleSignoffEvent(SignoffEvent event) {
        loggedIn = false;
        updateStatus(SIGN_IN, icons.getEndChat(), OFFLINE);
        statusMenu.setEnabled(false);
    }
    
    @EventSubscriber
    public void handleStatusChange(SelfAvailabilityUpdateEvent event) {
        Mode newMode = event.getNewMode();
        updateStatus(friendsButton.getText(), FriendsUtil.getIcon(newMode, icons), I18n.tr(newMode.toString()));
        ButtonModel model = newMode == Mode.available ? availablePopupItem.getModel() :
                            (newMode == Mode.away || newMode == Mode.xa) ? awayPopupItem.getModel() : null;
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
            new DisplayFriendsToggleEvent().publish();
        }
    }

    @Override
    public void clearUnseenMessages() {
        flasher.reset();
    }

    @Override
    public void unseenMessagesReceived() {
        flasher.flash();
    }
    
    private static class UnseenMessageFlasher {
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
