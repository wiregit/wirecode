package org.limewire.activation;

import java.util.Date;

import org.limewire.io.BadGGEPPropertyException;
import org.limewire.io.GGEP;

public class ActivationKey {
    private static final String KEY_VALID_FROM = "d";
    private static final String KEY_DURATION   = "u";
    private static final String KEY_USER_EMAIL = "e";
    private static final String KEY_ACTIVATION_ID = "i";

    private final GGEP ggep;

    public ActivationKey() {
        this(new GGEP());
        setValidFrom(new Date(Long.MAX_VALUE));
        setUserEmail("");
        setDuration(0);
    }

    ActivationKey(GGEP ggep) {
        this.ggep = ggep;
    }

    public void setValidFrom(Date date) {
        ggep.put(KEY_VALID_FROM, date.getTime());
    }
    
    public void setUserEmail(String email_address) {
        ggep.put(KEY_USER_EMAIL, email_address);
    }
    
    public void setDuration(long days) {
        ggep.put(KEY_DURATION, days);
    }

    public void setActivationID(String activationID) {
        ggep.put(KEY_ACTIVATION_ID, activationID);
    }

    
    /**
     * @return the date after which this key is valid, or a date far in the
     *         future if this field is missing or corrupt.
     */
    public Date getValidFrom() {
        try {
            if (ggep.hasKey(KEY_VALID_FROM))
                return new Date(ggep.getLong(KEY_VALID_FROM));
        } catch (BadGGEPPropertyException ignored) {
        }
        return new Date(Long.MAX_VALUE);
    }
    
    /**
     * @return the email address of the purchaser.
     */
    public String getUserEmail() {
        try {
            if (ggep.hasKey(KEY_USER_EMAIL))
                return ggep.getString(KEY_USER_EMAIL);
        } catch (BadGGEPPropertyException ignored) {
        }
        return "";
    }
    
    /**
     * @return the number of days that the key is good for.
     */
    public long getDuration() {
        try {
            if (ggep.hasKey(KEY_DURATION))
                return ggep.getLong(KEY_DURATION);
        } catch (BadGGEPPropertyException ignored) {
        }
        return 0;
    }
    
    /**
     * @return the activation ID of the purchaser.
     */
    public String getActivationID() {
        try {
            if (ggep.hasKey(KEY_ACTIVATION_ID))
                return ggep.getString(KEY_ACTIVATION_ID);
        } catch (BadGGEPPropertyException ignored) {
        }
        return "";
    }

    /**
     * @return A base64 and 64-char word wrapped version of the underlying GGEP
     *         inside this key. Always ends with a line break.
     */
    String toPEMEncoded() {
        byte[] ggepBytes = ggep.toByteArray();
        // This shouldn't happen because the constructor generally forces the
        // GGEP to have at least one value, but this exception prevents badly
        // coded keys from making it out into the wild.
        if (ggepBytes.length == 0)
            throw new RuntimeException("No keys set on GGEP!");
        return PemCodec.encode(ggepBytes);
    }
    
    
}
