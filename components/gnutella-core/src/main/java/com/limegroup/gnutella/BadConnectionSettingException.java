package com.limegroup.gnutella;

/** 
 * The exception thrown when you try to set your KEEP_ALIVE values to a bad
 * value.  Contains suggested new values for the settings and a reason for the
 * exception.
 */
public class BadConnectionSettingException extends RuntimeException {
    public final static int NEGATIVE_VALUE=0x1;
    public final static int TOO_HIGH_FOR_SPEED=0x2;
    
    /** INVARIANT: One of the static fields defined above. */
    private int reason;
    /** The suggested new value. */
    private int suggestion;

    /**
     * @param reason why the settings were rejected.  This value
     *  must be one of NEGATIVE_VALUE, TOO_HIGH_FOR_SPEED
     * @param suggestion the suggested new value
     */
    public BadConnectionSettingException(int reason,
                                         int suggestion) {
        this.reason=reason;
        this.suggestion=suggestion;
    }

    public int getReason() { return reason; }    
    public int getSuggestion() { return suggestion; }
}
