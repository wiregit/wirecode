package org.limewire.http.auth;

import org.apache.http.HttpRequest;
import org.apache.http.auth.Credentials;

public interface ServerAuthScheme {
    Credentials authenticate(HttpRequest request);
}
