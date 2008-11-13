package org.limewire.core.impl.xmpp;

import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPErrorListener;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.listener.EventListener;

public class XMPPConfigurationImpl implements XMPPConnectionConfiguration {

    private final XMPPServerSettings.XMPPServerConfiguration serverConfiguration;
    private final XMPPUserSettings.XMPPUserConfiguration userConfiguration;
    private final EventListener<RosterEvent> rosterListener;
    private final XMPPErrorListener errorListener;
    private final String resource;

    public XMPPConfigurationImpl(XMPPServerSettings.XMPPServerConfiguration serverConfiguration,
                                 XMPPUserSettings.XMPPUserConfiguration userConfiguration,
                                 EventListener<RosterEvent> rosterListener,
                                 XMPPErrorListener errorListener, String resource) {
        this.serverConfiguration = serverConfiguration;
        this.userConfiguration = userConfiguration;
        this.rosterListener = rosterListener;
        this.errorListener = errorListener;
        this.resource = resource;
    }

    public boolean isDebugEnabled() {
        return serverConfiguration.isDebugEnabled();
    }
    
    public boolean requiresDomain() {
        return serverConfiguration.requiresDomain();
    }

    public String getUsername() {
        return userConfiguration.getUsername();
    }

    public void setUsername(String username) {
        userConfiguration.setUsername(username);
    }

    public String getPassword() {
        return userConfiguration.getPassword();
    }

    public void setPassword(String password) {
        userConfiguration.setPassword(password);
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
    
    public String getFriendlyName() {
        return serverConfiguration.getFriendlyName();
    }

    public boolean isAutoLogin() {
        return userConfiguration.isAutoLogin();
    }

    public void setAutoLogin(boolean autoLogin) {
        userConfiguration.setAutoLogin(autoLogin);
    }

    public EventListener<RosterEvent> getRosterListener() {
        return rosterListener;
    }

    public XMPPErrorListener getErrorListener() {
        return errorListener;
    }

    public String getMyID() {
        return getUsername();
    }

    public String getNetworkName() {
        return getServiceName();
    }

    @Override
    public String getResource() {
        return resource;
    }
    
}
