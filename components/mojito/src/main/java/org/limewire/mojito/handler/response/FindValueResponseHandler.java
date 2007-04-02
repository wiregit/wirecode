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

package org.limewire.mojito.handler.response;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.messages.FindNodeResponse;
import org.limewire.mojito.messages.FindValueResponse;
import org.limewire.mojito.messages.LookupRequest;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.messages.ResponseMessage;
import org.limewire.mojito.result.FindValueResult;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.settings.KademliaSettings;
import org.limewire.mojito.statistics.FindValueLookupStatisticContainer;
import org.limewire.mojito.util.DatabaseUtils;
import org.limewire.security.SecurityToken;


/**
 * The FindNodeResponseHandler class implements FIND_VALUE specific features.
 */
public class FindValueResponseHandler extends LookupResponseHandler<FindValueResult> {
    
    private static final Log LOG = LogFactory.getLog(FindValueResponseHandler.class);

    /** Whether or not this is an exhaustive lookup. */
    private boolean exchaustive = false;
    
    private final FindValueLookupStatisticContainer lookupStat;
    
    /** */
    private final DHTValueType valueType;
    
    /** Collection of FindValueResponses if this is a FIND_VALUE lookup  */
    private Collection<FindValueResponse> valueResponses = null;
    
    public FindValueResponseHandler(Context context, KUID lookupId, 
            DHTValueType valueType) {
        super(context, lookupId);
        this.valueType = valueType;
        
        setExhaustive(KademliaSettings.EXHAUSTIVE_VALUE_LOOKUP.getValue());
        lookupStat = new FindValueLookupStatisticContainer(context, lookupId);
    }
    
    public DHTValueType getDHTValueType() {
        return valueType;
    }
    
    @Override
    protected void response(ResponseMessage message, long time) throws IOException {
        super.response(message, time);
        lookupStat.addReply();
    }

    @Override
    protected void timeout(KUID nodeId, SocketAddress dst, RequestMessage message, long time) throws IOException {
        super.timeout(nodeId, dst, message, time);
        lookupStat.addTimeout();
    }
    
    @Override
    protected boolean sendLookupRequest(Contact node) throws IOException {
        if (super.sendLookupRequest(node)) {
            lookupStat.addRequest();
            return true;
        }
        return false;
    }
    
    /**
     * Sets whether or not this is an exhaustive lookup
     * (works only with FIND_VALUE lookups)
     */
    public void setExhaustive(boolean exchaustive) {
        this.exchaustive = exchaustive;
    }

    @Override
    protected void finishLookup() {
        long time = getElapsedTime();
        
        Map<Contact, SecurityToken> path = getPath();
        Collection<FindValueResponse> values = getValues();
        
        if (values.isEmpty()) {
            lookupStat.FIND_VALUE_FAILURE.incrementStat();
        } else {
            lookupStat.FIND_VALUE_OK.incrementStat();
        }
        
        setReturnValue(new FindValueResult(getLookupID(), path, values, time, currentHop));
    }
    
    /**
     * Returns a Collection of FindValueResponse if this was 
     * a FIND_VALUE lookup
     */
    public Collection<FindValueResponse> getValues() {
        
        if (valueResponses != null) {
            return valueResponses;
        }
        return Collections.emptyList();
    }
    
    protected boolean nextStep(ResponseMessage message) throws IOException {
        if (message instanceof FindNodeResponse) {
            return handleNodeResponse((FindNodeResponse)message);
        }
        
        if (!(message instanceof FindValueResponse)) {
            throw new IllegalArgumentException("this is a find value handler");
        }
        
        FindValueResponse response = (FindValueResponse)message;
        Contact sender = response.getContact();
        
        Collection<KUID> keys = response.getSecondaryKeys();
        
        Collection<? extends DHTValueEntity> entities 
            = DatabaseUtils.filter(valueType, response.getDHTValueEntities());
        
        if (keys.isEmpty() && entities.isEmpty()) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(sender + " returned neither keys nor values for " + lookupId);
            }
            
            // Continue with the lookup...
            return true;
        }
        
        if (valueResponses == null) {
            valueResponses = new ArrayList<FindValueResponse>();
        }
        
        addToResponsePath(response);
        valueResponses.add(response);
        
        // Terminate the FIND_VALUE lookup if it isn't
        // an exhaustive lookup
        if (!exchaustive) {
            killActiveSearches();
            return false;
        }
        
        // Continue otherwise...
        return true;
    }
    
    protected int getDefaultParallelism() {
        return KademliaSettings.FIND_VALUE_PARALLEL_LOOKUPS.getValue();
    }
    
    protected boolean isTimeout(long time) {
        long lookupTimeout = KademliaSettings.FIND_VALUE_LOOKUP_TIMEOUT.getValue();
        return lookupTimeout > 0L && time >= lookupTimeout;
    }
    
    protected LookupRequest createLookupRequest(SocketAddress addr) {
        Collection<KUID> noKeys = Collections.emptySet();
        return context.getMessageHelper().createFindValueRequest(addr, lookupId, noKeys, valueType);
    }
    
    /*private static Collection<DHTValueEntity> filter(DHTValueType valueType, Collection<? extends DHTValueEntity> entities) {
        
    }*/
}
