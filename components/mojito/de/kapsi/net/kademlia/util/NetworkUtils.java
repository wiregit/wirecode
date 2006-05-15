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
 
package de.kapsi.net.kademlia.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;

/**
 * TODO Move this code to LimeWire's NetworkUtils class. We must
 * also check IPv6 compatibilty!
 */
public final class NetworkUtils {
    
    private NetworkUtils() {}
    
    /**
     * Returns true if the SocketAddress is any of our local machine addresses.
     */
    public static boolean isLocalHostAddress(SocketAddress addr) throws IOException {
        return isLocalHostAddress(((InetSocketAddress)addr).getAddress());
    }
    
    /**
     * Returns true if the InetAddress is any of our local machine addresses
     */
    public static boolean isLocalHostAddress(InetAddress addr) throws IOException {
        return NetworkInterface.getByInetAddress(addr) != null;
    }
    
    /**
     * Returns whether or not the specified InetAddress and Port is valid.
     */
    public static boolean isValidSocketAddress(SocketAddress address) {
        InetAddress addr = ((InetSocketAddress)address).getAddress();
        int port = ((InetSocketAddress)address).getPort();
        
        return com.limegroup.gnutella.util.NetworkUtils.isValidAddress(addr)
            && com.limegroup.gnutella.util.NetworkUtils.isValidPort(port);
    }
    
    /*public static boolean isLocalSocketAddress(SocketAddress address) {
        InetAddress addr = ((InetSocketAddress)address).getAddress();
        return com.limegroup.gnutella.util.NetworkUtils.isLocalAddress(addr);
    }
    
    public static boolean isPrivateSocketAddress(SocketAddress address) {
        InetAddress addr = ((InetSocketAddress)address).getAddress();
        return com.limegroup.gnutella.util.NetworkUtils.isPrivateAddress(addr);
    }*/
}
