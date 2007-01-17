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
 
package org.limewire.mojito.exceptions;

import java.net.SocketException;

/**
 * The IllegalSocketAddressException is thrown for "illegal" SocketAddresses.
 * Illegal means InetAddresses from the private address space for example.
 */
public class IllegalSocketAddressException extends SocketException {

    private static final long serialVersionUID = 5043640414346690375L;

    public IllegalSocketAddressException() {
        super();
    }

    public IllegalSocketAddressException(String s) {
        super(s);
    }
}
