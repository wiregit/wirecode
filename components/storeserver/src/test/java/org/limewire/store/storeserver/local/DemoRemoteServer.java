package org.limewire.store.storeserver.local;

import java.util.HashMap;
import java.util.Map;

import org.limewire.store.storeserver.api.Server;
import org.limewire.store.storeserver.core.RemoteServer;
import org.limewire.store.storeserver.core.AbstractServer;
import org.limewire.store.storeserver.util.Numbers;


/**
 * A simple remote server.
 * 
 * @author jpalm
 */
public class DemoRemoteServer extends RemoteServer {

  public final static int PORT = 8090;

  private final LocalServerDelegate del;

  public DemoRemoteServer(final int otherPort, final boolean loud) {
    super(PORT);
    this.del = new LocalServerDelegate(this, otherPort, loud);
  }

  @Override
  public String sendMsg(final String msg, final Map<String, String> args) {
    return del.sendMsg(msg, args);
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
      return this.key.hashCode() << Numbers.SIXTEEN + this.ip.hashCode();
    }

    public String toString() {
      return "<" + key + "," + ip + ">";
    }
  }

  private final Map<Pair, String> pairs2privateKeys = new HashMap<Pair, String>();

  @Override
  protected boolean storeKey(final String publicKey, final String privateKey, final String ip) {
    final Pair p = new Pair(publicKey, ip);
    note(p + " -> " + privateKey);
    return pairs2privateKeys.put(p, privateKey) != null;
  }

  @Override
  protected String lookUpPrivateKey(final String publicKey, final String ip) {
    final Pair p = new Pair(publicKey, ip);
    final String privateKey = pairs2privateKeys.get(p);
    note("found private key: " + privateKey + " for " + p);
    return privateKey == null ? Server.ErrorCodes.INVALID_PUBLIC_KEY_OR_IP : privateKey;
  }
  
  public static void main(String[] args) {
      AbstractServer.start(new DemoRemoteServer(LocalLocalServer.PORT, true));
  }

}
