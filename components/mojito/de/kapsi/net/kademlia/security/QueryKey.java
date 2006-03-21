package de.kapsi.net.kademlia.security;

import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Arrays;

/**
 * This is just a dummy QueryKey implementation. Once we're 
 * done with the raw implementation we can start using stuff 
 * from LimeWire (QueryKey, VendorMessages etc).
 * 
 * @deprecated
 */
public class QueryKey {
    
    private byte[] queryKey;
    
    private QueryKey(byte[] queryKey) {
        this.queryKey = queryKey;
    }
    
    public byte[] getBytes() {
        return queryKey;
    }
    
    public boolean isFor(SocketAddress address) {
        return equals(getQueryKey(address));
    }
    
    public boolean isFor(InetAddress ip, int port) {
        return equals(getQueryKey(ip, port));
    }

    public boolean equals(Object o) {
        if (!(o instanceof QueryKey)) {
            return false;
        }
        
        return Arrays.equals(queryKey, ((QueryKey)o).queryKey);
    }
    
    public static QueryKey getQueryKey(byte[] queryKey) {
        return (queryKey != null ? new QueryKey(queryKey) : null);
    }
    
    public static QueryKey getQueryKey(SocketAddress address) {
        return new QueryKey(new byte[]{1,2,3,4});
    }
    
    public static QueryKey getQueryKey(InetAddress ip, int port) {
        return new QueryKey(new byte[]{1,2,3,4});
    }
}
