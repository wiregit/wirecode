package org.limewire.mojito.io;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.message.RequestMessage;
import org.limewire.mojito.message.ResponseMessage;
import org.limewire.mojito.routing.Contact;

/**
 * An abstract implementation of {@link RequestHandler}.
 */
abstract class AbstractRequestHandler implements RequestHandler {

    private static final Log LOG 
        = LogFactory.getLog(AbstractRequestHandler.class);
    
    protected final Context context;
    
    public AbstractRequestHandler(Context context) {
        this.context = context;
    }
    
    /**
     * Sends a {@link ResponseMessage} to the given {@link Contact}.
     */
    protected void send(Contact dst, 
            ResponseMessage response) throws IOException {
        
        MessageDispatcher messageDispatcher 
            = context.getMessageDispatcher();
        messageDispatcher.send(dst, response);
    }
    
    @Override
    public void handleRequest(RequestMessage message) throws IOException {
        
        if (LOG.isTraceEnabled()) {
            LOG.trace(message.getContact() + " is requesting " + message);
        }
        
        processRequest(message);
    }
    
    /**
     * Called for each {@link RequestMessage}.
     */
    protected abstract void processRequest(
            RequestMessage message) throws IOException;
}
