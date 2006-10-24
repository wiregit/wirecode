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

package com.limegroup.mojito.manager;

import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.event.FindValueEvent;
import com.limegroup.mojito.handler.response.FindValueResponseHandler;
import com.limegroup.mojito.handler.response.LookupResponseHandler;

/**
 * FindValueManager manages lookups for values
 */
public class FindValueManager extends AbstractLookupManager<FindValueEvent> {
    
    public FindValueManager(Context context) {
        super(context);
    }

    /**
     * Creates and returns a FindValueResponseHandler
     */
    @Override
    protected LookupResponseHandler<FindValueEvent> createLookupHandler(
            KUID lookupId, int count) {
        return new FindValueResponseHandler(context, lookupId);
    }
}
