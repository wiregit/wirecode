package org.limewire.core.api.friend.impl;

import java.util.Arrays;

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

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AuthToken) {
            return Arrays.equals(token, ((AuthToken)obj).getToken());
        }
        return false;
    }
}
