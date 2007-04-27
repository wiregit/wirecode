package com.limegroup.gnutella.uploader;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

public class FreeLoaderRequestHandler implements HttpRequestHandler {

    // FIXME provide a working link
    public static final String FREELOADER_RESPONSE_PAGE = "<html>\r\n"
            + "<head>\r\n"
            + "<title>Please Share</title>\r\n"
            + "<meta http-equiv=\"refresh\" \r\n"
            + "content=\"0; \r\n"
            + "URL=http://www2.limewire.com/browser.htm\">\r\n"
            + "</head>\r\n"
            + "<body>\r\n"
            + "<a href=\"http://www2.limewire.com/browser.htm\">Please Share</a>\r\n"
            + "</body>\r\n" // 
            + "</html>\r\n";

    public void handle(HttpRequest request, HttpResponse response,
            HttpContext context) throws HttpException, IOException {
        response.setStatusCode(HttpStatus.SC_OK);
        response.setEntity(new StringEntity(FREELOADER_RESPONSE_PAGE));
    }

}
