package org.limewire.http;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.ResponseConnControl;

public class LimeResponseConnControl extends ResponseConnControl {

    public static final String KEEP_ALIVE = "org.limewire.keepalive";
    
    @Override
    public void process(HttpResponse response, HttpContext context)
            throws HttpException, IOException {
        Header header = response.getFirstHeader(HTTP.CONN_DIRECTIVE);
        if (header == null || !HTTP.CONN_KEEP_ALIVE.equals(header.getValue())) {
            super.process(response, context);
        }
    }
    
}
