package org.limewire.mojito.io;

import org.limewire.mojito.Context;

public interface MessageDispatcherFactory {
    
    public MessageDispatcher create(Context context);

}
