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

package com.limegroup.mojito.event;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;

import com.limegroup.mojito.util.CollectionUtils;

public class BootstrapEvent {
    
    public static enum Type {
        PING_SUCCEEDED,
        SUCCEEDED,
        FAILED;
    }
    
    private List<SocketAddress> failed;
    
    private long phaseZeroTime = 0L;
    private long phaseOneTime = 0L;
    private long phaseTwoTime = 0L;
    private boolean foundNewContacts = false;
    
    private Type type;
    
    public BootstrapEvent(List<? extends SocketAddress> failed, long phaseZeroTime) {
        this(failed, phaseZeroTime, 0L, 0L, false, Type.FAILED);
    }
    
    public BootstrapEvent(List<? extends SocketAddress> failed, 
            long phaseZeroTime, long phaseOneTime, long phaseTwoTime, 
            boolean foundNewContacts) {
        this(failed, phaseZeroTime, phaseOneTime, phaseTwoTime, foundNewContacts, Type.SUCCEEDED);
    }
    
    @SuppressWarnings("unchecked")
    public BootstrapEvent(Type type) {
        this(Collections.EMPTY_LIST, 0L, 0L, 0L, false, type);
    }
    
    @SuppressWarnings("unchecked")
    public BootstrapEvent(List<? extends SocketAddress> failed, 
            long phaseZeroTime, long phaseOneTime, long phaseTwoTime, 
            boolean foundNewContacts, Type type) {
        
        this.failed = (List<SocketAddress>)failed;
        this.phaseZeroTime = phaseZeroTime;
        this.phaseOneTime = phaseOneTime;
        this.phaseTwoTime = phaseTwoTime;
        this.foundNewContacts = foundNewContacts;
        this.type = type;
    }
    
    public Type getType() {
        return type;
    }
    
    public List<SocketAddress> getFailedHostList() {
        return failed;
    }
    
    public long getPhaseZeroTime() {
        return phaseZeroTime;
    }
    
    public long getPhaseOneTime() {
        return phaseOneTime;
    }

    public long getPhaseTwoTime() {
        return phaseTwoTime;
    }

    public long getTotalTime() {
        return phaseZeroTime + phaseOneTime + phaseTwoTime;
    }
    
    public boolean hasFoundNewContacts() {
        return foundNewContacts;
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("State: ").append(type).append("\n");
        buffer.append("Phase #0: ").append(getPhaseZeroTime()).append("ms\n");
        buffer.append("Phase #1: ").append(getPhaseOneTime()).append("ms\n");
        buffer.append("Phase #2: ").append(getPhaseTwoTime()).append("ms\n");
        buffer.append("Total: ").append(getTotalTime()).append("ms\n");
        
        if (failed.isEmpty()) {
            buffer.append("Failed: NONE");
        } else {
            buffer.append("Failed: ").append(CollectionUtils.toString(failed));
        }
        
        return buffer.toString();
    }
}
