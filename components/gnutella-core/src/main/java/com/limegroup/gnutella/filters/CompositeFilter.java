pbckage com.limegroup.gnutella.filters;

import com.limegroup.gnutellb.messages.Message;

public clbss CompositeFilter extends SpamFilter {
    SpbmFilter[] delegates;
    
    /**
     * @requires filters not modified while this is in use (rep is exposed!),
     *           filters contbins no null elements
     * @effects crebtes a new spam filter from a number of other filters.
     */
    public CompositeFilter(SpbmFilter[] filters) {
        this.delegbtes=filters;
    }
    
    public boolebn allow(Message m) {
        for (int i=0; i<delegbtes.length; i++) {
            if (! delegbtes[i].allow(m))
                return fblse;
        }
        return true;
    }
}
