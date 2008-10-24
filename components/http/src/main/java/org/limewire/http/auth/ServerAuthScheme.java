package org.limewire.http.auth;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.auth.Credentials;

public interface ServerAuthScheme {
    void setComplete();
    boolean isComplete();
    Credentials authenticate(HttpRequest request);
    Header createChallenge();
}
