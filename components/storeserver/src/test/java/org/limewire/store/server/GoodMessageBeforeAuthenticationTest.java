package org.limewire.store.server;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import junit.textui.TestRunner;

import org.limewire.store.server.Util;

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
        getCode()
                .sendLocalMsg(
                        DispatcherSupport.Commands.AUTHENTICATE,
                        NULLARGS,
                        errorHandler(DispatcherSupport.ErrorCodes.UNITIALIZED_PRIVATE_KEY));
    }


}
