package org.limewire.activation;

import java.util.Date;

import com.limegroup.gnutella.messages.BadGGEPPropertyException;
import com.limegroup.gnutella.messages.GGEP;

public class ActivationKey {
    private static final String KEY_VALID_FROM = "d";

    private final GGEP ggep;

    public ActivationKey() {
        this(new GGEP());
        setValidFrom(new Date(Long.MAX_VALUE));
    }

    ActivationKey(GGEP ggep) {
        this.ggep = ggep;
    }

    public void setValidFrom(Date date) {
        ggep.put(KEY_VALID_FROM, date.getTime());
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
