package org.limewire.xmpp.client.impl;

import org.limewire.listener.EventListener;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.XMPPErrorListener;
import org.limewire.xmpp.api.client.XMPPException;
import org.limewire.xmpp.api.client.XMPPService;

public class XMPPConnectionConfigurationMock implements XMPPConnectionConfiguration {
    private final String username;
    private final String password;
    private final String host;
    private final int port;
    private final String serviceName;
    private final EventListener<RosterEvent> rosterListener;

    public XMPPConnectionConfigurationMock(String username, String password,
            String host, int port, String serviceName,
            EventListener<RosterEvent> rosterListener) {
        this.username = username;
        this.password = password;
        this.host = host;
        this.port = port;
        this.serviceName = serviceName;
        this.rosterListener = rosterListener;
    }
    
    @Override
    public boolean isDebugEnabled() {
        return true;
    }
    
    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }
    
    @Override
    public String getMyID() {
        return getUsername();
    }

    @Override
    public String getNetworkName() {
        return getServiceName();
    }

    @Override
    public String getResource() {
        return "LimeWire";
    }
    
    @Override
    public EventListener<RosterEvent> getRosterListener() {
        return rosterListener;
    }
    
    @Override
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
}