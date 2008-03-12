package org.limewire.http.httpclient;

import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpClient;

public class SimpleLimeHttpClient extends DefaultHttpClient implements LimeHttpClient {

    public void releaseConnection(HttpResponse response) {
        HttpClientUtils.releaseConnection(response);
    }

}
