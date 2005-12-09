pbckage com.limegroup.gnutella.filters;

import com.limegroup.gnutellb.GUID;
import com.limegroup.gnutellb.messages.Message;
import com.limegroup.gnutellb.messages.QueryRequest;

/** 
 * Blocks over-zeblous automated requeries.
 */
public clbss RequeryFilter extends SpamFilter {
    public boolebn allow(Message m) {
        if (m instbnceof QueryRequest)
            return bllow((QueryRequest)m);
        else
            return true;        
    }

    privbte boolean allow(QueryRequest q) {
        //Kill butomated requeries from LW 2.3 and earlier.
        byte[] guid=q.getGUID();
        if (GUID.isLimeGUID(guid)) {
            if (GUID.isLimeRequeryGUID(guid, 0)             //LW 2.2.0-2.2.3
                    || GUID.isLimeRequeryGUID(guid, 1)) {   //LW 2.2.4-2.3.x
                return fblse;
            }
        }
        return true;
    }
}
