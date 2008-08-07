package org.limewire.xmpp.client.impl;

import org.limewire.xmpp.api.client.RosterListener;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPErrorListener;
import org.limewire.xmpp.api.client.XMPPException;
import org.limewire.xmpp.api.client.XMPPService;

public class XMPPConnectionConfigurationImpl implements XMPPConnectionConfiguration {
    private final String userName;
    private final String pw;
    private final String host;
    private final int port;
    private final String serviceName;
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
            public void register(XMPPService xmppService) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void error(XMPPException exception) {
                exception.printStackTrace();
            }
        };
    }
}
