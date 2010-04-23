package org.limewire.mojito.handler.request;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context2;
import org.limewire.mojito.handler.RequestHandler;
import org.limewire.mojito.io.MessageDispatcher2;
import org.limewire.mojito.messages.RequestMessage;

abstract class AbstractRequestHandler2 implements RequestHandler {

    private static final Log LOG = LogFactory.getLog(AbstractRequestHandler2.class);
    
    protected final MessageDispatcher2 messageDispatcher;
    
    protected final Context2 context;
    
    public AbstractRequestHandler2(
            MessageDispatcher2 messageDispatcher, Context2 context) {
        
        this.messageDispatcher = messageDispatcher;
        this.context = context;
    }
    
    @Override
    public void handleRequest(RequestMessage message) throws IOException {
        
        if (LOG.isTraceEnabled()) {
            LOG.trace(message.getContact() + " is requesting " + message);
        }
        
        processRequest(message);
    }
    
    /**
     * 
     */
    protected abstract void processRequest(
            RequestMessage message) throws IOException;
}
