package org.limewire.lws.server;

import java.util.HashMap;
import java.util.Map;

import org.limewire.lws.server.LWSDispatcherSupport.Parameters;

import junit.framework.Test;
import junit.textui.TestRunner;

/**
 * Tests bad forms of authenticating.
 */
public class BadAuthenticationTestWithBadPrivateKey extends AbstractCommunicationSupport {

    public BadAuthenticationTestWithBadPrivateKey(String s) {
        super(s);
    }

    public static Test suite() {
        return buildTestSuite(BadAuthenticationTestWithBadPrivateKey.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public void testSendAuthenticationWithBadPrivateKey() {
        Map<String, String> args = new HashMap<String, String>();
        args.put(Parameters.PRIVATE, "asdfasdf"); // This is too short to be valid
        getCode().sendLocalMsg(LWSDispatcherSupport.Commands.AUTHENTICATE,
                               args,
                               errorHandlerAny());
    }    

}
