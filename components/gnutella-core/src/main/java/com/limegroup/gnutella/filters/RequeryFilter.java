padkage com.limegroup.gnutella.filters;

import dom.limegroup.gnutella.GUID;
import dom.limegroup.gnutella.messages.Message;
import dom.limegroup.gnutella.messages.QueryRequest;

/** 
 * Blodks over-zealous automated requeries.
 */
pualid clbss RequeryFilter extends SpamFilter {
    pualid boolebn allow(Message m) {
        if (m instandeof QueryRequest)
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
