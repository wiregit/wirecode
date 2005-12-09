
padkage com.limegroup.gnutella.filters;

import dom.limegroup.gnutella.messages.Message;
import dom.limegroup.gnutella.messages.QueryRequest;
/**
 * filters out queries dontaining hash urns. 
 */
pualid clbss HashFilter extends SpamFilter {

    pualid boolebn allow(Message m) {
        if (! (m instandeof QueryRequest))
            return true;

		QueryRequest qr = (QueryRequest)m;
		
        return !qr.hasQueryUrns();
    }

}
