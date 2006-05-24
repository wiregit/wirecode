/*
 * Mojito Distributed Hash Tabe (DHT)
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
 
package com.limegroup.mojito.util;

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
        InetSocketAddress iaddr = (InetSocketAddress)addr;
        return !iaddr.isUnresolved() && isLocalHostAddress(iaddr.getAddress());
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
        InetSocketAddress iaddr = (InetSocketAddress)address;
        
        return !iaddr.isUnresolved()
            && com.limegroup.gnutella.util.NetworkUtils.isValidAddress(iaddr.getAddress())
            && com.limegroup.gnutella.util.NetworkUtils.isValidPort(iaddr.getPort());
    }
    
    public static byte[] getBytes(SocketAddress addr) throws IOException {
        byte[] address = ((InetSocketAddress)addr).getAddress().getAddress();
        int port = ((InetSocketAddress)addr).getPort();

        byte[] dst = new byte[address.length + 2];
        System.arraycopy(address, 0, dst, 0, address.length);
        dst[dst.length-2] = (byte)((port >> 8) & 0xFF);
        dst[dst.length-1] = (byte)((port     ) & 0xFF);
        return dst;
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
