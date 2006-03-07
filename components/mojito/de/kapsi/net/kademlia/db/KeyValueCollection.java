/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.db;

import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.settings.DatabaseSettings;
import de.kapsi.net.kademlia.util.FixedSizeHashMap;

public class KeyValueCollection implements Collection {
    
    private static final Log LOG = LogFactory.getLog(KeyValueCollection.class);
    
    private Context context;
    private KUID key;
    
    private PublicKey masterKey;
    private FixedSizeHashMap values;
    
    private boolean locked = false;
    
    public KeyValueCollection(Context context, KUID key, int maxSize) {
        this.context = context;
        this.key = key;
        
        masterKey = context.getMasterKey();
        values = new FixedSizeHashMap(Math.min(maxSize, 10), maxSize);
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
            
            if (isLocked() && !isTrustworthy) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("KeyValueCollection for " + key + " is locked and the new KeyValue is not trustworthy");
                }
                return false;
            }
            
            if (isAheadOfTime(keyValue) && !isTrustworthy) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("KeyValue " + key + " is ahead of time and the KeyValue is not trustworthy");
                }
                return false;
            }
            
            PublicKey pubKey = keyValue.getPublicKey();
            KeyValue current = (KeyValue)values.get(pubKey);
            
            if (current == null 
                    && isFull() 
                    && !isTrustworthy) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Cannot store " + key + " as Collection is full and the KeyValue is not trustworthy");
                }
                return false;
            }
            
            if (current == null 
                    || isTrustworthy
                    || current.verify(pubKey)) { // old and new Value are from the same orignator
                
                if (isTrustworthy && !keyValue.isStandard()) {
                    if (keyValue.isClear()) {
                        values.clear();
                    }
                    
                    locked = keyValue.isLock();
                }
                
                /*if (isDuplicate(current, keyValue)) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Dropping duplicate KeyValue " + key);
                    }
                    return false;
                }*/
                
                if (LOG.isTraceEnabled()) {
                    if (current != null) {
                        LOG.trace("Updating KeyValue " + current + " with " + keyValue);
                    } else {
                        LOG.trace("Adding KeyValue " + keyValue);
                    }
                }
                
                if (keyValue.getUpdateTime() > 0L) {
                    keyValue.setUpdateTime(System.currentTimeMillis());
                }
                
                // The Publisher has a handle to the OLD
                // value. Make sure we're not publishing it!
                if (current != null) {
                    current.setUpdateTime(Long.MAX_VALUE);
                }
                
                values.put(pubKey, keyValue);
                return true;
            }
            
            if (LOG.isTraceEnabled()) {
                LOG.trace("Could not add KeyValue " + keyValue);
            }
            return false;
            
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (SignatureException e) {
            throw new RuntimeException(e);
        }
    }
    
    public boolean contains(Object value) {
        PublicKey pubKey = ((KeyValue)value).getPublicKey();
        KeyValue current = (KeyValue)values.get(pubKey);
        if (current == null) {
            return false;
        }
        return current.equals(value);
    }
    
    public void clear() {
        values.clear();
        locked = false;
    }
    
    public boolean remove(Object value) {
        PublicKey pubKey = ((KeyValue)value).getPublicKey();
        KeyValue current = (KeyValue)values.remove(pubKey);
        if (current == null) {
            return false;
        }
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Removed KeyValue " + current);
        }
        
        // Stays locked until empty
        if (locked) {
            locked = !values.isEmpty();
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

    public boolean isLocked() {
        return locked;
    }
    
    private boolean isTrustworthy(KeyValue keyValue) 
            throws SignatureException, InvalidKeyException {
        return masterKey != null && keyValue.verify(masterKey);
    }
    
    private boolean isAheadOfTime(KeyValue keyValue) {
        return keyValue.getCreationTime() >= System.currentTimeMillis() + DatabaseSettings.MILLIS_PER_HOUR;
    }
    
    private boolean isDuplicate(KeyValue current, KeyValue keyValue) {
        if (current == null 
                || keyValue.getUpdateTime() <= 0L
                || !Arrays.equals(current.getSignature(), keyValue.getSignature())) {
            return false;
        }
        
        return keyValue.getUpdateTime()-current.getUpdateTime() < DatabaseSettings.REPUBLISH_INTERVAL;
    }
    
    public String toString() {
        return key + "={" + values.values() + "}";
    }
}
