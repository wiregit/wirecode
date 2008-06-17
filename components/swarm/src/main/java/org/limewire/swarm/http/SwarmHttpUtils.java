package org.limewire.swarm.http;

import java.io.IOException;

import org.apache.http.nio.IOControl;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.limewire.collection.Range;

public class SwarmHttpUtils {

    private SwarmHttpUtils() {
    }

    /**
     * Closes the connection associated with this HttpContext.
     */
    public static void closeConnectionFromContext(HttpContext context) {
        IOControl ioctrl = (IOControl) context.getAttribute(ExecutionContext.HTTP_CONNECTION);
        try {
            ioctrl.shutdown();
        } catch (IOException ignored) {
        }
    }

    /**
     * Returns a {@link Range} object representing the data within the range request.
     * The data should have 'bytes=X-Y'.
     */
    public static Range rangeForRequest(String value) {
        if(!value.startsWith("bytes") || value.length() <= 6) {
            return null;
        }
        
        int dash = value.indexOf('-');
        if(dash == -1 || dash == value.length()-1) {
            return null;
        }
        
        try {
            long low = Long.parseLong(value.substring(6, dash).trim());
            long high = Long.parseLong(value.substring(dash+1).trim());
            if(low > high) {
                return null;
            } else {
                return Range.createRange(low, high);
            }
        } catch(NumberFormatException nfe) {
            nfe.printStackTrace();
            return null;
        }
    }

}
