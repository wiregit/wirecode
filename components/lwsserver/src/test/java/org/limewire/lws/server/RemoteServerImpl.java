package org.limewire.lws.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.limewire.lws.server.AbstractServer;
import org.limewire.lws.server.DispatcherSupport;
import org.limewire.lws.server.LocalServerDelegate;

/**
 * A simple remote {@link RemoteServer}.
 */
public class RemoteServerImpl extends AbstractServer implements RemoteServer {

    /** The port on which we'll connect this server. */
    public final static int PORT = 8091;
    
    private final LocalServerDelegate del;

    public RemoteServerImpl(int otherPort) {
        super(PORT, null);
        setDispatcher(new DispatcherImpl());
        this.del = new LocalServerDelegate("localhost", otherPort, new URLSocketOpenner());
    }

    public String toString() {
        return "Remote Server";
    }
    
    public final class DispatcherImpl extends DispatcherSupport {

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

            public void handle(final Map<String, String> args, StringCallback cb) {
                String publicKey = args.get(DispatcherSupport.Parameters.PUBLIC);
                if (publicKey == null) {
                    cb.process(report(DispatcherSupport.ErrorCodes.MISSING_PUBLIC_KEY_PARAMETER));
                    return;
                }
                String privateKey = args.get(DispatcherSupport.Parameters.PRIVATE);
                if (privateKey == null) {
                    cb.process(report(DispatcherSupport.ErrorCodes.MISSING_PRIVATE_KEY_PARAMETER));
                    return;
                }
                String ip = args.get(DispatcherSupport.Parameters.IP);
                if (ip == null) {
                    cb.process(report(DispatcherSupport.ErrorCodes.MISSING_IP_PARAMETER));
                    return;
                }                
                if (LWSServerUtil.isEmpty(publicKey)) {
                    cb.process(DispatcherSupport.ErrorCodes.INVALID_PUBLIC_KEY);
                    return;
                }
                note("StoreKey: " + publicKey + " -> " + ip + "," + privateKey);
                storeKeys(publicKey, privateKey, ip);
                cb.process(DispatcherSupport.Responses.OK);
            }
        }

        /**
         * Sent from local code to retrieve the private key with parameter
         * {@link Parameters#PUBLIC}.
         * {@link Parameters#IP}
         */
        class GiveKey extends HandlerWithCallback {

            public void handleRest(final Map<String, String> args, StringCallback cb) {
                String publicKey = args.get(DispatcherSupport.Parameters.PUBLIC);
                if (publicKey == null) {
                    cb.process(report(DispatcherSupport.ErrorCodes.MISSING_PUBLIC_KEY_PARAMETER));
                    return;
                }
                String ip = args.get(DispatcherSupport.Parameters.IP);
                if (ip == null) {
                    cb.process(report(DispatcherSupport.ErrorCodes.MISSING_IP_PARAMETER));
                    return;
                }                
                String privateKey = lookupPrivateKey(publicKey, ip);
                note("GiveKey: " + publicKey + " -> " + ip + "," + privateKey);
                cb.process(privateKey);
            }
        }

        public void sendMessageToServer(String msg, Map<String,String> args, StringCallback cb) throws IOException {
            del.sendMessageToServer(msg, args, cb);
        }
    }    

    // ---------------------------------------------------------------
    // RemoteServer
    // ---------------------------------------------------------------
    
    /** A pair. */
    private static class Pair {

        final String key;
        final String ip;

        Pair(final String key, final String ip) {
            this.key = key;
            this.ip = ip;
        }

        public boolean equals(final Object o) {
            if (!(o instanceof Pair))
                return false;
            final Pair that = (Pair) o;
            return this.key.equals(that.key) && this.ip.equals(that.ip);
        }

        public int hashCode() {
            return this.key.hashCode() << 16 + this.ip.hashCode();
        }

        public String toString() {
            return "<" + key + "," + ip + ">";
        }
    }

    private final Map<Pair, String> pairs2privateKeys = new HashMap<Pair, String>();

    public boolean storeKeys(String publicKey, String privateKey, String ip) {
        Pair p = new Pair(publicKey, ip);
        return pairs2privateKeys.put(new Pair(publicKey, ip), privateKey) != null;
    }

    public String lookupPrivateKey(String publicKey, String ip) {
        Pair p = new Pair(publicKey, ip);
        String privateKey = pairs2privateKeys.get(p);
        String res = privateKey == null 
                ? DispatcherSupport.ErrorCodes.INVALID_PUBLIC_KEY_OR_IP
                : privateKey;
        return res;
    }
}
