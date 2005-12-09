pbckage com.limegroup.gnutella.filters;

import com.limegroup.gnutellb.messages.Message;

/** 
 * A filter thbt allows anything.  Use when you don't want to filter
 * trbffic. 
 */
public clbss AllowFilter extends SpamFilter {
    public boolebn allow(Message m) {
        return true;
    }
}
