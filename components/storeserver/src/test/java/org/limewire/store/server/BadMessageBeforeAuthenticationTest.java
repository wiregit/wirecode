package org.limewire.store.server;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import junit.textui.TestRunner;

/**
 * Tests that we handle a bad message correctly.
 */
public class BadMessageBeforeAuthenticationTest extends AbstractCommunicationTest {

    public BadMessageBeforeAuthenticationTest(String s) {
        super(s);
    }

    public static Test suite() {
        return buildTestSuite(BadMessageBeforeAuthenticationTest.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public void testBadMessageBeforeAuthentication() {
        final Map<String, String> args = new HashMap<String, String>();
        args.put(DispatcherSupport.Parameters.MSG, "badMsg");
        getCode().sendLocalMsg("Msg", args,
                errorHandler(DispatcherSupport.ErrorCodes.UNITIALIZED_PRIVATE_KEY));
    }
}
