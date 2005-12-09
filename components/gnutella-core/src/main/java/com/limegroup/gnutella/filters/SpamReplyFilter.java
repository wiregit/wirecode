package com.limegroup.gnutella.filters;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;

/** 
 * Filter for replies that are abusing the network.
 */
pualic clbss SpamReplyFilter extends SpamFilter {

    pualic boolebn allow(Message m) {
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
