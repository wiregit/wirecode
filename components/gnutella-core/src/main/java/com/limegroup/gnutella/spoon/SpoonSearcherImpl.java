package com.limegroup.gnutella.spoon;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.core.settings.SpoonSettings;
import org.limewire.http.httpclient.HttpClientUtils;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.inject.LazySingleton;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.limegroup.gnutella.spoon.SpoonSearcher.SpoonSearchCallback;

@LazySingleton
class SpoonSearcherImpl implements SpoonSearcher {
    
    private final Provider<LimeHttpClient> httpClientProvider;
    private final ExecutorService executor;
    
    @Inject
    public SpoonSearcherImpl(Provider<LimeHttpClient> httpClientProvider) {
        this.httpClientProvider = httpClientProvider;
        this.executor = ExecutorsHelper.newThreadPool("SpoonThread");
    }
    
    @Override
    public void search(String query, SpoonSearchCallback callback) {
        executor.execute(new Searcher(query, callback));
    }
    
    private class Searcher implements Runnable {

        private final String query;
        private final SpoonSearchCallback callback;
        
        public Searcher(String query, SpoonSearchCallback callback) {
            this.query = query;
            this.callback = callback;
        }
        
        @Override
        public void run() {
            try {
                URL response = sendToServer(query);
                if(response != null)
                    callback.handle(response);
            } catch (IOException e) {
                //DO NOTHING
                e.printStackTrace();
            }
        }
        
        private URL sendToServer(String query) throws IOException {
            String url = "http://sprint8.api.spoon.aws1.lime:8080/mock/ad/limewire";
            return new URL(url);
//            HttpEntity postContent = new StringEntity(query);   
//            HttpPost httpPost = new HttpPost(SpoonSettings.SPOON_SEARCH_SERVER.get());
//            httpPost.addHeader("Connection", "close");
//            httpPost.addHeader("Content-Type", URLEncodedUtils.CONTENT_TYPE);
//            httpPost.setEntity(postContent);
//            HttpClient httpClient = httpClientProvider.get();
//            
//            HttpResponse response = null;
//            try {
//                response = httpClient.execute(httpPost);
//                int statusCode = response.getStatusLine().getStatusCode();
//                HttpEntity entity = response.getEntity();
//                if (statusCode != 200 || entity == null) {
//                    throw new IOException("invalid http response, status: " + statusCode
//                           + ", entity: " + ((entity != null) ? EntityUtils.toString(entity) : "none"));
//                }
//                return new URL(EntityUtils.toString(entity));
//            } finally {
//                HttpClientUtils.releaseConnection(response);
//            }
        }
        
    }
}
