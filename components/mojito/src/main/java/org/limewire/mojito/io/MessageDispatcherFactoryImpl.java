package org.limewire.mojito.io;

import org.limewire.mojito.Context;

public class MessageDispatcherFactoryImpl implements MessageDispatcherFactory {

    public MessageDispatcher create(Context context) {
        return new MessageDispatcherImpl(context);
    }

}
