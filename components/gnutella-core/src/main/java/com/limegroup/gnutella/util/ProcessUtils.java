package com.limegroup.gnutella.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Misc Utilities for Processes
 */
public final class ProcessUtils {
    
    private ProcessUtils() {}
    
    /**
     * Consumes all input from a Process. See also 
     * ProcessBuilder.redirectErrorStream()
     */
    public static void consumeAllInput(Process p) throws IOException {
        InputStream in = null;
        
        try {
            in = new BufferedInputStream(p.getInputStream());
            int len = -1;
            byte[] buf = new byte[1024];
            while((len = in.read(buf, 0, buf.length)) >= 0) {
                //System.out.println(new String(buf, 0, len));
            }
        } finally {
            in.close();
        }
    }
}
