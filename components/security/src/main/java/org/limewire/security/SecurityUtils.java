package org.limewire.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.SecureRandom;

/**
 *  A collection of utility methods related to 
 *  com.limegroup.gnutella.security or java.security
 *
 */
public class SecurityUtils {

    /**
     * On some OSes, creating a new SeucureRandom instance
     * with the default constructor may block if the OS's
     * internal entropy pool runs low.  On MS Windows,
     * OS X, and Linux, and pretty much any modern
     * Unix, this method will not block.
     */
    public static SecureRandom createSecureRandomNoBlock() {
        File urandom = new File("/dev/urandom");
        try {
            if (urandom.canRead()) {
                // OS X, Linux, FreeBSD, Solaris, etc.
                byte[] seed = new byte[32];
                FileInputStream randStream = new FileInputStream(urandom);
                for(int offset=0; offset < 32;) {
                    offset += randStream.read(seed, offset, 32-offset);
                }
                return new SecureRandom(seed);
            }
        } catch (SecurityException ignored) {}
        catch (IOException ignored) {}
        
        // Either we're on MS Windows, or some fringe OS that
        // doesn't have /dev/urandom or doesn't let normal
        // users use /dev/urandom.  In the Windows case, this
        // won't block.
        return new SecureRandom();
    }
    
}
