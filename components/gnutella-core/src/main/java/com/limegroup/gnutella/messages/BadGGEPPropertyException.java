
// Commented for the Learning branch

package com.limegroup.gnutella.messages;

/**
 * Throw a BadGGEPPropertyException when a GGEP extension like "SCP" can't be found or parsed.
 * This doesn't mean the GGEP block is bad, other extensions can be extracted.
 * 
 * If the whole block is bad, throw BadGGEPBlockException.
 */
public class BadGGEPPropertyException extends Exception {

    /**
     * Throw when we can't find a GGEP extension or we can't parse its value.
     */
    public BadGGEPPropertyException() {}

    /**
     * Throw when we can't find a GGEP extension or we can't parse its value.
     * 
     * @param msg A message that describes what happened
     */
    public BadGGEPPropertyException(String msg) {

        // Call the Exception constructor, having it save the message in this new object
        super(msg);
    }
}
