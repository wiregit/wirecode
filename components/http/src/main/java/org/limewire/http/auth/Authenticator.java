package org.limewire.http.auth;

import org.apache.http.auth.Credentials;

public interface Authenticator {
    void register(AuthenticatorRegistry registry);
    boolean authenticate(Credentials credentials);
}
