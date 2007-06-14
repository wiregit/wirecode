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
