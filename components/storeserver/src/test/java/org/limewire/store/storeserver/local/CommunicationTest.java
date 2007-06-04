package org.limewire.store.storeserver.local;

import java.util.HashMap;
import java.util.Map;

import org.limewire.store.storeserver.api.Server;
import org.limewire.store.storeserver.util.Util;


/**
 * Tests basic communication.
 * 
 * @author jpalm
 */
public class CommunicationTest extends DemoSupport {
    
  public CommunicationTest() { super("CommunicationTest"); }
  
  public static void main(String[] args) throws Exception {
      CommunicationTest t = new CommunicationTest();
      t.setUp();
      t.testGiveKey();
      t.tearDown();
  }

  public void testEchoMsg() {
    final String pk = getPrivateKey();
    assertEquals(Server.Responses.OK, doAuthenticate(pk));
    final String want = "test";
    final String have = doEcho(pk, want);
    assertEquals(want, have);
  }

  public void testAuthenticate() {
    final String res = doAuthenticate();
    assertEquals(Server.Responses.OK, res);
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
    args.put(Server.Parameters.MSG, "badMsg");
    getCode().sendLocalMsg("Msg", args, errorHandler(Server.ErrorCodes.UNKNOWN_COMMAND));
  }

  public void testGoodMessageBeforeAuthentication() {
    getCode().sendLocalMsg(Server.Commands.AUTHENTICATE, NULLARGS, errorHandler(Server.ErrorCodes.UNITIALIZED_PRIVATE_KEY));
  }

  // -------------------------------------------------------
  // Convenience
  // -------------------------------------------------------

  private String doEcho(final String privateKey, final String msg) {
    final Map<String, String> args = new HashMap<String, String>();
    args.put(Server.Parameters.PRIVATE, privateKey);
    args.put(Server.Parameters.MSG, msg);
    return sendLocalMsg(Server.Commands.ECHO, args);
  }

  private String doAuthenticate() {
    return doAuthenticate(getPrivateKey());
  }

  private String doAuthenticate(final String privateKey) {
    final Map<String, String> args = new HashMap<String, String>();
    args.put(Server.Parameters.PRIVATE, privateKey);
    return sendLocalMsg(Server.Commands.AUTHENTICATE, args);
  }

  private String getPrivateKey() {
    final String publicKey = getPublicKey();
    final Map<String, String> args = new HashMap<String, String>();
    args.put(Server.Parameters.PUBLIC, publicKey);
    return sendRemoteMsg(Server.Commands.GIVE_KEY, args);
  }

  private String getPublicKey() {
    final Map<String, String> args = new HashMap<String, String>();
    return sendLocalMsg(Server.Commands.START_COM, args);
  }

  // -----------------------------------------------------------
  // Private
  // -----------------------------------------------------------

  private String sendLocalMsg(final String cmd, final Map<String, String> args) {
    args.put(Server.Parameters.CALLBACK, "dummy");
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
    args.put(Server.Parameters.CALLBACK, "dummy");
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
