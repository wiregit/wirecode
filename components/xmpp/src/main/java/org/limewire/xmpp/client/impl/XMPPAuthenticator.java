package org.limewire.xmpp.client.impl;

import java.security.SecureRandom;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.auth.Credentials;
import org.limewire.http.auth.Authenticator;
import org.limewire.http.auth.AuthenticatorRegistry;
import org.limewire.security.SHA1;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class XMPPAuthenticator implements Authenticator {

    private final byte[] seed = new SecureRandom().generateSeed(SHA1.HASH_LENGTH);
    
    @Override
    @Inject
    public void register(AuthenticatorRegistry registry) {
        registry.addAuthenticator(this);
    }

    public String getPassword(String user) {
        SHA1 sha1 = new SHA1();
        byte[] hash = sha1.digest(StringUtils.toUTF8Bytes(user));
        for (int i = 0; i < hash.length; i++) {
            hash[i] = (byte)(hash[i] ^ seed[i]);
        }
        sha1.reset();
        return StringUtils.getASCIIString(Base64.encodeBase64(sha1.digest(hash)));
    }

    @Override
    public boolean authenticate(Credentials credentials) {
        return credentials.getPassword().equals(getPassword(credentials.getUserPrincipal().getName()));
    }


}
