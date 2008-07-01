package org.jivesoftware.smackx.jingle.states;

import org.jivesoftware.smackx.packet.Jingle;
import org.jivesoftware.smack.XMPPException;

public interface State {
    State dispatch(Jingle jin) throws XMPPException;
}
