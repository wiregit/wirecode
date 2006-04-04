/*
 * Lime Kademlia Distributed Hash Table (DHT)
 * Copyright (C) 2006 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
 
package de.kapsi.net.kademlia.security;

import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Arrays;

import de.kapsi.net.kademlia.util.ArrayUtils;

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
    
    public String toString() {
        return "QueryKey: " + ArrayUtils.toHexString(queryKey);
    }
}
