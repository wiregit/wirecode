package org.limewire.core.api.friend.impl;

import org.limewire.core.api.friend.feature.features.AuthToken;
import org.limewire.util.Objects;

/**
 * Default implementation for {@link AuthToken}.
 */
public class AuthTokenImpl implements AuthToken {

    private final byte[] token;

    public AuthTokenImpl(byte[] token) {
        this.token = Objects.nonNull(token, "token");
    }
    
    @Override
    public byte[] getToken() {
        return token;
    }

}
