package org.limewire.xmpp.api.client;

/**
 * Allows the xmpp service user to provide configuration for xmpp login.
 */
public interface XMPPConnectionConfiguration {
    public boolean isDebugEnabled();
    public String getUsername();
    public void setUsername(String username);
    public String getPassword();
    public void setPassword(String password);
    public String getHost();
    public int getPort();
    public String getServiceName();
    public boolean isAutoLogin();
    public void setAutoLogin(boolean autoLogin);
    public RosterListener getRosterListener();
    public XMPPErrorListener getErrorListener();
}
