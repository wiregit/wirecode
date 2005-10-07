package com.limegroup.gnutella.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import com.limegroup.gnutella.ErrorService;

interface ListCreator {
    public List getList();
}

public class CoWList implements List {
    
    public static final ListCreator ARRAY_LIST = new ArrayCreator(); 
    
    public static final ListCreator LINKED_LIST = new LinkedCreator();
    
    private volatile List l;
    
    private final ListCreator creator;
    
    /** Object to synchronize the atomic operations on */
    private final Object lock;
    
    public CoWList(List l, Object lock) {
        this.l = l;
        this.creator = new ReflectiveCreator(l.getClass());
        this.lock = lock == null ? this : lock;
    }
    
    public CoWList(Class listType, Object lock) {
        this(new ReflectiveCreator(listType),lock);
    }
    
    public CoWList(ListCreator creator) {
        this(creator, null);
    }
    
    public CoWList(ListCreator creator, Object lock) {
        this.creator = creator;
        l = creator.getList();
        this.lock = lock == null ? this : lock;
    }
    
    private List getListCopy() {
        List ret = creator.getList();
        
        if (l != null)
            ret.addAll(l);
        
        return ret;
    }
    
    public void add(int arg0, Object arg1) {
        synchronized(lock) {
            List newList = getListCopy();
            newList.add(arg0, arg1);
            l = newList;
        }
    }

    public boolean add(Object arg0) {
        synchronized(lock) {
            List newList = getListCopy();
            boolean ret = newList.add(arg0);
            l = newList;
            return ret;
        }
    }

    public boolean addAll(Collection arg0) {
        synchronized(lock) {
            List newList = getListCopy();
            boolean ret = newList.addAll(arg0);
            l = newList;
            return ret;
        }
    }

    public boolean addAll(int arg0, Collection arg1) {
        synchronized(lock) {
            List newList = getListCopy();
            boolean ret = newList.addAll(arg0,arg1);
            l = newList;
            return ret;
        }
    }

    public void clear() {
        synchronized(lock) {
            List newList = getListCopy();
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

    public Object get(int index) {
        return l.get(index);
    }

    public int indexOf(Object o) {
        return l.indexOf(o);
    }

    public boolean isEmpty() {
        return l.isEmpty();
    }

    public Iterator iterator() {
        return l.iterator();
    }

    public int lastIndexOf(Object o) {
        return l.lastIndexOf(o);
    }

    public ListIterator listIterator() {
        return l.listIterator();
    }

    public ListIterator listIterator(int index) {
        return l.listIterator(index);
    }

    public Object remove(int index) {
        synchronized(lock) {
            Object ret = null;
            List newList = getListCopy();
            ret = newList.remove(index);
            l = newList;
            return ret;
        }
    }

    public boolean remove(Object o) {
        synchronized(lock) {
            List newList = getListCopy();
            boolean ret = newList.remove(o);
            l = newList;
            return ret;    
        }
    }

    public boolean removeAll(Collection arg0) {
        synchronized(lock) {
            List newList = getListCopy();
            boolean ret = newList.removeAll(arg0);
            l = newList;
            return ret;    
        }
    }

    public boolean retainAll(Collection arg0) {
        synchronized(lock) {
            List newList = getListCopy();
            boolean ret = newList.retainAll(arg0);
            l = newList;
            return ret;
        }
    }

    public Object set(int arg0, Object arg1) {
        synchronized(lock) {
            List newList = getListCopy();
            Object ret = newList.set(arg0,arg1);
            l = newList;
            return ret;
        }
    }

    public int size() {
        return l.size();
    }

    public List subList(int fromIndex, int toIndex) {
        return l.subList(fromIndex, toIndex);
    }

    public Object[] toArray() {
        return l.toArray();
    }

    public Object[] toArray(Object[] arg0) {
        return l.toArray(arg0);
    }
    
    private static class ReflectiveCreator implements ListCreator {
        private final Class listType;
        public ReflectiveCreator(Class c) {
            this.listType = c;
        }
        
        public List getList() {            
            List ret = null;
            try {
                ret = (List) listType.newInstance();
            } catch (IllegalAccessException bad) {
                ErrorService.error(bad);
            } catch (InstantiationException bad) {
                ErrorService.error(bad);
            }
            return ret;
        }
    }
    
    private static class ArrayCreator implements ListCreator {
        public List getList() {
            return new ArrayList();
        }
    }

    private static class LinkedCreator implements ListCreator {
        public List getList() {
            return new LinkedList();
        }
    }
}
