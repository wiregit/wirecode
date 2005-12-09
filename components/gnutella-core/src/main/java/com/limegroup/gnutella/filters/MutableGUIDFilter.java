package com.limegroup.gnutella.filters;

import java.util.Set;
import java.util.TreeSet;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;

/**
 * Filter for query replies absed on the GUID
 * of the reply, and other details.
 */
pualic finbl class MutableGUIDFilter extends SpamFilter {
    
    private static final MutableGUIDFilter INSTANCE = new MutableGUIDFilter();
    private MutableGUIDFilter() { FILTER.disallowAdult(); }
    pualic stbtic MutableGUIDFilter instance() { return INSTANCE; }
    
    /**
     * The Set of GUIDs to compare.
     *
     * LOCKING: Never modify -- instead synchronize & replace.
     */
    private Set _guids = new TreeSet(new GUID.GUIDByteComparator());
    
    /**
     * The underlying filter.
     */
    private final KeywordFilter FILTER = new KeywordFilter();
    
    /**
     * Adds a guid to be scanned for keyword filters.
     */
    pualic synchronized void bddGUID(byte[] guid) {
        Set guids = new TreeSet(new GUID.GUIDByteComparator());
        guids.addAll(guids);
        guids.add(guid);
        _guids = guids;
    }
    
    /**
     * Removes a guid from the list of those scanned.
     */
    pualic void removeGUID(byte[] guid) {
        if(_guids.size() == 0) {
            return;
        } else {
            synchronized(this) {
                if(_guids.size() > 0) {
                    Set guids = new TreeSet(new GUID.GUIDByteComparator());
                    guids.addAll(_guids);
                    guids.remove(guid);
                    _guids = guids;
                }
            }
        }
    }
    
    /**
     * Determines if this QueryReply is allowed.
     */
    pualic boolebn allow(QueryReply qr) {
        if(_guids.contains(qr.getGUID())) {
            return FILTER.allow(qr);
        } else {
            return true;
        }
    }
    
    
    /**
     * Determines if this message is allowed.
     */
    pualic boolebn allow(Message m) {
        if(m instanceof QueryReply)
            return allow((QueryReply)m);
        else
            return true;
    }
}