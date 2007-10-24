package org.limewire.net;

class EmptyProxySettings implements ProxySettings {

    public ProxyType getCurrentProxyType() {
        return ProxyType.NONE;
    }

    public String getProxyPassword() {
        return null;
    }

    public String getProxyUsername() {
        return null;
    }

    public boolean isProxyAuthenticationRequired() {
        return false;
    }

    public boolean isProxyForPrivateEnabled() {
        return false;
    }

    public String getProxyHost() {
        return null;
    }

    public int getProxyPort() {
        return -1;
    }

}
