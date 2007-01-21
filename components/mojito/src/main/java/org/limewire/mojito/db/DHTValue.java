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

package org.limewire.mojito.db;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

import org.limewire.mojito.routing.Version;

public interface DHTValue extends Serializable {

    /**
     * Returns the type of the value
     */
    public DHTValueType getValueType();

    /**
     * Returns the version of the value
     */
    public Version getVersion();

    /**
     * 
     */
    public void writeValue(OutputStream out) throws IOException;

    /**
     * Returns the actual value (a copy) as bytes
     */
    public byte[] getValue();

    /**
     * Returns the actual value (a copy) as bytes
     */
    public byte[] getValue(byte[] dst, int offset, int length);

    /**
     * Returns the size of the value in bytes
     */
    public int size();

    /**
     * Returns true if this is an empty value
     */
    public boolean isEmpty();

}