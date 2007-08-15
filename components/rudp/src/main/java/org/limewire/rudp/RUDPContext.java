package org.limewire.rudp;

import org.limewire.nio.observer.TransportListener;
import org.limewire.rudp.messages.RUDPMessageFactory;

/** A context to retrieve necessary RUDP components. */
public interface RUDPContext {

    /** The MessageFactory from which RUDP messages should be created. */
    public RUDPMessageFactory getMessageFactory();

    /** The TransportListener which should be notified when events are pending. */
    public TransportListener getTransportListener();
    
    /** The UDPService used to send messages & know about udp listening ports. */
    public UDPService getUDPService();
    
    /** The RUDPSettings to use controlling the algorithm. */
    public RUDPSettings getRUDPSettings();

}