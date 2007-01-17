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

/**
 * The DHTBadResponseException is thrown if we received a
 * bad response from a remote Node.
 */
@SuppressWarnings("serial")
public class DHTBadResponseException extends DHTException {

    public DHTBadResponseException() {
        super();
    }

    public DHTBadResponseException(String message, Throwable cause) {
        super(message, cause);
    }

    public DHTBadResponseException(String message) {
        super(message);
    }

    public DHTBadResponseException(Throwable cause) {
        super(cause);
    }
}
