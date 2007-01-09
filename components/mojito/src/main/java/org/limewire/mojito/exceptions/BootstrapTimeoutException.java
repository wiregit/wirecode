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

package org.limewire.mojito.exceptions;

import java.net.SocketAddress;
import java.util.Set;

/**
 * The BootstrapTimeoutException is thrown when the bootstrap
 * process isn't able to find an initial bootstrap Node.
 */
@SuppressWarnings("serial")
public class BootstrapTimeoutException extends DHTException {
    
    private Set<SocketAddress> failedHosts;
    
    public BootstrapTimeoutException(Set<SocketAddress> failedHosts) {
        this.failedHosts = failedHosts;
    }

    /**
     * Returns a Set of addresses that did not respond to our pings
     */
    public Set<SocketAddress> getFailedHosts() {
        return failedHosts;
    }
}
