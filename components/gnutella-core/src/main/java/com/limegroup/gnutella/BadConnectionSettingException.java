package com.limegroup.gnutella;

/** 
 * The exception thrown when you try to set your incoming/outgoing connections
 * to a bad value.  Contains suggested new values for the settings and a reason
 * for the exception.<p>
 *
 * Design note: some may argue that this should be multiple classes or an enum.
 * But there could be multiple reasons for an exception, so we may use masks.
 */
public class BadConnectionSettingException extends Exception {
    public final static int NEGATIVE_VALUE=0x1;
    public final static int TOO_HIGH_FOR_SPEED=0x2;
    public final static int TOO_HIGH_FOR_LEAF=0x3;
    public final static int TOO_LOW_FOR_ULTRAPEER=0x4;
    
    /** INVARIANT: One of the static fields defined above. */
    private int reason;
    /** The suggested KEEP_ALIVE. */
    private int suggestedOutgoing;

    /**
     * @param reason why the settings were rejected.  This value
     *  must be one of NEGATIVE_VALUE, TOO_HIGH_FOR_SPEED
     * @param suggestedOutgoing the suggested KEEP_ALIVE
     * @param suggestedIncoming the suggested MAX_INCOMING_CONNECTIONS
     */
    public BadConnectionSettingException(int reason,
                                         int suggestedOutgoing) {
        this.reason=reason;
        this.suggestedOutgoing=suggestedOutgoing;
    }

    public int getReason() { return reason; }
    public int getSuggestedOutgoing() { return suggestedOutgoing; }
}
