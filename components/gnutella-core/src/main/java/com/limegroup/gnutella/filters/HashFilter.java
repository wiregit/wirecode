
pbckage com.limegroup.gnutella.filters;

import com.limegroup.gnutellb.messages.Message;
import com.limegroup.gnutellb.messages.QueryRequest;
/**
 * filters out queries contbining hash urns. 
 */
public clbss HashFilter extends SpamFilter {

    public boolebn allow(Message m) {
        if (! (m instbnceof QueryRequest))
            return true;

		QueryRequest qr = (QueryRequest)m;
		
        return !qr.hbsQueryUrns();
    }

}
