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

package org.limewire.mojito.storage;

import java.util.concurrent.TimeUnit;

import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.routing.Contact;

/**
 * A {@link ValueTuple} is a row in a {@link Database}. It consists
 * of a creator of the {@link Value}, the sender, a primary key,
 * a secondary key and the {@link Value} itself.
 */
public class ValueTuple {
    
    /**
     * The creator of the value.
     */
    private final Contact creator;
    
    /**
     * The sender of the value (store forward).
     */
    private final Contact sender;
    
    /**
     * The (primary) key of the value.
     */
    private final KUID primaryKey;
    
    /**
     * The secondary key of the value.
     */
    private final KUID secondaryKey;
    
    /**
     * The actual value.
     */
    private final Value value;
    
    /**
     * The time when this value was created (local time).
     */
    private final long creationTime = System.currentTimeMillis();
    
    /**
     * Flag for whether or not this is a local entity
     * (i.e. Sender and Creator are both the local Node).
     */
    private final boolean local;
    
    /**
     * The hash code of this entity.
     */
    private final int hashCode;
    
    /**
     * Creates and returns a {@link ValueTuple} for the given primary 
     * key and value.
     */
    public static ValueTuple createValueTuple(Context context, 
            KUID primaryKey, Value value) {
        Contact src = context.getLocalNode();
        return new ValueTuple(src, src, primaryKey, value, true);
    }
    
    /**
     * Creates and returns a {@link ValueTuple} from arguments that were created.
     */
    public static ValueTuple createValueTuple(Contact creator, 
            Contact sender, KUID primaryKey, Value value) {
        return new ValueTuple(creator, sender, primaryKey, value, false);
    }
    
    /**
     * Constructor to create {@link ValueTuple}ies. It's package 
     * private for testing purposes. Use the factory methods!
     */
    ValueTuple(Contact creator, Contact sender, 
            KUID primaryKey, Value value, boolean local) {
        this.creator = creator;
        this.sender = sender;
        this.primaryKey = primaryKey;
        this.secondaryKey = creator.getContactId();
        this.value = value;
        this.local = local;
        
        this.hashCode = 17*primaryKey.hashCode() + secondaryKey.hashCode();
    }
    
    /**
     * Returns the creator of this value.
     */
    public Contact getCreator() {
        return creator;
    }
    
    /**
     * Returns the sender of this value.
     */
    public Contact getSender() {
        return sender;
    }
    
    /**
     * Returns the primary key of this value.
     */
    public KUID getPrimaryKey() {
        return primaryKey;
    }
    
    /**
     * Returns the secondary key of this value.
     */
    public KUID getSecondaryKey() {
        return secondaryKey;
    }
    
    /**
     * Returns the value.
     */
    public Value getValue() {
        return value;
    }
   
    /**
     * Returns the creation time.
     */
    public long getCreationTime() {
        return creationTime;
    }
    
    /**
     * Returns the age of the {@link ValueTuple} in 
     * the given {@link TimeUnit}.
     */
    public long getAge(TimeUnit unit) {
        long time = System.currentTimeMillis() - creationTime;
        return unit.convert(time, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Returns the age of the {@link ValueTuple} in milliseconds.
     */
    public long getAgeInMillis() {
        return getAge(TimeUnit.MILLISECONDS);
    }
    
    /**
     * Returns <code>true</code> if this entity was sent by
     * the creator of the value. In other words
     * if the creator and sender are equal.
     */
    public boolean isDirect() {
        return creator.equals(sender);
    }
    
    public boolean isLocalValue() {
        return local;
    }
    
    @Override
    public int hashCode() {
        return hashCode;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof ValueTuple)) {
            return false;
        }
        
        ValueTuple other = (ValueTuple)o;
        return primaryKey.equals(other.getPrimaryKey())
                    && secondaryKey.equals(other.getSecondaryKey());
    }
    
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Creator: ").append(getCreator()).append("\n");
        buffer.append("Sender: ").append(getSender()).append("\n");
        buffer.append("Primary Key: ").append(getPrimaryKey()).append("\n");
        buffer.append("Secondary Key: ").append(getSecondaryKey()).append("\n");
        buffer.append("Creation time: ").append(getCreationTime()).append("\n");
        buffer.append("---\n").append(getValue()).append("\n");
        return buffer.toString();
    }
}
