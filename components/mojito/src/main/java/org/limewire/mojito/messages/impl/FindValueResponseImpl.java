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
 
package org.limewire.mojito.messages.impl;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collection;

import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.io.MessageInputStream;
import org.limewire.mojito.io.MessageOutputStream;
import org.limewire.mojito.messages.FindValueResponse;
import org.limewire.mojito.messages.MessageID;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.Version;


/**
 * An implementation of FindValueResponse
 */
public class FindValueResponseImpl extends AbstractLookupResponse
        implements FindValueResponse {

    private final Collection<KUID> secondaryKeys;
    
    private final Collection<? extends DHTValueEntity> entities;
    
    private final float requestLoad;

    public FindValueResponseImpl(Context context, 
            Contact contact, MessageID messageId, 
            float requestLoad,
            Collection<? extends DHTValueEntity> entities, 
            Collection<KUID> secondaryKeys) {
        super(context, OpCode.FIND_VALUE_RESPONSE, contact, messageId);
        
        this.requestLoad = requestLoad;
        this.entities = entities;
        this.secondaryKeys = secondaryKeys;
    }

    public FindValueResponseImpl(Context context, SocketAddress src, 
            MessageID messageId, Version version, MessageInputStream in) throws IOException {
        super(context, OpCode.FIND_VALUE_RESPONSE, src, messageId, version, in);
        
        this.requestLoad = in.readFloat();
        this.entities = in.readDHTValueEntities(getContact(), context.getDHTValueFactory());
        this.secondaryKeys = in.readKUIDs();
    }
    
    public Collection<KUID> getSecondaryKeys() {
        return secondaryKeys;
    }
    
    public Collection<? extends DHTValueEntity> getDHTValueEntities() {
        return entities;
    }
    
    public float getRequestLoad() {
        return requestLoad;
    }

    protected void writeBody(MessageOutputStream out) throws IOException {
        out.writeFloat(requestLoad);
        out.writeDHTValueEntities(entities);
        out.writeKUIDs(secondaryKeys);
    }
    
    public String toString() {
        return "FindValueResponse: " + entities;
    }
}
