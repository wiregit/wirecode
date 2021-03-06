package org.limewire.activation.impl;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Locale;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.limewire.activation.api.ActivationSettingsController;
import org.limewire.http.httpclient.HttpClientUtils;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.io.IOUtils;
import org.limewire.io.InvalidDataException;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.security.certificate.CipherProvider;
import org.limewire.security.certificate.CipherProvider.CipherType;
import org.limewire.util.EncodingUtils;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Responsible for communicating with activation server.
 * <pre>
 * - activating / validating at startup and periodically with pkey
 * - retry automatically if server is down
 * - if activation is attempted while another is in progress, cancel old one
 *   in favor of new one.
 * </pre>
 */
class ActivationCommunicatorImpl implements ActivationCommunicator {    
    private static final Log log = LogFactory.getLog(ActivationCommunicatorImpl.class);
    
    private static final String ENCRYPTION_ALGORITHM = "RSA";
    private static final CipherType CIPHER_TYPE = CipherType.RSA;
    
    private final ActivationResponseFactory activationFactory;
    private final Provider<LimeHttpClient> httpClientProvider;
    private final ActivationSettingsController activationSettings;
    private final CipherProvider cipherProvider;

    @Inject
    public ActivationCommunicatorImpl(ActivationSettingsController activationSettings,
                                      ActivationResponseFactory activationFactory,
                                      Provider<LimeHttpClient> httpClientProvider,
                                      CipherProvider cipherProvider) {
        this.activationSettings = activationSettings;
        this.httpClientProvider = httpClientProvider;
        this.activationFactory = activationFactory;
        this.cipherProvider = cipherProvider;
    }
    
    public ActivationResponse activate(final String licenseId, RequestType type) throws IOException, InvalidDataException, InvalidTokenException {
        
        // add the license id and nonce parameter to the POST'ed content
        String randomNumber = getRandomToken();
        String lidToken = getEncryptedToken(licenseId, randomNumber);
        String postContent = activationSettings.getQueryString() + "&lid=" + licenseId + "&lidtoken=" + lidToken;
        String jsonResult = sendToServer(postContent, licenseId, type);
        
        ActivationResponse activationResponse =  activationFactory.createFromJson(jsonResult);
        
        boolean responseHasSameRandomNumber = randomNumber.equals(activationResponse.getToken());
        if ((activationResponse.getResponseType() == ActivationResponse.Type.VALID) && !responseHasSameRandomNumber) {
            log.debug("Invalid random number");
            throw new InvalidTokenException("random number security check failed");
        }
        
        return activationResponse;
    }
    
    /**
     * Returns a random number as a String. 
     */
    private static String getRandomToken() {
        return String.valueOf(Math.random()).substring(2);    
    }
    
    private String getEncryptedToken(String licenseId, String randomNumber) throws IOException {
        // get public key from string
        byte[] cipherData;
        byte[] keyBytes = Base64.decodeBase64(activationSettings.getServerKey().getBytes());
        try {
            KeyFactory fac = KeyFactory.getInstance(ENCRYPTION_ALGORITHM);
            PublicKey publicKey = fac.generatePublic(new X509EncodedKeySpec(keyBytes));

            byte[] messageToEcrypt = StringUtils.toUTF8Bytes(licenseId + "," + randomNumber);
            cipherData = cipherProvider.encrypt(messageToEcrypt, publicKey, CIPHER_TYPE);
        } catch (GeneralSecurityException e) {
            throw IOUtils.getIOException("Security exception while initializing key", e);
        }
        String noEncoding = new String(Base64.encodeBase64(cipherData));
        return EncodingUtils.encode(noEncoding);
    }

    String sendToServer(String queryStringToPost, String key, RequestType type) throws IOException {
        String submitUrl = activationSettings.getActivationHost();
        HttpEntity postContent = new StringEntity(queryStringToPost);
        HttpPost httpPost = new HttpPost(getQueryString(submitUrl, type, key));
        httpPost.addHeader("Connection", "close");
        httpPost.addHeader("Content-Type", URLEncodedUtils.CONTENT_TYPE);
        httpPost.setEntity(postContent);
        HttpClient httpClient = httpClientProvider.get();
        
        HttpResponse response = null;
        try {
            response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            if (statusCode != 200 || entity == null) {
                throw new IOException("invalid http response, status: " + statusCode
                       + ", entity: " + ((entity != null) ? EntityUtils.toString(entity) : "none"));
            }
            return EntityUtils.toString(entity);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }
    
    private String getQueryString(String url, RequestType type, String key) {
        if(url.indexOf('?') == -1)
            url += "?";
        
        return url +
        "reqtype=" + EncodingUtils.encode(type.toString().toLowerCase(Locale.US)) +
        (isBetaKey(key) ? ("&lid=" + key) : "");
    }
    
    /**
     * Returns true if this LicenseKey is used exclusively for Beta testing,
     * false otherwise.
     */
    private boolean isBetaKey(String key) {
        if(key.startsWith("BETA"))
            return true;
        else
            return false;
    }
}

