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
    public static final HandshakeStatus DISCONNECTED = new HandshakeStatus("I'm Disconnected");
    public static final HandshakeStatus WE_ARE_LEAVES = new HandshakeStatus("We're Leaves");
    public static final HandshakeStatus NOT_GOOD_UP = new HandshakeStatus("Not Good Ultrapeer.");
    public static final HandshakeStatus IDLE_LIMEWIRE = new HandshakeStatus("Idle, Need LimeWire");
    public static final HandshakeStatus STARTING_LIMEWIRE = new HandshakeStatus("Starting, Need LimeWire");
    public static final HandshakeStatus TOO_MANY_UPS = new HandshakeStatus("No Ultrapeer Slots");
    public static final HandshakeStatus NOT_ALLOWED_LEAF = new HandshakeStatus("Leaf Connection Failed");
    public static final HandshakeStatus NOT_GOOD_LEAF = new HandshakeStatus("Not Good Leaf");
    public static final HandshakeStatus TOO_MANY_LEAF = new HandshakeStatus("No Leaf Slots");
    public static final HandshakeStatus NOT_ALLOWED_UP = new HandshakeStatus("Ultrapeer Connection Failed");
    public static final HandshakeStatus NON_LIME_RATIO = new HandshakeStatus("Non-LimeWire Slots Full");
    public static final HandshakeStatus NO_LIME_SLOTS = new HandshakeStatus("No LimeWire Slots");
    public static final HandshakeStatus NO_HEADERS = new HandshakeStatus("No Headers Received.");
    public static final HandshakeStatus UNKNOWN = new HandshakeStatus("Unknown Handshake Failure");
    
    public String getMessage() {
        return msg;
    }
    
    public boolean isAcceptable() {
        return ok;
    }
    

}
