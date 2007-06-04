package org.limewire.store.storeserver.local;

import java.rmi.server.RemoteServer;
import java.util.Map;

import org.limewire.store.storeserver.core.ServerImpl;

/**
 * Sample local server that connects to a {@link RemoteServer} running on <tt>localhost:8090</tt>.
 */
public class LocalLocalServer extends ServerImpl {

  public final static int PORT = 8080;

  private final LocalServerDelegate del;

  public LocalLocalServer(final int otherPort, final boolean loud) {
    super(PORT, "Local Server");
    this.del = new LocalServerDelegate(this, otherPort, loud);
  }

  public String toString() {
    return "Local Server";
  }

  @Override
  public String sendMsg(final String msg, final Map<String, String> args) {
    return del.sendMsg(msg, args);
  }

  @Override
  protected void noteNewState(State newState) {
      note("new state: {0}", newState);
  }

}
