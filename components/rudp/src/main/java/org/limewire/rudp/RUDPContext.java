package org.limewire.rudp;

import org.limewire.nio.observer.TransportListener;
import org.limewire.rudp.messages.MessageFactory;

/** A context to retrieve necessary RUDP components. */
public interface RUDPContext {

    /** The MessageFactory from which RUDP messages should be created. */
    public MessageFactory getMessageFactory();

    /** The TransportListener which should be notified when events are pending. */
    public TransportListener getTransportListener();

}