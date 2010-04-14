package com.limegroup.bittorrent;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.Channels;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.DefaultedHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.limewire.bittorrent.TorrentScrapeData;
import org.limewire.bittorrent.bencoding.Token;
import org.limewire.nio.observer.Shutdownable;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HttpClientListener;
import com.limegroup.gnutella.http.HttpExecutor;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Reimplementation of libtorrents scrape code in java that detaches if 
 *  from the torrent manager control logic.
 *  
 * <p> Only supports HTTP scrape right now but UDP scrape is possible
 *      TODO: decouple udp_tracker_connection::send_udp_scrape()
 */
public class TrackerScraper {

    private final int TIMEOUT = 1500;
    
    /**
     *  Copied from escape_string.cpp in libtorrent
     */
    // http://www.ietf.org/rfc/rfc2396.txt
    // section 2.3
    private static String UNRESERVED_CHARS =
        // when determining if a url needs encoding
        // % should be ok
        "%+"
        // reserved (michaelt: ???? guess the trackers expect these ???)
        + ";?:@=&/"
        // unreserved (special characters) ' excluded,
        // since some buggy trackers fail with those
        // (michaelt:  ??? removed '$' since it seems to break things ??)
        + "-_.!~*(),"
        // unreserved (alphanumerics)
        + "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        + "0123456789";
    
    private static String ANNOUNCE_PATH = "/announce";
    private static String SCRAPE_PATH = "/scrape";
    
    private final Provider<HttpExecutor> httpExecutorProvider;
    private final Provider<HttpParams> defaultParamsProvider;

    @Inject
    public TrackerScraper(Provider<HttpExecutor> httpExecutorProvider,
            @Named("defaults") Provider<HttpParams> defaultParamsProvider) {
        this.httpExecutorProvider = httpExecutorProvider;
        this.defaultParamsProvider = defaultParamsProvider;
    }

    /**
     * Submit the scrape request.  Notification will be returned through the callback
     *
     * @return the shutdownable for the connection, or null if no 
     *          connection was supported.
     */
    public Shutdownable submitScrape(URI trackerAnnounceUri, URN urn,
            final ScrapeCallback callback) {

        System.out.println("attempting: " + trackerAnnounceUri);
        
        if (!canHTTPScrape(trackerAnnounceUri)) {
            System.out.println("scraping not available");
            
            // Tracker does not support scraping so don't attempt
            return null;
        }
        
        URI uri;
        try {
            uri = createScrapingRequest(trackerAnnounceUri, urn);
        } catch (URISyntaxException e) {
            // URI could not be generated for the scrape request so don't try
            return null;
        }

        HttpGet get = new HttpGet(uri);

        get.addHeader("User-Agent", LimeWireUtils.getHttpServer());
        get.addHeader(HTTPHeaderName.CONNECTION.httpStringValue(),"close");
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, TIMEOUT);
        params = new DefaultedHttpParams(params, defaultParamsProvider.get());
        
        System.out.println("submitting: " + uri);
        return httpExecutorProvider.get().execute(get, params, new HttpClientListener() {
            @Override
            public boolean requestFailed(HttpUriRequest request, HttpResponse response, IOException exc) {
                callback.failure("request failed");
                return false;
            }
            @Override
            public boolean requestComplete(HttpUriRequest request, HttpResponse response) {
                HttpEntity entity = response.getEntity();

                Object decoded = null;
                try {
                    decoded = Token.parse(Channels.newChannel(entity.getContent()), "UTF-8");
                } catch (IllegalStateException e) {
                    callback.failure(e.getMessage());
                    return false;
                } catch (IOException e) {
                    callback.failure(e.getMessage());
                    return false;
                }
                
                if(decoded == null || !(decoded instanceof Map<?,?>)) {
                    callback.failure("no scrape data in results downloaded");
                    return false;
                }
                
                Map<?,?> baseMap = (Map) decoded;
               
                Object filesElement = baseMap.get("files");
                
                if (filesElement == null || !(filesElement instanceof Map<?,?>)) {
                    callback.failure("scrape results had bad structure");
                    return false;
                }
                
                Map<?,?> torrentsMap = (Map) filesElement;
                
                if (torrentsMap.size() != 1) {
                    callback.failure("wrong number of elements in scrape results");
                    return false;
                }
                
                Object torrentScrapeEntry = torrentsMap.entrySet().iterator().next().getValue();
               
                if (!(torrentScrapeEntry instanceof Map<?,?>)) {
                    callback.failure("could not find torrent scrape entry in results");
                    return false;
                }
                
                Map<?,?> torrentScrapeEntryMap = (Map) torrentScrapeEntry;
                
                Object complete = torrentScrapeEntryMap.get("complete");
                Object incomplete = torrentScrapeEntryMap.get("incomplete");
                Object downloaded = torrentScrapeEntryMap.get("downloaded");

                if (complete == null || !(complete instanceof Long)) {
                    callback.failure("could not find well formed complete field");
                    return false;
                }
                
                if (incomplete == null || !(incomplete instanceof Long)) {
                    callback.failure("could not find well formed incomplete field");
                    return false;
                }
                
                if (downloaded == null || !(downloaded instanceof Long)) {
                    callback.failure("could not find well formed downloaded field");
                    return false;
                }

                callback.success(new ScrapeData((Long)complete, 
                        (Long)incomplete, 
                        (Long)downloaded));
                
                return false;
            }

            @Override
            public boolean allowRequest(HttpUriRequest request) {
                return true;
            }
        });
    }


    private static class ScrapeData implements TorrentScrapeData {
        private final long complete, incomplete, downloaded;

        private ScrapeData(long complete, long incomplete, long downloaded) {
            this.complete = complete;
            this.incomplete = incomplete;
            this.downloaded = downloaded;
        }
        public long getComplete() {
            return complete;
        }
        public long getIncomplete() {
            return incomplete;
        }
        public long getDownloaded() {
            return downloaded;
        }
        @Override
        public String toString() {
            return "[TrackerScraper complete=" + complete 
            + " incomplete=" + incomplete + 
            " downloaded=" + downloaded + "]";
        }
    }

    public static interface ScrapeCallback {
        void success(TorrentScrapeData data);
        void failure(String reason);
    }


    private static boolean canHTTPScrape(URI trackerAnnounceUri) {
        String announceString = trackerAnnounceUri.toString();
        
        return announceString.startsWith("http") && announceString.indexOf(ANNOUNCE_PATH) > 0;
    }
    
    private static URI createScrapingRequest(URI trackerAnnounceUri, URN urn) throws URISyntaxException {
        String scrapeUriString = trackerAnnounceUri.toString().replaceFirst(ANNOUNCE_PATH, SCRAPE_PATH);
        StringBuffer buffer = new StringBuffer(scrapeUriString);

        if (scrapeUriString.endsWith(SCRAPE_PATH)) {
            buffer.append('?');
        } else {
            buffer.append('&');
        }
        
        buffer.append("info_hash=");
        buffer.append(httpEncodeURN(urn));
        
        return new URI(buffer.toString());
    }
    
    private static String httpEncodeURN(URN urn) {
        StringBuffer sb = new StringBuffer();
        for ( byte b : urn.getBytes() ) {
            if (UNRESERVED_CHARS.indexOf((char)b) > -1) {
                sb.append((char)b);
            } else {
                sb.append('%');
                sb.append(Integer.toString((b & 0xff)+0x100, 16).substring(1));
            }
        }
        return sb.toString();
    }


}

