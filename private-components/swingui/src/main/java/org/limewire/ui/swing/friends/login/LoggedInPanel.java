package org.limewire.ui.swing.friends.login;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.components.HyperLinkButton;
import org.limewire.ui.swing.components.LimeComboBox;
import org.limewire.ui.swing.components.LimeComboBoxFactory;
import org.limewire.ui.swing.friends.settings.XMPPAccountConfigurationManager;
import org.limewire.ui.swing.painter.BarPainterFactory;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;

import com.google.inject.Inject;


class LoggedInPanel extends JXPanel {
    
    private final JLabel currentUser;
    private final JLabel loggingInLabel;
    private final JLabel statusMenuLabel;
    private final JButton signInButton;
    private final JButton switchUserButton;
    private final LimeComboBox optionsBox;
    private final LimeComboBox signoutBox;
    private final XMPPAccountConfigurationManager accountManager;
    private final FriendActions friendActions;    
    
    @Inject
    LoggedInPanel(LimeComboBoxFactory comboFactory,
                  XMPPAccountConfigurationManager accountManager,
                  FriendActions friendActions,
                  BarPainterFactory barPainterFactory) {
        GuiUtils.assignResources(this);
        setLayout(new MigLayout("insets 0, gap 0, hidemode 3, fill"));
        
        this.accountManager = accountManager;
        this.friendActions = friendActions;
        optionsBox = comboFactory.createMiniComboBox();
        signoutBox = comboFactory.createMiniComboBox();
        statusMenuLabel = new JLabel();        
        currentUser = new JLabel();
        loggingInLabel = new JLabel(I18n.tr("Logging in..."));
        signInButton = new HyperLinkButton(I18n.tr("Sign In"));
        switchUserButton = new HyperLinkButton(I18n.tr("Switch User"));        
        setBackgroundPainter(barPainterFactory.createFriendsBarPainter()); 
        
        initComponents();
    }
    
    private void initComponents() {
        JPopupMenu optionsMenu = new JPopupMenu(); 
        optionsMenu.add(new AbstractAction(I18n.tr("Add Friend")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                throw new RuntimeException("Implement me");
            }
        });
        optionsMenu.addSeparator();
        optionsMenu.add(new JLabel(I18n.tr("Show:")));
        optionsMenu.add(new JCheckBoxMenuItem(new AbstractAction(I18n.tr("Offline friends")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                            
            }
        }));
        optionsMenu.addSeparator();
        optionsMenu.add(statusMenuLabel);
        JCheckBoxMenuItem available = new JCheckBoxMenuItem(new AbstractAction(I18n.tr("Available")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                
            }
        });
        JCheckBoxMenuItem dnd = new JCheckBoxMenuItem(new AbstractAction(I18n.tr("Do Not Disturb")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                
            }
        });
        ButtonGroup group = new ButtonGroup();
        group.add(available);
        group.add(dnd);
        optionsMenu.add(available);
        optionsMenu.add(dnd);
        optionsBox.overrideMenu(optionsMenu);
        optionsBox.setText(I18n.tr("Options"));
        
        signoutBox.setText(I18n.tr("Sign out"));
        signoutBox.addAction(new AbstractAction(I18n.tr("Switch user")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                friendActions.signOut(true);
            }
        });
        signoutBox.addAction(new AbstractAction(I18n.tr("Sign out")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                friendActions.signOut(false);
            }
        });
        
        signInButton.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                friendActions.signIn();
            }
        });
        
        switchUserButton.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                accountManager.setAutoLoginConfig(null);
                friendActions.signIn();
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
            statusMenuLabel.setText(I18n.tr("Set {0} status", config.getLabel()));
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

    void connecting(XMPPConnectionConfiguration config) {
        autoLogin(config);
    }

}
