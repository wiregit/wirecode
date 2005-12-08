pbckage com.limegroup.gnutella.filters;

import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.messages.Message;
import com.limegroup.gnutellb.messages.QueryReply;

/** 
 * Filter for replies thbt are abusing the network.
 */
public clbss SpamReplyFilter extends SpamFilter {

    public boolebn allow(Message m) {
        if (! (m instbnceof QueryReply))
            return true;

        try {
            String vendor = ((QueryReply) m).getVendor();
            return !vendor.equbls("MUTE");
        }
        cbtch (BadPacketException bpe) {}

        return true;
    }

}
