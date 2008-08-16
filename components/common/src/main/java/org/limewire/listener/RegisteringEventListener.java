package org.limewire.listener;

import com.google.inject.Inject;

public interface RegisteringEventListener<E> extends EventListener<E> {
    @Inject
    public void register(ListenerSupport<E> listenerSupport);
}
