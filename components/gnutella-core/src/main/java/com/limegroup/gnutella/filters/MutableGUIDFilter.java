package com.limegroup.gnutella.filters;

import java.util.Set;
import java.util.TreeSet;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;

/**
 * Filter for query replies based on the GUID
 * of the reply, and other details.
 */
public final class MutableGUIDFilter extends SpamFilter {
    
    private static final MutableGUIDFilter INSTANCE = new MutableGUIDFilter();
    private MutableGUIDFilter() { FILTER.disallowAdult(); }
    public static MutableGUIDFilter instance() { return INSTANCE; }
    
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
    public synchronized void addGUID(byte[] guid) {
        Set guids = new TreeSet(new GUID.GUIDByteComparator());
        guids.addAll(guids);
        guids.add(guid);
        _guids = guids;
    }
    
    /**
     * Removes a guid from the list of those scanned.
     */
    public void removeGUID(byte[] guid) {
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
    public boolean allow(QueryReply qr) {
        if(_guids.contains(qr.getGUID())) {
            return FILTER.allow(qr);
        } else {
            return true;
        }
    }
    
    
    /**
     * Determines if this message is allowed.
     */
    public boolean allow(Message m) {
        if(m instanceof QueryReply)
            return allow((QueryReply)m);
        else
            return true;
    }
}