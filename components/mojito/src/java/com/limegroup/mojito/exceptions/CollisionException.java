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

package com.limegroup.mojito.exceptions;

import com.limegroup.mojito.routing.Contact;

/**
 * The CollisionException is thrown if this Node detects
 * a Node ID collision with an another DHT Node.
 */
@SuppressWarnings("serial")
public class CollisionException extends Exception {
    
    /** The Contact that collides with the local Node */
    private Contact collides;
    
    public CollisionException(Contact collides) {
        super();
        this.collides = collides;
    }

    public CollisionException(Contact collides, String message, Throwable cause) {
        super(message, cause);
        this.collides = collides;
    }

    public CollisionException(Contact collides, String message) {
        super(message);
        this.collides = collides;
    }

    public CollisionException(Contact collides, Throwable cause) {
        super(cause);
        this.collides = collides;
    }
    
    /**
     * Returns the Contact that collides with the local Node
     */
    public Contact getCollideContact() {
        return collides;
    }
}
