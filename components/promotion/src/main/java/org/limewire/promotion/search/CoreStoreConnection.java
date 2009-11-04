package org.limewire.promotion.search;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.limewire.core.api.Application;
import org.limewire.core.api.search.store.StoreConnection;
import org.limewire.http.httpclient.HttpClientUtils;
import org.limewire.io.GUID;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ApplicationServices;

/**
 * Implementation of StoreConnection for the live core.
 */
public class CoreStoreConnection implements StoreConnection {
    
    private static final Log LOG = LogFactory.getLog(CoreStoreConnection.class);
    
    private static final int COUNT = 5;

    private final Provider<String> storeAPIURL;
    private final ClientConnectionManager clientConnectionManager;
    private final ApplicationServices applicationServices;
    private final Application application;
    private final BasicCookieStore cookieStore;

    @Inject
    public CoreStoreConnection(@StoreAPIURL Provider<String> storeAPIURL,
                               @Named("sslConnectionManager") ClientConnectionManager connectionManager,
                               ApplicationServices applicationServices,
                               Application application) {
        this.storeAPIURL = storeAPIURL;
        this.clientConnectionManager = connectionManager;
        this.applicationServices = applicationServices;
        this.application = application;
        cookieStore = new BasicCookieStore();   
    }

    /**
     * Performs a search using the specified query text, and returns the 
     * result as a JSON text string.
     */
    @Override
    public String doQuery(String query) throws IOException {
        if (StringUtils.isEmpty(query)) {
            return "";
        }
        final String request = buildSearchRequestURL(query);
        return makeHTTPRequest(request);
    }

    private String makeHTTPRequest(String request) throws IOException {
        HttpClient client = createHttpClient();            
        LOG.debugf("calling store api: {0} ...", request);
        HttpResponse response = client.execute(new HttpGet(request));
        if (response.getEntity() != null) {
            String responseStr = EntityUtils.toString(response.getEntity());
            LOG.debugf(" ... response:\n{0}", responseStr);
            HttpClientUtils.releaseConnection(response);
            return responseStr;
        } else {
            return "";
        }
    }

    @Override
    public Icon loadIcon(String iconUri) {
        // TODO caching?
        try {
            return new ImageIcon(new URL(iconUri));
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
    
    @Override
    public String loadStyle(String styleId) throws IOException {
        if (StringUtils.isEmpty(styleId)) {
            return "";
        }
        final String request = buildStyleRequestURL(styleId);
        return makeHTTPRequest(request);
    }
    
    @Override
    public String loadTracks(String albumId, int startTrackNumber) throws IOException {
        if (StringUtils.isEmpty(albumId)) {
            return "";
        }
        final String request = buildTracksRequestURL(albumId, startTrackNumber);
        return makeHTTPRequest(request);
    }

    private String buildSearchRequestURL(String query) throws UnsupportedEncodingException {
        return storeAPIURL.get() + "/search?query=" + URLEncoder.encode(query, "UTF-8") + 
                "&lv=" + application.getVersion() + 
                "&guid=" + new GUID(applicationServices.getMyGUID()).toHexString() + 
                "&start=1" + 
                "&count=" + COUNT;
    }
    
    private String buildTracksRequestURL(String albumId, int startTrackNumber) throws UnsupportedEncodingException {
        return storeAPIURL.get() + "/tracks?albumId=" + albumId + 
                "&start=" + startTrackNumber + 
                "&lv=" + application.getVersion() + 
                "&guid=" + new GUID(applicationServices.getMyGUID()).toHexString();
    }
    
    private String buildStyleRequestURL(String styleId) throws UnsupportedEncodingException {
        return storeAPIURL.get() + "/style?styleId=" + styleId + 
                "&lv=" + application.getVersion() + 
                "&guid=" + new GUID(applicationServices.getMyGUID()).toHexString();
    }

    private HttpClient createHttpClient() {
        DefaultHttpClient httpClient = new DefaultHttpClient(clientConnectionManager, null);
        httpClient.setCookieStore(cookieStore);
        cookieStore.clearExpired(new Date());
        return httpClient;
    }
}
