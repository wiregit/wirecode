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

package org.limewire.mojito.io;

import org.limewire.mojito.Context;

/**
 * A factory interface to create instances of {@link MessageDispatcher}
 */
public interface MessageDispatcherFactory {
    
    /**
     * Creates and returns an instance of {@link MessageDispatcher}
     * 
     * @param context The Mojito {@link Context}
     * @return {@link MessageDispatcher}
     */
    public MessageDispatcher create(Context context);
}
