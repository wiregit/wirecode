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
import java.util.Map;

import de.kapsi.net.kademlia.KUID;

public interface FindNodeListener {
    
    /**
     * Called after a FIND_NODE lookup has finished.
     * 
     * @param lookup The ID we were looking for
     * @param nodes Collection of ContactNodes that were found (K closest to lookup ID sorted by closeness)
     * @param queryKeys Map of ContactNode -> QueryKeys
     * @param time Time in milliseconds
     */
    public void foundNodes(KUID lookup, Collection nodes, Map queryKeys, long time);
}
