package org.limewire.core.impl.search;

import static org.limewire.core.api.search.SearchResult.PropertyKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.GregorianCalendar;
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
    }

    @Override
    public void addSearchListener(SearchListener searchListener) {
        listeners.add(searchListener);
    }
    
    // TODO: RMV What should this method do?
    @Override
    public SearchCategory getCategory() {
        return null;
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
    
    @Override
    public void stop() {
        for (SearchListener listener : listeners) {
            listener.searchStopped();
        }
    }
    
    private void addResults(final String suffix) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                MockSearchResult msr;
                String name;

                // Create a search result that will be categorized as "Documents".
                msr = new MockSearchResult();
                name = "Lab19.doc";
                msr.setDescription(name);
                msr.setExtension("doc");
                msr.setResultType(ResultType.DOCUMENT);
                msr.setSize(4567L);
                msr.addSource("ross");
                msr.setUrn("www.mizzou.edu");
                msr.setProperty(PropertyKey.AUTHOR, "Dr. Java");
                msr.setProperty(PropertyKey.DATE_CREATED,
                    new GregorianCalendar(2008, 7, 27));
                msr.setProperty(PropertyKey.NAME, name);
                msr.setProperty(PropertyKey.RELEVANCE, 0.3f);
                handleSearchResult(msr);

                // Create a search result that will be categorized as "Other".
                msr = new MockSearchResult();
                name = "When Everyone has a Sweet Party";
                msr.setDescription(name);
                msr.setExtension("tmp");
                msr.setResultType(ResultType.OTHER);
                msr.setSize(1L);
                msr.addSource("phoebe");
                msr.setUrn("www.partytime.com");
                msr.setProperty(PropertyKey.ARTIST_NAME, "Night Life");
                msr.setProperty(PropertyKey.COMMENTS, "Our album is awesome!");
                msr.setProperty(PropertyKey.NAME, name);
                msr.setProperty(PropertyKey.LENGTH, "4:19");
                msr.setProperty(PropertyKey.RELEVANCE, 0.9f);
                msr.setProperty(PropertyKey.QUALITY, "good quality");
                handleSearchResult(msr);

                // Create a search result that will be categorized as "Music".
                msr = new MockSearchResult();
                name = "The Night Won't Last Long";
                msr.setDescription(name);
                msr.setExtension("mp3");
                msr.setResultType(ResultType.AUDIO);
                msr.setSize(1234L);
                msr.addSource("monica");
                msr.setUrn("www.solarsystem.net");
                msr.setProperty(PropertyKey.ALBUM_TITLE, "Nightfall");
                msr.setProperty(PropertyKey.ARTIST_NAME, "The Buddies");
                msr.setProperty(PropertyKey.BITRATE, "192");
                msr.setProperty(PropertyKey.COMMENTS, "very jazzy");
                msr.setProperty(PropertyKey.GENRE, "Jazz");
                msr.setProperty(PropertyKey.NAME, name);
                msr.setProperty(PropertyKey.LENGTH, "4:31");
                msr.setProperty(PropertyKey.RELEVANCE, 0.7f);
                msr.setProperty(PropertyKey.QUALITY, "good quality");
                msr.setProperty(PropertyKey.QUALITY, "excellent quality");
                msr.setProperty(PropertyKey.SAMPLE_RATE, "44,100 Hz");
                msr.setProperty(PropertyKey.TRACK_NUMBER, "3");
                handleSearchResult(msr);

                // Create a search result that will be categorized as "Video".
                msr = new MockSearchResult();
                name = "Monkey on Skateboard";
                msr.setDescription(name);
                msr.setExtension("ogv");
                msr.setResultType(ResultType.VIDEO);
                msr.setSize(1234L);
                msr.addSource("chandler");
                msr.addSource("joey");
                msr.setUrn("www.stlzoo.com");
                msr.setProperty(PropertyKey.ARTIST_NAME, "St. Louis Zoo");
                msr.setProperty(PropertyKey.COMMENTS,
                    "Who knew they could do that?");
                msr.setProperty(PropertyKey.HEIGHT, "480");
                msr.setProperty(PropertyKey.NAME, name);
                msr.setProperty(PropertyKey.LENGTH, "0:48");
                msr.setProperty(PropertyKey.RATING, "8");
                msr.setProperty(PropertyKey.RELEVANCE, 0.3f);
                msr.setProperty(PropertyKey.QUALITY, "somewhat grainy");
                msr.setProperty(PropertyKey.WIDTH, "640");
                msr.setProperty(PropertyKey.YEAR, "2008");
                handleSearchResult(msr);

                // Create a search result that will be categorized as "Images".
                msr = new MockSearchResult();
                name = "EightGoldMedals.png";
                msr.setDescription(name);
                msr.setExtension("png");
                msr.setResultType(ResultType.IMAGE);
                msr.setSize(5678L);
                msr.addSource("rachel");
                msr.addSource("phoebe");
                msr.setUrn("www.swimming.org");
                msr.setProperty(PropertyKey.NAME, name);
                msr.setProperty(PropertyKey.DATE_CREATED,
                    new GregorianCalendar(2008, 7, 20));
                msr.setProperty(PropertyKey.RELEVANCE, 0.8f);
                // TODO: RMV For some reason the last search result doesn't make it to the GUI!
                handleSearchResult(msr);

                try {
                    Thread.sleep(1000);
                } catch(InterruptedException ignored) {}

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

        private final Map<PropertyKey, Object> properties =
            new HashMap<PropertyKey, Object>();

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
        public Object getProperty(PropertyKey key) {
             return getProperties().get(key);
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

        public void setProperty(PropertyKey key, Object value) {
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

        @Override
        public String toString() {
            return getClass().getName() + ": " + getProperty(PropertyKey.NAME);
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