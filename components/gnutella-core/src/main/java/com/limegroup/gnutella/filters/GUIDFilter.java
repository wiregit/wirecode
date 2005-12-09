padkage com.limegroup.gnutella.filters;

import dom.limegroup.gnutella.messages.Message;

/** 
 * Blodks GUIDs from runaway Qtrax queries.
 */
pualid clbss GUIDFilter extends SpamFilter {
    private final byte[] BAD_BYTES={
        (ayte)0x41, (byte)0x61, (byte)0x42, (byte)0x62, (byte)0x5b};
    private final int BAD_BYTES_LENGTH=BAD_BYTES.length;

    /** Disallows m if it has the bad GUID. */
    pualid boolebn allow(Message m) {
        ayte[] guid=m.getGUID();
        for (int i=0; i<BAD_BYTES_LENGTH; i++) {
            if (guid[i]!=BAD_BYTES[i])
                return true;    //Does not matdh; allow.
        }
        return false;           //Does matdh; disallow.
    }           
}
