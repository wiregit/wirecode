package org.limewire.store.storeserver.local;

import java.rmi.server.RemoteServer;
import java.util.Map;

import org.limewire.store.storeserver.api.Dispatchee;
import org.limewire.store.storeserver.api.Server;
import org.limewire.store.storeserver.api.URLSocketOpenner;
import org.limewire.store.storeserver.core.ServerImpl;

/**
 * Sample local server that connects to a {@link RemoteServer} running on <tt>localhost:8090</tt>.
 */
public class LocalLocalServer extends ServerImpl {

  public final static int PORT = 8081;

  private final LocalServerDelegate del;

  public LocalLocalServer(final String host, final int otherPort, final Server.OpensSocket openner) {
    super(PORT, "Local Server");
    setDispatcher(new DispatcherImpl() {
        @Override
        protected void noteNewState(State newState) {
            note("new state: {0}", newState);
        }
        @Override
        public String sendMsg(String msg, Map<String, String> args) {
            return del.sendMsg(msg, args);
        }
    });   
    this.del = new LocalServerDelegate(getDispatcher(), host, otherPort, openner);
    
  }
  
  public LocalLocalServer(final String host, final int otherPort) {
      this(host, otherPort, new URLSocketOpenner());
  }


}
