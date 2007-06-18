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
 * An abstract class that provides common statistics for 
 * all DHT operations.
 */
public abstract class BasicGroup extends StatisticsGroup {
    
    /**
     * Counts the number of sent requests (we sent a request to somebody)
     */
    private final Statistic<Long> requestsSent = new Statistic<Long>();
    
    /**
     * Counts the number of received responses (we received a response from somebody)
     */
    private final Statistic<Long> responsesReceived = new Statistic<Long>();
    
    /**
     * Counts the number of requests received (we received a request from somebody)
     */
    private final Statistic<Long> requestsReceived = new Statistic<Long>();
    
    /**
     * Counts the number of responses sent (we sent a response to somebody)
     */
    private final Statistic<Long> responsesSent = new Statistic<Long>();
    
    public Statistic<Long> getRequestsSent() {
        return requestsSent;
    }
    
    public Statistic<Long> getResponsesReceived() {
        return responsesReceived;
    }
    
    public Statistic<Long> getRequestsReceived() {
        return requestsReceived;
    }
    
    public Statistic<Long> getResponsesSent() {
        return responsesSent;
    }
}
