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

package com.limegroup.mojito.concurrent;

import java.util.concurrent.Future;

import com.limegroup.mojito.event.DHTEventListener;

/**
 * The DHTFuture extends Futures by Listeners.
 */
public interface DHTFuture<T> extends Future<T> {
    
    /**
     * Adds a DHTEventListener to the DHTFuture. The listener
     * is called when the DHTFuture finishes or if it has
     * alredy finished it will call the listener immediately.
     */
    public void addDHTEventListener(DHTEventListener<T> listener);
}
