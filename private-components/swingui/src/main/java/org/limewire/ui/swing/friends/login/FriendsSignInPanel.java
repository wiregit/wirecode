package org.limewire.ui.swing.friends.login;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.VerticalLayout;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.components.HyperLinkButton;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.friends.DisplayFriendsToggleEvent;
import org.limewire.ui.swing.friends.settings.XMPPAccountConfiguration;
import org.limewire.ui.swing.friends.settings.XMPPAccountConfigurationManager;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FriendsSignInPanel extends JXPanel {
    
    private final HyperLinkButton shareLabel;
    private final LoginPanel loginPanel;
    private final LoggedInPanel loggedInPanel;
    private final XMPPService xmppService;
    private final XMPPAccountConfigurationManager accountManager;
    
    @Inject
    FriendsSignInPanel(LoginPanel loginPanel,
                       LoggedInPanel loggedInPanel,
                       XMPPService xmppService,
                       XMPPAccountConfigurationManager accountManager) {
        this.loggedInPanel = loggedInPanel;
        this.loginPanel = loginPanel;
        this.xmppService = xmppService;
        this.accountManager = accountManager;
        EventAnnotationProcessor.subscribe(this);
        
        setLayout(new VerticalLayout(0));
        
        shareLabel = new HyperLinkButton(I18n.tr("Share files with your friends!"), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                FriendsSignInPanel.this.loginPanel.setVisible(true);
            }
        });
        shareLabel.setMouseOverColor(Color.BLUE);
        FontUtils.changeSize(shareLabel, -1);
        add(shareLabel);
        add(loginPanel);
        add(loggedInPanel);
        
        // Presetup the UI so that it looks correct until services start.
        XMPPConnectionConfiguration config = accountManager.getAutoLoginConfig();
        if(config != null) {
            loggedInPanel.autoLogin(config);
            shareLabel.setVisible(false);
            loginPanel.setVisible(false);
            loggedInPanel.setVisible(true);
        } else {
            shareLabel.setVisible(true);
            loginPanel.setVisible(false);
            loggedInPanel.setVisible(false);
        }
    }
    
    private void connecting(XMPPConnectionConfiguration config) {
        loginPanel.connecting(config);
        loggedInPanel.connecting(config);
    }
    
    private void connected(XMPPConnectionConfiguration config) {
        shareLabel.setVisible(false);
        loginPanel.setVisible(false);
        loggedInPanel.setVisible(true);

        loginPanel.connected(config);
        loggedInPanel.connected(config);        
    }
    
    private void disconnected(Exception reason) {
        XMPPAccountConfiguration config = accountManager.getAutoLoginConfig();
        
        if(reason == null && config != null) {
            loggedInPanel.setVisible(true);
            shareLabel.setVisible(false);
            loginPanel.setVisible(false);
        } else {
            loggedInPanel.setVisible(false);
            shareLabel.setVisible(true);
            loginPanel.setVisible(reason != null);
        }
        
        loginPanel.disconnected(reason);
        loggedInPanel.disconnected(reason, config);   
    }
    
    private void autoLogin(XMPPAccountConfiguration config) {
        shareLabel.setVisible(false);
        loginPanel.setVisible(false);
        loggedInPanel.setVisible(true);
        
        loggedInPanel.autoLogin(config);
        loginPanel.autoLogin(config);
    }
        
    @Inject void register(ServiceRegistry registry) {
        registry.register(new Service() {
            @Override
            public String getServiceName() {
                return tr("Friend Auto-Login");
            }
            @Override
            public void initialize() {
            }
            
            @Override
            public void start() {
                // If there's an auto-login account, select it and log in
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        XMPPAccountConfiguration auto = accountManager.getAutoLoginConfig();
                        if(auto != null) {
                            autoLogin(auto);
                        }
                    }
                });
            }
            
            @Override
            public void stop() {
            }
        });
    }
    
    @Inject void register(ListenerSupport<XMPPConnectionEvent> connectionSupport) {
        connectionSupport.addListener(new EventListener<XMPPConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(XMPPConnectionEvent event) {
                switch(event.getType()) {
                case CONNECTING:
                    connecting(event.getSource().getConfiguration());
                    break;
                case CONNECTED:
                    connected(event.getSource().getConfiguration());
                    break;
                case RECONNECTING:
                case RECONNECTING_FAILED:
                case DISCONNECTED:
                case CONNECT_FAILED:
                    disconnected(event.getData());
                    break;
                }
            }
        });
    }
    
    @EventSubscriber
    public void handleAppear(DisplayFriendsToggleEvent event) {       
        if(event.getVisible() == null | Boolean.TRUE.equals(event.getVisible())) {
            if(!xmppService.isLoggedIn() && !xmppService.isLoggingIn()) {
                XMPPAccountConfiguration config = accountManager.getAutoLoginConfig();
                if(config == null) {
                    shareLabel.setVisible(true);
                    loginPanel.setVisible(true);
                    loggedInPanel.setVisible(false);
                } else {
                    autoLogin(config);
                }
            }
        }
    }
}
