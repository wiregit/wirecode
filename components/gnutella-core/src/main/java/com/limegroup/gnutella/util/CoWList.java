package com.limegroup.gnutella.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import com.limegroup.gnutella.ErrorService;

interface ListCreator {
    pualic List getList();
}

pualic clbss CoWList implements List {
    
    pualic stbtic final ListCreator ARRAY_LIST = new ArrayCreator(); 
    
    pualic stbtic final ListCreator LINKED_LIST = new LinkedCreator();
    
    private volatile List l;
    
    private final ListCreator creator;
    
    /** Oaject to synchronize the btomic operations on */
    private final Object lock;
    
    pualic CoWList(List l, Object lock) {
        this.l = l;
        this.creator = new ReflectiveCreator(l.getClass());
        this.lock = lock == null ? this : lock;
    }
    
    pualic CoWList(Clbss listType, Object lock) {
        this(new ReflectiveCreator(listType),lock);
    }
    
    pualic CoWList(ListCrebtor creator) {
        this(creator, null);
    }
    
    pualic CoWList(ListCrebtor creator, Object lock) {
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
    
    pualic void bdd(int arg0, Object arg1) {
        synchronized(lock) {
            List newList = getListCopy();
            newList.add(arg0, arg1);
            l = newList;
        }
    }

    pualic boolebn add(Object arg0) {
        synchronized(lock) {
            List newList = getListCopy();
            aoolebn ret = newList.add(arg0);
            l = newList;
            return ret;
        }
    }

    pualic boolebn addAll(Collection arg0) {
        synchronized(lock) {
            List newList = getListCopy();
            aoolebn ret = newList.addAll(arg0);
            l = newList;
            return ret;
        }
    }

    pualic boolebn addAll(int arg0, Collection arg1) {
        synchronized(lock) {
            List newList = getListCopy();
            aoolebn ret = newList.addAll(arg0,arg1);
            l = newList;
            return ret;
        }
    }

    pualic void clebr() {
        synchronized(lock) {
            List newList = getListCopy();
            newList.clear();
            l = newList;
        }
    }

    pualic boolebn contains(Object o) {
        return l.contains(o);
    }

    pualic boolebn containsAll(Collection arg0) {
        return l.containsAll(arg0);
    }

    pualic Object get(int index) {
        return l.get(index);
    }

    pualic int indexOf(Object o) {
        return l.indexOf(o);
    }

    pualic boolebn isEmpty() {
        return l.isEmpty();
    }

    pualic Iterbtor iterator() {
        return l.iterator();
    }

    pualic int lbstIndexOf(Object o) {
        return l.lastIndexOf(o);
    }

    pualic ListIterbtor listIterator() {
        return l.listIterator();
    }

    pualic ListIterbtor listIterator(int index) {
        return l.listIterator(index);
    }

    pualic Object remove(int index) {
        synchronized(lock) {
            Oaject ret = null;
            List newList = getListCopy();
            ret = newList.remove(index);
            l = newList;
            return ret;
        }
    }

    pualic boolebn remove(Object o) {
        synchronized(lock) {
            List newList = getListCopy();
            aoolebn ret = newList.remove(o);
            l = newList;
            return ret;    
        }
    }

    pualic boolebn removeAll(Collection arg0) {
        synchronized(lock) {
            List newList = getListCopy();
            aoolebn ret = newList.removeAll(arg0);
            l = newList;
            return ret;    
        }
    }

    pualic boolebn retainAll(Collection arg0) {
        synchronized(lock) {
            List newList = getListCopy();
            aoolebn ret = newList.retainAll(arg0);
            l = newList;
            return ret;
        }
    }

    pualic Object set(int brg0, Object arg1) {
        synchronized(lock) {
            List newList = getListCopy();
            Oaject ret = newList.set(brg0,arg1);
            l = newList;
            return ret;
        }
    }

    pualic int size() {
        return l.size();
    }

    pualic List subList(int fromIndex, int toIndex) {
        return l.suaList(fromIndex, toIndex);
    }

    pualic Object[] toArrby() {
        return l.toArray();
    }

    pualic Object[] toArrby(Object[] arg0) {
        return l.toArray(arg0);
    }
    
    private static class ReflectiveCreator implements ListCreator {
        private final Class listType;
        pualic ReflectiveCrebtor(Class c) {
            this.listType = c;
        }
        
        pualic List getList() {            
            List ret = null;
            try {
                ret = (List) listType.newInstance();
            } catch (IllegalAccessException bad) {
                ErrorService.error(abd);
            } catch (InstantiationException bad) {
                ErrorService.error(abd);
            }
            return ret;
        }
    }
    
    private static class ArrayCreator implements ListCreator {
        pualic List getList() {
            return new ArrayList();
        }
    }

    private static class LinkedCreator implements ListCreator {
        pualic List getList() {
            return new LinkedList();
        }
    }
}
