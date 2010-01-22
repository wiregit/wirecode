package org.limewire.activation.impl;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.limewire.activation.api.ActivationManager;
import org.limewire.activation.api.ActivationUrls;
import org.limewire.http.httpclient.HttpClientUtils;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.io.InvalidDataException;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Responsible for communicating with activation server.
 *
 * - activating / validating at startup and periodically with pkey
 * - retry automatically if server is down
 * - if activation is attemted while another is in progress, cancel old one
 *   in favor of new one.
 */
// todo: write tests
class ActivationCommunicatorImpl implements ActivationCommunicator {
    private static Log LOG = LogFactory.getLog(ActivationCommunicatorImpl.class);
    
    private final Provider<String> activationUrlProvider;
    private final ActivationResponseFactory activationFactory;
    private final Provider<LimeHttpClient> httpClientProvider;
    private final ApplicationServices applicationServices;
    private final ActivationManager activationManager;

    @Inject
    public ActivationCommunicatorImpl(@ActivationUrls Provider<String> activationUrlProvider,
                                      ActivationResponseFactory activationFactory,
                                      Provider<LimeHttpClient> httpClientProvider,
                                      ApplicationServices applicationServices,
                                      ActivationManager activationManager) {
        this.activationUrlProvider = activationUrlProvider;
        this.httpClientProvider = httpClientProvider;
        this.activationFactory = activationFactory;
        this.applicationServices = applicationServices;
        this.activationManager = activationManager;
    }
    
    public ActivationResponse activate(final String key) throws IOException, InvalidDataException {
        // get query string
        String query = LimeWireUtils.getLWInfoQueryString(applicationServices.getMyGUID(), 
            activationManager.isProActive(), activationManager.getMCode()) + "&lid=" + key;

        String jsonResult = sendToServer(query);
        LOG.debugf("Activation server response: " + jsonResult);

        return activationFactory.createFromJson(jsonResult);
    }


    String sendToServer(String queryStringToPost) throws IOException {
        String submitUrl = activationUrlProvider.get();
        HttpEntity postContent = new StringEntity(queryStringToPost);
        HttpPost httpPost = new HttpPost(submitUrl);
        httpPost.addHeader("Connection", "close");
        httpPost.addHeader("Content-Type", URLEncodedUtils.CONTENT_TYPE);
        httpPost.setEntity(postContent);

        HttpClient httpClient = httpClientProvider.get();
        HttpResponse response = httpClient.execute(httpPost);
        int statusCode = response.getStatusLine().getStatusCode();
        HttpEntity entity = response.getEntity();
        if (statusCode != 200 || entity == null) {
            throw new IOException("invalid http response, status: " + statusCode
                   + ", entity: " + ((entity != null) ? EntityUtils.toString(entity) : "none"));
        }
        HttpClientUtils.releaseConnection(response);
        return EntityUtils.toString(entity);
    }
}

