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
import com.limegroup.mojito.routing.Contact;

/**
 * An interface for FindNodeResponse implementations
 */
public interface FindNodeResponse extends LookupResponse {

    /**
     * Returns the remote Node's QueryKey we're supposted to
     * use if we're planning to store values at the remote
     * Node for example
     */
    public QueryKey getQueryKey();

    /**
     * Returns the k-closest (or less) Node's to the
     * KUID we were looking for
     */
    public Collection<? extends Contact> getNodes();
}
