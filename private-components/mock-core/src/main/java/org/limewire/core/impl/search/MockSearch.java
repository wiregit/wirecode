package org.limewire.core.impl.search;

import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.core.api.Category;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult.PropertyKey;
import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.core.api.search.sponsored.SponsoredResultTarget;
import org.limewire.core.impl.search.sponsored.MockSponsoredResult;

public class MockSearch implements Search {
    public static final String SIMILAR_RESULT_PREFIX = "mock-similar-result-";
    
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
                for (int i = 0; i < 2; i++) {
                    try {
                        Thread.sleep((long)(1000 * (Math.random() * 5)));
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    addRecords(i);
                }
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

    private void addRecords(int i) {
        MockSearchResult msr;
        String name;

        // Create a search result that will be categorized as "Documents".
        msr = new MockSearchResult();
        name = "Lab19";
        msr.setExtension("doc");
        msr.setResultType(Category.DOCUMENT);
        msr.setSize(4567L);
        msr.addSource("ross");
        msr.addSource("phoebe");
        msr.addSource("rachel");
        msr.setUrn("www.mizzou.edu" + i);
        msr.setProperty(PropertyKey.AUTHOR, "Dr. Java");
        msr.setProperty(PropertyKey.DATE_CREATED,
            new GregorianCalendar(2008, 7, 27).getTimeInMillis());
        msr.setProperty(PropertyKey.FILE_SIZE, 1.7);
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.OWNER, "Ross Geller");
        msr.setProperty(PropertyKey.RELEVANCE, 0.3f);

        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "Lab19";
        msr.setExtension("doc");
        msr.setResultType(Category.DOCUMENT);
        msr.setSize(4567L);
        msr.addSource("similar");
        msr.setUrn("similar-www.mizzou.edu" + i);
        msr.setProperty(PropertyKey.AUTHOR, "Dr. Java");
        msr.setProperty(PropertyKey.DATE_CREATED,
                new GregorianCalendar(2008, 7, 27).getTimeInMillis());
        msr.setProperty(PropertyKey.FILE_SIZE, 1.7);
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.OWNER, "Ross Geller");
        msr.setProperty(PropertyKey.RELEVANCE, 0.3f);
        
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "Lab19";
        msr.setExtension("doc");
        msr.setResultType(Category.DOCUMENT);
        msr.setSize(4567L);
        msr.addSource("similar2");
        msr.setUrn("similar2-www.mizzou.edu" + i);
        msr.setProperty(PropertyKey.AUTHOR, "Dr. Java");
        msr.setProperty(PropertyKey.DATE_CREATED,
                new GregorianCalendar(2008, 7, 27).getTimeInMillis());
        msr.setProperty(PropertyKey.FILE_SIZE, 1.7);
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.OWNER, "Ross Geller");
        msr.setProperty(PropertyKey.RELEVANCE, 0.3f);
        
        handleSearchResult(msr);

        // Create a search result that will be categorized as "Other".
        msr = new MockSearchResult();
        name = "When Everyone has a Sweet Party and you're invited!";
        msr.setExtension("tmp");
        msr.setResultType(Category.OTHER);
        msr.setSize(1L);
        msr.addSource("phoebe");
        msr.setUrn("www.partytime.com" + i);
        msr.setProperty(PropertyKey.ARTIST_NAME, "Night Life");
        msr.setProperty(PropertyKey.COMMENTS, "Our album is awesome!");
        msr.setProperty(PropertyKey.FILE_SIZE, 2.8);
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.LENGTH, "4:19");
        msr.setProperty(PropertyKey.OWNER, "Phoebe Buffet");
        msr.setProperty(PropertyKey.RELEVANCE, 0.9f);
        msr.setProperty(PropertyKey.QUALITY, "good quality");
        handleSearchResult(msr);

        // Create a search result that will be categorized as "Music".
        msr = new MockSearchResult();
        name = "The Night Won't Last Long";
        msr.setExtension("mp3");
        msr.setResultType(Category.AUDIO);
        msr.setSize(1234L);
        msr.addSource("monica");
        msr.setUrn("www.solarsystem.net" + i);
        msr.setProperty(PropertyKey.ALBUM_TITLE, "Nightfall");
        msr.setProperty(PropertyKey.ARTIST_NAME, "The Buddies");
        msr.setProperty(PropertyKey.BITRATE, "192");
        msr.setProperty(PropertyKey.COMMENTS, "very jazzy");
        msr.setProperty(PropertyKey.FILE_SIZE, 3.9);
        msr.setProperty(PropertyKey.GENRE, "Jazz");
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.LENGTH, "4:31");
        msr.setProperty(PropertyKey.OWNER, "Monica Geller");
        msr.setProperty(PropertyKey.RELEVANCE, 0.7f);
        msr.setProperty(PropertyKey.QUALITY, "good quality");
        msr.setProperty(PropertyKey.QUALITY, "excellent quality");
        msr.setProperty(PropertyKey.SAMPLE_RATE, "44,100 Hz");
        msr.setProperty(PropertyKey.TRACK_NUMBER, "3");
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "The Night Won't Last Long";
        msr.setExtension("mp3");
        msr.setResultType(Category.AUDIO);
        msr.setSize(1234L);
        msr.addSource("monica-similar");
        msr.setUrn("similar-www.solarsystem.net" + i);
        msr.setProperty(PropertyKey.ALBUM_TITLE, "Nightfall");
        msr.setProperty(PropertyKey.ARTIST_NAME, "The Buddies");
        msr.setProperty(PropertyKey.BITRATE, "192");
        msr.setProperty(PropertyKey.COMMENTS, "very jazzy");
        msr.setProperty(PropertyKey.FILE_SIZE, 3.9);
        msr.setProperty(PropertyKey.GENRE, "Jazz");
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.LENGTH, "4:31");
        msr.setProperty(PropertyKey.OWNER, "Monica Geller");
        msr.setProperty(PropertyKey.RELEVANCE, 0.7f);
        msr.setProperty(PropertyKey.QUALITY, "good quality");
        msr.setProperty(PropertyKey.QUALITY, "excellent quality");
        msr.setProperty(PropertyKey.SAMPLE_RATE, "44,100 Hz");
        msr.setProperty(PropertyKey.TRACK_NUMBER, "3");
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "The Night Won't Last Long";
        msr.setExtension("mp3");
        msr.setResultType(Category.AUDIO);
        msr.setSize(1234L);
        for(int j = 0; j < i; j++) {
            msr.addSource("monica-similar2" + j);
        }
        msr.setUrn("similar-www.solarsystem.net2");
        msr.setProperty(PropertyKey.ALBUM_TITLE, "Nightfall");
        msr.setProperty(PropertyKey.ARTIST_NAME, "The Buddies");
        msr.setProperty(PropertyKey.BITRATE, "192");
        msr.setProperty(PropertyKey.COMMENTS, "very jazzy");
        msr.setProperty(PropertyKey.FILE_SIZE, 3.9);
        msr.setProperty(PropertyKey.GENRE, "Jazz");
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.LENGTH, "4:31");
        msr.setProperty(PropertyKey.OWNER, "Monica Geller");
        msr.setProperty(PropertyKey.RELEVANCE, 0.7f);
        msr.setProperty(PropertyKey.QUALITY, "good quality");
        msr.setProperty(PropertyKey.QUALITY, "excellent quality");
        msr.setProperty(PropertyKey.SAMPLE_RATE, "44,100 Hz");
        msr.setProperty(PropertyKey.TRACK_NUMBER, "3");
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "The Night Won't Last Long";
        msr.setExtension("mp3");
        msr.setResultType(Category.AUDIO);
        msr.setSize(1234L);
        msr.addSource("monica-similar3-is-a-loooooooooong-name");
        msr.setUrn("similar-www.solarsystem.net3" + i);
        msr.setProperty(PropertyKey.ALBUM_TITLE, "Nightfall");
        msr.setProperty(PropertyKey.ARTIST_NAME, "The Buddies");
        msr.setProperty(PropertyKey.BITRATE, "192");
        msr.setProperty(PropertyKey.COMMENTS, "very jazzy");
        msr.setProperty(PropertyKey.FILE_SIZE, 3.9);
        msr.setProperty(PropertyKey.GENRE, "Jazz");
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.LENGTH, "4:31");
        msr.setProperty(PropertyKey.OWNER, "Monica Geller");
        msr.setProperty(PropertyKey.RELEVANCE, 0.7f);
        msr.setProperty(PropertyKey.QUALITY, "good quality");
        msr.setProperty(PropertyKey.QUALITY, "excellent quality");
        msr.setProperty(PropertyKey.SAMPLE_RATE, "44,100 Hz");
        msr.setProperty(PropertyKey.TRACK_NUMBER, "3");
        handleSearchResult(msr);

        // Create a search result that will be categorized as "Video".
        msr = new MockSearchResult();
        name = "Monkey on Skateboard";
        msr.setExtension("ogm");
        msr.setResultType(Category.VIDEO);
        msr.setSize(9876L);
        msr.addSource("chandler");
        msr.addSource("joey");
        msr.setUrn("www.stlzoo.com" + i);
        msr.setProperty(PropertyKey.ARTIST_NAME, "St. Louis Zoo");
        msr.setProperty(PropertyKey.BITRATE, "5000");
        msr.setProperty(PropertyKey.COMMENTS,
            "Who knew they could do that?");
        msr.setProperty(PropertyKey.FILE_SIZE, 5.1);
        msr.setProperty(PropertyKey.HEIGHT, "480");
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.LENGTH, "0:48");
        msr.setProperty(PropertyKey.OWNER, "Chandler Bing");
        msr.setProperty(PropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(PropertyKey.RATING, "8");
        msr.setProperty(PropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(PropertyKey.QUALITY, "somewhat grainy");
        msr.setProperty(PropertyKey.WIDTH, "640");
        msr.setProperty(PropertyKey.YEAR, "2008");
        handleSearchResult(msr);

        // Create a search result that will be categorized as "Programs".
        msr = new MockSearchResult();
        name = "SuperSpreadsheet";
        msr.setExtension("exe");
        msr.setResultType(Category.PROGRAM);
        msr.setSize(8765L);
        msr.addSource("chandler");
        msr.setUrn("www.superspread.org" + i);
        msr.setProperty(PropertyKey.AUTHOR, "James Gosling");
        msr.setProperty(PropertyKey.COMPANY, "FriendSoft");
        msr.setProperty(PropertyKey.FILE_SIZE, 3.4);
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.OWNER, "Chandler Bing");
        msr.setProperty(PropertyKey.PLATFORM, "Mac OS X");
        msr.setProperty(PropertyKey.DATE_CREATED,
            new GregorianCalendar(2008, 9, 2).getTimeInMillis());
        msr.setProperty(PropertyKey.RELEVANCE, 0.6f);
        handleSearchResult(msr);

        // Create a search result that will be categorized as "Images".
        msr = new MockSearchResult();
        name = "EightGoldMedals";
        msr.setExtension("png");
        msr.setResultType(Category.IMAGE);
        msr.setSize(5678L);
        msr.addSource("rachel");
        msr.addSource("phoebe");
        msr.setUrn("www.swimming.org" + i);
        msr.setProperty(PropertyKey.FILE_SIZE, 0.9);
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.OWNER, "Rachel Green");
        msr.setProperty(PropertyKey.DATE_CREATED,
            new GregorianCalendar(2008, 7, 20).getTimeInMillis());
        msr.setProperty(PropertyKey.RELEVANCE, 0.8f);
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

    static class MockRemoteHost implements RemoteHost {
        private final String description;

        public MockRemoteHost(String description) {
            this.description = description;
        }
        
        @Override
        public boolean isBrowseHostEnabled() {
            return false;
        }

        @Override
        public boolean isChatEnabled() {
            return false;
        }

        @Override
        public boolean isSharingEnabled() {
            return false;
        }

        @Override
        public FriendPresence getFriendPresence() {
            return null;
        }

        @Override
        public String getRenderName() {
            return description;
        }
    }
}