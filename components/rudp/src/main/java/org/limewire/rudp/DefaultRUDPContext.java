package org.limewire.rudp;

import org.limewire.nio.observer.TransportListener;
import org.limewire.rudp.messages.MessageFactory;
import org.limewire.rudp.messages.impl.DefaultMessageFactory;

/**
 * Contains all factories for a given RUDPContext.
 */
public class DefaultRUDPContext implements RUDPContext {
    
    /** The MessageFactory RUDP should create messages from. */
    private final MessageFactory messageFactory;
    
    /** The TransportListener that should be notified when events are pending. */
    private final TransportListener transportListener;
    
    /** The service to send UDP messages through. */
    
    /** The settings which control the rate of... */
   
    public DefaultRUDPContext() {
        this.messageFactory = new DefaultMessageFactory();
        this.transportListener = null;
    }
    
    public DefaultRUDPContext(MessageFactory factory) {
        this(factory, null);
    }
    
    public DefaultRUDPContext(TransportListener transportListener) {
        this(new DefaultMessageFactory(), transportListener);
    }
    
    public DefaultRUDPContext(MessageFactory factory, TransportListener transportListener) {
        this.messageFactory = factory;
        if(transportListener == null)
            this.transportListener = new NoOpTransportListener();
        else
            this.transportListener = transportListener;
    }
    
    /* (non-Javadoc)
     * @see org.limewire.rudp.RUDPContext#getMessageFactory()
     */
    public MessageFactory getMessageFactory() {
        return messageFactory;
    }
    
    /* (non-Javadoc)
     * @see org.limewire.rudp.RUDPContext#getTransportListener()
     */
    public TransportListener getTransportListener() {
        return transportListener;
    }
    
    /** A NoOp TransportListener. */
    private static final class NoOpTransportListener implements TransportListener {
        public void eventPending() {
        }
    }

}
