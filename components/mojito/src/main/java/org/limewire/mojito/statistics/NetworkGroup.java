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

import org.limewire.mojito.io.MessageDispatcher.MessageDispatcherEvent;
import org.limewire.mojito.io.MessageDispatcher.MessageDispatcherListener;
import org.limewire.mojito.io.MessageDispatcher.MessageDispatcherEvent.EventType;
import org.limewire.mojito.messages.DHTMessage;
import org.limewire.mojito.messages.RequestMessage;

public class NetworkGroup extends BasicGroup implements MessageDispatcherListener {

    private final Statistic<Long> lateResponse = new Statistic<Long>();
    
    private final Statistic<Long> filtered = new Statistic<Long>();
    
    private final Statistic<Long> receiptTimeout = new Statistic<Long>();
    
    private final Statistic<Long> receiptEvicted = new Statistic<Long>();
    
    public void handleMessageDispatcherEvent(MessageDispatcherEvent evt) {
        EventType type = evt.getEventType();
        if (type.equals(EventType.MESSAGE_SENT)) {
            DHTMessage message = evt.getMessage();
            if (message instanceof RequestMessage) {
                getRequestsSent().incrementByOne();
            } else {
                getResponsesSent().incrementByOne();
            }
        } else if (type.equals(EventType.MESSAGE_RECEIVED)) {
            DHTMessage message = evt.getMessage();
            if (message instanceof RequestMessage) {
                getRequestsReceived().incrementByOne();
            } else {
                getResponsesReceived().incrementByOne();
            }
        } else if (type.equals(EventType.LATE_RESPONSE)) {
            lateResponse.incrementByOne();
        } else if (type.equals(EventType.MESSAGE_FILTERED)) {
            filtered.incrementByOne();
        } else if (type.equals(EventType.RECEIPT_TIMEOUT)) {
            receiptTimeout.incrementByOne();
        } else if (type.equals(EventType.RECEIPT_EVICTED)) {
            receiptEvicted.incrementByOne();
        }
    }
}
