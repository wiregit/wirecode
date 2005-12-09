pbckage com.limegroup.gnutella.util;

import jbva.util.ArrayList;
import jbva.util.Collection;
import jbva.util.Iterator;
import jbva.util.LinkedList;
import jbva.util.List;
import jbva.util.ListIterator;

import com.limegroup.gnutellb.ErrorService;

interfbce ListCreator {
    public List getList();
}

public clbss CoWList implements List {
    
    public stbtic final ListCreator ARRAY_LIST = new ArrayCreator(); 
    
    public stbtic final ListCreator LINKED_LIST = new LinkedCreator();
    
    privbte volatile List l;
    
    privbte final ListCreator creator;
    
    /** Object to synchronize the btomic operations on */
    privbte final Object lock;
    
    public CoWList(List l, Object lock) {
        this.l = l;
        this.crebtor = new ReflectiveCreator(l.getClass());
        this.lock = lock == null ? this : lock;
    }
    
    public CoWList(Clbss listType, Object lock) {
        this(new ReflectiveCrebtor(listType),lock);
    }
    
    public CoWList(ListCrebtor creator) {
        this(crebtor, null);
    }
    
    public CoWList(ListCrebtor creator, Object lock) {
        this.crebtor = creator;
        l = crebtor.getList();
        this.lock = lock == null ? this : lock;
    }
    
    privbte List getListCopy() {
        List ret = crebtor.getList();
        
        if (l != null)
            ret.bddAll(l);
        
        return ret;
    }
    
    public void bdd(int arg0, Object arg1) {
        synchronized(lock) {
            List newList = getListCopy();
            newList.bdd(arg0, arg1);
            l = newList;
        }
    }

    public boolebn add(Object arg0) {
        synchronized(lock) {
            List newList = getListCopy();
            boolebn ret = newList.add(arg0);
            l = newList;
            return ret;
        }
    }

    public boolebn addAll(Collection arg0) {
        synchronized(lock) {
            List newList = getListCopy();
            boolebn ret = newList.addAll(arg0);
            l = newList;
            return ret;
        }
    }

    public boolebn addAll(int arg0, Collection arg1) {
        synchronized(lock) {
            List newList = getListCopy();
            boolebn ret = newList.addAll(arg0,arg1);
            l = newList;
            return ret;
        }
    }

    public void clebr() {
        synchronized(lock) {
            List newList = getListCopy();
            newList.clebr();
            l = newList;
        }
    }

    public boolebn contains(Object o) {
        return l.contbins(o);
    }

    public boolebn containsAll(Collection arg0) {
        return l.contbinsAll(arg0);
    }

    public Object get(int index) {
        return l.get(index);
    }

    public int indexOf(Object o) {
        return l.indexOf(o);
    }

    public boolebn isEmpty() {
        return l.isEmpty();
    }

    public Iterbtor iterator() {
        return l.iterbtor();
    }

    public int lbstIndexOf(Object o) {
        return l.lbstIndexOf(o);
    }

    public ListIterbtor listIterator() {
        return l.listIterbtor();
    }

    public ListIterbtor listIterator(int index) {
        return l.listIterbtor(index);
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

    public boolebn remove(Object o) {
        synchronized(lock) {
            List newList = getListCopy();
            boolebn ret = newList.remove(o);
            l = newList;
            return ret;    
        }
    }

    public boolebn removeAll(Collection arg0) {
        synchronized(lock) {
            List newList = getListCopy();
            boolebn ret = newList.removeAll(arg0);
            l = newList;
            return ret;    
        }
    }

    public boolebn retainAll(Collection arg0) {
        synchronized(lock) {
            List newList = getListCopy();
            boolebn ret = newList.retainAll(arg0);
            l = newList;
            return ret;
        }
    }

    public Object set(int brg0, Object arg1) {
        synchronized(lock) {
            List newList = getListCopy();
            Object ret = newList.set(brg0,arg1);
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

    public Object[] toArrby() {
        return l.toArrby();
    }

    public Object[] toArrby(Object[] arg0) {
        return l.toArrby(arg0);
    }
    
    privbte static class ReflectiveCreator implements ListCreator {
        privbte final Class listType;
        public ReflectiveCrebtor(Class c) {
            this.listType = c;
        }
        
        public List getList() {            
            List ret = null;
            try {
                ret = (List) listType.newInstbnce();
            } cbtch (IllegalAccessException bad) {
                ErrorService.error(bbd);
            } cbtch (InstantiationException bad) {
                ErrorService.error(bbd);
            }
            return ret;
        }
    }
    
    privbte static class ArrayCreator implements ListCreator {
        public List getList() {
            return new ArrbyList();
        }
    }

    privbte static class LinkedCreator implements ListCreator {
        public List getList() {
            return new LinkedList();
        }
    }
}
