package org.limewire.io;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Allows setting of a service provider that maintains
 * the running program's address & port.
 * 
 * By default, the port is -1 (unknown), and the address
 * is InetAddress.getLocalHost, and local addresses are
 * private.
 */
public class LocalSocketAddressService {
    
    private static volatile LocalSocketAddressProvider activeService = new DefaultProvider();    
    
    /** Sets the the shared SocketAddressProvider. */
    public static void setSocketAddressProvider(LocalSocketAddressProvider provider) {
        activeService = provider;
    }
    
    /** Retrieves the currently active SocketAddressProvider. */
    public static LocalSocketAddressProvider getSharedProvider() {
        return activeService;
    }
    
    /** Utility method for calling getSharedProvider().getLocalAddress() */
    public static byte[] getLocalAddress() {
        return activeService.getLocalAddress();
    }
    
    /** Utility method for calling getSharedProvider().getLocalPort() */
    public static int getLocalPort() {
        return activeService.getLocalPort();
    }
    
    /** Utility method for calling getSharedProvider().isLocalAddressPrivate() */
    public static boolean isLocalAddressPrivate() {
        return activeService.isLocalAddressPrivate();
    }
    
    private static class DefaultProvider implements LocalSocketAddressProvider {
        public byte[] getLocalAddress() {
            try {
                return InetAddress.getLocalHost().getAddress();
            } catch (UnknownHostException e) {
                throw new IllegalStateException(e);
            }
        }

        public int getLocalPort() {
            return -1;
        }

        public boolean isLocalAddressPrivate() {
            return true;
        }
        
    }

}
