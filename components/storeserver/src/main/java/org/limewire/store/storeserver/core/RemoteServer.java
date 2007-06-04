package org.limewire.store.storeserver.core;

import java.util.Map;

import org.limewire.store.storeserver.api.Server;
import org.limewire.store.storeserver.util.Util;


/**
 * Reference implementation for a remote server.  This would
 * actually be done as a JSP of PHP or something.
 * 
 * @author jpalm
 */
public abstract class RemoteServer extends AbstractServer {

  public RemoteServer(final int port, final String name) {
    super(port, name);
  }
  
  public RemoteServer(final int port) {
    this(port, "Remote Server");
  }

  @Override
  protected final Handler[] createHandlers() {
    return new Handler[] {
        new StoreKey(), 
        new GiveKey(),
    };

  }

  /**
   * Override this to store these values as a tuple, so that when the local code
   * gives you the public key and IP, you give back the private key.
   * 
   * @param publicKey   public key generated from local server
   * @param privateKey  private key generated from local server
   * @param ip          ip address extracted from the local server
   * @return            <tt>true</tt> on success
   */
  protected abstract boolean storeKey(String publicKey, String privateKey, String ip);

  /**
   * Implement this to look up and return a private key for the values given
   * previously from the local server, if not return <tt>null</tt>.
   * 
   * @param publicKey   some public key
   * @param ip          ip address extracted from the incoming request
   * @return            private key found for (<tt>publicKey</tt>,<tt>ip</tt>) or
   *                    <tt>null</tt> if not found.
   */
  protected abstract String lookUpPrivateKey(String publicKey, String ip);

  // ------------------------------------------------------------
  // Handlers
  // ------------------------------------------------------------

  /**
   * Sent from local server with parameters {@link Parameters#PUBLIC} and {@link Parameters#PRIVATE}
   * to store the in coming ip and these parameters for when the local code asks for it.
   */
  class StoreKey extends AbstractHandler {
    
    public String handle(final Map<String, String> args, final Request req) {
        
      String publicKey = getArg(args, Server.Parameters.PUBLIC);
      if (publicKey == null) {
        return report(Server.ErrorCodes.MISSING_PUBLIC_KEY_PARAMETER);
      }
      String privateKey = getArg(args, Server.Parameters.PRIVATE);
      if (privateKey == null) {
        return report(Server.ErrorCodes.MISSING_PRIVATE_KEY_PARAMETER);
      }
      String ip = req.getIP();
      //
      // XXX: Locally, this will be null
      //
      if (ip == null)
        ip = "localhost:8080";
      if (Util.isEmpty(publicKey)) {
        return Server.ErrorCodes.INVALID_PUBLIC_KEY;
      }
      storeKey(publicKey, privateKey, ip);
      return Server.Responses.OK;
    }
  }

  /**
   * Sent from local code to retrieve the private key with parameter {@link Parameters#PUBLIC}.
   * 
   * @author jpalm
   */
  class GiveKey extends HandlerWithCallback {
    
    public String handleRest(final Map<String, String> args, final Request req) {
        
        note("hargs : " + args);
        
      String publicKey = getArg(args, Server.Parameters.PUBLIC);
      if (publicKey == null) {
        return report(Server.ErrorCodes.MISSING_PUBLIC_KEY_PARAMETER);
      }
      String ip = req.getIP();
      return lookUpPrivateKey(publicKey, ip);
    }
  }

}
