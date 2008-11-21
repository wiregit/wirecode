package org.limewire.ui.swing.friends.login;

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.VerticalLayout;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.components.HyperLinkButton;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.friends.DisplayFriendsToggleEvent;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.I18n;
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
    
    @Inject
    FriendsSignInPanel(LoginPanel loginPanel,
                       LoggedInPanel loggedInPanel,
                       XMPPService xmppService) {
        this.loggedInPanel = loggedInPanel;
        this.loginPanel = loginPanel;
        this.xmppService = xmppService;
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
        
        disconnected();
    }
    
    private void connected() {
        shareLabel.setVisible(false);
        loginPanel.setVisible(false);
        loggedInPanel.setVisible(true);
    }
    
    private void disconnected() {
        loggedInPanel.setVisible(false);
        shareLabel.setVisible(true);
        loginPanel.setVisible(false);
    }
    
    @Inject void register(ListenerSupport<XMPPConnectionEvent> connectionSupport) {
        connectionSupport.addListener(new EventListener<XMPPConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(XMPPConnectionEvent event) {
                switch(event.getType()) {
                case CONNECTED:
                    connected();
                    break;
                case RECONNECTING:
                case RECONNECTING_FAILED:
                case DISCONNECTED:
                    disconnected();
                    break;
                }
            }
        });
    }
    
    @EventSubscriber
    public void handleAppear(DisplayFriendsToggleEvent event) {       
        if(event.getVisible() == null | Boolean.TRUE.equals(event.getVisible())) {
            if(!xmppService.isLoggedIn()) {
                loginPanel.setVisible(true);
            }
        }
    }
}
