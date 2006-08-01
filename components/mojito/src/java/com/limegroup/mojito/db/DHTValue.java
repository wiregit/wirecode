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

package com.limegroup.mojito.db;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.settings.DatabaseSettings;
import com.limegroup.mojito.settings.KademliaSettings;

public class DHTValue implements Serializable {
    
    private static final long serialVersionUID = -5172585009004184680L;

    public static final byte[] EMPTY_DATA = new byte[0];

    private Contact originator;
    
    private Contact sender;
    
    private KUID valueId;
    
    private byte[] data;
    
    private long creationTime = System.currentTimeMillis();
    
    private transient long lastRepublishingTime = 0L;
    
    private transient int locations = 0;
    
    private boolean isLocalValue = true;
    
    private boolean nearby = true;
    
    private int hashCode = -1;
    
    public static DHTValue createLocalValue(Contact originator, 
            KUID valueId, byte[] data) {
        return new DHTValue(originator, originator, valueId, data, true);
    }
    
    public static DHTValue createRemoteValue(Contact originator, Contact sender, 
            KUID valueId, byte[] data) {
        return new DHTValue(originator, sender, valueId, data, false);
    }
    
    private DHTValue(Contact originator, Contact sender, KUID valueId, byte[] data, 
            boolean isLocalValue) {
        this.originator = originator;
        this.sender = sender;
        this.valueId = valueId;
        this.data = (data != null) ? data : EMPTY_DATA;
        this.isLocalValue = isLocalValue;
    }
    
    private void init() {
        lastRepublishingTime = 0L;
        locations = 0;
    }
    
    public KUID getValueID() {
        return valueId;
    }
    
    public byte[] getData() {
        return data;
    }
    
    public int size() {
        return data.length;
    }
    
    public boolean isEmpty() {
        return size() == 0;
    }
    
    public void setOriginator(Contact originator) {
        this.originator = originator;
    }
    
    public Contact getOriginator() {
        return originator;
    }
    
    public void setSender(Contact sender) {
        this.sender = sender;
    }
    
    public Contact getSender() {
        return sender;
    }
    
    public long getCreationTime() {
        return creationTime;
    }
    
    public boolean isDirect() {
        return originator.getNodeID().equals(sender.getNodeID());
    }
    
    public boolean isLocalValue() {
        return isLocalValue;
    }
    
    public void setNearby(boolean nearby) {
        this.nearby = nearby;
    }
    
    public boolean isNearby() {
        return nearby;
    }
    
    public boolean isExpired() {
        if (isLocalValue()) {
            return false;
        }

        long expirationTime = getCreationTime()
                + DatabaseSettings.EXPIRATION_TIME_CLOSEST_NODE.getValue();
        
        return System.currentTimeMillis() >= expirationTime;
    }
    
    public boolean isRepublishingRequired() {
        if (!isLocalValue()) {
            return false;
        }
        
        long t = (long)((locations 
                * DatabaseSettings.REPUBLISH_INTERVAL.getValue()) 
                    / KademliaSettings.REPLICATION_PARAMETER.getValue());
        
        // never republish more than every X minutes
        long nextPublishTime = Math.max(t, DatabaseSettings.MIN_REPUBLISH_INTERVAL.getValue());
        long time = lastRepublishingTime + nextPublishTime;

        return System.currentTimeMillis() >= time;
    }
    
    public void publishedTo(int locations) {
        if (locations < 0) {
            throw new IllegalArgumentException("locations: " + locations);
        }
        
        this.locations = locations;
        this.lastRepublishingTime = System.currentTimeMillis();
    }
    
    public int hashCode() {
        if (hashCode == -1) {
            hashCode = 17*valueId.hashCode() + Arrays.hashCode(data);
            if (hashCode == -1) {
                hashCode = 0;
            }
        }
        return hashCode;
    }
    
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof DHTValue)) {
            return false;
        }
        
        DHTValue c = (DHTValue)o;
        return valueId.equals(c.valueId) && Arrays.equals(data, c.data);
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        if (isEmpty()) {
            buffer.append(valueId).append(", originator=")
                .append(getOriginator()).append(" (REMOVE)");
        } else {
            buffer.append(valueId).append("\n")
                .append("Originator: ").append(getOriginator()).append("\n")
                .append("Sender: ").append(getSender()).append("\n")
                //.append("Hex: ").append(ArrayUtils.toHexString(data, 80));
                .append("Data: ").append(new String(data));
        }
        return buffer.toString();
    }
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        init(); // Init transient fields
        in.defaultReadObject();
    }
}
