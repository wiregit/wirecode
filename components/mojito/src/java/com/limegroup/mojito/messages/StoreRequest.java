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

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.db.DHTValueEntity;

/**
 * An interface for StoreRequest implementations
 */
public interface StoreRequest extends RequestMessage {

    /**
     * The QueryKey the remote Node is using to store
     * the DHTValue(s) at our Node.
     */
    public QueryKey getQueryKey();

    /**
     * A Collection of DHTValue(s) we're supposed to
     * store at our Node
     */
    public Collection<DHTValueEntity> getDHTValues();
}
