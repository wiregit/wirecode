package org.limewire.core.impl.xmpp;

import org.limewire.xmpp.api.client.RosterListener;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPErrorListener;

public class XMPPConfigurationImpl implements XMPPConnectionConfiguration {

    private final XMPPServerSettings.XMPPServerConfiguration serverConfiguration;
    private final XMPPUserSettings.XMPPUserConfiguration userConfiguration;
    private final RosterListener rosterListener;
    private final XMPPErrorListener errorListener;

    public XMPPConfigurationImpl(XMPPServerSettings.XMPPServerConfiguration serverConfiguration,
                                 XMPPUserSettings.XMPPUserConfiguration userConfiguration,
                                 RosterListener rosterListener,
                                 XMPPErrorListener errorListener) {
        this.serverConfiguration = serverConfiguration;
        this.userConfiguration = userConfiguration;
        this.rosterListener = rosterListener;
        this.errorListener = errorListener;
    }

    public boolean isDebugEnabled() {
        return serverConfiguration.isDebugEnabled();
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

    public boolean isAutoLogin() {
        return userConfiguration.isAutoLogin();
    }

    public void setAutoLogin(boolean autoLogin) {
        userConfiguration.setAutoLogin(autoLogin);
    }

    public RosterListener getRosterListener() {
        return rosterListener;
    }

    public XMPPErrorListener getErrorListener() {
        return errorListener;
    }
}
