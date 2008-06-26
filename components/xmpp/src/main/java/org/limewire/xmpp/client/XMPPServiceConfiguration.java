package org.limewire.xmpp.client;

/**
 * Allows the xmpp service user to provide configuration for xmpp login.
 */
public interface XMPPServiceConfiguration {
    boolean isDebugEnabled();
    String getUsername();
    String getPassword();
    String getHost();
    int getPort();
    String getServiceName();
}
