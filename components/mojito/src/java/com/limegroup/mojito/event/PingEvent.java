/*
 * Mojito Distributed Hash Table (Mojito DHT)
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

package com.limegroup.mojito.event;

import java.math.BigInteger;
import java.net.SocketAddress;

import com.limegroup.mojito.routing.Contact;

/**
 * PingEvent(s) are fired for successful pings
 */
public class PingEvent {
    
    /** The remote Contact */
    private Contact node;
    
    /** My external address */
    private SocketAddress externalAddress;
    
    /** The estimated DHT size */
    private BigInteger estimatedSize;
    
    /** the Round Trip Time (RTT) */
    private long time;
    
    /**
     * Constructs a new PingEvent
     * 
     * @param node The remote Contact
     * @param externalAddress My external address
     * @param estimatedSize The estimated DHT size
     * @param time The Round Trip Time (RTT)
     */
    public PingEvent(Contact node, SocketAddress externalAddress, 
            BigInteger estimatedSize, long time) {
        this.node = node;
        this.externalAddress = externalAddress;
        this.estimatedSize = estimatedSize;
        this.time = time;
    }
    
    /**
     * Returns the remote Node's Contact info
     */
    public Contact getContact() {
        return node;
    }
    
    /**
     * Returns the external address
     */
    public SocketAddress getExternalAddress() {
        return externalAddress;
    }
    
    /**
     * Returns the estimated DHT size
     */
    public BigInteger getEstimatedSize() {
        return estimatedSize;
    }
    
    /**
     * Returns the Round Trip Time (RTT)
     */
    public long getTime() {
        return time;
    }
    
    public String toString() {
        return node + ", externalAddress=" + externalAddress 
                    + ", estimatedSize=" + estimatedSize
                    + ", time=" + time + "ms";
    }
}
