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
 
package com.limegroup.mojito;

import java.io.IOException;
import java.io.Serializable;
import java.net.SocketAddress;


/**
 * A Contact is a Node in the DHT. The Contact interface
 * encapsulates all required informations to contact the
 * remote Node or to keep track of its current State in
 * our RouteTable.
 */
public interface Contact extends Serializable {
    
    /**
     * Various states a Contact may have
     */
    public static enum State {
        // A Contact is alive either if we 
        // got a response to a request or 
        // we received a request from the Contact
        ALIVE, 
        
        // A Contact is dead if it fails to respond
        // a certain number of times to our requests
        DEAD,
        
        // An unknown Contact is neither dead nor
        // alive. Unknown Contacts are Contacts
        // we're receiving with FIND_NODE and FIND_VALUE
        // responses. We're using them to fill up our
        // RouteTable but they'll never replace alive
        // or dead Contacts in our RouteTable. An alive
        // Contact will always replace an unknown Contact!
        UNKNOWN;
    }
    
    /**
     * A constant for the time stamp that may only be used
     * for the local Contact.
     */
    public static final long LOCAL_CONTACT = Long.MAX_VALUE;
    
    /**
     * A constant for the the time stamp field to indicate 
     * that a Contact is a priority Contact which means it
     * may replace any live Contact (except for the local
     * Contact) in a Bucket
     */
    public static final long PRIORITY_CONTACT = Long.MAX_VALUE-1L;
    
    /**
     * Returns the Vendor of this Contact
     */
    public int getVendor();
    
    /**
     * Returns the Version of this Contact
     */
    public int getVersion();
    
    /**
     * Returns the Node ID of this Contact
     */
    public KUID getNodeID();
    
    /**
     * Returns the contact address. Use the contact
     * address to send requests or responses to the
     * remote Node.
     */
    public SocketAddress getContactAddress();
    
    /**
     * Returns the source address. The is the address
     * as read from the IP packet. Depending on the
     * network configuration of the remote Host it's
     * maybe not an valid address to respond to. 
     */
    public SocketAddress getSourceAddress();
    
    /**
     * Returns the instance ID of this Contact
     */
    public int getInstanceID();
    
    /**
     * Sets the time of the last successful Contact
     */
    public void setTimeStamp(long timeStamp);
    
    /**
     * Returns the time of the last successful exchange with this node
     */
    public long getTimeStamp();
    
    /**
     * Sets the Round Trip Time (RTT).
     */
    public void setRoundTripTime(long rtt);
    
    /**
     * Returns the Round Trip Time (RTT)
     */
    public long getRoundTripTime();
    
    /**
     * Returns an adaptive timeout based on the RTT and number of failures
     */
    public long getAdaptativeTimeout();
    
    /**
     * Returns the time of the last successful or unseccssful contact
     * attempt
     */
    public long getLastFailedTime();
    
    /**
     * Returns whether or not this Contact has been recently alive 
     */
    public boolean hasBeenRecentlyAlive();
    
    /**
     * Returns whether or not this Contact is firewalled
     */
    public boolean isFirewalled();
    
    /**
     * Returns whether or not this Contact is alive
     */
    public boolean isAlive();
    
    /**
     * Returns whether or not this Contact is dead
     */
    public boolean isDead();
    
    /**
     * Returns whether or not this Contact has failed since
     * the last successful contact
     */
    public boolean hasFailed();
    
    /**
     * Returns the number of failures have occured since
     * the last successful contact
     */
    public int getFailures();
    
    /**
     * Increments the failure counter, sets the last dead or alive time
     * and so on
     */
    public void handleFailure();
    
    /**
     * Returns whether or not this Contact is in unknown state
     */
    public boolean isUnknown();
    
    /**
     * Sets the Contact state to Unknown
     */
    public void unknown();
    
    /**
     * Updates this Contact with information from an existing Contact.
     * 
     * The updated fields are:
     * 
     * - Round trip time
     * - Time stamp
     * - Last dead or alive time
     * - Failure count
     * 
     * The latter three only if this Contact is not alive.
     */
    public void updateWithExistingContact(Contact existing);
    
    /**
     * An interface to check whether or not 'node' collides
     * with the local Node
     */
    public static interface CollisionVerifyer {
        public void doCollisionCheck(Contact node) throws IOException;
    }
}
