package org.limewire.rest.oauth;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.NameValuePair;
import org.limewire.rest.RestUtils;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * Implementation of OAuthValidator used to validate requests using the OAuth
 * protocol.  At present, only the HMAC-SHA1 signature method is supported. 
 */
public class OAuthValidatorImpl implements OAuthValidator {

    private static final String VERSION = "1.0";
    private static final String SIG_METHOD = "HMAC-SHA1";
    private static final String MAC_NAME = "HmacSHA1";
    
    private static final char AMPERSAND = '&';
    private static final char EQUAL = '=';

    private final String baseUrl;
    private final String consumerSecret;
    private final String tokenSecret;

    /**
     * Constructs an OAuthValidator with the specified base URL, port number,
     * and consumer secret.  By default, the token secret is an empty string
     * for use with two-legged OAuth.
     */
    @Inject
    public OAuthValidatorImpl(
            @Assisted("baseUrl") String baseUrl,
            @Assisted int port,
            @Assisted("secret") String secret) {
        this.baseUrl = createBaseUrl(baseUrl, port);
        this.consumerSecret = secret;
        this.tokenSecret = "";
    }
    
    /**
     * Creates the base URL using the specified URL and port number.  The
     * default port numbers 80 (http) or 443 (https) are ignored because 
     * OAuth specifies that these must be excluded from the signature base 
     * string.
     */
    private String createBaseUrl(String baseUrl, int port) {
        // Split protocol and domain in URL string.
        int pos = baseUrl.indexOf("//");
        String protocol = (pos < 0) ? "" : baseUrl.substring(0, pos + 2);
        String domain = (pos < 0) ? baseUrl : baseUrl.substring(pos + 2);
        
        // Split uri from domain.
        int uriPos = domain.indexOf('/');
        String uri = (uriPos < 0) ? "" : domain.substring(uriPos);
        domain = (uriPos < 0) ? domain : domain.substring(0, uriPos);
        
        // Remove old port number.
        int portPos = domain.indexOf(':');
        domain = (portPos < 0) ? domain : domain.substring(0, portPos);
        
        // Add port number to domain.
        if ((port != 80) && (port != 443)) {
            domain = domain + ':' + port;
        }
        
        // Recreate url string.
        return protocol + domain + uri;
    }
    
    @Override
    public void validateRequest(OAuthRequest request) throws OAuthException {
        validateParameters(request);
        validateVersion(request);
        validateSignatureMethod(request);
        validateSignature(request);
    }
    
    /**
     * Validates the required OAuth parameters in the specified request.
     */
    private void validateParameters(OAuthRequest request) throws OAuthException {
        if (request.getParameter(OAuthRequest.OAUTH_CONSUMER_KEY) == null) {
            throw new OAuthException("Missing " + OAuthRequest.OAUTH_CONSUMER_KEY);
        }
        if (request.getParameter(OAuthRequest.OAUTH_SIGNATURE_METHOD) == null) {
            throw new OAuthException("Missing " + OAuthRequest.OAUTH_SIGNATURE_METHOD);
        }
        if (request.getParameter(OAuthRequest.OAUTH_SIGNATURE) == null) {
            throw new OAuthException("Missing " + OAuthRequest.OAUTH_SIGNATURE);
        }
        if (request.getParameter(OAuthRequest.OAUTH_TIMESTAMP) == null) {
            throw new OAuthException("Missing " + OAuthRequest.OAUTH_TIMESTAMP);
        }
        if (request.getParameter(OAuthRequest.OAUTH_NONCE) == null) {
            throw new OAuthException("Missing " + OAuthRequest.OAUTH_NONCE);
        }
    }
    
    /**
     * Validates the OAuth version in the specified request.  The version is
     * an optional parameter.
     */
    private void validateVersion(OAuthRequest request) throws OAuthException {
        String version = request.getParameter(OAuthRequest.OAUTH_VERSION);
        if ((version != null) && !VERSION.equalsIgnoreCase(version)) {
            throw new OAuthException("Invalid OAuth version");
        }
    }
    
