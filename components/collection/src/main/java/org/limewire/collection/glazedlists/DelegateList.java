package org.limewire.collection.glazedlists;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;

import ca.odell.glazedlists.CompositeList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.event.ListEventPublisher;
import ca.odell.glazedlists.util.concurrent.ReadWriteLock;

public class DelegateList<E> implements EventList<E> {

    private final CompositeList<E> compositeList;
    private EventList<E> currentDelegate;
    private List<DelegateListener<E>> listeners = new CopyOnWriteArrayList<DelegateListener<E>>();
    
    public DelegateList(ListEventPublisher publisher, ReadWriteLock lock) {
        this.compositeList = new CompositeList<E>(publisher, lock);
    }
    
    public void addDelegateListListener(DelegateListener<E> listener) {
        listeners.add(listener);
    }
    
    public void removeDelegateListListener(DelegateListener<E> listener) {
        listeners.remove(listener);
    }
    
    public void setDelegateList(EventList<E> delegateList) {
        delegateList.getReadWriteLock().writeLock().lock();
        try {
            if(currentDelegate != delegateList) {
                if(currentDelegate != null) {
                    compositeList.removeMemberList(currentDelegate);
                }
                currentDelegate = delegateList;
                compositeList.addMemberList(currentDelegate);
                for(DelegateListener<E> listener : listeners) {
                    listener.delegateChanged(this, currentDelegate);
                }
            }
        } finally {
            delegateList.getReadWriteLock().writeLock().unlock();    
        }
    }

    public boolean add(E value) {
        return compositeList.add(value);
    }

    public void add(int index, E value) {
        compositeList.add(index, value);
    }

    public boolean addAll(Collection<? extends E> values) {
        return compositeList.addAll(values);
    }

    public boolean addAll(int index, Collection<? extends E> values) {
        return compositeList.addAll(index, values);
    }

    public void addListEventListener(ListEventListener<E> listChangeListener) {
        compositeList.addListEventListener(listChangeListener);
    }

    public void clear() {
        compositeList.clear();
    }

    public boolean contains(Object object) {
        return compositeList.contains(object);
    }

    public boolean containsAll(Collection<?> values) {
        return compositeList.containsAll(values);
    }

    public void dispose() {
        compositeList.dispose();
    }

    public boolean equals(Object object) {
        return compositeList.equals(object);
    }

    public E get(int index) {
        return compositeList.get(index);
    }

    public ListEventPublisher getPublisher() {
        return compositeList.getPublisher();
    }

    public ReadWriteLock getReadWriteLock() {
        return compositeList.getReadWriteLock();
    }

    public int hashCode() {
        return compositeList.hashCode();
    }

    public int indexOf(Object object) {
        return compositeList.indexOf(object);
    }

    public boolean isEmpty() {
        return compositeList.isEmpty();
    }

    public Iterator<E> iterator() {
        return compositeList.iterator();
    }

    public int lastIndexOf(Object object) {
        return compositeList.lastIndexOf(object);
    }

    public ListIterator<E> listIterator() {
        return compositeList.listIterator();
    }

    public ListIterator<E> listIterator(int index) {
        return compositeList.listIterator(index);
    }

    public E remove(int index) {
        return compositeList.remove(index);
    }

    public boolean remove(Object toRemove) {
        return compositeList.remove(toRemove);
    }

    public boolean removeAll(Collection<?> collection) {
        return compositeList.removeAll(collection);
    }

    public void removeListEventListener(ListEventListener<E> listChangeListener) {
        compositeList.removeListEventListener(listChangeListener);
    }

    public boolean retainAll(Collection<?> values) {
        return compositeList.retainAll(values);
    }

    public E set(int index, E value) {
        return compositeList.set(index, value);
    }

    public int size() {
        return compositeList.size();
    }

    public List<E> subList(int fromIndex, int toIndex) {
        return compositeList.subList(fromIndex, toIndex);
    }

    public Object[] toArray() {
        return compositeList.toArray();
    }

    public <T> T[] toArray(T[] array) {
        return compositeList.toArray(array);
    }

    public String toString() {
        return compositeList.toString();
    }
    
    
   public static interface DelegateListener<E> {
       public void delegateChanged(DelegateList<E> delegateList, EventList<E> theDelegate);
   }

}
