package org.limewire.ui.swing.friends;

import java.util.List;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPException;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class XMPPEventHandler {
    private final XMPPService xmppService;

    @Inject
    public XMPPEventHandler(XMPPService service) {
        this.xmppService = service;
        
        EventAnnotationProcessor.subscribe(this);
    }
    
    public void login(String serviceName, String username, String password, boolean autologin) throws XMPPException {
        List<XMPPConnection> connections = xmppService.getConnections();
        for(XMPPConnection connection : connections) {
            XMPPConnectionConfiguration configuration = connection.getConfiguration();
            //FIXME: Update to distinguish for Facebook service name (whenever Facebook enables XMPP service)
            if(configuration.getServiceName().equals(serviceName)) {
                if(!connection.isLoggedIn()) {
                    configuration.setUsername(username);
                    configuration.setPassword(password);
                    configuration.setAutoLogin(autologin);
                    connection.login();
                }
            }
        }
    }
    
    @EventSubscriber
    public void handleSignoffEvent(SignoffEvent event) {
        ThreadExecutor.startThread(new Runnable() {
            public void run() {
                logout();
            }
        }, "xmpp-logout");
    }
    
    private void logout() {
        List<XMPPConnection> connections = xmppService.getConnections();
        for(XMPPConnection connection : connections) {
            if (connection.isLoggedIn()) {
                connection.logout();
            }
        }
    }
    
    public XMPPConnectionConfiguration getConfig(String serviceName) {
        XMPPConnectionConfiguration config = null;
        List<XMPPConnection> connections = xmppService.getConnections();
        for(XMPPConnection connection : connections) {
            XMPPConnectionConfiguration configuration = connection.getConfiguration();
            if(configuration.getServiceName().equals(serviceName)) {
                config = configuration;
            }
        }
        return config;
    }
}
