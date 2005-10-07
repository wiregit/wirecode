package com.limegroup.gnutella.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import com.limegroup.gnutella.ErrorService;

public class CoWList implements List {
    
    public static final Class ARRAY_LIST = ArrayList.class;
    public static final Class LINKED_LIST = LinkedList.class;
    
    private volatile List l;
    
    private final Class listType;
    
    public CoWList(List l) {
        this.l = l;
        this.listType = l.getClass();
    }
    
    public CoWList(Class listType) {
        this.listType = listType;
        l = newInstance();
    }
    
    private List newInstance() {
        List ret = null;
        try {
            ret = (List) listType.newInstance();
        } catch (IllegalAccessException bad) {
            ErrorService.error(bad);
        } catch (InstantiationException bad) {
            ErrorService.error(bad);
        }
        
        if (l != null)
            ret.addAll(l);
        
        return ret;
    }

    public synchronized void add(int arg0, Object arg1) {
        List newList = newInstance();
        newList.add(arg0, arg1);
        l = newList;
    }

    public synchronized boolean add(Object arg0) {
        List newList = newInstance();
        boolean ret = newList.add(arg0);
        l = newList;
        return ret;
    }

    public synchronized boolean addAll(Collection arg0) {
        List newList = newInstance();
        boolean ret = newList.addAll(arg0);
        l = newList;
        return ret;
    }

    public synchronized boolean addAll(int arg0, Collection arg1) {
        List newList = newInstance();
        boolean ret = newList.addAll(arg0,arg1);
        l = newList;
        return ret;
    }

    public synchronized void clear() {
        List newList = newInstance();
        newList.clear();
        l = newList;
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

    public synchronized Object remove(int index) {
        Object ret = null;
        List newList = newInstance();
        ret = newList.remove(index);
        l = newList;
        return ret;
    }

    public synchronized boolean remove(Object o) {
        List newList = newInstance();
        boolean ret = newList.remove(o);
        l = newList;
        return ret;    
    }

    public synchronized boolean removeAll(Collection arg0) {
        List newList = newInstance();
        boolean ret = newList.removeAll(arg0);
        l = newList;
        return ret;    
    }

    public synchronized boolean retainAll(Collection arg0) {
        List newList = newInstance();
        boolean ret = newList.retainAll(arg0);
        l = newList;
        return ret;
    }

    public synchronized Object set(int arg0, Object arg1) {
        List newList = newInstance();
        Object ret = newList.set(arg0,arg1);
        l = newList;
        return ret;
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

}
