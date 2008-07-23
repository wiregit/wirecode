package com.limegroup.gnutella.xmpp;

import org.limewire.xmpp.client.service.XMPPConnectionConfiguration;
import org.limewire.xmpp.client.service.RosterListener;
import org.limewire.xmpp.client.service.XMPPErrorListener;

public class XMPPConfigurationAdapter implements XMPPConnectionConfiguration {

    private final XMPPServerConfiguration serverConfiguration;
    private final RosterListener rosterListener;
    private final XMPPErrorListener errorListener;

    public XMPPConfigurationAdapter(XMPPServerConfiguration serverConfiguration, RosterListener rosterListener, XMPPErrorListener errorListener) {
        this.serverConfiguration = serverConfiguration;
        this.rosterListener = rosterListener;
        this.errorListener = errorListener;
    }

    public boolean isDebugEnabled() {
        return serverConfiguration.isDebugEnabled();
    }

    public String getUsername() {
        return serverConfiguration.getUsername();
    }

    public String getPassword() {
        return serverConfiguration.getPassword();
    }

    public String getHost() {
        return serverConfiguration.getHost();
    }

    public int getPort() {
        return serverConfiguration.getPort();
    }

    public String getServiceName() {
        return serverConfiguration.getServiceName();
    }

    public boolean isAutoLogin() {
        return serverConfiguration.isAutoLogin();
    }

    public RosterListener getRosterListener() {
        return rosterListener;
    }

    public XMPPErrorListener getErrorListener() {
        return errorListener;
    }
}
