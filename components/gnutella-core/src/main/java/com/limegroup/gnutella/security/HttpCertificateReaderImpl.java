package com.limegroup.gnutella.security;

import java.io.IOException;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.limewire.http.httpclient.LimeHttpClient;

import com.google.inject.Provider;

public class HttpCertificateReaderImpl {

    private final Provider<LimeHttpClient> httpClient;
    private final CertificateParser certificateParser;

    public HttpCertificateReaderImpl(Provider<LimeHttpClient> httpClient, 
            CertificateParser certificateParser) {
        this.httpClient = httpClient;
        this.certificateParser = certificateParser;
    }
    
    Certificate read(URI uri) throws IOException {
        HttpGet get = new HttpGet(uri);
        LimeHttpClient limeHttpClient = httpClient.get();
        HttpResponse response = null;
        try {
            response = limeHttpClient.execute(get);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException("could not get content from: " + uri);
            }
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                throw new IOException("no entity from: " + uri);
            }
            String contents = EntityUtils.toString(entity);
            return certificateParser.parseCertificate(contents);
        } finally {
            limeHttpClient.releaseConnection(response);
        }
    }
    
}
