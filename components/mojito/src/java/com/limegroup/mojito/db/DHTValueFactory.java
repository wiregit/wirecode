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

package com.limegroup.mojito.db;

import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValue.ValueType;
import com.limegroup.mojito.db.impl.DHTValueImpl;
import com.limegroup.mojito.routing.Contact;

/**
 * A Factory to create DHTValue(s)
 */
public class DHTValueFactory {

    private DHTValueFactory() {
        
    }
    
    /** 
     * Creates and returns a local DHTValue with a default version of 0. 
     */
    public static DHTValue createLocalValue(Contact creator, 
            ValueType type, KUID valueId, byte[] data) {
        return createLocalValue(creator, type, valueId, data, 0);
    }
    
    /**
     * Creates and returns a local DHTValue.
     */
    public static DHTValue createLocalValue(Contact creator, 
            ValueType type, KUID valueId, byte[] data, int version) {
        return new DHTValueImpl(creator, creator, type, valueId, data, version, true);
    }
    
    /** 
     * Creates and returns a remote DHTValue with a default version of 0.
     */
    public static DHTValue createRemoteValue(Contact creator, Contact sender, 
            ValueType type, KUID valueId, byte[] data) {
        return createRemoteValue(creator, sender, type, valueId, data, 0);
    }
    
    /** 
     * Creates and returns a remote DHTValue.
     */
    public static DHTValue createRemoteValue(Contact creator, Contact sender, 
            ValueType type, KUID valueId, byte[] data, int version) {
        return new DHTValueImpl(creator, sender, type, valueId, data, version, false);
    }
    
    /**
     * A helper method to set the creator of the DHTValue 
     */
    public static void setCreator(DHTValue value, Contact creator) {
        if (!value.isLocalValue()) {
            throw new IllegalArgumentException(value + " is not a local value!");
        }
        
        ((DHTValueImpl)value).setCreator(creator);
    }
}
