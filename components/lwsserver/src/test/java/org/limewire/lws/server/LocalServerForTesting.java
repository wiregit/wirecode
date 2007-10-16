package org.limewire.lws.server;

import java.rmi.server.RemoteServer;
import java.util.Map;

import org.limewire.lws.server.AbstractDispatchee;
import org.limewire.lws.server.DispatcherSupport;
import org.limewire.lws.server.SenderOfMessagesToServer;
import org.limewire.lws.server.LWSServerDispatcher;

/**
 * Sample local server that connects to a {@link RemoteServer} running on
 * <tt>localhost:8090</tt>.
 */
public class LocalServerForTesting extends ServerImpl {

  final LocalServerDelegate del;
  public final static int PORT = 45100;

  public LocalServerForTesting(final String host, final int otherPort, final DispatcherSupport.OpensSocket openner) {
    super(PORT, "Local Server");
    LWSServerDispatcher ssd = new LWSServerDispatcher(new SenderOfMessagesToServer() {
        public String semdMessageToServer(String msg, Map<String, String> args) {
            return del.semdMessageToServer(msg, args);
        }
    });
    setDispatcher(ssd);
    
    ssd.setDispatchee(new AbstractDispatchee() {

        @Override
        protected void connectionChanged(boolean isConnected) {
        }

        public String dispatch(String cmd, Map<String, String> args) {
            return null;
        }
        
    });
    this.del = new LocalServerDelegate(host, otherPort, openner);
    
    
  }
  
  public LocalServerForTesting(final String host, final int otherPort) {
      this(host, otherPort, new URLSocketOpenner());
  }


}
