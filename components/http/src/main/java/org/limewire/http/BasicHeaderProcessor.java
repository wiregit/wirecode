package org.limewire.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;

public class BasicHeaderProcessor {

    private List<HeaderInterceptor> headerInterceptors = null;

    public void addInterceptor(final HeaderInterceptor headerInterceptor) {
        if (headerInterceptor == null) {
            throw new IllegalArgumentException();
        }

        if (this.headerInterceptors == null) {
            this.headerInterceptors = new ArrayList<HeaderInterceptor>();
        }
        this.headerInterceptors.add(headerInterceptor);
    }

    public void clearInterceptors() {
        this.headerInterceptors = null;
    }

    public HeaderInterceptor[] getInterceptors() {
        return headerInterceptors.toArray(new HeaderInterceptor[0]);
    }

    public void process(final HttpRequest request, final HttpContext context)
            throws IOException, HttpException {
        if (this.headerInterceptors != null) {
            Header[] headers = request.getAllHeaders();
            for (Header header : headers) {
                for (HeaderInterceptor interceptor : headerInterceptors) {
                    interceptor.process(header, context);
                }
            }
        }
    }

}
