package org.limewire.promotion.search;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;

import javax.swing.Icon;

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

    private final Provider<String> storeSearchURL;
    private final ClientConnectionManager clientConnectionManager;
    private final ApplicationServices applicationServices;
    private final Application application;
    private final BasicCookieStore cookieStore;

    @Inject
    public CoreStoreConnection(@StoreSearchURL Provider<String> storeSearchURL,
                               @Named("sslConnectionManager") ClientConnectionManager connectionManager,
                               ApplicationServices applicationServices,
                               Application application) {
        this.storeSearchURL = storeSearchURL;
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
    public String doQuery(String query) {
        if (StringUtils.isEmpty(query)) {
            return "";
        }
        
        HttpClient client = createHttpClient();
        try {
            final String request = buildURL(query);
            LOG.debugf("searching store: {0} ...", request);
            HttpResponse response = client.execute(new HttpGet(request));
            if (response.getEntity() != null) {
                String responseStr = EntityUtils.toString(response.getEntity());
                LOG.debugf(" ... response:\n{0}", responseStr);
                HttpClientUtils.releaseConnection(response);
                return responseStr;
            } else {
                return "";
            }
        } catch (IOException e) {
            LOG.debug(e.getMessage(), e);
        }
        return "";
    }
    
    @Override
    public Icon loadIcon(String iconUri) {
        // TODO implement
        return null;
    }
    
    @Override
    public String loadStyle(String styleId) {
        // TODO implement
        return "";
    }
    
    @Override
    public String loadTracks(String albumId) {
        // TODO implement
        return "";
    }

    private String buildURL(String query) throws UnsupportedEncodingException {
        return storeSearchURL.get() + "?query=" + URLEncoder.encode(query, "UTF-8") + 
                "&lv=" + application.getVersion() + 
                "&guid=" + new GUID(applicationServices.getMyGUID()).toHexString() + 
                "&start=1" + 
                "&count=" + COUNT;
    }

    private HttpClient createHttpClient() {
        DefaultHttpClient httpClient = new DefaultHttpClient(clientConnectionManager, null);
        httpClient.setCookieStore(cookieStore);
        cookieStore.clearExpired(new Date());
        return httpClient;
    }
}
