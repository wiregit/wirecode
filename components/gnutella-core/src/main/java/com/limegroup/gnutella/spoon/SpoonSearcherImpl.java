package com.limegroup.gnutella.spoon;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieIdentityComparator;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie2;
import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.settings.SpoonSettings;
import org.limewire.http.httpclient.HttpClientInstanceUtils;
import org.limewire.http.httpclient.HttpClientUtils;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.inject.LazySingleton;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.StringUtils;
import org.limewire.util.URIUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

@LazySingleton
class SpoonSearcherImpl implements SpoonSearcher {
    
    private static final Log LOG = LogFactory.getLog(SpoonSearcherImpl.class);
    
    private final Provider<LimeHttpClient> httpClientProvider;
    private final ExecutorService executor;
    
    private final CookieStore cookieStore = new BasicCookieStore();
    
    private final AtomicReference<String> spoonQueryPart = new AtomicReference<String>(null);

    private final HttpClientInstanceUtils httpClientInstanceUtils;
    
    @Inject
    public SpoonSearcherImpl(Provider<LimeHttpClient> httpClientProvider,
            HttpClientInstanceUtils httpClientInstanceUtils) {
        this.httpClientProvider = httpClientProvider;
        this.httpClientInstanceUtils = httpClientInstanceUtils;
        this.executor = ExecutorsHelper.newThreadPool("SpoonThread");
    }
    
    @Override
    public void search(SearchDetails searchDetails, SpoonSearchCallback callback) {
        executor.execute(new Searcher(searchDetails, callback));
    }
    
    private String getSpoonQueryString(SearchDetails searchDetails) throws URISyntaxException {
        // TODO could apply QueryUtils.extractKeywords on any of these
        Map<FilePropertyKey, String> advancedDetails = searchDetails.getAdvancedDetails();
        StringBuilder builder = new StringBuilder();
        if (!advancedDetails.isEmpty()) {
            builder.append("?");
            String title = advancedDetails.get(FilePropertyKey.TITLE);
            if (title != null) {
                builder.append("&title=").append(URIUtils.encodeUriComponent(title));
            }
            String artist = advancedDetails.get(FilePropertyKey.AUTHOR);
            if (artist != null) {
                builder.append("&artist=").append(URIUtils.encodeUriComponent(artist));
            }
            String album = advancedDetails.get(FilePropertyKey.ALBUM);
            if (album != null) {
                builder.append("&album=").append(URIUtils.encodeUriComponent(album));
            }
        } else {
            builder.append(URIUtils.encodeUriComponent(searchDetails.getSearchQuery()));
        }
        return builder.toString();
    }
    
    private void readSpoonProperties() {
        if (spoonQueryPart.compareAndSet(null, "")) {
            readCookies();
            File parent = CommonUtils.getUserSettingsDir().getParentFile();
            if (parent != null) {
                File spoonPropsFile = new File(new File(parent, ".spoon"), "spoon.props");
                if (!spoonPropsFile.isFile()) {
                    spoonPropsFile = new File(new File(parent, "Spoon"), "spoon.props");
                }
                try {
                    Properties props = FileUtils.readProperties(spoonPropsFile);
                    spoonQueryPart.set(createSpoonQueryPart(props));
                } catch (IOException e) {
                    LOG.debug("couldn't read props file", e);
                } catch (URISyntaxException e) {
                    LOG.debug("couldn't create query part", e);
                }
            }
        }
    }
    
    private void readCookies() {
        List<Cookie> cookies = CookieSerializer.toCookies(SpoonSettings.SPOON_COOKIES.get());
        for (Cookie cookie : cookies) {
            cookieStore.addCookie(cookie);
        }
        cookieStore.clearExpired(new Date());
    }

    private void updateCookies() {
        cookieStore.clearExpired(new Date());
        SpoonSettings.SPOON_COOKIES.set(CookieSerializer.toString(cookieStore.getCookies()));
    }
    
    private String createSpoonQueryPart(Properties props) throws URISyntaxException {
        if (props.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("client.")) {
                builder.append("&").append(URIUtils.encodeUriComponent(key)).append("=").append(URIUtils.encodeUriComponent(props.getProperty(key)));
            }
        }
        return builder.toString();
    }
    
    private String addSpoonQueryPart(String url) {
        String queryPart = spoonQueryPart.get();
        if (queryPart.isEmpty()) {
            return url;
        }
        if (url.contains("?")) {
            return url + queryPart;
        } else {
            return url + "?" + queryPart;
        }
    }
    
    private class Searcher implements Runnable {

        private final SearchDetails query;
        private final SpoonSearchCallback callback;
        
        public Searcher(SearchDetails searchDetails, SpoonSearchCallback callback) {
            this.query = searchDetails;
            this.callback = callback;
        }
        
        @Override
        public void run() {
            readSpoonProperties();
            try {
                URL url = sendToServer(query);
                if(url != null)
                    callback.handle(url);
            } catch (IOException e) {
                LOG.debug("error sending query", e);
            } catch (URISyntaxException e) {
                LOG.error("error building query", e);
            }
        }
        
        private URL sendToServer(SearchDetails query) throws IOException, URISyntaxException {
            String uri = SpoonSettings.SPOON_SEARCH_SERVER.get() + getSpoonQueryString(query);
            uri = addSpoonQueryPart(httpClientInstanceUtils.addClientInfoToUrl(uri));
            LOG.debugf("ilsr uri {0}", uri);
            HttpGet get = new HttpGet(uri);
            LimeHttpClient httpClient = httpClientProvider.get();
            httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BEST_MATCH);
            httpClient.setCookieStore(cookieStore);
            HttpResponse response = null;
            try {
                response = httpClient.execute(get);
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        String html = EntityUtils.toString(entity);
                        LOG.debugf("html response {0}", html);
                        URL doneUrl = new URL(uri);
                        return new URL(doneUrl, "", new URLHandler(html, doneUrl));
                    }
                } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_TEMPORARY_REDIRECT) {
                    Header header = response.getFirstHeader("Location");
                    LOG.debugf("redirected to {0}", header);
                    if (header != null) {
                        return new URL(header.getValue());
                    }
                }
            } finally {
                HttpClientUtils.releaseConnection(response);
                updateCookies();
            }
            return null;
        }

        
    }
    
    private static class URLHandler extends URLStreamHandler {
        
        private final byte[] source;
        private URL url;

        public URLHandler(String source, URL url) {
            this.url = url;
            this.source = StringUtils.toUTF8Bytes(source);
        }
        
        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            if (u.equals(url)) {
                return new URLConnection(u) {
                    @Override
                    public void connect() throws IOException {
                    }
                    @Override
                    public InputStream getInputStream() throws IOException {
                        return new ByteArrayInputStream(source);
                    }
                    @Override
                    public int getContentLength() {
                        return source.length;
                    }
                    @Override
                    public String getContentType() {
                        return "text/html";
                    }
                    @Override
                    public String getContentEncoding() {
                        return "utf-8";
                    }
                };
            } else {
                return new URL(u.toExternalForm()).openConnection();
            }
        }
    }

}
