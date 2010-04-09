package org.limewire.mojito.concurrent;

import org.limewire.concurrent.ListeningFuture;

public interface DHTFuture2<V> extends ListeningFuture<V> {

    public boolean setValue(V value);
    
    public boolean setException(Throwable exception);
    
    public boolean isCompletedAbnormally();
}
