package org.limewire.http;

import org.apache.http.Header;
import org.apache.http.HttpRequest;

public class HttpCoreUtils {

    public static boolean hasHeader(HttpRequest request, String name, String value) {
        Header[] headers = request.getHeaders(name);
        for (Header header : headers) {
            if (header.getValue().contains(value)) {
                return true;
            }
        }
        return false;
    }
    
}
