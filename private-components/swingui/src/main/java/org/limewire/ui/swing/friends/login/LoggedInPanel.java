package org.limewire.ui.swing.friends.login;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;

import org.limewire.core.settings.XMPPSettings;
import org.limewire.ui.swing.action.StatusActions;
import org.limewire.ui.swing.components.HyperLinkButton;
import org.limewire.ui.swing.components.LimeComboBox;
import org.limewire.ui.swing.components.LimeComboBoxFactory;
import org.limewire.ui.swing.friends.AddFriendDialog;
import org.limewire.ui.swing.painter.BarPainterFactory;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Inject;

class LoggedInPanel extends JXPanel {

    private final JLabel currentUser;
    private final JLabel loggingInLabel;
    private final JLabel statusMenuLabel;
    private final JButton signInButton;
    private final JButton switchUserButton;
    private final LimeComboBox optionsBox;
    private final LimeComboBox signoutBox;
    private final FriendActions friendActions;

    @Inject
    LoggedInPanel(LimeComboBoxFactory comboFactory,
            FriendActions friendActions, BarPainterFactory barPainterFactory,
            StatusActions statusActions, XMPPService xmppService) {
        GuiUtils.assignResources(this);
        setLayout(new MigLayout("insets 0, gap 0, hidemode 3, fill"));

        this.friendActions = friendActions;
        optionsBox = comboFactory.createMiniComboBox();
        signoutBox = comboFactory.createMiniComboBox();
        statusMenuLabel = new JLabel();        
        currentUser = new JLabel();
        loggingInLabel = new JLabel(I18n.tr("Signing in..."));
        signInButton = new HyperLinkButton();
        switchUserButton = new HyperLinkButton();
        setBackgroundPainter(barPainterFactory.createFriendsBarPainter()); 

        initComponents(statusActions, xmppService);
    }

    private void initComponents(final StatusActions statusActions,
                                final XMPPService xmppService) {
        JPopupMenu optionsMenu = new JPopupMenu(); 
        optionsMenu.add(new AbstractAction(I18n.tr("Add Friend")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                new AddFriendDialog(LoggedInPanel.this,
                        xmppService.getActiveConnection());
            }
        });
        optionsMenu.addSeparator();
        optionsMenu.add(new JLabel(I18n.tr("Show:")));
        final JCheckBoxMenuItem showOfflineFriends = new JCheckBoxMenuItem(I18n.tr("Offline Friends"));
        showOfflineFriends.setSelected(XMPPSettings.XMPP_SHOW_OFFLINE.getValue());
        showOfflineFriends.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                XMPPSettings.XMPP_SHOW_OFFLINE.setValue(showOfflineFriends.isSelected());
            } 
        });

        optionsMenu.add(showOfflineFriends);
        optionsMenu.addSeparator();
        optionsMenu.add(statusMenuLabel);
        optionsMenu.add(statusActions.getAvailableAction());
        optionsMenu.add(statusActions.getDnDAction());
        optionsBox.overrideMenu(optionsMenu);
        optionsBox.setText(I18n.tr("Options"));

        signoutBox.setText(I18n.tr("Sign Out"));
        signoutBox.addAction(new AbstractAction(I18n.tr("Switch User")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                friendActions.signOut(true);
            }
        });
        signoutBox.addAction(new AbstractAction(I18n.tr("Sign Out")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                friendActions.signOut(false);
            }
        });

        signInButton.setAction(new AbstractAction(I18n.tr("Sign In")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                friendActions.signIn();
            }
        });

        switchUserButton.setAction(new AbstractAction(I18n.tr("Switch User")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                friendActions.signOut(true);
            }
        });

        add(currentUser, "gapleft 9, gaptop 2, wmin 0, wrap");

        add(optionsBox, "gapleft 2, alignx left, gapbottom 2, split");
        add(signoutBox, "gapleft push, gapbottom 2, wrap");

        add(signInButton, "gapleft 2, alignx left, gapbottom 2, split");
        add(switchUserButton, "gapleft push, gapbottom 2, wrap");

        add(loggingInLabel, "alignx left, gapleft 9, gaptop 2, gapbottom 2");

        optionsBox.setVisible(false);
        signoutBox.setVisible(false);
        signInButton.setVisible(false);
        switchUserButton.setVisible(false);
        loggingInLabel.setVisible(false);
    }

    private void setConfig(XMPPConnectionConfiguration config) {
        if(config != null) {
            currentUser.setText(config.getUsername());
            statusMenuLabel.setText(I18n.tr("Set {0} Status:", config.getLabel()));
        }
    }

    void autoLogin(XMPPConnectionConfiguration config) {
        setConfig(config);
        optionsBox.setVisible(false);
        signoutBox.setVisible(false);
        signInButton.setVisible(false);
        switchUserButton.setVisible(false);
        loggingInLabel.setVisible(true);
    }

    void connected(XMPPConnectionConfiguration config) {
        setConfig(config);
        optionsBox.setVisible(true);
        signoutBox.setVisible(true);
        signInButton.setVisible(false);
        switchUserButton.setVisible(false);
        loggingInLabel.setVisible(false);
    }

    void disconnected(XMPPConnectionConfiguration config) {
        setConfig(config);
        optionsBox.setVisible(false);
        signoutBox.setVisible(false);
        signInButton.setVisible(true);
        switchUserButton.setVisible(true);
        loggingInLabel.setVisible(false);
    }

    void connecting(XMPPConnectionConfiguration config) {
        autoLogin(config);
    }

}
