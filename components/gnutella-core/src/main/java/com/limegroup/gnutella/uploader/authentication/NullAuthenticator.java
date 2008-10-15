package com.limegroup.gnutella.uploader.authentication;

import org.limewire.security.SecurityToken;
import org.limewire.security.SecurityToken.TokenData;

public class NullAuthenticator implements Authenticator {

    @Override
    public boolean authenticate(SecurityToken token, TokenData data) {
        return true;
    }

}
