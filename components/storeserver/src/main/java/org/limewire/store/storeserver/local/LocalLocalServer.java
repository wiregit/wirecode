package org.limewire.store.storeserver.local;

import java.rmi.server.RemoteServer;
import java.util.Map;

import org.limewire.store.storeserver.api.Server;
import org.limewire.store.storeserver.api.URLSocketOpenner;
import org.limewire.store.storeserver.core.ServerImpl;

/**
 * Sample local server that connects to a {@link RemoteServer} running on <tt>localhost:8090</tt>.
 */
public class LocalLocalServer extends ServerImpl {

  public final static int PORT = 8080;

  private final LocalServerDelegate del;

  public LocalLocalServer(final String host, final int otherPort, final Server.OpensSocket openner) {
    super(PORT, "Local Server");
    this.del = new LocalServerDelegate(this, host, otherPort, openner);
  }
  
  public LocalLocalServer(final String host, final int otherPort) {
      this(host, otherPort, new URLSocketOpenner());
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
