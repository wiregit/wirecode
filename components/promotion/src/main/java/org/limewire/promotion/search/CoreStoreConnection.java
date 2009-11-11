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
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;
import org.limewire.core.api.Application;
import org.limewire.core.api.search.store.StoreConnection;
import org.limewire.http.httpclient.HttpClientUtils;
import org.limewire.io.GUID;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.mozilla.browser.XPCOMUtils;
import org.mozilla.interfaces.nsICookie;
import org.mozilla.interfaces.nsICookieManager;
import org.mozilla.interfaces.nsICookieManager2;
import org.mozilla.interfaces.nsISimpleEnumerator;

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

    @Inject
    public CoreStoreConnection(@StoreAPIURL Provider<String> storeAPIURL,
                               @Named("sslConnectionManager") ClientConnectionManager connectionManager,
                               ApplicationServices applicationServices,
                               Application application) {
        this.storeAPIURL = storeAPIURL;
        this.clientConnectionManager = connectionManager;
        this.applicationServices = applicationServices;
        this.application = application;
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
        DefaultHttpClient client = createHttpClient();            
        LOG.debugf("calling store api: {0} ...", request);
        HttpResponse response = client.execute(new HttpGet(request));
        updateBrowserCookies(client.getCookieStore());  // TODO only for Set-Cookie headers
        if (response.getEntity() != null) {
            String responseStr = EntityUtils.toString(response.getEntity());
            LOG.debugf(" ... response:\n{0}", responseStr);
            HttpClientUtils.releaseConnection(response);
            return responseStr;
        } else {
            return "";
        }
    }

    private void updateBrowserCookies(CookieStore cookieStore) {
        nsICookieManager2 cookieService = XPCOMUtils.getServiceProxy("@mozilla.org/cookiemanager;1",
            nsICookieManager2.class);
        for(Cookie cookie : cookieStore.getCookies()) {
            long expiryDate; 
            if(cookie.getExpiryDate() != null) {
                if(cookie.getExpiryDate().getTime() == 0) {
                    // special value representing 
                    // Jan 1, 1970
                    expiryDate = 1;
                } else {
                    // normal case
                    expiryDate = cookie.getExpiryDate().getTime() / 1000;
                }
            } else {
                // session cookie
                expiryDate = 0;
            }
            cookieService.add(cookie.getDomain(),
                    cookie.getPath(),
                    cookie.getName(), 
                    cookie.getValue(), 
                    cookie.isSecure(), 
                    true, 
                    !cookie.isPersistent(), 
                    expiryDate);
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

    private DefaultHttpClient createHttpClient() {
        DefaultHttpClient httpClient = new DefaultHttpClient(clientConnectionManager, null);        
        CookieStore cookieStore = loadCookiesFromBrowser();
        httpClient.setCookieStore(cookieStore);
        cookieStore.clearExpired(new Date());
        return httpClient;
    }
    
    private CookieStore loadCookiesFromBrowser() {
        // TODO share code with FacebookFriendAccountConfigurationImpl
        BasicCookieStore cookieStore = new BasicCookieStore();
        nsICookieManager cookieService = XPCOMUtils.getServiceProxy("@mozilla.org/cookiemanager;1",
            nsICookieManager.class);
        nsISimpleEnumerator enumerator = cookieService.getEnumerator();
        while(enumerator.hasMoreElements()) {                        
            nsICookie cookie = XPCOMUtils.proxy(enumerator.getNext(), nsICookie.class);
            if(cookie.getHost() != null && cookie.getHost().endsWith(".store.limewire.com")) {
                LOG.debugf("adding cookie {0} = {1} for host {2}", cookie.getName(), cookie.getValue(), cookie.getHost());
                BasicClientCookie copy = new BasicClientCookie(cookie.getName(), cookie.getValue());
                copy.setDomain(cookie.getHost());
                double expiry = cookie.getExpires();
                if(expiry != 0 && expiry != 1) {
                    long expiryMillis = (long) expiry * 1000;
                    copy.setExpiryDate(new Date(expiryMillis));
                }
                copy.setPath(cookie.getPath());
                copy.setSecure(cookie.getIsSecure());
                // TODO copy.setVersion();
                cookieStore.addCookie(copy);
            } else {
                LOG.debugf("dropping cookie {0} = {1} for host {2}", cookie.getName(), cookie.getValue(), cookie.getHost());
            }
        }
        return cookieStore;
    }
}
