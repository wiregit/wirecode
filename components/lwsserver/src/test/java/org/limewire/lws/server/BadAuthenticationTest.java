package org.limewire.lws.server;

import org.limewire.lws.server.LWSDispatcherSupport;

import junit.framework.Test;
import junit.textui.TestRunner;

/**
 * Tests bad forms of authenticating.
 */
public class BadAuthenticationTest extends AbstractCommunicationSupport {

    public BadAuthenticationTest(String s) {
        super(s);
    }

    public static Test suite() {
        return buildTestSuite(BadAuthenticationTest.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public void testSendAuthenticationWithoutPrivateKey() {
        getCode().sendLocalMsg(LWSDispatcherSupport.Commands.AUTHENTICATE,
                               DUMMY_CALLBACK_ARGS,
                               errorHandlerAny());
    }

}
