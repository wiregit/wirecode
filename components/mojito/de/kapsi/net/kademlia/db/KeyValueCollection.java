/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.db;

import java.io.Serializable;
import java.net.SocketAddress;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.util.FixedSizeHashMap;

public class KeyValueCollection implements Collection, Serializable {
    
    private static final long serialVersionUID = 1992547869696103652L;

    private static final Log LOG = LogFactory.getLog(KeyValueCollection.class);
    
    private Context context;
    private PublicKey masterKey;
    
    private KUID key;
    private FixedSizeHashMap values;

    public KeyValueCollection(Context context, KUID key, int maxSize) {
        setContext(context);
        
        this.key = key;
        values = new FixedSizeHashMap(Math.min(maxSize, 10), maxSize);
    }
    
    public void setContext(Context context) {
        this.context = context;
        masterKey = context.getMasterKey();
    }
    
    public KUID getKey() {
        return key;
    }
    
    public int size() {
        return values.size();
    }
    
    public boolean isEmpty() {
        return values.isEmpty();
    }
    
    public boolean isFull() {
        return values.isFull();
    }
    
    public boolean add(Object value) {
        KeyValue keyValue = (KeyValue)value;
        
        try {
            boolean isTrustworthy = isTrustworthy(keyValue);
            boolean isLocalKeyValue = keyValue.isLocalKeyValue();
            
            KUID nodeId = keyValue.getNodeID();
            KeyValue current = (KeyValue)values.get(nodeId);
            
            if (current != null 
                    && (!isTrustworthy || !isLocalKeyValue)) {
                
                SocketAddress currentSrc = current.getSocketAddress();
                if (currentSrc != null 
                        && !currentSrc.equals(keyValue.getSocketAddress())) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Cannot replace " + current + " with " + keyValue);
                    }
                    return false;
                }
            }
            
            if (current != null) {
                
                if (!isTrustworthy && isTrustworthy(current)) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Cannot replace " + current + " with " + keyValue);
                    }
                    return false;
                }
                
                SocketAddress currentSrc = current.getSocketAddress();
                if (currentSrc != null 
                        && !currentSrc.equals(keyValue.getSocketAddress())
                        ) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Cannot replace " + current + " with " + keyValue);
                    }
                    return false;
                }
            }
            
            
            if (isFull() && current == null 
                    && (!isTrustworthy || !isLocalKeyValue)) {
                
                if (LOG.isTraceEnabled()) {
                    LOG.trace("KeyValueCollection is full and " + keyValue 
                            + " is neither trustworthy nor a local KeyValue");
                }
                return false;
            }
            
            if (LOG.isTraceEnabled()) {
                if (current != null) {
                    LOG.trace("Updating KeyValue " + current + " with " + keyValue);
                } else {
                    LOG.trace("Adding KeyValue " + keyValue);
                }
            }
            
            values.put(nodeId, keyValue);
            return true;
            
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (SignatureException e) {
            throw new RuntimeException(e);
        }
    }
    
    public boolean contains(Object value) {
        KUID nodeId = ((KeyValue)value).getNodeID();
        KeyValue current = (KeyValue)values.get(nodeId);
        if (current == null) {
            return false;
        }
        return current.equals(value);
    }
    
    public void clear() {
        values.clear();
    }
    
    public boolean remove(Object value) {
        KUID nodeId = ((KeyValue)value).getNodeID();
        KeyValue current = (KeyValue)values.remove(nodeId);
        if (current == null) {
            return false;
        }
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Removed KeyValue " + current);
        }
        
        return true;
    }
    
    public boolean addAll(Collection c) {
        boolean changed = false;
        for(Iterator it = c.iterator(); it.hasNext(); ) {
            if (add(it.next())) {
                changed = true;
            }
        }
        return changed;
    }

    public boolean containsAll(Collection c) {
        for(Iterator it = c.iterator(); it.hasNext(); ) {
            if (!contains(it.next())) {
                return false;
            }
        }
        return true;
    }

    public Iterator iterator() {
        return values.values().iterator();
    }

    public boolean removeAll(Collection c) {
        boolean changed = false;
        for(Iterator it = c.iterator(); it.hasNext(); ) {
            if (remove(it.next())) {
                changed = true;
            }
        }
        return changed;
    }

    public boolean retainAll(Collection c) {
        boolean changed = false;
        for(Iterator it = iterator(); it.hasNext(); ) {
            if (!c.contains(it.next())) {
                it.remove();
                changed = true;
            }
        }
        return changed;
    }

    public Object[] toArray() {
        return values.values().toArray();
    }

    public Object[] toArray(Object[] a) {
        return values.values().toArray(a);
    }
    
    private boolean isTrustworthy(KeyValue keyValue) 
            throws SignatureException, InvalidKeyException {
        return masterKey != null && keyValue.verify(masterKey);
    }
    
    public String toString() {
        return key + "={" + values.values() + "}";
    }
}
