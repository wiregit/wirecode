package org.limewire.rest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Random;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.nio.entity.NStringEntity;
import org.limewire.http.HttpCoreUtils;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.CommonUtils;

/**
 * Static constants and utility methods to support REST functions.
 */
public abstract class RestUtils {
    
    public static final String GET = "GET";
    public static final String PUT = "PUT";
    public static final String POST = "POST";
    public static final String DELETE = "DELETE";
    
    private static final String ENCODING = "UTF-8";
    private static final String ACCESS_FILE = "restaccess.txt";
    private static final Log LOG = LogFactory.getLog(RestUtils.class);

    /** Symbols used for random string generator. */
    private static final char[] SYMBOLS = new char[62];
    static {
        for (int i = 0; i < 10; i++) {
            SYMBOLS[i] = (char) ('0' + i);
        }
        for (int i = 0; i < 26; i++) {
            SYMBOLS[i + 10] = (char) ('A' + i);
        }
        for (int i = 0; i < 26; i++) {
            SYMBOLS[i + 36] = (char) ('a' + i);
        }
    }
    
    /**
     * Generates a random alphanumeric string with the specified length.
     */
    public static String createRandomString(int length) {
        char[] buf = new char[length];
        Random random = new Random();
        
        for (int i = 0; i < length; i++) {
            buf[i] = SYMBOLS[random.nextInt(SYMBOLS.length)];
        }
        
        return new String(buf);
    }
    
    /**
     * Returns a new HttpEntity containing the specified content string.
     */
    public static HttpEntity createStringEntity(String content) throws IOException {
        return new NStringEntity(content, ENCODING);
    }

    /**
     * Performs percent decoding on the specified string according to the 
     * OAuth 1.0a specification.
     */
    public static String percentDecode(String s) {
        try {
            return URLDecoder.decode(s, ENCODING);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Performs percent encoding on the specified string according to the 
     * OAuth 1.0a specification.
     */
    public static String percentEncode(String s) {
        try {
            // Encode string.  For OAuth, unreserved characters like tilde
            // must not be encoded.
            return URLEncoder.encode(s, ENCODING).replace("%7E", "~");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Returns the base URI by stripping off the query parameters from the
     * specified URI string.
     */
    public static String getBaseUri(String uriStr) {
        // Strip off query parameters.
        int pos = uriStr.indexOf("?");
        return (pos < 0) ? uriStr : uriStr.substring(0, pos);
    }
    
    /**
     * Parses the URI in the specified request, and returns URI target.  The
     * target is the piece between the prefix and the query parameters.  For
     * example, if the URI is "http://localhost/remote/library/files?type=audio"
     * and the prefix is "/library", then the target is "/files". 
     */
    public static String getUriTarget(HttpRequest request, String uriPrefix) throws IOException {
        // Get uri string.
        String uriStr = request.getRequestLine().getUri();
        
        // Strip off uri prefix.
        int pos = uriStr.indexOf(uriPrefix);
        if (pos < 0) throw new IOException("Invalid URI");
        String uriTarget = uriStr.substring(pos + uriPrefix.length());
        
        // Strip off query parameters.
        pos = uriTarget.indexOf("?");
        return (pos < 0) ? uriTarget : uriTarget.substring(0, pos);
    }
    
    /**
     * Returns a map of name/value pairs corresponding to the query parameters
     * in the specified request.
     */
    public static Map<String, String> getQueryParams(HttpRequest request) throws IOException {
        // Get uri string.
        String uriStr = request.getRequestLine().getUri();
        return getQueryParams(uriStr);
    }
    
    /**
     * Returns a map of name/value pairs corresponding to the query parameters
     * in the specified URI string.
     */
    public static Map<String, String> getQueryParams(String uriStr) throws IOException {
        try {
            // Get query parameters.
            URI uri = new URI(uriStr);
            return HttpCoreUtils.parseQuery(uri, null);
            
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }
    
    /**
     * Returns the REST access secret.
     */
    public static String getAccessSecret() {
        // Read access secret from file.
        File accessFile = new File(CommonUtils.getUserSettingsDir(), ACCESS_FILE);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(accessFile));
            return reader.readLine();
            
        } catch (IOException e) {
            LOG.debugf(e, "Unable to read REST OAuth secret {0}", e.getMessage());
            return "";
            
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {}
            }
        }
    }
    
    /**
     * Updates the REST access secret if necessary.
     */
    public static void updateAccessSecret() {
        // Update access secret only if not already saved.
        File accessFile = new File(CommonUtils.getUserSettingsDir(), ACCESS_FILE);
        if (!accessFile.exists()) {
            Writer writer = null;
            try {
                writer = new FileWriter(accessFile);
                writer.write(RestUtils.createRandomString(32));
                
            } catch (IOException e) {
                LOG.debugf(e, "Unable to save REST OAuth secret {0}", e.getMessage());
                
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException ex) {}
                }
            }
        }
    }
}
