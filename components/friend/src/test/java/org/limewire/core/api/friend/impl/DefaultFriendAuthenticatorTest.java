package org.limewire.core.api.friend.impl;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.limewire.core.api.friend.feature.features.AuthToken;
import org.limewire.util.BaseTestCase;
import org.limewire.util.StringUtils;

public class DefaultFriendAuthenticatorTest extends BaseTestCase {

    private DefaultFriendAuthenticator authenticator;

    public DefaultFriendAuthenticatorTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        authenticator = new DefaultFriendAuthenticator();
    }
    
    public void testAuthenticatesItsAuthToken() {
        AuthToken authToken = authenticator.getAuthToken("me@you.com");
        assertTrue(authenticator.authenticate(new UsernamePasswordCredentials("me@you.com", StringUtils.getASCIIString(Base64.encodeBase64(authToken.getToken())))));
    }
    
    public void testAuthenticateLooksAtUsername() {
        AuthToken authToken = authenticator.getAuthToken("me@you.com");
        assertFalse(authenticator.authenticate(new UsernamePasswordCredentials("different@username", StringUtils.getASCIIString(Base64.encodeBase64(authToken.getToken())))));
    }
    
    public void testAuthenticateHandlesNullPassword() {
        assertFalse(authenticator.authenticate(new UsernamePasswordCredentials("different@username", null)));
    }
}
