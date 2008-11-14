package org.limewire.xmpp.client;

import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPErrorListener;
import org.limewire.xmpp.api.client.XMPPException;
import org.limewire.xmpp.api.client.XMPPService;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.listener.EventListener;

public class XMPPConnectionConfigurationMock implements XMPPConnectionConfiguration {
    private String userName;
    private String pw;
    private final String host;
    private final int port;
    private final String serviceName;
    private final String friendlyName;
    private final String registrationURL;
    private final EventListener<RosterEvent> rosterListener;

    public XMPPConnectionConfigurationMock(String userName, String pw,
            String host, int port, String serviceName, String friendlyName,
            String registrationURL, EventListener<RosterEvent> rosterListener) {
        this.userName = userName;
        this.pw = pw;
        this.host = host;
        this.port = port;
        this.serviceName = serviceName;
        this.friendlyName = friendlyName;
        this.registrationURL = registrationURL;
        this.rosterListener = rosterListener;
    }
    
    public XMPPConnectionConfigurationMock() {
        this("foo", "bar", "gmail.com", 5555, "gmail.com", "Gmail",
                "http://gmail.com/", new RosterListenerMock());
    }
    
    public boolean isDebugEnabled() {
        return true;
    }
    
    public boolean requiresDomain() {
        return false;
    }

    public String getUsername() {
        return userName;
    }

    public void setUsername(String username) {
        this.userName = username;
    }

    public String getPassword() {
        return pw;
    }

    public void setPassword(String password) {
        this.pw = password;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getServiceName() {
        return serviceName;
    }
    
    public String getFriendlyName() {
        return friendlyName;
    }
    
    public String getRegistrationURL() {
        return registrationURL;
    }

    public boolean isAutoLogin() {
        return true;
    }

    public void setAutoLogin(boolean autoLogin) {
        
    }

    public EventListener<RosterEvent> getRosterListener() {
        return rosterListener;
    }

    public XMPPErrorListener getErrorListener() {
        return new XMPPErrorListener() {
            public void register(XMPPService xmppService) {
                xmppService.setXmppErrorListener(this);
            }

            public void error(XMPPException exception) {
                exception.printStackTrace();
            }
        };
    }

    public String getMyID() {
        return getUsername();
    }

    public String getNetworkName() {
        return getServiceName();
    }

    @Override
    public String getResource() {
        return "LimeWire";
    }
}
