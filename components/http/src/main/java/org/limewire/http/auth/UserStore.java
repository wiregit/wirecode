package org.limewire.http.auth;

import java.security.Principal;

import org.apache.http.auth.Credentials;

public interface UserStore {
    Principal authenticate(Credentials credentials);
}
