package com.limegroup.gnutella.handshaking;

/** Outlines all the states an asynchronous handshake can be in. */
class HandshakeState {
    private HandshakeState() {}
    
    
    static int INCOMING_READING_REQUEST = 1;
    static int INCOMING_WRITING_RESPONSE = 2;
    static int INCOMING_READING_RESPONSE = 3;
    static int OUTGOING_WRITING_REQUEST = 4;
    static int OUTGOING_READING_RESPONSE = 5;
    static int OUTGOING_WRITING_RESPONSE = 6;
    static int DONE = 7;
    
    static int nextState(int current) {
        if(current == 3 || current == 6)
            return DONE;
        if(current == 7)
            throw new IllegalArgumentException("no next state");
        
        return current++;
    }
    
    static int startIncoming() {
        return INCOMING_READING_REQUEST;
    }
    
    static int startOutgoing() {
        return OUTGOING_WRITING_REQUEST;
    }

}
