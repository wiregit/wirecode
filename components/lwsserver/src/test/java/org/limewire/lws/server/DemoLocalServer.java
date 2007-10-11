package org.limewire.lws.server;

import java.rmi.server.RemoteServer;
import java.util.Map;

import org.limewire.lws.server.AbstractDispatchee;
import org.limewire.lws.server.DispatcherSupport;
import org.limewire.lws.server.SendsMessagesToServer;
import org.limewire.lws.server.LWSServerDispatcher;
import org.limewire.lws.server.URLSocketOpenner;

/**
 * Sample local server that connects to a {@link RemoteServer} running on <tt>localhost:8090</tt>.
 */
public class DemoLocalServer extends ServerImpl {

  final LocalServerDelegate del;
  public final static int PORT = 45100;

  public DemoLocalServer(final String host, final int otherPort, final DispatcherSupport.OpensSocket openner) {
    super(PORT, "Local Server");
    LWSServerDispatcher ssd = new LWSServerDispatcher(new SendsMessagesToServer() {
        public String sendMsgToRemoteServer(String msg, Map<String, String> args) {
            return del.sendMsgToRemoteServer(msg, args);
        }
    });
    setDispatcher(ssd);
    
    ssd.setDispatchee(new AbstractDispatchee() {

        @Override
        protected void connectionChanged(boolean isConnected) {
            System.out.println("connectionChanged(" + isConnected + ")");
        }

        public String dispatch(String cmd, Map<String, String> args) {
            System.out.println(cmd + ":" + args);
            return null;
        }
        
    });
    this.del = new LocalServerDelegate(host, otherPort, openner);
    
    
  }
  
  public DemoLocalServer(final String host, final int otherPort) {
      this(host, otherPort, new URLSocketOpenner());
  }


}
