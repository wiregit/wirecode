package org.limewire.xmpp.client.impl.messages.authtoken;

import java.util.Random;

import org.limewire.util.BaseTestCase;
import org.limewire.xmpp.client.impl.messages.IQTestUtils;
import org.limewire.xmpp.client.impl.messages.InvalidIQException;

public class AuthTokenIQTest extends BaseTestCase {

    public AuthTokenIQTest(String name) {
        super(name);
    }
    
    public void testParsesOwnOutput() throws Exception {
        byte[] token = new byte[20];
        new Random().nextBytes(token);
        AuthTokenIQ authTokenIQ = new AuthTokenIQ(token);
        
        AuthTokenIQ parsedAuthTokenIQ = new AuthTokenIQ(IQTestUtils.createParser(authTokenIQ.getChildElementXML()));
        assertEquals(token, parsedAuthTokenIQ.getAuthToken());
        
        authTokenIQ = new AuthTokenIQ((byte[])null);
        parsedAuthTokenIQ = new AuthTokenIQ(IQTestUtils.createParser(authTokenIQ.getChildElementXML()));
        assertNull(parsedAuthTokenIQ.getAuthToken());
    }

    public void testParsesMissingValueAttributeGracefully() throws Exception {
        try {
            new AuthTokenIQ(IQTestUtils.createParser("<auth-token xmlns=\"jabber:iq:lw-auth-token\"><token/></auth-token>"));
        } catch (InvalidIQException iie) {
        }
    }
}
