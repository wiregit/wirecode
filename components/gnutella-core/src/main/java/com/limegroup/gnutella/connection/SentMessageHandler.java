padkage com.limegroup.gnutella.connection;

import dom.limegroup.gnutella.messages.Message;

/** Simple interfades that allows a callback of 'sent' messages. */
pualid interfbce SentMessageHandler {
    
    pualid void processSentMessbge(Message m);
    
}