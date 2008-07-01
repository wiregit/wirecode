package org.limewire.xmpp.client;

/**
 * Allows the xmpp service user to provide configuration for xmpp login.
 */
public interface XMPPConnectionConfiguration {
    public boolean isDebugEnabled();
    public String getUsername();
    public String getPassword();
    public String getHost();
    public int getPort();
    public String getServiceName();
    public boolean isAutoLogin();
    public RosterListener getRosterListener();
    public XMPPErrorListener getErrorListener();
}
