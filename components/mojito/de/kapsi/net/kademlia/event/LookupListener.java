/*
 * Lime Kademlia Distributed Hash Table (DHT)
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
 
package de.kapsi.net.kademlia.event;

import java.util.Collection;

import de.kapsi.net.kademlia.KUID;

public interface LookupListener extends ResponseListener {
    
    /**
     * Called after a lookup has finished.
     * 
     * Collection <tt>c</tt> is a collection of Map.Entries. The
     * mapping depends on the lookup key type.
     * 
     * If it's a Node ID lookup then is the Entry an instanceof
     * LookupResponseHandler.ContactNodeEntry, Entry.getKey() returns the
     * ContactNode and Entry.getValue() returns the QueryKey.
     * 
     * If it's a Value ID lookup then is the Entry an instance of KeyValue, 
     * Entry.getKey() returns the Value ID and Entry.getValue() returns the 
     * value which is a byte Array.
     * 
     * In both cases is Entry.setValue() not implemented and will throw an
     * UnsupportedOperationException!
     */
    public void found(KUID lookup, Collection c, long time);
}
