package org.limewire.promotion.containers;

import java.io.InvalidObjectException;

import com.limegroup.gnutella.messages.BadGGEPBlockException;
import com.limegroup.gnutella.messages.GGEP;

/**
 * The most basic block of information conveyed by the promotion system, a block
 * of bytes, with a standard 8-byte header: first 4 bytes gives the type, next 4
 * bytes gives the length of the packet not including the header.
 */
public interface MessageContainer {
    final String TYPE_KEY = "T"; 
    /**
     * @return the type code for this container.
     */
    byte[] getType();

    /**
     * @return The full encoded version of this container (A raw GGEP, generally)
     */
    byte[] getEncoded();

    /**
     * Take the given passed in GGEP-encoded bytes and parse out the data to
     * fill yourself in.
     * 
     * @throws InvalidObjectException If the passed-in value doesn't represent
     *         an instance of this object type, or a wrapped MessageContainer
     *         throws this exception during its own parse method.
     */
    void parse(GGEP rawGGEP) throws BadGGEPBlockException;
   
}
