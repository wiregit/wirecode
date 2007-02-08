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

import org.limewire.mojito.KUID;
import org.limewire.mojito.exceptions.DHTValueException;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.Version;

/**
 * A factory interface to create DHTValues and DHTValueEntities
 */
public interface DHTValueFactory {
    
    /**
     * Creates a DHTValueIdentity
     * 
     * @param creator The creator of the value
     * @param sender The sender of the value (creator and sender are
     * different if a value was store forwarded)
     * @param primaryKey The primary key of the value
     * @param value The aactual value
     * @param localValue Indicates if it's a local or remote value
     */
    public DHTValueEntity createDHTValueEntity(Contact creator, Contact sender, 
            KUID primaryKey, DHTValue value, boolean localValue);
    
    /**
     * Creates a DHTValue
     * 
     * @param type The type of the value
     * @param version The version of the value
     * @param value The actual value
     */
    public DHTValue createDHTValue(DHTValueType type, Version version, byte[] value) throws DHTValueException;
}
