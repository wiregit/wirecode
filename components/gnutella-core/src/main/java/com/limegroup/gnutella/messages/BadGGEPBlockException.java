
// Commented for the Learning branch

package com.limegroup.gnutella.messages;

/**
 * Throw a BadGGEPBlockException when the data in a GGEP block is corrupt, making it impossible for us to continue reading it.
 * Only code in the GGEP class throws this exception.
 */
public class BadGGEPBlockException extends Exception {

    /**
     * Make a new BadGGEPBlockException to throw when the data in a GGEP block we're parsing is corrupt.
     */
    public BadGGEPBlockException() {}

    /**
     * Make a new BadGGEPBlockException to throw when the data in a GGEP block we're parsing is corrupt.
     * 
     * @param msg A message that describes what happened
     */
    public BadGGEPBlockException(String msg) {

        // Call the Exception constructor, having it save the message in this new object
        super(msg);
    }
}
