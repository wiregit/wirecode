package org.limewire.xmpp.client;

import org.limewire.xmpp.client.service.XMPPConnectionConfiguration;
import org.limewire.xmpp.client.service.RosterListener;
import org.limewire.xmpp.client.service.XMPPErrorListener;
import org.limewire.xmpp.client.impl.XMPPException;

public class XMPPConnectionConfigurationImpl implements XMPPConnectionConfiguration {
    String userName;
    String pw;
    String host;
    int port;
    String serviceName;
    private final RosterListener rosterListener;

    public XMPPConnectionConfigurationImpl(String userName, String pw, String host, int port, String serviceName, RosterListener rosterListener) {
        this.userName = userName;
        this.pw = pw;
        this.host = host;
        this.port = port;
        this.serviceName = serviceName;
        this.rosterListener = rosterListener;
    }
    
    public boolean isDebugEnabled() {
        return true;
    }

    public String getUsername() {
        return userName;
    }

    public String getPassword() {
        return pw;
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

    public boolean isAutoLogin() {
        return true;
    }

    public RosterListener getRosterListener() {
        return rosterListener;
    }

    public XMPPErrorListener getErrorListener() {
        return new XMPPErrorListener() {
            public void error(XMPPException exception) {
                exception.printStackTrace();
            }
        };
    }
}
