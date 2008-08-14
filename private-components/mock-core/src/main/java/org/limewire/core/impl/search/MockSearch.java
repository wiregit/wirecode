package org.limewire.core.impl.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.endpoint.RemoteHostAction;
import org.limewire.core.api.search.ResultType;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.core.api.search.sponsored.SponsoredResultTarget;
import org.limewire.core.impl.search.sponsored.MockSponsoredResult;

public class MockSearch implements Search {
    
    private CopyOnWriteArrayList<SearchListener> listeners =
        new CopyOnWriteArrayList<SearchListener>();

    private int repeatCount = 0;

    public MockSearch(SearchDetails searchDetails) {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void stop() {
        for (SearchListener listener : listeners) {
            listener.searchStopped();
        }
    }
    
    @Override
    public SearchCategory getCategory() {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public void addSearchListener(SearchListener searchListener) {
        listeners.add(searchListener);
    }
    
    @Override
    public void removeSearchListener(SearchListener searchListener) {
        listeners.remove(searchListener);
    }
    
    @Override
    public void start() {
        for (SearchListener listener : listeners) {
            listener.searchStarted();
        }
        addResults("");
    }
    
    private void addResults(final String suffix) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                MockSearchResult msr = new MockSearchResult();
                String title = "When Everyone has a Sweet Party";
                msr.setDescription(title);
                msr.setExtension("tmp");
                msr.setResultType(ResultType.UNKNOWN);
                msr.setSize(1L);
                msr.addSource("phoebe");
                msr.setUrn("www.partytime.com");
                msr.setProperty(SearchResult.PropertyKey.ARTIST_NAME,
                    "Night Life");
                msr.setProperty(SearchResult.PropertyKey.COMMENT,
                    "Our album is awesome!");
                msr.setProperty(SearchResult.PropertyKey.QUALITY,
                    "good quality");
                msr.setProperty(SearchResult.PropertyKey.TRACK_NAME,
                    title);
                msr.setProperty(SearchResult.PropertyKey.TRACK_TIME,
                    "4:19");
                handleSearchResult(msr);

                msr = new MockSearchResult();
                title = "The Night Won't Last Long";
                msr.setDescription(title);
                msr.setExtension("mp3");
                msr.setResultType(ResultType.AUDIO);
                msr.setSize(1234L);
                msr.addSource("monica");
                msr.setUrn("www.solarsystem.net");
                msr.setProperty(SearchResult.PropertyKey.ARTIST_NAME,
                    "The Buddies");
                msr.setProperty(SearchResult.PropertyKey.COMMENT,
                    "very jazzy");
                msr.setProperty(SearchResult.PropertyKey.QUALITY,
                    "excellent quality");
                msr.setProperty(SearchResult.PropertyKey.TRACK_NAME,
                    title);
                msr.setProperty(SearchResult.PropertyKey.TRACK_TIME,
                    "4:31");
                handleSearchResult(msr);

                msr = new MockSearchResult();
                title = "Monkey on Skateboard";
                msr.setDescription(title);
                msr.setExtension("ogv");
                msr.setResultType(ResultType.VIDEO);
                msr.setSize(1234L);
                msr.addSource("chandler");
                msr.addSource("joey");
                msr.setUrn("www.stlzoo.com");
                msr.setProperty(SearchResult.PropertyKey.ARTIST_NAME,
                    "St. Louis Zoo");
                msr.setProperty(SearchResult.PropertyKey.COMMENT,
                    "Who knew they could do that?");
                msr.setProperty(SearchResult.PropertyKey.QUALITY,
                    "somewhat grainy");
                msr.setProperty(SearchResult.PropertyKey.TRACK_NAME,
                    title);
                msr.setProperty(SearchResult.PropertyKey.TRACK_TIME,
                    "0:48");
                handleSearchResult(msr);

                try { Thread.sleep(1000); } catch(InterruptedException ignored) {}
                SponsoredResult sponsored = new MockSponsoredResult(
                    "Internal Ad",
                    "a ad a daflad fajla\naldjfla awejl sdaf", 
                    "store.limewire.com",
                    "http://www.store.limewire.com/store/app/pages/help/Help/",
                    SponsoredResultTarget.STORE);
                SponsoredResult sponsored2 = new MockSponsoredResult(
                    "External Ad",
                    "a ad a daflad fajla\naldjfla awejl sdaf",
                    "google.com",
                    "http://google.com",
                    SponsoredResultTarget.EXTERNAL);
                handleSponsoredResults(sponsored, sponsored2);
            }
        }).start();
    }
    
    private void handleSearchResult(MockSearchResult mock) {
        for (SearchListener listener : listeners) {
            listener.handleSearchResult(mock);
        }
    }
    
    private void handleSponsoredResults(SponsoredResult... sponsoredResults) {
        List<SponsoredResult> mockList =  Arrays.asList(sponsoredResults);
        for (SearchListener listener : listeners) {
            listener.handleSponsoredResults(mockList);
        }
    }
    
    @Override
    public void repeat() {
        for (SearchListener listener : listeners) {
            listener.searchStarted();
        }
        addResults("rp" + repeatCount++);
    }

    private static class MockSearchResult implements SearchResult {

        private final List<RemoteHost> sources = new ArrayList<RemoteHost>();

        private final Map<SearchResult.PropertyKey, Object> properties =
            new HashMap<SearchResult.PropertyKey, Object>();

        private String description;
        private String extension;
        private String urn;
        private ResultType resultType;
        private long size;

        public void addSource(String host) {
            sources.add(new MockRemoteHost(host));
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public String getFileExtension() {
            return extension;
        }

        @Override
        public Map<PropertyKey, Object> getProperties() {
            return properties;
        }

        @Override
        public ResultType getResultType() {
            return resultType;
        }

        @Override
        public long getSize() {
            return size;
        }

        @Override
        public List<RemoteHost> getSources() {
            return sources;
        }

        @Override
        public String getUrn() {
            return urn;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public void setExtension(String extension) {
            this.extension = extension;
        }

        public void setProperty(SearchResult.PropertyKey key, Object value) {
            properties.put(key, value);
        }

        public void setResultType(ResultType resultType) {
            this.resultType = resultType;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public void setUrn(String urn) {
            this.urn = urn;
        }
    }

    private static class MockRemoteHost implements RemoteHost {
        private final String description;

        public MockRemoteHost(String description) {
            this.description = description;
        }

        @Override
        public List<RemoteHostAction> getHostActions() {
            return Collections.emptyList();
        }

        @Override
        public String getHostDescription() {
            return description;
        }
    }
}