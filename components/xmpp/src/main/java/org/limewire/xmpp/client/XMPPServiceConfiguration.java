package org.limewire.xmpp.client;

public interface XMPPServiceConfiguration {
    boolean isDebugEnabled();
    String getUsername();
    String getPassword();
    String getHost();
    int getPort();
    String getServiceName();
}
