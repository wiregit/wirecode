package org.limewire.http.auth;

public interface AuthenticatorRegistry {
    void addAuthenticator(Authenticator userStore);
}
