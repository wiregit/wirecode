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
 
package com.limegroup.mojito.messages;

import java.util.Collection;

import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValue;

/**
 * An interface for FindValueResponse implementations
 */
public interface FindValueResponse extends LookupResponse {
    
    /**
     * Returns a Collection of KUIDs that a Node has to offer
     */
    public Collection<KUID> getKeys();
    
    /**
     * Returns a Collection of DHTValue(s)
     */
    public Collection<DHTValue> getValues();
    
    /**
     * Returns the request load for this key
     */
    public float getRequestLoad();
}
