package org.limewire.http.auth;

import org.apache.http.auth.Credentials;

public class ServerAuthState {

    public static final String AUTH_STATE = "http.server.auth"; 

    private ServerAuthScheme scheme;
    private Credentials credentials;

    public ServerAuthScheme getScheme() {
        return scheme;
    }

    public void setScheme(ServerAuthScheme scheme) {
        this.scheme = scheme;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }
}
