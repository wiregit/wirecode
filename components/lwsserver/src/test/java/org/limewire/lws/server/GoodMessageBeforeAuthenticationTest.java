package org.limewire.lws.server;

import junit.framework.Test;
import junit.textui.TestRunner;

/**
 * Tests the proper way to handle a bad message.
 */
public class GoodMessageBeforeAuthenticationTest extends AbstractCommunicationTest {

    public GoodMessageBeforeAuthenticationTest(String s) {
        super(s);
    }

    public static Test suite() {
        return buildTestSuite(GoodMessageBeforeAuthenticationTest.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public void testGoodMessageBeforeAuthentication() {
        getCode().sendLocalMsg(LWSDispatcherSupport.Commands.AUTHENTICATE,
                               DUMMY_CALLBACK_ARGS,
                               errorHandlerAny());
    }


}
