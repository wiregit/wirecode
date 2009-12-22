package org.limewire.promotion.search;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.concurrent.ScheduledListeningExecutorService;
import org.limewire.core.api.search.store.StoreConnection;
import org.limewire.http.httpclient.HttpClientInstanceUtils;
import org.limewire.http.httpclient.HttpClientUtils;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.StringUtils;
import org.mozilla.browser.XPCOMUtils;
import org.mozilla.interfaces.nsICookie;
import org.mozilla.interfaces.nsICookieManager;
import org.mozilla.interfaces.nsICookieManager2;
import org.mozilla.interfaces.nsISimpleEnumerator;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

/**
 * Implementation of StoreConnection for the live core.
 */
class CoreStoreConnection implements StoreConnection {
    
    private static final Log LOG = LogFactory.getLog(CoreStoreConnection.class);
    
    private static final int MAX_SEARCH_RESULTS = 5;

    private final Provider<String> storeAPIURL;
    private final Provider<String> storeCookieDomain;
    private final ScheduledListeningExecutorService executorService;
    private final Provider<LimeHttpClient> httpClientProvider;
    private final HttpClientInstanceUtils httpUtils;

    @Inject
    public CoreStoreConnection(@StoreAPIURL Provider<String> storeAPIURL,
                               @StoreCookieDomain Provider<String> storeCookieDomain,
                               @Named("backgroundExecutor") ScheduledListeningExecutorService executorService,  // TODO not background
                               Provider<LimeHttpClient> httpClientProvider,
                               HttpClientInstanceUtils httpUtils) {
        this.storeAPIURL = storeAPIURL;
        this.storeCookieDomain = storeCookieDomain;
        this.executorService = executorService;
        this.httpClientProvider = httpClientProvider;
        this.httpUtils = httpUtils;
    }

    @Override
    public void logout() {
        
    }

    /**
     * Performs a search using the specified query text, and returns the 
     * result as a JSON text string.
     */
    @Override
    public JSONObject doQuery(String query) throws IOException, JSONException {
        if (StringUtils.isEmpty(query)) {
            return new JSONObject();
        }
        final String request = buildSearchRequestURL(query);
        return new JSONObject(makeHTTPRequest(request));
    }

    private String makeHTTPRequest(String request) throws IOException {
        LimeHttpClient client = createHttpClient();
        LOG.debugf("calling store api: {0} ...", request);
        HttpResponse response = null;
        try {
            response = client.execute(new HttpGet(request));
            updateBrowserCookies(client.getCookieStore());  // TODO only for Set-Cookie headers
            if (response.getEntity() != null) {
                String responseStr = EntityUtils.toString(response.getEntity());
                LOG.debugf(" ... response:\n{0}", responseStr);
                return responseStr;
            } else {
                return "";
            }
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    /**
     * Used to keep httpclient and mozilla cookie stores in sync.
     * @param cookieStore
     */
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
    public Icon loadIcon(final String iconUri) throws MalformedURLException {
        // TODO caching?
        return new ImageIcon(new URL(iconUri));
        
    }
    
    @Override
    public JSONObject loadStyle(String styleId) throws IOException, JSONException {
        final String request = buildStyleRequestURL(styleId);
        return new JSONObject(makeHTTPRequest(request));
    }
    
    @Override
    public JSONObject loadTracks(String albumId, int startTrackNumber) throws IOException, JSONException {
        final String request = buildTracksRequestURL(albumId, startTrackNumber);
        return new JSONObject(makeHTTPRequest(request));
    }

    private String buildSearchRequestURL(String query) throws UnsupportedEncodingException {
        return httpUtils.addClientInfoToUrl(storeAPIURL.get() + "/search?query=" + URLEncoder.encode(query, "UTF-8") + 
                "&start=1" + 
                "&count=" + MAX_SEARCH_RESULTS);
    }
    
    private String buildTracksRequestURL(String albumId, int startTrackNumber) throws UnsupportedEncodingException {
        return httpUtils.addClientInfoToUrl(storeAPIURL.get() + "/tracks?albumId=" + albumId + 
                "&start=" + startTrackNumber);
    }
    
    private String buildStyleRequestURL(String styleId) throws UnsupportedEncodingException {
        return httpUtils.addClientInfoToUrl(storeAPIURL.get() + "/style?id=" + styleId);
    }

    private LimeHttpClient createHttpClient() {
        LimeHttpClient httpClient = httpClientProvider.get();
        CookieStore cookieStore = loadCookiesFromBrowser();
        httpClient.setCookieStore(cookieStore);
        cookieStore.clearExpired(new Date());
        return httpClient;
    }

    /**
     * Used to keep httpclient and mozilla cookie stores in sync.
     * @return a <code>CookieStore</code> containing the <code>.store.limewire.com</code>
     * cookies from the in-client mozilla browser
     */
    private CookieStore loadCookiesFromBrowser() {
        BasicCookieStore cookieStore = new BasicCookieStore();
        nsICookieManager cookieService = XPCOMUtils.getServiceProxy("@mozilla.org/cookiemanager;1",
            nsICookieManager.class);
        nsISimpleEnumerator enumerator = cookieService.getEnumerator();
        while(enumerator.hasMoreElements()) {                        
            nsICookie cookie = XPCOMUtils.proxy(enumerator.getNext(), nsICookie.class);
            if(cookie.getHost() != null && cookie.getHost().endsWith(storeCookieDomain.get())) {
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
