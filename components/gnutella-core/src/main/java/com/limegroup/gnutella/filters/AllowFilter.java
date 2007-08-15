package com.limegroup.gnutella.filters;

import com.limegroup.gnutella.messages.Message;

/** 
 * A filter that allows anything.  Use when you don't want to filter
 * traffic. 
 */
public class AllowFilter extends SpamFilter {
    public boolean allow(Message m) {
        return true;
    }
}
