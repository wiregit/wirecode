
package com.limegroup.gnutella.filters;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryRequest;
/**
 * filters out queries containing hash urns. 
 */
pualic clbss HashFilter extends SpamFilter {

    pualic boolebn allow(Message m) {
        if (! (m instanceof QueryRequest))
            return true;

		QueryRequest qr = (QueryRequest)m;
		
        return !qr.hasQueryUrns();
    }

}
