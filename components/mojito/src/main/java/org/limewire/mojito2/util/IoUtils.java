package org.limewire.mojito2.util;

import java.io.Closeable;
import java.io.IOException;

public class IoUtils {

    private IoUtils() {}
    
    public static boolean close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
                return true;
            } catch (IOException ignore) {
            }
        }
        return false;
    }
    
    public static boolean closeAll(Closeable... closeables) {
        boolean success = true;
        if (closeables != null) {
            for (Closeable closeable : closeables) {
                success |= close(closeable);
            }
        }
        return success;
    }
    
    public static boolean closeAll(Iterable<? extends Closeable> closeables) {
        boolean success = true;
        if (closeables != null) {
            for (Closeable closeable : closeables) {
                success |= close(closeable);
            }
        }
        return success;
    }
}
