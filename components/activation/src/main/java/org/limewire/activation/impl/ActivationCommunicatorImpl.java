package org.limewire.activation.impl;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.limewire.activation.api.ActSettings;
import org.limewire.http.httpclient.HttpClientUtils;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.io.InvalidDataException;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Responsible for communicating with activation server.
 *
 * - activating / validating at startup and periodically with pkey
 * - retry automatically if server is down
 * - if activation is attempted while another is in progress, cancel old one
 *   in favor of new one.
 */
class ActivationCommunicatorImpl implements ActivationCommunicator {
    private static Log LOG = LogFactory.getLog(ActivationCommunicatorImpl.class);
    
    private final ActivationResponseFactory activationFactory;
    private final Provider<LimeHttpClient> httpClientProvider;
    private final ActSettings activationSettings;

    @Inject
    public ActivationCommunicatorImpl(ActSettings activationSettings,
                                      ActivationResponseFactory activationFactory,
                                      Provider<LimeHttpClient> httpClientProvider) {
        this.activationSettings = activationSettings;
        this.httpClientProvider = httpClientProvider;
        this.activationFactory = activationFactory;
    }
    
    public ActivationResponse activate(final String key) throws IOException, InvalidDataException {
        String query = activationSettings.getQueryString() + "&lid=" + key;

        String jsonResult = sendToServer(query);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Activation server response: " + jsonResult);
        }

        return activationFactory.createFromJson(jsonResult);
    }


    String sendToServer(String queryStringToPost) throws IOException {
        String submitUrl = activationSettings.getActivationHost();
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

