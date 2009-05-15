package org.limewire.listener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class AsynchronousSourcedEventMultcasterImpl<E extends SourcedEvent<S>, S> implements
        AsynchronousSourcedEventMultcaster<E, S> {

    /** The list of listeners for every change event. */
    private final AsynchronousMulticaster<E> listenersForAll;

    /** A Map of listeners for each source. */
    private final Map<S, AsynchronousMulticaster<E>> sourceListeners;

    private final Executor executor;

    public AsynchronousSourcedEventMultcasterImpl(Executor executor) {
        this.executor = executor;
        listenersForAll = new AsynchronousMulticaster<E>(executor);
        sourceListeners = new ConcurrentHashMap<S, AsynchronousMulticaster<E>>();
    }

    @Override
    public void addListener(S source, EventListener<E> listener) {
        synchronized (sourceListeners) {
            AsynchronousMulticaster<E> list = sourceListeners.get(source);
            if (list == null) {
                list = new AsynchronousMulticaster<E>(executor);
                sourceListeners.put(source, list);
            }
            list.addListener(listener);
        }

    }

    @Override
    public boolean removeListener(S source, EventListener<E> listener) {
        synchronized (sourceListeners) {
            AsynchronousMulticaster<E> list = sourceListeners.get(source);
            if (list == null) {
                return false;
            } else {
                boolean removed = list.removeListener(listener);
                if (list.size() == 0) {
                    sourceListeners.remove(source);
                }
                return removed;
            }
        }
    }

    @Override
    public boolean removeListeners(S source) {
        synchronized (sourceListeners) {
            return sourceListeners.remove(source) != null;
        }
    }

    @Override
    public void addListener(EventListener<E> listener) {
        listenersForAll.addListener(listener);
    }

    @Override
    public boolean removeListener(EventListener<E> listener) {
        return listenersForAll.removeListener(listener);
    }

    @Override
    public void handleEvent(E event) {
        broadcast(event);
    }

    @Override
    public void broadcast(E event) {
        listenersForAll.broadcast(event);
        // No lock is necessary here, because a ConcurrentHashMap is used,
        // so it is thread-safe as far as retrieval goes.
        AsynchronousMulticaster<E> list = sourceListeners.get(event.getSource());
        if (list != null) {
            list.broadcast(event);
        }
    }

}
