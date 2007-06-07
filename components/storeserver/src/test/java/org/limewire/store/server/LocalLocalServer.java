package org.limewire.store.server;

import java.rmi.server.RemoteServer;
import java.util.Map;


/**
 * Sample local server that connects to a {@link RemoteServer} running on <tt>localhost:8090</tt>.
 */
public class LocalLocalServer extends ServerImpl {

  public final static int PORT = 8081;

  public LocalLocalServer(final String host, final int otherPort, final DispatcherSupport.OpensSocket openner) {
    super(PORT, "Local Server");
    final LocalServerDelegate del = new LocalServerDelegate(getDispatcher(), host, otherPort, openner);
    setDispatcher(new StoreServerDispatcher(new SendsMessagesToServer() {
        public String sendMsgToRemoteServer(String msg, Map<String, String> args) {
            return del.sendMsg(msg, args);
        }
    }));
    
    
  }
  
  public LocalLocalServer(final String host, final int otherPort) {
      this(host, otherPort, new URLSocketOpenner());
  }


}
