package com.limegroup.gnutella.uploader.authentication;

import org.limewire.security.SecurityToken;
import org.limewire.security.SecurityToken.TokenData;

public interface Authenticator {

    boolean authenticate(SecurityToken token, TokenData data);
        
}
