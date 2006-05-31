package com.limegroup.gnutella.handshaking;

public class HandshakeStatus {

    private final String msg;
    private final boolean ok;
    
    private HandshakeStatus(String msg, boolean ok) {
        this.msg = msg;
        this.ok = ok;
    }
    
    private HandshakeStatus(String msg) {
        this(msg, false);
    }
    
    /** The only good status. */
    public static final HandshakeStatus OK = new HandshakeStatus("OK", true);
    
    /* All bad statuses. */
    public static final HandshakeStatus NO_X_ULTRAPEER = new HandshakeStatus("No X-Ultrapeer.");
    public static final HandshakeStatus DISCONNECTED = new HandshakeStatus("I am disconnected");
    public static final HandshakeStatus WE_ARE_LEAVES = new HandshakeStatus("You and I are leaves");
    public static final HandshakeStatus NOT_GOOD_UP = new HandshakeStatus("You are not a 'good' Ultrapeer.");
    public static final HandshakeStatus IDLE_LIMEWIRE = new HandshakeStatus("I am idle and want a LimeWire");
    public static final HandshakeStatus STARTING_LIMEWIRE = new HandshakeStatus("I am just starting and want a LimeWire");
    public static final HandshakeStatus TOO_MANY_UPS = new HandshakeStatus("I have too many ultrapeer connections");
    public static final HandshakeStatus NOT_ALLOWED_LEAF = new HandshakeStatus("This leaf connection not allowed");
    public static final HandshakeStatus NOT_GOOD_LEAF = new HandshakeStatus("You are not a 'good' leaf.");
    public static final HandshakeStatus TOO_MANY_LEAF = new HandshakeStatus("I have too many leaf connections");
    public static final HandshakeStatus NOT_ALLOWED_UP = new HandshakeStatus("This ultrapeer connection not allowed");
    public static final HandshakeStatus NON_LIME_RATIO = new HandshakeStatus("Non-LimeWire peer ratio exceeded.");
    public static final HandshakeStatus NO_LIME_SLOTS = new HandshakeStatus("No LimeWire slots available");
    public static final HandshakeStatus NO_HEADERS = new HandshakeStatus("No headers received.");
    public static final HandshakeStatus UNKNOWN = new HandshakeStatus("Unknown connection failure");
    
    public String getMessage() {
        return msg;
    }
    
    public boolean isAcceptable() {
        return ok;
    }
    

}
