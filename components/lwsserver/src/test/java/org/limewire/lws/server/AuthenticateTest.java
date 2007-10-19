package org.limewire.lws.server;

import org.limewire.lws.server.LWSDispatcherSupport;

import junit.framework.Test;
import junit.textui.TestRunner;

/**
 * Tests basic authentication.
 */
public class AuthenticateTest extends AbstractCommunicationTest {
    
  public AuthenticateTest(String s) { super(s); }
  
  public static Test suite() {
      return buildTestSuite(AuthenticateTest.class);
  }
  
  public static void main(String[] args) {
      TestRunner.run(suite());
  }

  public void testAuthenticate() {
    final String res = doAuthenticate();
    assertEquals(LWSDispatcherSupport.Responses.OK, res);
  }

}
