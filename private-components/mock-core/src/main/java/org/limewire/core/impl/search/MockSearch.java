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

    private SearchDetails searchDetails;
    private CopyOnWriteArrayList<SearchListener> listeners =
        new CopyOnWriteArrayList<SearchListener>();

    private int repeatCount = 0;

    public MockSearch(SearchDetails searchDetails) {
        this.searchDetails = searchDetails;
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
        
        String query = searchDetails.getSearchQuery();
        query = query.toLowerCase();
        if(query.indexOf("juggl") > -1){
          addRcordsJuggling(i);
        }
        if(query.indexOf("paris") > -1){
            addRecordsParis(i);
        }
        if(query.indexOf("chicken") > -1){
            addRecordsWedding(i);
        }
        if(query.indexOf("monkey") > -1){
            addRecordsWedding(i);
            addRecordsMonkey(i);
        }
        if(query.indexOf("water") > -1){
            addRecordsWater(i);
        }
        if(query.indexOf("cookie") > -1){
            addRecordsWedding(i);
        }

        addRecordsSpam(i);
        addRecordsGeneral(i);

    }

    private void addRecordsGeneral(int i){
        MockSearchResult msr;
        String name;
        // Create a search result that will be categorized as "Documents".
        msr = new MockSearchResult();
        name = "Lab19";
        msr.setExtension("doc");
        msr.setResultType(Category.DOCUMENT);
        msr.setSize(45367L);
        msr.addSource("123.12.1.21");
        msr.addSource("123.123.1.21");
        msr.addSource("123.12.1.221");
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
        msr.setSize(45267L);
        msr.addSource("123.12.2.21");
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
        msr.setSize(14567L);
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
        name = "When Everyone has a Sweet Party and you're invited! I was at this totally swinging hepcat party last weekend. Oh man, that joint was jumpin!";
        msr.setExtension("tmp");
        msr.setResultType(Category.OTHER);
        msr.setSize(12L);
        msr.addSource("12.12.1.21");
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
        msr.setSize(4234L);
        msr.addSource("123.2.1.21");
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
        msr.setUrn("www.solarsystem.net" + i);
        msr.addSource("monica-similar");
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
        msr.setSize(21234L);
        for(int j = 0; j < i; j++) {
            msr.addSource("123.12.1.21-similar2" + j);
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
        msr.setSize(71234L);
        msr.setUrn("www.miza.com");
        msr.addSource("123.12.1.21-similar3-is-a-loooooooooong-name");
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
    private void addRecordsMonkey(int i){
        //monkey
        MockSearchResult msr;
        String name;

        msr = new MockSearchResult();
        name = "Monkey business";
        msr.setExtension("ogm");
        msr.setResultType(Category.VIDEO);
        msr.setSize(9876L);
        msr.addSource("127.1.1.21");
        msr.addSource("134.11.4.123");
        msr.setUrn("www.mtownzoo.com" + i);
        msr.setProperty(PropertyKey.ARTIST_NAME, "Morristown Zoo");
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

        msr = new MockSearchResult();
        name = "Dog on Skateboard";
        msr.setExtension("ogm");
        msr.setResultType(Category.VIDEO);
        msr.setSize(9876L);
        msr.addSource("127.1.1.21");
        msr.addSource("134.11.4.123");
        msr.setUrn("www.sdzoo.com" + i);
        msr.setProperty(PropertyKey.ARTIST_NAME, "San Diego Zoo");
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
        
        msr = new MockSearchResult();
        name = "Monkey on Skateboard";
        msr.setExtension("ogm");
        msr.setResultType(Category.VIDEO);
        msr.setSize(9876L);
        msr.addSource("127.1.1.21");
        msr.addSource("134.11.4.123");
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

        msr = new MockSearchResult();
        name = "No Monkeying in the cafeteria";
        msr.setExtension("avi");
        msr.setResultType(Category.VIDEO);
        msr.setSize(9876L);
        msr.addSource("127.1.1.21");
        msr.addSource("134.11.4.123");
        msr.setUrn("www.bracket.edu" + i);
        msr.setProperty(PropertyKey.ARTIST_NAME, "Bracket High School");
        msr.setProperty(PropertyKey.BITRATE, "5000");
        msr.setProperty(PropertyKey.COMMENTS,
            "Food fight");
        msr.setProperty(PropertyKey.FILE_SIZE, 5.1);
        msr.setProperty(PropertyKey.HEIGHT, "480");
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.LENGTH, "0:48");
        msr.setProperty(PropertyKey.OWNER, "Jo Hendricks");
        msr.setProperty(PropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(PropertyKey.RATING, "8");
        msr.setProperty(PropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(PropertyKey.QUALITY, "excellent");
        msr.setProperty(PropertyKey.WIDTH, "230");
        msr.setProperty(PropertyKey.YEAR, "2007");
        handleSearchResult(msr);
        
        msr = new MockSearchResult();
        name = "Monkey see monkey do";
        msr.setExtension("png");
        msr.setResultType(Category.IMAGE);
        msr.setSize(9876L);
        msr.addSource("127.1.1.21");
        msr.addSource("134.11.4.123");
        msr.setUrn("www.bracket.edu" + i);
        msr.setProperty(PropertyKey.ARTIST_NAME, "Monkey Matt");
        msr.setProperty(PropertyKey.BITRATE, "5000");
        msr.setProperty(PropertyKey.COMMENTS,
            "Food fight");
        msr.setProperty(PropertyKey.FILE_SIZE, 5.1);
        msr.setProperty(PropertyKey.HEIGHT, "480");
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.LENGTH, "0:48");
        msr.setProperty(PropertyKey.OWNER, "Matt Meddler");
        msr.setProperty(PropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(PropertyKey.RATING, "8");
        msr.setProperty(PropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(PropertyKey.QUALITY, "excellent");
        msr.setProperty(PropertyKey.WIDTH, "230");
        msr.setProperty(PropertyKey.YEAR, "2007");
        handleSearchResult(msr);
        
        msr = new MockSearchResult();
        name = "Monkey in the middle";
        msr.setExtension("txt");
        msr.setResultType(Category.DOCUMENT);
        msr.setSize(9876L);
        msr.addSource("127.1.1.21");
        msr.addSource("134.11.4.123");
        msr.setUrn("www.bracket.edu" + i);
        msr.setProperty(PropertyKey.ARTIST_NAME, "Fleet Corrs");
        msr.setProperty(PropertyKey.BITRATE, "5000");
        msr.setProperty(PropertyKey.COMMENTS,
            "Food fight");
        msr.setProperty(PropertyKey.FILE_SIZE, 5.1);
        msr.setProperty(PropertyKey.HEIGHT, "480");
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.LENGTH, "0:48");
        msr.setProperty(PropertyKey.OWNER, "Fleet Corrs");
        msr.setProperty(PropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(PropertyKey.RATING, "8");
        msr.setProperty(PropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(PropertyKey.QUALITY, "excellent");
        msr.setProperty(PropertyKey.WIDTH, "230");
        msr.setProperty(PropertyKey.YEAR, "2007");
        handleSearchResult(msr);
    }
    private void addRecordsWater(int i){
        MockSearchResult msr;
        String name;

        //waterfall
        msr = new MockSearchResult();
        name = "Waterfall";
        msr.setExtension("bmp");
        msr.setResultType(Category.IMAGE);
        msr.setSize(4567L);
        msr.setUrn("www.Fenix.edu" + i);
        msr.setProperty(PropertyKey.AUTHOR, "DownTown Stumpy");
        msr.setProperty(PropertyKey.DATE_CREATED,
            new GregorianCalendar(2008, 7, 27).getTimeInMillis());
        msr.setProperty(PropertyKey.FILE_SIZE, 11.7);
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.OWNER, "Floss Sential");
        msr.setProperty(PropertyKey.RELEVANCE, 0.9f);
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "water under the bridge";
        msr.setExtension("jpg");
        msr.setResultType(Category.IMAGE);
        msr.setSize(4567L);
        msr.setUrn("www.mizzou.edu" + i);
        msr.setProperty(PropertyKey.AUTHOR, "Jonsie Java");
        msr.setProperty(PropertyKey.DATE_CREATED,
            new GregorianCalendar(2008, 7, 27).getTimeInMillis());
        msr.setProperty(PropertyKey.FILE_SIZE, 11.7);
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.OWNER, "Moss Sential");
        msr.setProperty(PropertyKey.RELEVANCE, 0.9f);
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "water mellon dessert recipe";
        msr.setExtension("txt");
        msr.setResultType(Category.DOCUMENT);
        msr.setSize(4567L);
        msr.setUrn("www.mizzou.edu" + i);
        msr.setProperty(PropertyKey.AUTHOR, "Chef Sarah");
        msr.setProperty(PropertyKey.DATE_CREATED,
            new GregorianCalendar(2008, 4, 27).getTimeInMillis());
        msr.setProperty(PropertyKey.FILE_SIZE, 11.7);
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.OWNER, "Sarah Thistle");
        msr.setProperty(PropertyKey.RELEVANCE, 0.9f);
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "water fight";
        msr.setExtension("avi");
        msr.setResultType(Category.VIDEO);
        msr.setSize(4567L);
        msr.setUrn("www.mizzou.edu" + i);
        msr.setProperty(PropertyKey.AUTHOR, "Spencer Turtle");
        msr.setProperty(PropertyKey.DATE_CREATED,
            new GregorianCalendar(2008, 4, 27).getTimeInMillis());
        msr.setProperty(PropertyKey.FILE_SIZE, 11.7);
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.OWNER, "Spencer");
        msr.setProperty(PropertyKey.RELEVANCE, 0.9f);
        handleSearchResult(msr);
        
    }
    private void addRecordsSpam(int i){
        MockSearchResult msr;
        String name;
        
        // Create a search result that will be categorized as "Programs".
        msr = new MockSearchResult();
        name = "SuperSilly";
        msr.setExtension("exe");
        msr.setResultType(Category.PROGRAM);
        msr.setSize(8765L);
        msr.addSource("chandler");
        msr.setUrn("www.superspread.org" + i);
        msr.setProperty(PropertyKey.AUTHOR, "James Vanderbing");
        msr.setProperty(PropertyKey.COMPANY, "FriendSoft");
        msr.setProperty(PropertyKey.FILE_SIZE, 3.4);
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.OWNER, "Ding Bing");
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
        msr.addSource("143.12.1.21");
        msr.addSource("123.32.1.21");
        msr.setUrn("www.swimming.org" + i);
        msr.setProperty(PropertyKey.FILE_SIZE, 0.9);
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.OWNER, "Mister Green");
        msr.setProperty(PropertyKey.DATE_CREATED,
            new GregorianCalendar(2008, 7, 20).getTimeInMillis());
        msr.setProperty(PropertyKey.RELEVANCE, 0.8f);
        handleSearchResult(msr);

        
    }
    private void addRecordsWedding(int i){
        MockSearchResult msr;
        String name;
        
        msr = new MockSearchResult();
        name = "TheChickenDance";
        msr.setExtension("avi");
        msr.setResultType(Category.VIDEO);
        msr.setSize(9876L);
        msr.addSource("1.1.1.123");
        msr.setUrn("www.johnswedding.com");
        msr.setProperty(PropertyKey.ARTIST_NAME, "John's Wedding");
        msr.setProperty(PropertyKey.BITRATE, "5000");
        msr.setProperty(PropertyKey.COMMENTS,
            "Someone likes to dance");
        msr.setProperty(PropertyKey.FILE_SIZE, 5.1);
        msr.setProperty(PropertyKey.HEIGHT, "480");
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.LENGTH, "0:48");
        msr.setProperty(PropertyKey.OWNER, "John Stone");
        msr.setProperty(PropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(PropertyKey.RATING, "8");
        msr.setProperty(PropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(PropertyKey.QUALITY, "excellent");
        msr.setProperty(PropertyKey.WIDTH, "640");
        msr.setProperty(PropertyKey.YEAR, "2008");
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "Chicken Filet Recipe";
        msr.setExtension("doc");
        msr.setResultType(Category.DOCUMENT);
        msr.setSize(9876L);
        msr.addSource("1.1.43.123");
        msr.setUrn("www.catering.com");
        msr.setProperty(PropertyKey.FILE_SIZE, 3.9);
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.OWNER, "Chef Sarah");
        msr.setProperty(PropertyKey.RELEVANCE, 0.6f);
        handleSearchResult(msr);
       
        msr = new MockSearchResult();
        name = "Don't be a chicken";
        msr.setExtension("jpg");
        msr.setResultType(Category.IMAGE);
        msr.setSize(9876L);
        msr.addSource("1.1.1.123");
        msr.setUrn("www.joeswedding.com");
        msr.setProperty(PropertyKey.ARTIST_NAME, "Joe's Wedding");
        msr.setProperty(PropertyKey.COMMENTS,
            "Live life to live");
        msr.setProperty(PropertyKey.FILE_SIZE, 5.1);
        msr.setProperty(PropertyKey.OWNER, "Brent Sarah");
        msr.setProperty(PropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(PropertyKey.RATING, "8");
        msr.setProperty(PropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(PropertyKey.QUALITY, "excellent");
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.YEAR, "2008");
        handleSearchResult(msr);
        
        msr = new MockSearchResult();
        name = "Chicken pox treatment instructions";
        msr.setExtension("mp3");
        msr.setResultType(Category.AUDIO);
        msr.setSize(9876L);
        msr.addSource("1.1.1.123");
        msr.setUrn("www.medicalMe.com");
        msr.setProperty(PropertyKey.ARTIST_NAME, "Dr. John");
        msr.setProperty(PropertyKey.COMMENTS,
            "don't itch");
        msr.setProperty(PropertyKey.FILE_SIZE, 5.1);
        msr.setProperty(PropertyKey.OWNER, "John Hill");
        msr.setProperty(PropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(PropertyKey.RATING, "8");
        msr.setProperty(PropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(PropertyKey.YEAR, "2008");
        msr.setProperty(PropertyKey.NAME, name);
        handleSearchResult(msr);
        
        msr = new MockSearchResult();
        name = "Cookies";
        msr.setExtension("avi");
        msr.setResultType(Category.VIDEO);
        msr.setSize(9876L);
        msr.addSource("1.1.1.123");
        msr.setUrn("www.GoodForSole.com");
        msr.setProperty(PropertyKey.ARTIST_NAME, "PB&J");
        msr.setProperty(PropertyKey.BITRATE, "5000");
        msr.setProperty(PropertyKey.COMMENTS,
            "Peanut butter ones are the best");
        msr.setProperty(PropertyKey.FILE_SIZE, 5.1);
        msr.setProperty(PropertyKey.HEIGHT, "480");
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.LENGTH, "0:48");
        msr.setProperty(PropertyKey.OWNER, "John Stone");
        msr.setProperty(PropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(PropertyKey.RATING, "8");
        msr.setProperty(PropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(PropertyKey.QUALITY, "excellent");
        msr.setProperty(PropertyKey.WIDTH, "640");
        msr.setProperty(PropertyKey.YEAR, "2008");
        handleSearchResult(msr);
        

        
    }
    private void addRcordsJuggling(int i){

        MockSearchResult msr;
        String name;
        msr = new MockSearchResult();
        name = "Juggling at night";
        msr.setExtension("avi");
        msr.setResultType(Category.VIDEO);
        msr.setSize(5678L);
        msr.addSource("143.12.1.21");
        msr.addSource("123.32.1.21");
        msr.setUrn("www.juggling.org" + i);
        msr.setProperty(PropertyKey.FILE_SIZE, 50.9);
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.OWNER, "Greg Green");
        msr.setProperty(PropertyKey.DATE_CREATED,
            new GregorianCalendar(2008, 7, 20).getTimeInMillis());
        msr.setProperty(PropertyKey.RELEVANCE, 0.8f);
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "Juggling at Mignight";
        msr.setExtension("avi");
        msr.setResultType(Category.VIDEO);
        msr.setSize(5678L);
        msr.addSource("123.132.1.21");
        msr.setUrn("similar-www.juggling.com");
        msr.setProperty(PropertyKey.FILE_SIZE, 50.9);
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.OWNER, "Greg Green");
        msr.setProperty(PropertyKey.DATE_CREATED,
            new GregorianCalendar(2008, 7, 20).getTimeInMillis());
        msr.setProperty(PropertyKey.RELEVANCE, 0.8f);
        handleSearchResult(msr);

        
        msr = new MockSearchResult();
        name = "Ten ball Juggling";
        msr.setExtension("avi");
        msr.setResultType(Category.VIDEO);
        msr.setSize(5678L);
        msr.addSource("123.123.1.21");
        msr.setUrn("www.asdf.com");
        msr.setProperty(PropertyKey.FILE_SIZE, 15.9);
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.OWNER, "Michael Madison");
        msr.setProperty(PropertyKey.DATE_CREATED,
            new GregorianCalendar(2008, 7, 20).getTimeInMillis());
        msr.setProperty(PropertyKey.RELEVANCE, 0.8f);
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "Juggling a busy day";
        msr.setExtension("wav");
        msr.setResultType(Category.AUDIO);
        msr.setSize(5678L);
        msr.addSource("123.18.1.21");
        msr.setUrn("www.asdf3asdf.com");
        msr.setProperty(PropertyKey.FILE_SIZE, 15.9);
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.OWNER, "Michael Madison");
        msr.setProperty(PropertyKey.DATE_CREATED,
            new GregorianCalendar(2008, 7, 20).getTimeInMillis());
        msr.setProperty(PropertyKey.RELEVANCE, 0.8f);
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "How to juggle";
        msr.setExtension("avi");
        msr.setResultType(Category.VIDEO);
        msr.setSize(5678L);
        msr.addSource("123.12.12.21");
        msr.setUrn("www.ddasdrf.net");
        msr.setProperty(PropertyKey.FILE_SIZE, 15.9);
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.OWNER, "Michael Crede");
        msr.setProperty(PropertyKey.DATE_CREATED,
            new GregorianCalendar(2008, 7, 20).getTimeInMillis());
        msr.setProperty(PropertyKey.RELEVANCE, 0.8f);
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "Chainsaw Juggling";
        msr.setExtension("avi");
        msr.setResultType(Category.VIDEO);
        msr.setSize(5678L);
        msr.addSource("123.12.1.21");
        msr.setUrn("www.3asdfNet.com");
        msr.setProperty(PropertyKey.FILE_SIZE, 5.9);
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.OWNER, "Greg Green");
        msr.setProperty(PropertyKey.DATE_CREATED,
            new GregorianCalendar(2008, 7, 20).getTimeInMillis());
        msr.setProperty(PropertyKey.RELEVANCE, 0.8f);
        handleSearchResult(msr);

    }
    private void addRecordsParis(int i){
        MockSearchResult msr;
        String name;

        msr = new MockSearchResult();
        name = "Train schedules to Paris";
        msr.setExtension("txt");
        msr.setResultType(Category.DOCUMENT);
        msr.setSize(9876L);
        msr.addSource("1.12.13.2");
        msr.setUrn("www.23sola3rsystem.net3");
        msr.setProperty(PropertyKey.FILE_SIZE, 3.9);
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.OWNER, "Fred Teller");
        msr.setProperty(PropertyKey.RELEVANCE, 0.9f);
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "Proposal at Paris";
        msr.setExtension("ogm");
        msr.setResultType(Category.VIDEO);
        msr.setSize(9876L);
        msr.addSource("127.1.1.21");
        msr.setUrn("www.stlzood.com" + i);
        msr.setProperty(PropertyKey.ARTIST_NAME, "I'm engaged");
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

        msr = new MockSearchResult();
        name = "My Dog Paris";
        msr.setExtension("jpg");
        msr.setResultType(Category.IMAGE);
        msr.setSize(9876L);
        msr.addSource("1.1.1.123");
        msr.setUrn("www.joeswedding.com");
        msr.setProperty(PropertyKey.ARTIST_NAME, "Joe's Wedding");
        msr.setProperty(PropertyKey.COMMENTS,
            "Live life to live");
        msr.setProperty(PropertyKey.FILE_SIZE, 5.1);
        msr.setProperty(PropertyKey.OWNER, "Brent Sarah");
        msr.setProperty(PropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(PropertyKey.RATING, "8");
        msr.setProperty(PropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(PropertyKey.QUALITY, "excellent");
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.YEAR, "2008");
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "Paris Museum";
        msr.setExtension("jpg");
        msr.setResultType(Category.IMAGE);
        msr.setSize(9876L);
        msr.addSource("1.1.1.123");
        msr.setUrn("www.jsdfoeswedding.com");
        msr.setProperty(PropertyKey.ARTIST_NAME, "Joe's Wedding");
        msr.setProperty(PropertyKey.COMMENTS,
            "Live life to live");
        msr.setProperty(PropertyKey.FILE_SIZE, 5.1);
        msr.setProperty(PropertyKey.OWNER, "Sarah");
        msr.setProperty(PropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(PropertyKey.RATING, "8");
        msr.setProperty(PropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(PropertyKey.QUALITY, "excellent");
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.YEAR, "2008");
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "Paris Trip Plans";
        msr.setExtension("doc");
        msr.setResultType(Category.DOCUMENT);
        msr.setSize(9876L);
        msr.addSource("1.12.13.234");
        msr.setUrn("www.23sola3ddddrsystem.net3");
        msr.setProperty(PropertyKey.FILE_SIZE, 3.9);
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.OWNER, "Fred Teller");
        msr.setProperty(PropertyKey.RELEVANCE, 0.9f);
        handleSearchResult(msr);
        
        msr = new MockSearchResult();
        name = "Paris at night";
        msr.setExtension(".jpg");
        msr.setResultType(Category.IMAGE);
        msr.setSize(9876L);
        msr.setUrn("www.1sdfmizaz.com");
        msr.addSource("1.13.1.12");
        msr.setProperty(PropertyKey.FILE_SIZE, 31.9);
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.OWNER, "Sammy Teufle");
        msr.setProperty(PropertyKey.RELEVANCE, 0.7f);
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "Passport to Paris ";
        msr.setExtension(".doc");
        msr.setResultType(Category.DOCUMENT);
        msr.setSize(9876L);
        msr.addSource("2.1.1.13");
        msr.setUrn("www.tadsf3iza.com");
        msr.setProperty(PropertyKey.FILE_SIZE, 11.9);
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.OWNER, "John Bakker");
        msr.setProperty(PropertyKey.RELEVANCE, 0.7f);
        handleSearchResult(msr);
        msr = new MockSearchResult();

        name = "Notes on Paris";
        msr.setExtension("txt");
        msr.setResultType(Category.DOCUMENT);
        msr.setSize(9876L);
        msr.addSource("81.12.13.32");
        msr.setUrn("www.23sol3324a3rsystem.net3");
        msr.setProperty(PropertyKey.FILE_SIZE, 3.9);
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.OWNER, "Fred Teller");
        msr.setProperty(PropertyKey.RELEVANCE, 0.9f);
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "Ode to Paris ";
        msr.setExtension(".txt");
        msr.setResultType(Category.DOCUMENT);
        msr.setSize(976L);
        msr.addSource("11.1.13.1");
        msr.setUrn("www.figiza.com");
        msr.setProperty(PropertyKey.FILE_SIZE, 1.9);
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.OWNER, "John Derrick");
        msr.setProperty(PropertyKey.RELEVANCE, 0.7f);
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "Paris, Texas";
        msr.setExtension(".jpg");
        msr.setResultType(Category.IMAGE);
        msr.setSize(9476L);
        msr.addSource("1.121.1.1");
        msr.setUrn("www.azdfaiza4.com");
        msr.setProperty(PropertyKey.FILE_SIZE, 10.9);
        msr.setProperty(PropertyKey.NAME, name);
        msr.setProperty(PropertyKey.OWNER, "Eric Johanson");
        msr.setProperty(PropertyKey.RELEVANCE, 0.7f);
        handleSearchResult(msr);

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