package org.limewire.lws.server;

import java.util.HashMap;
import java.util.Map;

import org.limewire.lws.server.LWSDispatcherSupport.Parameters;

import junit.framework.Test;
import junit.textui.TestRunner;

/**
 * Tests bad forms of authenticating.
 */
public class BadAuthenticationTestAlreadyAuthenticated extends AbstractCommunicationSupport {

    public BadAuthenticationTestAlreadyAuthenticated(String s) {
        super(s);
    }

    public static Test suite() {
        return buildTestSuite(BadAuthenticationTestAlreadyAuthenticated.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public void testAlreadyAuthenticated() {
        String privateKey = doAuthenticate();
        Map<String, String> args = new HashMap<String, String>();
        args.put(Parameters.PRIVATE, privateKey);
        getCode().sendLocalMsg(LWSDispatcherSupport.Commands.AUTHENTICATE,
                args,
                errorHandlerAny());
    } 

}
