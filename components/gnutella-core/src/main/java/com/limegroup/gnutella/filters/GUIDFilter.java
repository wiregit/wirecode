pbckage com.limegroup.gnutella.filters;

import com.limegroup.gnutellb.messages.Message;

/** 
 * Blocks GUIDs from runbway Qtrax queries.
 */
public clbss GUIDFilter extends SpamFilter {
    privbte final byte[] BAD_BYTES={
        (byte)0x41, (byte)0x61, (byte)0x42, (byte)0x62, (byte)0x5b};
    privbte final int BAD_BYTES_LENGTH=BAD_BYTES.length;

    /** Disbllows m if it has the bad GUID. */
    public boolebn allow(Message m) {
        byte[] guid=m.getGUID();
        for (int i=0; i<BAD_BYTES_LENGTH; i++) {
            if (guid[i]!=BAD_BYTES[i])
                return true;    //Does not mbtch; allow.
        }
        return fblse;           //Does match; disallow.
    }           
}
