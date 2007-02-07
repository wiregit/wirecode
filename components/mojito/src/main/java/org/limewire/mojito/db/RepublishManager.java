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

import org.limewire.mojito.Context;


/**
 * An interface that's used by Mojito to determinate if
 * a value is expired or requires republishing. 
 * 
 * Note: An incorrect implementation can lead to reference
 * leaks, high network traffic and all kinds of other quirks!
 */
public interface RepublishManager {
    
    /**
     * Returns true if it's necessary to republish the given DHTValueEntity
     */
    public boolean isRepublishingRequired(Context context, DHTValueEntity entity);
    
    /**
     * Returns true if the given DHTValueEntity is expired
     */
    public boolean isExpired(Context context, DHTValueEntity entity);
}
