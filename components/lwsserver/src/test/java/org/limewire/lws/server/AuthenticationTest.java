package org.limewire.lws.server;

import java.util.HashMap;
import java.util.Map;

import org.limewire.lws.server.LWSDispatcherSupport;

import com.limegroup.gnutella.lws.server.LWSUtil;

import junit.framework.Test;
import junit.textui.TestRunner;

/**
 * Tests for an OK from authentication.
 */
public class AuthenticationTest extends AbstractCommunicationSupport {

    public AuthenticationTest(String s) {
        super(s);
    }

    public static Test suite() {
        return buildTestSuite(AuthenticationTest.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public void testSendAuthentication() {
        Map<String,String> args = new HashMap<String,String>();
        args.put(LWSDispatcherSupport.Parameters.PRIVATE, getPrivateKey());
        args.put(LWSDispatcherSupport.Parameters.SHARED, getSharedKey());
        args.put(LWSDispatcherSupport.Parameters.CALLBACK, "dummy");
        getCode().sendLocalMsg(LWSDispatcherSupport.Commands.AUTHENTICATE,
                               args,
                               new FakeJavascriptCodeInTheWebpage.Handler() {
                                public void handle(String res) {
                                    assertTrue("OK != " + res, LWSDispatcherSupport.Responses.OK.equalsIgnoreCase(LWSServerUtil.removeCallback(res)));
                                }
            
        });
    }
  
}
