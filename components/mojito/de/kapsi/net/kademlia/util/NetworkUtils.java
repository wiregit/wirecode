package de.kapsi.net.kademlia.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;

public final class NetworkUtils {
    
    private NetworkUtils() {}
    
    /**
     * Returns true if the SocketAddress is any of our local machine addresses
     */
    public static boolean isLocalAddress(SocketAddress addr) throws IOException {
        return isLocalAddress(((InetSocketAddress)addr).getAddress());
    }
    
    /**
     * Returns true if the InetAddress is any of our local machine addresses
     */
    public static boolean isLocalAddress(InetAddress addr) throws IOException {
        return NetworkInterface.getByInetAddress(addr) != null;
    }
}
