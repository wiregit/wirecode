package org.limewire.http;

import org.apache.http.Header;
import org.apache.http.HttpMessage;

/**
 * Provides utility methods for HttpCore.
 */
public class HttpCoreUtils {

    /**
     * Returns true, if message contains a header with <code>name</code> and a
     * value of <code>value</code>.
     */
    public static boolean hasHeader(HttpMessage message, String name, String value) {
        Header[] headers = message.getHeaders(name);
        for (Header header : headers) {
            if (header.getValue().contains(value)) {
                return true;
            }
        }
        return false;
    }
    
}
