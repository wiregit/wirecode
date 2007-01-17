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

package org.limewire.mojito.manager;

import java.util.concurrent.Callable;

import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.handler.response.FindValueResponseHandler;
import org.limewire.mojito.result.FindValueResult;


/**
 * FindValueManager manages lookups for values
 */
public class FindValueManager extends AbstractLookupManager<FindValueResult> {
    
    public FindValueManager(Context context) {
        super(context);
    }

    /**
     * Creates and returns a FindValueResponseHandler
     */
    @Override
    protected Callable<FindValueResult> createLookupHandler(
            KUID lookupId, int count) {
        return new FindValueResponseHandler(context, lookupId);
    }
}
