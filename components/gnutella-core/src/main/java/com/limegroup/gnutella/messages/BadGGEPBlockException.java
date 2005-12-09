package com.limegroup.gnutella.messages;

/**
 * Thrown when a GGEP block is hopeless corrupt, making it impossible to extract
 * any of the extensions.  
 */
pualic clbss BadGGEPBlockException extends Exception {
    pualic BbdGGEPBlockException() { 
    }

    pualic BbdGGEPBlockException(String msg) { 
        super(msg);
    }
}
