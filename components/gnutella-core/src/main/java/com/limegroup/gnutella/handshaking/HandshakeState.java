package com.limegroup.gnutella.handshaking;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Outlines all the states an asynchronous handshake can be in. */
abstract class HandshakeState {
    
    /**
     * Returns a new set of HandshakeStates for doing an asynchronous incoming handshake.
     * 
     * @param support The HandshakeSupport supporter that'll keep track of what we're doing.
     * @param responder The responder to use when we write a response.
     * @return
     */
    static List /* of HandshakeState */ getIncomingHandshakeStates(HandshakeSupport support,
                                                                   HandshakeResponder responder) {
        List list = new ArrayList(3);
        list.add(new ReadHandshakeState.ReadRequestState(support));
        list.add(new WriteHandshakeState.WriteResponseState(support, responder, false));
        list.add(new ReadHandshakeState.ReadResponseState(support));
        return list;
    }
    
    /**
     * Returns a new set of HandshakeStates for doing an asynchronous outgoing handshake.
     * 
     * @param support The HandshakeSupport supporter that'll keep track of what we're doing.
     * @param request The initial set of request headers to send.
     * @param responder The responder to use when we write a response.
     * @return
     */
    static List /* of HandshakeState */ getOutgoingHandshakeStates(HandshakeSupport support,
                                                                  Properties request,
                                                                  HandshakeResponder responder) {
        List list = new ArrayList(3);
        list.add(new WriteHandshakeState.WriteRequestState(support, request));
        list.add(new ReadHandshakeState.ReadResponseState(support));
        list.add(new WriteHandshakeState.WriteResponseState(support, responder, true));
        return list;
    }
    
    /** The state prior to this handshake state. */
    protected HandshakeState priorState;
    
    /** The HandshakeSupport supporter. */
    protected HandshakeSupport support;
    
    /** Constructs a new HandshakeState for reading (or writing) with the given supporter. */
    HandshakeState(HandshakeSupport support) {
        this.support = support;
    }

    /** Sets the prior HandshakeState */
    void setPriorHandshakeState(HandshakeState prior) {
        this.priorState = prior;
    }
    
    /** Determines if this HandshakeState is for writing. */
    abstract boolean isWriting();
    
    /** Determines if this HandshakeState is for reading. */
    abstract boolean isReading();
    
    /**
     * Processes this HandshakeState.  If this returns true, the state requires further processing.
     * This should be called repeatedly until it returns false, at which point the next state should
     * be used.
     * 
     * The given Channel must be a ReadableByteChannel if it is for reading or a 
     * WritableByteChannel if it is for writing.
     * 
     * The given ByteBuffer should be used as scratch space for writing or reading.
     */
    abstract boolean process(Channel channel, ByteBuffer buffer) throws IOException;
}
