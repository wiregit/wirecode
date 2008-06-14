package org.limewire.swarm.http;

import java.io.IOException;

import org.apache.http.nio.IOControl;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

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

}
