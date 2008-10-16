package org.limewire.http.auth;

import java.net.URI;

public interface ProtectedURLRegistry {
    void addProtectedURL(URI url);
}
