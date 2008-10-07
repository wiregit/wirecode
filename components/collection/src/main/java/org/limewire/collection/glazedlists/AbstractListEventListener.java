package org.limewire.collection.glazedlists;

import java.util.ArrayList;
import java.util.List;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.util.concurrent.Lock;

/** 
 * An abstract ListEventListener for use with glazed lists when you really
 * need the deleted object.  Use this class sparingly, as it will cache
 * the contents of the list.
 */
public abstract class AbstractListEventListener<E> implements ListEventListener<E> {
    private final List<E> cache = new ArrayList<E>();
    
    public void install(EventList<E> source) {
        Lock lock = source.getReadWriteLock().readLock();
        lock.lock();
        try {
            cache.addAll(source);
            source.addListEventListener(this);
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public void listChanged(ListEvent<E> changes) {
        while (changes.next()) {
            int type = changes.getType();
            int idx = changes.getIndex();
            switch (type) {
            case ListEvent.INSERT:
                cache.add(idx, changes.getSourceList().get(idx));
                itemAdded(cache.get(idx));
                break;
            case ListEvent.DELETE:
                E removed = cache.remove(idx);
                itemRemoved(removed);
                break;
            case ListEvent.UPDATE:
                cache.set(idx, changes.getSourceList().get(idx));
                itemUpdated(cache.get(idx));
                break;
            }
        }
    }

     protected abstract void itemAdded(E item);

    protected abstract void itemRemoved(E item);

    protected abstract void itemUpdated(E item);
    

}
