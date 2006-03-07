/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.messages.response;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.messages.ResponseMessage;

public abstract class LookupResponse extends ResponseMessage 
        implements Collection {
    
    protected final Collection values;
    
    public LookupResponse(int vendor, int version, KUID nodeId,
            KUID messageId, Collection responseValues) {
        super(vendor, version, nodeId, messageId);
        
        this.values = Collections.unmodifiableCollection(responseValues);
    }
    
    public abstract boolean isKeyValueResponse();

    public boolean add(Object o) {
        return values.add(o);
    }

    public boolean addAll(Collection c) {
        return values.addAll(c);
    }

    public void clear() {
        values.clear();
    }

    public boolean contains(Object o) {
        return values.contains(o);
    }

    public boolean containsAll(Collection c) {
        return values.containsAll(c);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public Iterator iterator() {
        return values.iterator();
    }

    public boolean remove(Object o) {
        return values.remove(o);
    }

    public boolean removeAll(Collection c) {
        return values.removeAll(c);
    }

    public boolean retainAll(Collection c) {
        return values.retainAll(c);
    }

    public int size() {
        return values.size();
    }

    public Object[] toArray() {
        return values.toArray();
    }

    public Object[] toArray(Object[] a) {
        return values.toArray(a);
    }
    
    public String toString() {
        return values.toString();
    }
}
