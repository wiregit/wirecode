package com.limegroup.gnutella.xmpp;

public class XMPPServerConfiguration {
    private final boolean isDebugEnabled;
    private final String username;
    private final String password;
    private final String host;
    private final int port;
    private final String serviceName;
    private final boolean isAutoLogin;

    public XMPPServerConfiguration(boolean debugEnabled,
                                   String username,
                                   String password,
                                   String host,
                                   int port,
                                   String serviceName,
                                   boolean autoLogin) {
        isDebugEnabled = debugEnabled;
        this.username = username;
        this.password = password;
        this.host = host;
        this.port = port;
        this.serviceName = serviceName;
        isAutoLogin = autoLogin;
    }

    public boolean isDebugEnabled() {
        return isDebugEnabled;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getServiceName() {
        return serviceName;
    }

    public boolean isAutoLogin() {
        return isAutoLogin;
    }
}
