/*
 * Mojito Distributed Hash Tabe (DHT)
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

package com.limegroup.mojito;

import java.net.SocketAddress;

/**
 *
 */
public interface Contact {
    
    public static enum State {
        ALIVE,
        DEAD,
        UNKNOWN;
    }
    
    public int getVendor();
    
    public int getVersion();
    
    public KUID getNodeID();
    
    public SocketAddress getSocketAddress();
    
    @Deprecated
    public void setSocketAddress(SocketAddress address);
    
    public int getInstanceID();
    
    @Deprecated
    public void setInstanceID(int instanceId);
    
    // TODO ???
    @Deprecated
    public int getFlags();
    
    public long getTimeStamp();
    
    @Deprecated
    public void setTimeStamp(long timeStamp);
    
    public long getAdaptativeTimeout();
    
    public long getRoundTripTime();
    
    public void setRoundTripTime(long rtt);
    
    public long getLastDeadOrAliveTime();
    
    public boolean hasBeenRecentlyAlive();
    
    public void setFirewalled(boolean firewalled);
    
    public boolean isFirewalled();
    
    public boolean isAlive();
    
    public void alive();
    
    public boolean isDead();
    
    public boolean hasFailed();
    
    public void handleFailure();
    
    public boolean isUnknown();
    
    public void unknown();
    
    public void set(Contact contact);
    
    public State getState();
    
    public void setState(State state);
}
