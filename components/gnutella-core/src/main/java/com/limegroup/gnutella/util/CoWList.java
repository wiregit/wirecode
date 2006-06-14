package com.limegroup.gnutella.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import com.limegroup.gnutella.ErrorService;

interface ListCreator<T> {
    public List<T> getList();
}

public class CoWList<T> implements List<T> {
    
    public static final ListCreator ARRAY_LIST = new ArrayCreator(); 
    
    public static final ListCreator LINKED_LIST = new LinkedCreator();
    
    private volatile List<T> l;
    
    private final ListCreator<T> creator;
    
    /** Object to synchronize the atomic operations on */
    private final Object lock;
    
    public CoWList(List<T> l, Object lock) {
        this.l = l;
        this.creator = new ReflectiveCreator<T>(l.getClass());
        this.lock = lock == null ? this : lock;
    }
    
    public CoWList(Class listType, Object lock) {
        this(new ReflectiveCreator<T>(listType),lock);
    }
    
    public CoWList(ListCreator<T> creator) {
        this(creator, null);
    }
    
    public CoWList(ListCreator<T> creator, Object lock) {
        this.creator = creator;
        l = creator.getList();
        this.lock = lock == null ? this : lock;
    }
    
    private List<T> getListCopy() {
        List<T> ret = creator.getList();
        
        if (l != null)
            ret.addAll(l);
        
        return ret;
    }
    
    public void add(int arg0, T arg1) {
        synchronized(lock) {
            List<T> newList = getListCopy();
            newList.add(arg0, arg1);
            l = newList;
        }
    }

    public boolean add(T arg0) {
        synchronized(lock) {
            List<T> newList = getListCopy();
            boolean ret = newList.add(arg0);
            l = newList;
            return ret;
        }
    }

    public boolean addAll(Collection<? extends T> arg0) {
        synchronized(lock) {
            List<T> newList = getListCopy();
            boolean ret = newList.addAll(arg0);
            l = newList;
            return ret;
        }
    }

    public boolean addAll(int arg0, Collection<? extends T> arg1) {
        synchronized(lock) {
            List<T> newList = getListCopy();
            boolean ret = newList.addAll(arg0,arg1);
            l = newList;
            return ret;
        }
    }

    public void clear() {
        synchronized(lock) {
            List<T> newList = getListCopy();
            newList.clear();
            l = newList;
        }
    }

    public boolean contains(Object o) {
        return l.contains(o);
    }

    public boolean containsAll(Collection arg0) {
        return l.containsAll(arg0);
    }

    public T get(int index) {
        return l.get(index);
    }

    public int indexOf(Object o) {
        return l.indexOf(o);
    }

    public boolean isEmpty() {
        return l.isEmpty();
    }

    public Iterator<T> iterator() {
        return l.iterator();
    }

    public int lastIndexOf(Object o) {
        return l.lastIndexOf(o);
    }

    public ListIterator<T> listIterator() {
        return l.listIterator();
    }

    public ListIterator<T> listIterator(int index) {
        return l.listIterator(index);
    }

    public T remove(int index) {
        synchronized(lock) {
            T ret = null;
            List<T> newList = getListCopy();
            ret = newList.remove(index);
            l = newList;
            return ret;
        }
    }

    public boolean remove(Object o) {
        synchronized(lock) {
            List<T> newList = getListCopy();
            boolean ret = newList.remove(o);
            l = newList;
            return ret;    
        }
    }

    public boolean removeAll(Collection arg0) {
        synchronized(lock) {
            List<T> newList = getListCopy();
            boolean ret = newList.removeAll(arg0);
            l = newList;
            return ret;    
        }
    }

    public boolean retainAll(Collection arg0) {
        synchronized(lock) {
            List<T> newList = getListCopy();
            boolean ret = newList.retainAll(arg0);
            l = newList;
            return ret;
        }
    }

    public T set(int arg0, T arg1) {
        synchronized(lock) {
            List<T> newList = getListCopy();
            T ret = newList.set(arg0,arg1);
            l = newList;
            return ret;
        }
    }

    public int size() {
        return l.size();
    }

    public List<T> subList(int fromIndex, int toIndex) {
        return l.subList(fromIndex, toIndex);
    }

    public Object[] toArray() {
        return l.toArray();
    }

    public <O>O[] toArray(O[] arg0) {
        return l.toArray(arg0);
    }
    
    private static class ReflectiveCreator<T> implements ListCreator<T> {
        private final Class listType;
        public ReflectiveCreator(Class c) {
            this.listType = c;
        }
        
        public List<T> getList() {            
            List<T> ret = null;
            try {
                ret = (List<T>) listType.newInstance();
            } catch (IllegalAccessException bad) {
                ErrorService.error(bad);
            } catch (InstantiationException bad) {
                ErrorService.error(bad);
            }
            return ret;
        }
    }
    
    private static class ArrayCreator<T> implements ListCreator<T> {
        public List<T> getList() {
            return new ArrayList<T>();
        }
    }

    private static class LinkedCreator<T> implements ListCreator<T> {
        public List<T> getList() {
            return new LinkedList<T>();
        }
    }
}
