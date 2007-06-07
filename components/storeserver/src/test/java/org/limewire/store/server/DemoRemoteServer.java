package org.limewire.store.server;

import java.util.HashMap;
import java.util.Map;

import org.limewire.store.server.AbstractServer;
import org.limewire.store.server.DispatcherSupport;
import org.limewire.store.server.LocalLocalServer;
import org.limewire.store.server.LocalServerDelegate;
import org.limewire.store.server.RemoteServer;
import org.limewire.store.server.URLSocketOpenner;


/**
 * A simple remote server.
 * 
 * @author jpalm
 */
public class DemoRemoteServer extends RemoteServer {

  public final static int PORT = 8090;

  private final LocalServerDelegate del;

  public DemoRemoteServer(final int otherPort, final DispatcherSupport.OpensSocket openner) {
    super(PORT, null);
    setDispatcher(new DispatcherImpl() {
        @Override
        public String sendMsgToRemoteServer(String msg, Map<String, String> args) {
            return del.sendMsg(msg, args);
        }
    });
    this.del = new LocalServerDelegate(getDispatcher(), "localhost", otherPort, openner);
  }
  
  public DemoRemoteServer(final int otherPort) {
      this(otherPort, new URLSocketOpenner());
  }

  public String toString() {
    return "Remote Server";
  }

  // ---------------------------------------------------------------
  // RemoteServer
  // ---------------------------------------------------------------

  /** A pair. */
  private static class Pair {
    
    private final String key;
    private final String ip;

    Pair(final String key, final String ip) {
      this.key = key;
      this.ip = ip;
    }
    
    public String getKey() { return key; }
    public String getIP() { return ip; }

    public boolean equals(final Object o) {
      if (!(o instanceof Pair))
        return false;
      final Pair that = (Pair) o;
      return this.key.equals(that.getKey()) && this.ip.equals(that.getIP());
    }

    public int hashCode() {
      return this.key.hashCode() << 16 + this.ip.hashCode();
    }

    public String toString() {
      return "<" + key + "," + ip + ">";
    }
  }

  private final Map<Pair, String> pairs2privateKeys = new HashMap<Pair, String>();

  @Override
  protected boolean storeKey(final String publicKey, final String privateKey, final String ip) {
    final Pair p = new Pair(publicKey, ip);
    getDispatcher().note(p + " -> " + privateKey);
    return pairs2privateKeys.put(p, privateKey) != null;
  }

  @Override
  protected String lookUpPrivateKey(final String publicKey, final String ip) {
    final Pair p = new Pair(publicKey, ip);
    final String privateKey = pairs2privateKeys.get(p);
    getDispatcher().note("pairs: {0}", pairs2privateKeys);
    getDispatcher().note("found private key {0} for {1}", privateKey, p);
    return privateKey == null ? DispatcherSupport.ErrorCodes.INVALID_PUBLIC_KEY_OR_IP : privateKey;
  }
  
  public static void main(String[] args) {
      AbstractServer.start(new DemoRemoteServer(LocalLocalServer.PORT));
  }

}
