/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
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

package org.limewire.mojito.util;

import java.net.SocketAddress;
import java.util.concurrent.ExecutionException;

import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.result.BootstrapResult;
import org.limewire.mojito.result.PingResult;


public class MojitoUtils {
    
    private MojitoUtils() {
        
    }
    
    /**
     * A helper method to bootstrap a MojitoDHT instance.
     * 
     * It tries to ping the given SocketAddress (this blocks) and in
     * case of a success it will kick off a bootstrap process and returns
     * a DHTFuture for the process.
     */
    public static DHTFuture<BootstrapResult> bootstrap(MojitoDHT dht, SocketAddress addr) 
            throws ExecutionException, InterruptedException {
        PingResult pong = dht.ping(addr).get();
        return dht.bootstrap(pong.getContact());
    }
}
