package com.limegroup.gnutella.messages;

import com.limegroup.gnutella.Assert;
import com.sun.java.util.collections.*;
import java.io.*;

/** Code useful for GGEP usage.
 *  Contains static instances of different GGEP blocks.
 */
public class GGEPUtil {


    /** The standard GGEP block for LimeWire.  Currently has no keys.
     */
    private static byte[] standardGGEP = new byte[0];
    
    /** A GGEP block that has the 'Browse Host' extension.  Useful for Query
     *  Replies.
     */
    private static byte[] qrGGEP = null;

    static {
        ByteArrayOutputStream oStream = new ByteArrayOutputStream();

        // the standard GGEP has nothing.
        try {
            GGEP standard = new GGEP(false);
            standard.write(oStream);
            standardGGEP = oStream.toByteArray();
        }
        catch (IOException writeError) {
        }

        // a GGEP block with BHOST
        oStream.reset();
        try {
            GGEP bhost = new GGEP(false);
            bhost.put(GGEP.GGEP_HEADER_BROWSE_HOST);
            bhost.write(oStream);
            qrGGEP = oStream.toByteArray();
        }
        catch (IOException writeError) {
        }
        Assert.that(qrGGEP != null);
    }

    /** @return The appropriate byte[] corresponding to the GGEP block you
     * desire. 
     */
    public static byte[] getQRGGEP(boolean supportsBH) {
        byte[] retGGEPBlock = standardGGEP;
        if (supportsBH)
            retGGEPBlock = qrGGEP;
        return retGGEPBlock;
    }

    /** @return whether or not browse host support can be inferred from this
     * block of GGEPs.
     */
    public static boolean allowsBrowseHost(GGEP[] ggeps) {
        boolean retBool = false;

        for (int i = 0; 
             (ggeps != null) && (i < ggeps.length) && !retBool; 
             i++) {
            Set headers = ggeps[i].getHeaders();
            retBool = headers.contains(GGEP.GGEP_HEADER_BROWSE_HOST);
        }

        return retBool;
    }

}
