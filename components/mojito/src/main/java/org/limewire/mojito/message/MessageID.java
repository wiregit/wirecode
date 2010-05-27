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

package org.limewire.mojito.message;

import java.io.Serializable;
import java.net.SocketAddress;

import org.limewire.mojito.io.Writeable;

/**
 * A {@link MessageID} is an unique identifier for {@link Message}s. 
 */
public interface MessageID extends Serializable, Writeable {
    
    /**
     * Returns the length of the {@link MessageID} in bytes.
     */
    public int getLength();
    
    /**
     * Returns the raw bytes of the {@link MessageID}.
     */
    public byte[] getBytes();

    /**
     * Returns the raw bytes of the current {@link MessageID}.
     */
    public byte[] getBytes(int srcPos, byte[] dest, int destPos, int length);
    
    /**
     * A mix-in interface for implementations of {@link MessageID}s to 
     * indicate that it's supporting tagging.
     */
    public static interface Tagging {
        
        /**
         * Returns whether or not we're the originator of the {@link MessageID}.
         */
        public boolean isFor(SocketAddress dst);
    }
}