package org.limewire.store.server;

import java.util.Map;


/**
 * Reference implementation for a remote server. This would actually be done as
 * a JSP of PHP or something.
 */
public abstract class AbstractRemoteServer extends AbstractServer {

    public AbstractRemoteServer(final int port, final String name, final DispatcherImpl dis) {
        super(port, name, dis);
    }

    public AbstractRemoteServer(final int port, final DispatcherImpl dis) {
        this(port, "Remote Server", dis);
    }

    public abstract class DispatcherImpl extends DispatcherSupport {

        @Override
        protected final Handler[] createHandlers() {
            return new Handler[] { new StoreKey(), new GiveKey(), };

        }

        // ------------------------------------------------------------
        // Handlers
        // ------------------------------------------------------------

        /**
         * Sent from local server with parameters {@link Parameters#PUBLIC} and
         * {@link Parameters#PRIVATE} to store the in coming ip and these
         * parameters for when the local code asks for it.
         */
        class StoreKey extends AbstractHandler {

            public String handle(final Map<String, String> args) {
                String publicKey = Util.getArg(args, DispatcherSupport.Parameters.PUBLIC);
                if (publicKey == null) {
                    return report(DispatcherSupport.ErrorCodes.MISSING_PUBLIC_KEY_PARAMETER);
                }
                String privateKey = Util.getArg(args, DispatcherSupport.Parameters.PRIVATE);
                if (privateKey == null) {
                    return report(DispatcherSupport.ErrorCodes.MISSING_PRIVATE_KEY_PARAMETER);
                }
                String ip = Util.getArg(args, DispatcherSupport.Parameters.IP);
                if (ip == null) {
                    return report(DispatcherSupport.ErrorCodes.MISSING_IP_PARAMETER);
                }                
                if (Util.isEmpty(publicKey)) {
                    return DispatcherSupport.ErrorCodes.INVALID_PUBLIC_KEY;
                }
                note("StoreKey: " + publicKey + " -> " + ip + "," + privateKey);
                storeKey(publicKey, privateKey, ip);
                return DispatcherSupport.Responses.OK;
            }
        }

        /**
         * Sent from local code to retrieve the private key with parameter
         * {@link Parameters#PUBLIC}.
         * {@link Parameters#IP}
         */
        class GiveKey extends HandlerWithCallback {

            public String handleRest(final Map<String, String> args) {
                String publicKey = Util.getArg(args, DispatcherSupport.Parameters.PUBLIC);
                if (publicKey == null) {
                    return report(DispatcherSupport.ErrorCodes.MISSING_PUBLIC_KEY_PARAMETER);
                }
                String ip = Util.getArg(args, DispatcherSupport.Parameters.IP);
                if (ip == null) {
                    return report(DispatcherSupport.ErrorCodes.MISSING_IP_PARAMETER);
                }                
                String privateKey = lookUpPrivateKey(publicKey, ip);
                note("GiveKey: " + publicKey + " -> " + ip + "," + privateKey);
                return privateKey;
            }
        }
    }

    /**
     * Override this to store these values as a tuple, so that when the local
     * code gives you the public key and IP, you give back the private key.
     * 
     * @param publicKey public key generated from local server
     * @param privateKey private key generated from local server
     * @param ip ip address extracted from the local server
     * @return <tt>true</tt> on success
     */
    public abstract boolean storeKey(String publicKey, String privateKey,
            String ip);

    /**
     * Implement this to look up and return a private key for the values given
     * previously from the local server, if not return <tt>null</tt>.
     * 
     * @param publicKey some public key
     * @param ip ip address extracted from the incoming request
     * @return private key found for (<tt>publicKey</tt>,<tt>ip</tt>) or
     *         <tt>null</tt> if not found.
     */
    public abstract String lookUpPrivateKey(String publicKey, String ip);

}
