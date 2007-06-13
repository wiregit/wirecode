/**
 * 
 */
package org.limewire.store.server;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class DefaultRemoteServer implements RemoteServer {

    private final static Log LOG = LogFactory.getLog(RemoteServer.class);

    /** A pair. */
    private static class Pair {

        private final String key;

        private final String ip;

        Pair(final String key, final String ip) {
            this.key = key;
            this.ip = ip;
        }

        public String getKey() {
            return key;
        }

        public String getIP() {
            return ip;
        }

        public boolean equals(final Object o) {
            if (!(o instanceof DefaultRemoteServer.Pair))
                return false;
            final DefaultRemoteServer.Pair that = (DefaultRemoteServer.Pair) o;
            return this.key.equals(that.getKey())
                    && this.ip.equals(that.getIP());
        }

        public int hashCode() {
            return this.key.hashCode() << 16 + this.ip.hashCode();
        }

        public String toString() {
            return "<" + key + "," + ip + ">";
        }
    }

    private final Map<DefaultRemoteServer.Pair, String> pairs2privateKeys = new HashMap<DefaultRemoteServer.Pair, String>();

    public final boolean storeKey(String publicKey, String privateKey, String ip) {
        final DefaultRemoteServer.Pair p = new Pair(publicKey, ip);
        note("storeKey: " + publicKey + " -> " + p);
        return pairs2privateKeys.put(p, privateKey) != null;
    }

    public final String lookUpPrivateKey(String publicKey, String ip) {
        final DefaultRemoteServer.Pair p = new Pair(publicKey, ip);
        final String privateKey = pairs2privateKeys.get(p);
        final String res = privateKey == null ? DispatcherSupport.ErrorCodes.INVALID_PUBLIC_KEY_OR_IP
                : privateKey;
        note("lookUpKey: " + p + " -> " + privateKey);
        return res;
    }
    
    private void note(String s) {
        LOG.debug(s);
    }
}