package org.limewire.xmpp.client;

/**
 * Allows the xmpp service user to provide configuration for xmpp login.
 */
public interface XMPPConnectionConfiguration {
    boolean isDebugEnabled();
    String getUsername();
    String getPassword();
    String getHost();
    int getPort();
    String getServiceName();
    boolean isAutoLogin();
    RosterListener getRosterListener();
}
