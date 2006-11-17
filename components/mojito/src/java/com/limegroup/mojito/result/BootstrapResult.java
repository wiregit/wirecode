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

package com.limegroup.mojito.result;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;

import com.limegroup.mojito.util.CollectionUtils;

/**
 * BootstrapResults are fired during bootstrapping and after
 * bootstrapping has finished
 */
public class BootstrapResult {
    
    /**
     * Various types of Bootstrap Events
     */
    public static enum ResultType {
        
        /**
         * Fired when the initial bootstrap ping succeded.
         * The bootstrap process continues and finishes
         * with either BOOTSTRAPPING_SUCCEEDED or 
         * BOOTSTRAPPING_FAILED
         */
        BOOTSTRAP_PING_SUCCEEDED,
        
        /**
         * Fired when the bootstrap process finished
         * successfully
         */
        BOOTSTRAP_SUCCEEDED,
        
        /**
         * Fired when the bootstrap process finished
         * unsuccessfully
         */
        BOOTSTRAP_FAILED;
    }
    
    private List<SocketAddress> failedHosts;
    
    private long phaseZeroTime = -1L;
    private long phaseOneTime = -1L;
    private long phaseTwoTime = -1L;
    private boolean foundNewContacts = false;
    
    private ResultType eventType;
    
    /**
     * Factory method to create BOOTSTRAP_PING_SUCCEEDED BootstrapEvents
     */
    @SuppressWarnings("unchecked")
    public static BootstrapResult createBootstrapPingSucceededResult() {
        
        return new BootstrapResult(Collections.EMPTY_LIST, -1L, 
                -1L, -1L, false, ResultType.BOOTSTRAP_PING_SUCCEEDED);
    }
    
    /**
     * Factory method to create BOOTSTRAP_FAILED BootstrapEvents
     */
    public static BootstrapResult createBootstrappingFailedResult(
            List<? extends SocketAddress> failedHosts, long phaseZero) {
        
        return new BootstrapResult(failedHosts, phaseZero, 
                -1L, -1L, false, ResultType.BOOTSTRAP_FAILED);
    }
    
    /**
     * Factory method to create BOOTSTRAP_SUCCEEDED BootstrapEvents
     */
    public static BootstrapResult createBootstrappingSucceededResult(
            List<? extends SocketAddress> failedHosts, long phaseZero, 
            long phaseOne, long phaseTwo, boolean foundsNewContacts) {
        
        return new BootstrapResult(failedHosts, phaseZero, phaseOne, 
                phaseTwo, foundsNewContacts, ResultType.BOOTSTRAP_SUCCEEDED);
    }
    
    @SuppressWarnings("unchecked")
    private BootstrapResult(List<? extends SocketAddress> failedHosts, 
            long phaseZeroTime, long phaseOneTime, long phaseTwoTime, 
            boolean foundNewContacts, ResultType eventType) {
        
        this.failedHosts = (List<SocketAddress>)failedHosts;
        this.phaseZeroTime = phaseZeroTime;
        this.phaseOneTime = phaseOneTime;
        this.phaseTwoTime = phaseTwoTime;
        this.foundNewContacts = foundNewContacts;
        this.eventType = eventType;
    }
    
    /**
     * Returns the EventType
     */
    public ResultType getEventType() {
        return eventType;
    }
    
    /**
     * Returns a List of SocketAddresses that failed during
     * bootstrapping
     */
    public List<SocketAddress> getFailedHosts() {
        return failedHosts;
    }
    
    /**
     * Returns the time how long phase zero (ping bootstrap host) took
     */
    public long getPhaseZeroTime() {
        return phaseZeroTime;
    }
    
    /**
     * Returns the time how long phase one (find nearest Nodes) took
     */
    public long getPhaseOneTime() {
        return phaseOneTime;
    }

    /**
     * Returns the time how long phase two (find random Nodes) took
     */
    public long getPhaseTwoTime() {
        return phaseTwoTime;
    }

    /**
     * Returns the total bootstrapping time
     */
    public long getTotalTime() {
        if (phaseZeroTime >= 0L) {
            if (phaseOneTime >= 0L) {
                if (phaseTwoTime >= 0L) {
                    return phaseZeroTime + phaseOneTime + phaseTwoTime;
                }
                return phaseZeroTime + phaseOneTime;
            }
            return phaseZeroTime;
        }
        return -1L;
    }
    
    /**
     * Returns whether or not phase two found any new Nodes
     */
    public boolean hasFoundNewContacts() {
        return foundNewContacts;
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("State: ").append(eventType).append("\n");
        buffer.append("Phase #0: ").append(getPhaseZeroTime()).append("ms\n");
        buffer.append("Phase #1: ").append(getPhaseOneTime()).append("ms\n");
        buffer.append("Phase #2: ").append(getPhaseTwoTime()).append("ms\n");
        buffer.append("Total: ").append(getTotalTime()).append("ms\n");
        
        if (!failedHosts.isEmpty()) {
            buffer.append("Failed: ").append(CollectionUtils.toString(failedHosts));
        }
        
        return buffer.toString();
    }
}
