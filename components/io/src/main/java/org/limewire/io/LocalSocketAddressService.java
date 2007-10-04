package org.limewire.io;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.google.inject.Inject;

/**
 * Returns local IP Address information via a {@link LocalSocketAddressProvider}.
 * <code>LocalSocketAddresService</code> returns the local address, port and if
 * the local address is private. 
 * <p>
 * The default service provider returns an address of 
 * <code>Inet.getLocalHost().getAddress()</code>, a port of -1, and the local 
 * address is considered private.
 */
public class LocalSocketAddressService {
    
    private static volatile LocalSocketAddressProvider activeService = new DefaultProvider();    
    
    /** Sets the the shared SocketAddressProvider. */
    @Inject public static void setSocketAddressProvider(LocalSocketAddressProvider provider) {
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
    
    /** Utility method for calling getSharedProvider().isTLSCapable() */
    public static boolean isTLSCapable() {
        return activeService.isTLSCapable();
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
        
        public boolean isTLSCapable() {
            return false;
        }
        
    }

}
