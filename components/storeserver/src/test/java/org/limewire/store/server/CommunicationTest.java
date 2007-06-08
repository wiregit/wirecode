package org.limewire.store.server;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import junit.textui.TestRunner;

import org.limewire.store.server.Util;

/**
 * Tests basic communication.
 * 
 * @author jpalm
 */
public class CommunicationTest extends DemoSupport {
    
  public CommunicationTest(String s) { super(s); }
  
  public static Test suite() {
      return buildTestSuite(CommunicationTest.class);
  }
  
  public static void main(String[] args) {
      TestRunner.run(suite());
  }

  public void testEchoMsg() {
    final String pk = getPrivateKey();
    assertEquals(DispatcherSupport.Responses.OK, doAuthenticate(pk));
    final String want = "test";
    final String have = doEcho(pk, want);
    assertEquals(want, have);
  }

  public void testAuthenticate() {
    final String res = doAuthenticate();
    assertEquals(DispatcherSupport.Responses.OK, res);
  }

  public void testGiveKey() {
    final String privateKey = getPrivateKey();
    assertTrue(privateKey, Util.isValidPrivateKey(privateKey));
  }

  public void testStartCom() {
    final String publicKey = getPublicKey();
    assertTrue(publicKey, Util.isValidPublicKey(publicKey));
  }

  public void testBadMessageBeforeAuthentication() {
    final Map<String, String> args = new HashMap<String, String>();
    args.put(DispatcherSupport.Parameters.MSG, "badMsg");
    getCode().sendLocalMsg("Msg", args, errorHandler(DispatcherSupport.ErrorCodes.UNKNOWN_COMMAND));
  }

  public void testGoodMessageBeforeAuthentication() {
    getCode().sendLocalMsg(DispatcherSupport.Commands.AUTHENTICATE, NULLARGS, errorHandler(DispatcherSupport.ErrorCodes.UNITIALIZED_PRIVATE_KEY));
  }

  // -------------------------------------------------------
  // Convenience
  // -------------------------------------------------------

  private String doEcho(final String privateKey, final String msg) {
    final Map<String, String> args = new HashMap<String, String>();
    args.put(DispatcherSupport.Parameters.PRIVATE, privateKey);
    args.put(DispatcherSupport.Parameters.MSG, msg);
    return sendLocalMsg(DispatcherSupport.Commands.ECHO, args);
  }

  private String doAuthenticate() {
    return doAuthenticate(getPrivateKey());
  }

  private String doAuthenticate(final String privateKey) {
    final Map<String, String> args = new HashMap<String, String>();
    args.put(DispatcherSupport.Parameters.PRIVATE, privateKey);
    return sendLocalMsg(DispatcherSupport.Commands.AUTHENTICATE, args);
  }

  private String getPrivateKey() {
    final String publicKey = getPublicKey();
    final Map<String, String> args = new HashMap<String, String>();
    args.put(DispatcherSupport.Parameters.PUBLIC, publicKey);
    return sendRemoteMsg(DispatcherSupport.Commands.GIVE_KEY, args);
  }

  private String getPublicKey() {
    final Map<String, String> args = new HashMap<String, String>();
    return sendLocalMsg(DispatcherSupport.Commands.START_COM, args);
  }

  // -----------------------------------------------------------
  // Private
  // -----------------------------------------------------------

  private String sendLocalMsg(final String cmd, final Map<String, String> args) {
    args.put(DispatcherSupport.Parameters.CALLBACK, "dummy");
    final String[] result = new String[1];
    final Object lock = new Object();
    final boolean[] shouldWait = new boolean[] {true};
    getCode().sendLocalMsg(cmd, args, new FakeCode.Handler() {
      public void handle(final String res) {
        final String msg = Util.removeCallback(res);
        result[0] = msg;
        synchronized (lock) {
          lock.notify();
          shouldWait[0] = false;
        }
      }
    });
    synchronized (lock) {
      try {
        if (shouldWait[0])
          lock.wait();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    return result[0];
  }

  private String sendRemoteMsg(final String cmd, final Map<String, String> args) {
    args.put(DispatcherSupport.Parameters.CALLBACK, "dummy");
    final String[] result = new String[1];
    final Object lock = new Object();
    final boolean[] shouldWait = new boolean[] {true};
    getCode().sendRemoteMsg(cmd, args, new FakeCode.Handler() {
      public void handle(final String res) {
        final String msg = Util.removeCallback(res);
        result[0] = msg;
        synchronized (lock) {
          lock.notify();
          shouldWait[0] = false;
        }
      }
    });
    synchronized (lock) {
      try {
        if (shouldWait[0])
          lock.wait();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    return result[0];
  }
}
