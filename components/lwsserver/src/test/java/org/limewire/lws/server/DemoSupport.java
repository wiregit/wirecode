package org.limewire.lws.server;

import java.util.Map;

import org.limewire.lws.server.AbstractRemoteServer;
import org.limewire.lws.server.AbstractServer;
import org.limewire.lws.server.DemoLocalServer;
import org.limewire.lws.server.ServerImpl;
import org.limewire.lws.server.Util;
import org.limewire.util.BaseTestCase;

/**
 * Skeleton code for test cases using the servers.
 */
public abstract class DemoSupport extends BaseTestCase {
    
  public DemoSupport(String s) { super(s); }

  public final static int LOCAL_PORT  = DemoLocalServer.PORT;
  public final static int REMOTE_PORT = DemoRemoteServer.PORT;

  private DemoLocalServer localServer;
  private DemoRemoteServer remoteServer;
  private FakeCode code;
  
  private Thread localThread;
  private Thread remoteThread;

  protected final ServerImpl getLocalServer() {
    return this.localServer;
  }

  protected final AbstractRemoteServer getRemoteServer() {
    return this.remoteServer;
  }

  protected final FakeCode getCode() {
    return this.code;
  }

  protected static final Map<String, String> NULLARGS = Util.EMPTY_MAP_OF_STRING_X_STRING;

  /** Override with functionality <b>after</b> {@link #setUp()}. */
  protected void beforeSetup() {
  }

  /** Override with functionality <b>after</b> {@link #setUp()}. */
  protected void afterSetup() {
  }

  /** Override with functionality <b>after</b> {@link #tearDown()}. */
  protected void beforeTearDown() {
  }

  /** Override with functionality <b>after</b> {@link #tearDown()}. */
  protected void afterTearDown() {
  }

  /**
   * Returns a handler that ensures that the response returned is <tt>code</tt>.
   * 
   * @param want
   *          expected code
   * @return a handler that ensures that the response returned is <tt>code</tt>
   */
  protected final FakeCode.Handler errorHandler(final String want) {
    return new FakeCode.Handler() {
      public void handle(final String res) {
        //
        // We first have to remove the parens and single quotes
        // from around the message, because we always pass a
        // callback back to javascript
        //
        final String have = Util.unwrapError(Util.removeCallback(res));
        assertEquals(want, have);
      }
    };
  }
  
  protected final Thread getLocalThread() {
      return localThread;
  }
  
  protected final Thread getRemoteThread() {
      return remoteThread;
  }

  @Override
  protected final void setUp() throws Exception {

    beforeSetup();

    localServer = new DemoLocalServer("localhost", REMOTE_PORT);
    remoteServer = new DemoRemoteServer(LOCAL_PORT);
    localThread = AbstractServer.start(localServer);
    remoteThread = AbstractServer.start(remoteServer);
    code = new FakeCode(localServer, remoteServer);

    afterSetup();
  }

  @Override
  protected final void tearDown() throws Exception {

    beforeTearDown();

    stop(localServer);
    stop(remoteServer);
    
    localThread = null;
    remoteThread = null;

    afterTearDown();
  }

  private void stop(final AbstractServer t) {
      System.out.println("stopping " + t);
    if (t != null) {
      t.shutDown();
    }
  }
}
