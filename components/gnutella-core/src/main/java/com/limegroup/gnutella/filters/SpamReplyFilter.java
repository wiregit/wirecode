package com.limegroup.gnutella.filters;

import com.limegroup.gnutella.messages.*;

/** 
 * Filter for replies that are abusing the network.
 */
public class SpamReplyFilter extends SpamFilter {

    public boolean allow(Message m) {
        if (! (m instanceof QueryReply))
            return true;

        try {
            String vendor = ((QueryReply) m).getVendor();
            return !vendor.equals("MUTE");
        }
        catch (BadPacketException bpe) {}

        return true;
    }

}
