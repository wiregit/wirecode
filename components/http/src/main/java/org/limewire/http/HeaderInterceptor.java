package org.limewire.http;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.protocol.HttpContext;

public interface HeaderInterceptor {

    void process(Header header, HttpContext context) throws HttpException, IOException;
    
}
