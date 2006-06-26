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
    
    /**
     * 
     */
    public static enum State {
        ALIVE,
        DEAD,
        UNKNOWN;
    }
    
    /**
     * 
     */
    public State getState();

    /**
     * 
     */
    public void setState(State state);
    
    /**
     * 
     */
    public int getVendor();
    
    /**
     * 
     */
    public int getVersion();
    
    /**
     * 
     */
    public KUID getNodeID();
    
    /**
     * 
     */
    public void setSocketAddress(SocketAddress address);
    
    /**
     * 
     */
    public SocketAddress getSocketAddress();
    
    /**
     * 
     */
    public int getInstanceID();
    
    /**
     * 
     */
    // TODO ???
    @Deprecated
    public int getFlags();
    
    /**
     * 
     */
    public void setTimeStamp(long timeStamp);
    
    /**
     * 
     */
    public long getTimeStamp();
    
    /**
     * 
     */
    public void setRoundTripTime(long rtt);
    
    /**
     * 
     */
    public long getRoundTripTime();
    
    /**
     * 
     */
    public long getAdaptativeTimeout();
    
    /**
     * 
     */
    public long getLastDeadOrAliveTime();
    
    /**
     * 
     */
    public boolean hasBeenRecentlyAlive();
    
    /**
     * 
     */
    public void setFirewalled(boolean firewalled);
    
    /**
     * 
     */
    public boolean isFirewalled();
    
    /**
     * 
     */
    public boolean isAlive();
    
    /**
     * 
     */
    public boolean isDead();
    
    /**
     * Has the this Contact ever failed since the last contact
     */
    public boolean hasFailed();
    
    /**
     * 
     */
    public void handleFailure();
    
    /**
     * 
     */
    public boolean isUnknown();
    
    /**
     * 
     */
    public void unknown();
    
    /**
     * 
     */
    public void set(Contact contact);
}
