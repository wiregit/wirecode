package org.limewire.listener;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class PendingEventBroadcasterImpl<E> implements EventMulticaster<E>, PendingEventBroadcaster<E> {

    private final EventListenerList<E> listeners = new EventListenerList<E>();
    private final ConcurrentLinkedQueue<E> queuedEvents = new ConcurrentLinkedQueue<E>();
    private final AtomicBoolean firing = new AtomicBoolean();

    @Override
    public void addListener(EventListener<E> eventListener) {
        listeners.addListener(eventListener);
    }

    @Override
    public boolean removeListener(EventListener<E> eventListener) {
        return listeners.removeListener(eventListener);
    }
    
    @Override
    public void handleEvent(E event) {
        broadcast(event);
    }
    
    @Override
    public void broadcast(E event) {
        addPendingEvent(event);
        firePendingEvents();
    }

    @Override
    public void addPendingEvent(E event) {
        queuedEvents.add(event);
    }

    @Override
    public void firePendingEvents() {
        // It's possible that an event
        // was queued and another thread called firePendingEvents
        // before after we finished polling queuedEvents
        // but before firing was set back to false. To allow
        // the queued event to fire, we loop and exit if
        // someone else is firing. This guarantees that atleast
        // one thread will actively be sending queued events until
        // no queued events remain.
        while (!queuedEvents.isEmpty()) {
            if (firing.compareAndSet(false, true)) {
                E e;
                while ((e = queuedEvents.poll()) != null) {
                    listeners.broadcast(e);
                }
                firing.set(false);
            } else {
                // Exit while loop, allow firing thread
                // to take control of broadcasting the pending events.
                break;
            }
        }
    }

}
