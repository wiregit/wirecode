package com.limegroup.gnutella;

import com.limegroup.gnutella.gui.MessageHandler;

/**
 * Interface to bind list of {@link MessageHandler}s to a {@link MessageRouter}.
 */
public interface MessageHandlerBinder {

    public void bind(MessageRouter messageRouter);
    
}
