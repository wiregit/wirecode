package org.limewire.ui.swing.friends.login;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.limewire.core.settings.FacebookSettings;
import org.limewire.friend.api.FriendConnectionFactory;
import org.limewire.ui.swing.components.decorators.ButtonDecorator;
import org.limewire.ui.swing.friends.settings.FriendAccountConfiguration;
import org.limewire.ui.swing.friends.settings.FriendAccountConfigurationManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.ResizeUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class ServiceSelectionLoginPanel extends JPanel {
    
    private static final String CONFIG = "limewire.configProperty";
    
    @Resource Color labelTextForeground;
    @Resource Font labelTextFont;
    @Resource Color messageTextForeground;
    @Resource Font messageTextFont;
    
    @Resource Icon facebookIcon;
    
    @Resource private Icon shareIcon;
    @Resource private Icon browseIcon;
    @Resource private Icon chatIcon;
    
    private final LoginPopupPanel parent;
    private final Provider<XMPPUserEntryLoginPanelFactory> xmppLoginPanelFactory;

    @Inject
    public ServiceSelectionLoginPanel(LoginPopupPanel parent, FriendAccountConfigurationManager accountManager,
            ButtonDecorator buttonDecorator, FriendConnectionFactory friendConnectionFactory,
            Provider<XMPPUserEntryLoginPanelFactory> xmppLoginPanelFactory) {
        
        super(new BorderLayout());
        setOpaque(false);
    
        GuiUtils.assignResources(this);
        
        this.parent = parent;
        this.xmppLoginPanelFactory = xmppLoginPanelFactory;
        
        JPanel topPanel = new JPanel(new MigLayout("insets 0, gap 0, alignx center, flowy"));
        
        JLabel shareLabel = new JLabel(I18n.tr("Share"));
        shareLabel.setFont(labelTextFont);
        shareLabel.setForeground(labelTextForeground);
        topPanel.add(shareLabel, "gaptop 8");
        
        JLabel shareIconLabel = new JLabel(shareIcon);
        //ResizeUtils.forceSize(shareIcon, new Dimension(153,90));
        topPanel.add(shareIconLabel, "gapright 14, wrap");
        
        JLabel browseLabel = new JLabel(I18n.tr("Browse"));
        browseLabel.setFont(labelTextFont);
        browseLabel.setForeground(labelTextForeground);
        topPanel.add(browseLabel);
        
        JLabel browseIconLabel = new JLabel(browseIcon);
        //ResizeUtils.forceSize(browseIcon, new Dimension(153,90));
        topPanel.add(browseIconLabel, "gapright 14, wrap");
        
        JLabel chatLabel = new JLabel(I18n.tr("Chat"));
        chatLabel.setFont(labelTextFont);
        chatLabel.setForeground(labelTextForeground);
        topPanel.add(chatLabel);
        
        JLabel chatIconLabel = new JLabel(chatIcon);
        //ResizeUtils.forceSize(chatIcon, new Dimension(153,90));
        topPanel.add(chatIconLabel);
        
        
        JPanel centerPanel = new JPanel(new MigLayout("gap 0, insets 15 0 0 0, align center"));
        
        JLabel signOnMessageLabel = new JLabel(I18n.tr("Sign on with any of the services below:"));
        signOnMessageLabel.setOpaque(false);
        signOnMessageLabel.setForeground(messageTextForeground);
        signOnMessageLabel.setFont(messageTextFont);
        
        centerPanel.add(signOnMessageLabel);        
        
        JPanel bottomPanel = new JPanel(new MigLayout("gap 0, insets 0, align center"));
        bottomPanel.setOpaque(false);
        
        JXButton facebookButton = new JXButton(new FacebookLoginAction(accountManager.getConfig("Facebook"), friendConnectionFactory, parent));
        facebookButton.setVisible(FacebookSettings.FACEBOOK_ENABLED.getValue());
        JXButton gmailButton = new JXButton(new ServiceAction(accountManager.getConfig("Gmail")));
        JXButton liveJournalButton = new JXButton(new ServiceAction(accountManager.getConfig("LiveJournal")));
        JXButton otherButton = new JXButton(new ServiceAction(I18n.tr("Other"), accountManager.getConfig("Jabber")));
        
        ResizeUtils.forceSize(facebookButton, new Dimension(180, 58));
        ResizeUtils.forceSize(gmailButton, new Dimension(180, 58));
        ResizeUtils.forceSize(liveJournalButton, new Dimension(180, 58));
        ResizeUtils.forceSize(otherButton, new Dimension(180, 58));
        
        buttonDecorator.decorateFlatButton(facebookButton);
        buttonDecorator.decorateFlatButton(gmailButton);
        buttonDecorator.decorateFlatButton(liveJournalButton);
        buttonDecorator.decorateFlatButton(otherButton);
        
        JPanel selectionPanel = new JPanel(new MigLayout("gap 0, insets 0 0 30 0, alignx center, filly"));
        selectionPanel.setOpaque(false);
        selectionPanel.add(facebookButton, "gaptop 10, gapright 30");
        selectionPanel.add(gmailButton, "wrap");
        selectionPanel.add(liveJournalButton, "gapright 30, gaptop 30");
        selectionPanel.add(otherButton);
        bottomPanel.add(selectionPanel);
        
        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        
    }
    
    private class ServiceAction extends org.limewire.ui.swing.action.AbstractAction {

        public ServiceAction(FriendAccountConfiguration config) {
            super(config.getLabel(), config.getLargeIcon());
            putValue(CONFIG, config);
        }
        
        public ServiceAction(String nameOverride, FriendAccountConfiguration config) {
            super(nameOverride, config.getLargeIcon());
            this.
            putValue(CONFIG, config);
        }
        
        @Override
        public void actionPerformed(ActionEvent arg0) {
            FriendAccountConfiguration config = (FriendAccountConfiguration)getValue(CONFIG);
            JComponent component = xmppLoginPanelFactory.get().create(config);
            parent.setLoginComponent(component);
        }
        
    }

}
