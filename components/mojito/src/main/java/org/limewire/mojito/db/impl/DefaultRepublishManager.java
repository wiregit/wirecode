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

package org.limewire.mojito.db.impl;

import org.limewire.mojito.Context;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.RepublishManager;
import org.limewire.mojito.util.DatabaseUtils;

public class DefaultRepublishManager implements RepublishManager {

    public boolean isRepublishingRequired(Context context, DHTValueEntity entity) {
        // Don't republish non-local values
        if (!entity.isLocalValue()) {
            return false;
        }
        
        if (entity.getPublishTime() == 0L) {
            return true;
        }
        
        return DatabaseUtils.isRepublishingRequired(
                entity.getPublishTime(), entity.getLocationCount());
    }
    
    public boolean isExpired(Context context, DHTValueEntity entity) {
        // Local values don't expire
        if (entity.isLocalValue()) {
            return false;
        }
        
        return DatabaseUtils.isExpired(context.getRouteTable(), entity);
    }
}
