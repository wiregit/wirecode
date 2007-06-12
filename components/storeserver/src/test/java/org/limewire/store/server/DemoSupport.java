package org.limewire.store.server;

import java.util.Map;

import org.limewire.store.server.AbstractServer;
import org.limewire.store.server.DemoLocalServer;
import org.limewire.store.server.AbstractRemoteServer;
import org.limewire.store.server.ServerImpl;
import org.limewire.store.server.Util;
import org.limewire.util.BaseTestCase;

/**
 * Skeleton code for test cases using the servers.
 * 
 * @author jpalm
 */
public abstract class DemoSupport extends BaseTestCase {
    
  public DemoSupport(String s) { super(s); }

  public final static int LOCAL_PORT  = 8080;
  public final static int REMOTE_PORT = 8090;

  private DemoLocalServer localServer;
  private DemoRemoteServer remoteServer;
  private FakeCode code;

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

  @Override
  protected final void setUp() throws Exception {

    beforeSetup();

    localServer = new DemoLocalServer("localhost", REMOTE_PORT);
    remoteServer = new DemoRemoteServer(LOCAL_PORT);
    AbstractServer.start(localServer);
    AbstractServer.start(remoteServer);
    code = new FakeCode(localServer, remoteServer);

    afterSetup();
  }

  @Override
  protected final void tearDown() throws Exception {

    beforeTearDown();

    stop(localServer);
    stop(remoteServer);

    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    afterTearDown();
  }

  private void stop(final AbstractServer t) {
    if (t != null) {
      t.shutDown();
    }
  }
}