    /**
     * Validates the OAuth signature method in the specified request.  Only
     * HMAC-SHA1 is supported.
     */
    private void validateSignatureMethod(OAuthRequest request) throws OAuthException {
        String sigMethod = request.getParameter(OAuthRequest.OAUTH_SIGNATURE_METHOD);
        if (!SIG_METHOD.equalsIgnoreCase(sigMethod)) {
            throw new OAuthException("Unsupported OAuth signature method");
        }
    }
    
    /**
     * Validates the OAuth signature in the specified request.
     */
    private void validateSignature(OAuthRequest request) throws OAuthException {
        // Retrieve request signature.
        String oauthSignature = request.getParameter(OAuthRequest.OAUTH_SIGNATURE);
        byte[] oauthBytes = Base64.decodeBase64(oauthSignature.getBytes());
        
        try {
            // Create base string and compute signature.
            String baseString = createSignatureBaseString(request);
            byte[] signatureBytes = computeSignature(baseString);
            
            // Compare signatures.
            if (!Arrays.equals(oauthBytes, signatureBytes)) {
                throw new OAuthException("Invalid OAuth signature");
            }
            
        } catch (GeneralSecurityException ex) {
            throw new OAuthException(ex);
        } catch (UnsupportedEncodingException ex) {
            throw new OAuthException(ex); 
        }
    }
    
    /**
     * Creates the signature base string for the specified request.  This is 
     * composed of three elements: HTTP request method, request URL, and 
     * normalized request parameters.
     */
    private String createSignatureBaseString(OAuthRequest request) {
        StringBuilder buf = new StringBuilder();
        
        buf.append(request.getMethod().toUpperCase()).append(AMPERSAND);
        buf.append(RestUtils.percentEncode(baseUrl + request.getUri())).append(AMPERSAND);
        buf.append(RestUtils.percentEncode(createParameterString(request)));
        
        return buf.toString();
    }
    
    /**
     * Creates the request parameter string for the specified request.  This
     * includes all parameters except the realm and signature, sorted by
     * parameter name.
     */
    private String createParameterString(OAuthRequest request) {
        StringBuilder buf = new StringBuilder();
        
        List<NameValuePair> parameters = request.getParameters();
        Collections.sort(parameters, new NameValueComparator());
        for (NameValuePair parameter : parameters) {
            // Skip realm and signature parameters.
            if (OAuthRequest.AUTH_REALM.equalsIgnoreCase(parameter.getName()) || 
                OAuthRequest.OAUTH_SIGNATURE.equalsIgnoreCase(parameter.getName())) {
                continue;
            }
            
            // Append parameter to string.
            if (buf.length() > 0) {
                buf.append(AMPERSAND);
            }
            buf.append(RestUtils.percentEncode(parameter.getName()));
            buf.append(EQUAL).append(RestUtils.percentEncode(parameter.getValue()));
        }
        
        // Return parameter string.
        return buf.toString();
    }
    
    /**
     * Computes the signature for the specified base string.  The HMAC-SHA1 
     * signature method is used.
     */
    private byte[] computeSignature(String baseString)
        throws GeneralSecurityException, UnsupportedEncodingException {
        
        // Create key.
        String keyString = RestUtils.percentEncode(consumerSecret) + '&' + RestUtils.percentEncode(tokenSecret);
        byte[] keyBytes = StringUtils.toUTF8Bytes(keyString);
        SecretKey key = new SecretKeySpec(keyBytes, MAC_NAME);

        // Compute signature using HmacSHA1. 
        Mac mac = Mac.getInstance(MAC_NAME);
        mac.init(key);
        byte[] text = StringUtils.toUTF8Bytes(baseString);
        return mac.doFinal(text);
    }
    
    /**
     * Comparator for sorting the request parameters by name.
     */
    private static class NameValueComparator implements Comparator<NameValuePair> {
        
        @Override
        public int compare(NameValuePair o1, NameValuePair o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }
}
