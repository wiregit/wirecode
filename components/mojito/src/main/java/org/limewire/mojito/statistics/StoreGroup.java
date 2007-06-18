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

package org.limewire.mojito.statistics;

/**
 * Provides various statistics for STORE operations
 */
public class StoreGroup extends BasicGroup {
    
    /**
     * Counts the number of STORE requests that have no security token
     */
    private final Statistic<Long> noSecurityToken = new Statistic<Long>();
    
    /**
     * Counts the number of STORE requests that have a bad security token
     */
    private final Statistic<Long> badSecurityToken = new Statistic<Long>();
    
    /**
     * Counts the number of STORE requests that were rejected
     */
    private final Statistic<Long> storeRequestRejected = new Statistic<Long>();
    
    /**
     * Counts the number of store-forwards
     */
    private final Statistic<Long> forwardToNearest = new Statistic<Long>();
    
    /**
     * Counts the number of times the furthest Node deleted a value
     */
    private final Statistic<Long> removeFromFurthest = new Statistic<Long>();
    
    /**
     * Counts how many values or how many times this Node has published values
     */
    private final Statistic<Long> publishedValues = new Statistic<Long>();
    
    /**
     * Counts how many values have expired on this Node
     */
    private final Statistic<Long> expiredValues = new Statistic<Long>();
    
    public Statistic<Long> getNoSecurityToken() {
        return noSecurityToken;
    }
    
    public Statistic<Long> getBadSecurityToken() {
        return badSecurityToken;
    }
    
    public Statistic<Long> getRequestRejected() {
        return storeRequestRejected;
    }
    
    public Statistic<Long> getForwardToNearest() {
        return forwardToNearest;
    }
    
    public Statistic<Long> getRemoveFromFurthest() {
        return removeFromFurthest;
    }
    
    public Statistic<Long> getPublishedValues() {
        return publishedValues;
    }
    
    public Statistic<Long> getExpiredValues() {
        return expiredValues;
    }
}
