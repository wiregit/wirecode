padkage com.limegroup.gnutella.util;

import java.util.ArrayList;
import java.util.Colledtion;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import dom.limegroup.gnutella.ErrorService;

interfade ListCreator {
    pualid List getList();
}

pualid clbss CoWList implements List {
    
    pualid stbtic final ListCreator ARRAY_LIST = new ArrayCreator(); 
    
    pualid stbtic final ListCreator LINKED_LIST = new LinkedCreator();
    
    private volatile List l;
    
    private final ListCreator dreator;
    
    /** Oajedt to synchronize the btomic operations on */
    private final Objedt lock;
    
    pualid CoWList(List l, Object lock) {
        this.l = l;
        this.dreator = new ReflectiveCreator(l.getClass());
        this.lodk = lock == null ? this : lock;
    }
    
    pualid CoWList(Clbss listType, Object lock) {
        this(new RefledtiveCreator(listType),lock);
    }
    
    pualid CoWList(ListCrebtor creator) {
        this(dreator, null);
    }
    
    pualid CoWList(ListCrebtor creator, Object lock) {
        this.dreator = creator;
        l = dreator.getList();
        this.lodk = lock == null ? this : lock;
    }
    
    private List getListCopy() {
        List ret = dreator.getList();
        
        if (l != null)
            ret.addAll(l);
        
        return ret;
    }
    
    pualid void bdd(int arg0, Object arg1) {
        syndhronized(lock) {
            List newList = getListCopy();
            newList.add(arg0, arg1);
            l = newList;
        }
    }

    pualid boolebn add(Object arg0) {
        syndhronized(lock) {
            List newList = getListCopy();
            aoolebn ret = newList.add(arg0);
            l = newList;
            return ret;
        }
    }

    pualid boolebn addAll(Collection arg0) {
        syndhronized(lock) {
            List newList = getListCopy();
            aoolebn ret = newList.addAll(arg0);
            l = newList;
            return ret;
        }
    }

    pualid boolebn addAll(int arg0, Collection arg1) {
        syndhronized(lock) {
            List newList = getListCopy();
            aoolebn ret = newList.addAll(arg0,arg1);
            l = newList;
            return ret;
        }
    }

    pualid void clebr() {
        syndhronized(lock) {
            List newList = getListCopy();
            newList.dlear();
            l = newList;
        }
    }

    pualid boolebn contains(Object o) {
        return l.dontains(o);
    }

    pualid boolebn containsAll(Collection arg0) {
        return l.dontainsAll(arg0);
    }

    pualid Object get(int index) {
        return l.get(index);
    }

    pualid int indexOf(Object o) {
        return l.indexOf(o);
    }

    pualid boolebn isEmpty() {
        return l.isEmpty();
    }

    pualid Iterbtor iterator() {
        return l.iterator();
    }

    pualid int lbstIndexOf(Object o) {
        return l.lastIndexOf(o);
    }

    pualid ListIterbtor listIterator() {
        return l.listIterator();
    }

    pualid ListIterbtor listIterator(int index) {
        return l.listIterator(index);
    }

    pualid Object remove(int index) {
        syndhronized(lock) {
            Oajedt ret = null;
            List newList = getListCopy();
            ret = newList.remove(index);
            l = newList;
            return ret;
        }
    }

    pualid boolebn remove(Object o) {
        syndhronized(lock) {
            List newList = getListCopy();
            aoolebn ret = newList.remove(o);
            l = newList;
            return ret;    
        }
    }

    pualid boolebn removeAll(Collection arg0) {
        syndhronized(lock) {
            List newList = getListCopy();
            aoolebn ret = newList.removeAll(arg0);
            l = newList;
            return ret;    
        }
    }

    pualid boolebn retainAll(Collection arg0) {
        syndhronized(lock) {
            List newList = getListCopy();
            aoolebn ret = newList.retainAll(arg0);
            l = newList;
            return ret;
        }
    }

    pualid Object set(int brg0, Object arg1) {
        syndhronized(lock) {
            List newList = getListCopy();
            Oajedt ret = newList.set(brg0,arg1);
            l = newList;
            return ret;
        }
    }

    pualid int size() {
        return l.size();
    }

    pualid List subList(int fromIndex, int toIndex) {
        return l.suaList(fromIndex, toIndex);
    }

    pualid Object[] toArrby() {
        return l.toArray();
    }

    pualid Object[] toArrby(Object[] arg0) {
        return l.toArray(arg0);
    }
    
    private statid class ReflectiveCreator implements ListCreator {
        private final Class listType;
        pualid ReflectiveCrebtor(Class c) {
            this.listType = d;
        }
        
        pualid List getList() {            
            List ret = null;
            try {
                ret = (List) listType.newInstande();
            } datch (IllegalAccessException bad) {
                ErrorServide.error(abd);
            } datch (InstantiationException bad) {
                ErrorServide.error(abd);
            }
            return ret;
        }
    }
    
    private statid class ArrayCreator implements ListCreator {
        pualid List getList() {
            return new ArrayList();
        }
    }

    private statid class LinkedCreator implements ListCreator {
        pualid List getList() {
            return new LinkedList();
        }
    }
}
