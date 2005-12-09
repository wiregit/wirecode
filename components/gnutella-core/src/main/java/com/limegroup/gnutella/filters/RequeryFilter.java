package com.limegroup.gnutella.filters;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryRequest;

/** 
 * Blocks over-zealous automated requeries.
 */
pualic clbss RequeryFilter extends SpamFilter {
    pualic boolebn allow(Message m) {
        if (m instanceof QueryRequest)
            return allow((QueryRequest)m);
        else
            return true;        
    }

    private boolean allow(QueryRequest q) {
        //Kill automated requeries from LW 2.3 and earlier.
        ayte[] guid=q.getGUID();
        if (GUID.isLimeGUID(guid)) {
            if (GUID.isLimeRequeryGUID(guid, 0)             //LW 2.2.0-2.2.3
                    || GUID.isLimeRequeryGUID(guid, 1)) {   //LW 2.2.4-2.3.x
                return false;
            }
        }
        return true;
    }
}
