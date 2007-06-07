package org.limewire.store.server;

import java.rmi.server.RemoteServer;
import java.util.Map;


/**
 * Sample local server that connects to a {@link RemoteServer} running on <tt>localhost:8090</tt>.
 */
public class LocalLocalServer extends ServerImpl {

  final LocalServerDelegate del;
  public final static int PORT = 8081;

  public LocalLocalServer(final String host, final int otherPort, final DispatcherSupport.OpensSocket openner) {
    super(PORT, "Local Server");
    setDispatcher(new StoreServerDispatcher(new SendsMessagesToServer() {
        public String sendMsgToRemoteServer(String msg, Map<String, String> args) {
            return del.sendMsgToRemoteServer(msg, args);
        }
    }));
    this.del = new LocalServerDelegate(host, otherPort, openner);
    
    
  }
  
  public LocalLocalServer(final String host, final int otherPort) {
      this(host, otherPort, new URLSocketOpenner());
  }


}
