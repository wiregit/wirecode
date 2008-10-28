package org.limewire.core.impl.search;

import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchListener;
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
        msr.setProperty(FilePropertyKey.AUTHOR, "Dr. Java");
        msr.setProperty(FilePropertyKey.DATE_CREATED,
            new GregorianCalendar(2008, 7, 27).getTimeInMillis());
        msr.setProperty(FilePropertyKey.FILE_SIZE, 1.7);
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.OWNER, "Ross Geller");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.3f);

        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "Lab19";
        msr.setExtension("doc");
        msr.setResultType(Category.DOCUMENT);
        msr.setSize(45267L);
        msr.addSource("123.12.2.21");
        msr.setUrn("similar-www.mizzou.edu" + i);
        msr.setProperty(FilePropertyKey.AUTHOR, "Dr. Java");
        msr.setProperty(FilePropertyKey.DATE_CREATED,
                new GregorianCalendar(2008, 7, 27).getTimeInMillis());
        msr.setProperty(FilePropertyKey.FILE_SIZE, 1.7);
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.OWNER, "Ross Geller");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.3f);
        
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "Lab19";
        msr.setExtension("doc");
        msr.setResultType(Category.DOCUMENT);
        msr.setSize(14567L);
        msr.addSource("similar2");
        msr.setUrn("similar2-www.mizzou.edu" + i);
        msr.setProperty(FilePropertyKey.AUTHOR, "Dr. Java");
        msr.setProperty(FilePropertyKey.DATE_CREATED,
                new GregorianCalendar(2008, 7, 27).getTimeInMillis());
        msr.setProperty(FilePropertyKey.FILE_SIZE, 1.7);
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.OWNER, "Ross Geller");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.3f);
        
        handleSearchResult(msr);

        // Create a search result that will be categorized as "Other".
        msr = new MockSearchResult();
        name = "When Everyone has a Sweet Party and you're invited! I was at this totally swinging hepcat party last weekend. Oh man, that joint was jumpin!";
        msr.setExtension("tmp");
        msr.setResultType(Category.OTHER);
        msr.setSize(12L);
        msr.addSource("12.12.1.21");
        msr.setUrn("www.partytime.com" + i);
        msr.setProperty(FilePropertyKey.AUTHOR, "Night Life");
        msr.setProperty(FilePropertyKey.COMMENTS, "Our album is awesome!");
        msr.setProperty(FilePropertyKey.FILE_SIZE, 2.8);
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.LENGTH, "4:19");
        msr.setProperty(FilePropertyKey.OWNER, "Phoebe Buffet");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.9f);
        msr.setProperty(FilePropertyKey.QUALITY, "good quality");
        handleSearchResult(msr);

        // Create a search result that will be categorized as "Music".
        msr = new MockSearchResult();
        name = "The Night Won't Last Long";
        msr.setExtension("mp3");
        msr.setResultType(Category.AUDIO);
        msr.setSize(4234L);
        msr.addSource("123.2.1.21");
        msr.setUrn("www.solarsystem.net" + i);
        msr.setProperty(FilePropertyKey.TITLE, "Nightfall");
        msr.setProperty(FilePropertyKey.AUTHOR, "The Buddies");
        msr.setProperty(FilePropertyKey.BITRATE, "192");
        msr.setProperty(FilePropertyKey.COMMENTS, "very jazzy");
        msr.setProperty(FilePropertyKey.FILE_SIZE, 3.9);
        msr.setProperty(FilePropertyKey.GENRE, "Jazz");
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.LENGTH, "4:31");
        msr.setProperty(FilePropertyKey.OWNER, "Monica Geller");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.7f);
        msr.setProperty(FilePropertyKey.QUALITY, "good quality");
        msr.setProperty(FilePropertyKey.QUALITY, "excellent quality");
        msr.setProperty(FilePropertyKey.SAMPLE_RATE, "44,100 Hz");
        msr.setProperty(FilePropertyKey.TRACK_NUMBER, "3");
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "The Night Won't Last Long";
        msr.setExtension("mp3");
        msr.setResultType(Category.AUDIO);
        msr.setSize(1234L);
        msr.setUrn("www.solarsystem.net" + i);
        msr.addSource("monica-similar");
        msr.setProperty(FilePropertyKey.TITLE, "Nightfall");
        msr.setProperty(FilePropertyKey.AUTHOR, "The Buddies");
        msr.setProperty(FilePropertyKey.BITRATE, "192");
        msr.setProperty(FilePropertyKey.COMMENTS, "very jazzy");
        msr.setProperty(FilePropertyKey.FILE_SIZE, 3.9);
        msr.setProperty(FilePropertyKey.GENRE, "Jazz");
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.LENGTH, "4:31");
        msr.setProperty(FilePropertyKey.OWNER, "Monica Geller");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.7f);
        msr.setProperty(FilePropertyKey.QUALITY, "good quality");
        msr.setProperty(FilePropertyKey.QUALITY, "excellent quality");
        msr.setProperty(FilePropertyKey.SAMPLE_RATE, "44,100 Hz");
        msr.setProperty(FilePropertyKey.TRACK_NUMBER, "3");
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
        msr.setProperty(FilePropertyKey.TITLE, "Nightfall");
        msr.setProperty(FilePropertyKey.AUTHOR, "The Buddies");
        msr.setProperty(FilePropertyKey.BITRATE, "192");
        msr.setProperty(FilePropertyKey.COMMENTS, "very jazzy");
        msr.setProperty(FilePropertyKey.FILE_SIZE, 3.9);
        msr.setProperty(FilePropertyKey.GENRE, "Jazz");
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.LENGTH, "4:31");
        msr.setProperty(FilePropertyKey.OWNER, "Monica Geller");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.7f);
        msr.setProperty(FilePropertyKey.QUALITY, "good quality");
        msr.setProperty(FilePropertyKey.QUALITY, "excellent quality");
        msr.setProperty(FilePropertyKey.SAMPLE_RATE, "44,100 Hz");
        msr.setProperty(FilePropertyKey.TRACK_NUMBER, "3");
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "The Night Won't Last Long";
        msr.setExtension("mp3");
        msr.setResultType(Category.AUDIO);
        msr.setSize(71234L);
        msr.setUrn("www.miza.com");
        msr.addSource("123.12.1.21-similar3-is-a-loooooooooong-name");
        msr.setProperty(FilePropertyKey.TITLE, "Nightfall");
        msr.setProperty(FilePropertyKey.AUTHOR, "The Buddies");
        msr.setProperty(FilePropertyKey.BITRATE, "192");
        msr.setProperty(FilePropertyKey.COMMENTS, "very jazzy");
        msr.setProperty(FilePropertyKey.FILE_SIZE, 3.9);
        msr.setProperty(FilePropertyKey.GENRE, "Jazz");
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.LENGTH, "4:31");
        msr.setProperty(FilePropertyKey.OWNER, "Monica Geller");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.7f);
        msr.setProperty(FilePropertyKey.QUALITY, "good quality");
        msr.setProperty(FilePropertyKey.QUALITY, "excellent quality");
        msr.setProperty(FilePropertyKey.SAMPLE_RATE, "44,100 Hz");
        msr.setProperty(FilePropertyKey.TRACK_NUMBER, "3");
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
        msr.setProperty(FilePropertyKey.AUTHOR, "Morristown Zoo");
        msr.setProperty(FilePropertyKey.BITRATE, "5000");
        msr.setProperty(FilePropertyKey.COMMENTS,
            "Who knew they could do that?");
        msr.setProperty(FilePropertyKey.FILE_SIZE, 5.1);
        msr.setProperty(FilePropertyKey.HEIGHT, "480");
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.LENGTH, "0:48");
        msr.setProperty(FilePropertyKey.OWNER, "Chandler Bing");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(FilePropertyKey.RATING, "8");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(FilePropertyKey.QUALITY, "somewhat grainy");
        msr.setProperty(FilePropertyKey.WIDTH, "640");
        msr.setProperty(FilePropertyKey.YEAR, "2008");
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "Dog on Skateboard";
        msr.setExtension("ogm");
        msr.setResultType(Category.VIDEO);
        msr.setSize(9876L);
        msr.addSource("127.1.1.21");
        msr.addSource("134.11.4.123");
        msr.setUrn("www.sdzoo.com" + i);
        msr.setProperty(FilePropertyKey.AUTHOR, "San Diego Zoo");
        msr.setProperty(FilePropertyKey.BITRATE, "5000");
        msr.setProperty(FilePropertyKey.COMMENTS,
            "Who knew they could do that?");
        msr.setProperty(FilePropertyKey.FILE_SIZE, 5.1);
        msr.setProperty(FilePropertyKey.HEIGHT, "480");
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.LENGTH, "0:48");
        msr.setProperty(FilePropertyKey.OWNER, "Chandler Bing");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(FilePropertyKey.RATING, "8");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(FilePropertyKey.QUALITY, "somewhat grainy");
        msr.setProperty(FilePropertyKey.WIDTH, "640");
        msr.setProperty(FilePropertyKey.YEAR, "2008");
        handleSearchResult(msr);
        
        msr = new MockSearchResult();
        name = "Monkey on Skateboard";
        msr.setExtension("ogm");
        msr.setResultType(Category.VIDEO);
        msr.setSize(9876L);
        msr.addSource("127.1.1.21");
        msr.addSource("134.11.4.123");
        msr.setUrn("www.stlzoo.com" + i);
        msr.setProperty(FilePropertyKey.AUTHOR, "St. Louis Zoo");
        msr.setProperty(FilePropertyKey.BITRATE, "5000");
        msr.setProperty(FilePropertyKey.COMMENTS,
            "Who knew they could do that?");
        msr.setProperty(FilePropertyKey.FILE_SIZE, 5.1);
        msr.setProperty(FilePropertyKey.HEIGHT, "480");
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.LENGTH, "0:48");
        msr.setProperty(FilePropertyKey.OWNER, "Chandler Bing");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(FilePropertyKey.RATING, "8");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(FilePropertyKey.QUALITY, "somewhat grainy");
        msr.setProperty(FilePropertyKey.WIDTH, "640");
        msr.setProperty(FilePropertyKey.YEAR, "2008");
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "No Monkeying in the cafeteria";
        msr.setExtension("avi");
        msr.setResultType(Category.VIDEO);
        msr.setSize(9876L);
        msr.addSource("127.1.1.21");
        msr.addSource("134.11.4.123");
        msr.setUrn("www.bracket.edu" + i);
        msr.setProperty(FilePropertyKey.AUTHOR, "Bracket High School");
        msr.setProperty(FilePropertyKey.BITRATE, "5000");
        msr.setProperty(FilePropertyKey.COMMENTS,
            "Food fight");
        msr.setProperty(FilePropertyKey.FILE_SIZE, 5.1);
        msr.setProperty(FilePropertyKey.HEIGHT, "480");
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.LENGTH, "0:48");
        msr.setProperty(FilePropertyKey.OWNER, "Jo Hendricks");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(FilePropertyKey.RATING, "8");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(FilePropertyKey.QUALITY, "excellent");
        msr.setProperty(FilePropertyKey.WIDTH, "230");
        msr.setProperty(FilePropertyKey.YEAR, "2007");
        handleSearchResult(msr);
        
        msr = new MockSearchResult();
        name = "Monkey see monkey do";
        msr.setExtension("png");
        msr.setResultType(Category.IMAGE);
        msr.setSize(9876L);
        msr.addSource("127.1.1.21");
        msr.addSource("134.11.4.123");
        msr.setUrn("www.bracket.edu" + i);
        msr.setProperty(FilePropertyKey.AUTHOR, "Monkey Matt");
        msr.setProperty(FilePropertyKey.BITRATE, "5000");
        msr.setProperty(FilePropertyKey.COMMENTS,
            "Food fight");
        msr.setProperty(FilePropertyKey.FILE_SIZE, 5.1);
        msr.setProperty(FilePropertyKey.HEIGHT, "480");
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.LENGTH, "0:48");
        msr.setProperty(FilePropertyKey.OWNER, "Matt Meddler");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(FilePropertyKey.RATING, "8");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(FilePropertyKey.QUALITY, "excellent");
        msr.setProperty(FilePropertyKey.WIDTH, "230");
        msr.setProperty(FilePropertyKey.YEAR, "2007");
        handleSearchResult(msr);
        
        msr = new MockSearchResult();
        name = "Monkey in the middle";
        msr.setExtension("txt");
        msr.setResultType(Category.DOCUMENT);
        msr.setSize(9876L);
        msr.addSource("127.1.1.21");
        msr.addSource("134.11.4.123");
        msr.setUrn("www.bracket.edu" + i);
        msr.setProperty(FilePropertyKey.AUTHOR, "Fleet Corrs");
        msr.setProperty(FilePropertyKey.BITRATE, "5000");
        msr.setProperty(FilePropertyKey.COMMENTS,
            "Food fight");
        msr.setProperty(FilePropertyKey.FILE_SIZE, 5.1);
        msr.setProperty(FilePropertyKey.HEIGHT, "480");
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.LENGTH, "0:48");
        msr.setProperty(FilePropertyKey.OWNER, "Fleet Corrs");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(FilePropertyKey.RATING, "8");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(FilePropertyKey.QUALITY, "excellent");
        msr.setProperty(FilePropertyKey.WIDTH, "230");
        msr.setProperty(FilePropertyKey.YEAR, "2007");
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
        msr.setProperty(FilePropertyKey.AUTHOR, "DownTown Stumpy");
        msr.setProperty(FilePropertyKey.DATE_CREATED,
            new GregorianCalendar(2008, 7, 27).getTimeInMillis());
        msr.setProperty(FilePropertyKey.FILE_SIZE, 11.7);
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.OWNER, "Floss Sential");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.9f);
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "water under the bridge";
        msr.setExtension("jpg");
        msr.setResultType(Category.IMAGE);
        msr.setSize(4567L);
        msr.setUrn("www.mizzou.edu" + i);
        msr.setProperty(FilePropertyKey.AUTHOR, "Jonsie Java");
        msr.setProperty(FilePropertyKey.DATE_CREATED,
            new GregorianCalendar(2008, 7, 27).getTimeInMillis());
        msr.setProperty(FilePropertyKey.FILE_SIZE, 11.7);
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.OWNER, "Moss Sential");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.9f);
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "water mellon dessert recipe";
        msr.setExtension("txt");
        msr.setResultType(Category.DOCUMENT);
        msr.setSize(4567L);
        msr.setUrn("www.mizzou.edu" + i);
        msr.setProperty(FilePropertyKey.AUTHOR, "Chef Sarah");
        msr.setProperty(FilePropertyKey.DATE_CREATED,
            new GregorianCalendar(2008, 4, 27).getTimeInMillis());
        msr.setProperty(FilePropertyKey.FILE_SIZE, 11.7);
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.OWNER, "Sarah Thistle");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.9f);
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "water fight";
        msr.setExtension("avi");
        msr.setResultType(Category.VIDEO);
        msr.setSize(4567L);
        msr.setUrn("www.mizzou.edu" + i);
        msr.setProperty(FilePropertyKey.AUTHOR, "Spencer Turtle");
        msr.setProperty(FilePropertyKey.DATE_CREATED,
            new GregorianCalendar(2008, 4, 27).getTimeInMillis());
        msr.setProperty(FilePropertyKey.FILE_SIZE, 11.7);
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.OWNER, "Spencer");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.9f);
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
        msr.setProperty(FilePropertyKey.AUTHOR, "James Vanderbing");
        msr.setProperty(FilePropertyKey.COMPANY, "FriendSoft");
        msr.setProperty(FilePropertyKey.FILE_SIZE, 3.4);
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.OWNER, "Ding Bing");
        msr.setProperty(FilePropertyKey.PLATFORM, "Mac OS X");
        msr.setProperty(FilePropertyKey.DATE_CREATED,
            new GregorianCalendar(2008, 9, 2).getTimeInMillis());
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.6f);
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
        msr.setProperty(FilePropertyKey.FILE_SIZE, 0.9);
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.OWNER, "Mister Green");
        msr.setProperty(FilePropertyKey.DATE_CREATED,
            new GregorianCalendar(2008, 7, 20).getTimeInMillis());
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.8f);
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
        msr.setProperty(FilePropertyKey.AUTHOR, "John's Wedding");
        msr.setProperty(FilePropertyKey.BITRATE, "5000");
        msr.setProperty(FilePropertyKey.COMMENTS,
            "Someone likes to dance");
        msr.setProperty(FilePropertyKey.FILE_SIZE, 5.1);
        msr.setProperty(FilePropertyKey.HEIGHT, "480");
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.LENGTH, "0:48");
        msr.setProperty(FilePropertyKey.OWNER, "John Stone");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(FilePropertyKey.RATING, "8");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(FilePropertyKey.QUALITY, "excellent");
        msr.setProperty(FilePropertyKey.WIDTH, "640");
        msr.setProperty(FilePropertyKey.YEAR, "2008");
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "Chicken Filet Recipe";
        msr.setExtension("doc");
        msr.setResultType(Category.DOCUMENT);
        msr.setSize(9876L);
        msr.addSource("1.1.43.123");
        msr.setUrn("www.catering.com");
        msr.setProperty(FilePropertyKey.FILE_SIZE, 3.9);
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.OWNER, "Chef Sarah");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.6f);
        handleSearchResult(msr);
       
        msr = new MockSearchResult();
        name = "Don't be a chicken";
        msr.setExtension("jpg");
        msr.setResultType(Category.IMAGE);
        msr.setSize(9876L);
        msr.addSource("1.1.1.123");
        msr.setUrn("www.joeswedding.com");
        msr.setProperty(FilePropertyKey.AUTHOR, "Joe's Wedding");
        msr.setProperty(FilePropertyKey.COMMENTS,
            "Live life to live");
        msr.setProperty(FilePropertyKey.FILE_SIZE, 5.1);
        msr.setProperty(FilePropertyKey.OWNER, "Brent Sarah");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(FilePropertyKey.RATING, "8");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(FilePropertyKey.QUALITY, "excellent");
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.YEAR, "2008");
        handleSearchResult(msr);
        
        msr = new MockSearchResult();
        name = "Chicken pox treatment instructions";
        msr.setExtension("mp3");
        msr.setResultType(Category.AUDIO);
        msr.setSize(9876L);
        msr.addSource("1.1.1.123");
        msr.setUrn("www.medicalMe.com");
        msr.setProperty(FilePropertyKey.AUTHOR, "Dr. John");
        msr.setProperty(FilePropertyKey.COMMENTS,
            "don't itch");
        msr.setProperty(FilePropertyKey.FILE_SIZE, 5.1);
        msr.setProperty(FilePropertyKey.OWNER, "John Hill");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(FilePropertyKey.RATING, "8");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(FilePropertyKey.YEAR, "2008");
        msr.setProperty(FilePropertyKey.NAME, name);
        handleSearchResult(msr);
        
        msr = new MockSearchResult();
        name = "Cookies";
        msr.setExtension("avi");
        msr.setResultType(Category.VIDEO);
        msr.setSize(9876L);
        msr.addSource("1.1.1.123");
        msr.setUrn("www.GoodForSole.com");
        msr.setProperty(FilePropertyKey.AUTHOR, "PB&J");
        msr.setProperty(FilePropertyKey.BITRATE, "5000");
        msr.setProperty(FilePropertyKey.COMMENTS,
            "Peanut butter ones are the best");
        msr.setProperty(FilePropertyKey.FILE_SIZE, 5.1);
        msr.setProperty(FilePropertyKey.HEIGHT, "480");
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.LENGTH, "0:48");
        msr.setProperty(FilePropertyKey.OWNER, "John Stone");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(FilePropertyKey.RATING, "8");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(FilePropertyKey.QUALITY, "excellent");
        msr.setProperty(FilePropertyKey.WIDTH, "640");
        msr.setProperty(FilePropertyKey.YEAR, "2008");
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
        msr.setProperty(FilePropertyKey.FILE_SIZE, 50.9);
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.OWNER, "Greg Green");
        msr.setProperty(FilePropertyKey.DATE_CREATED,
            new GregorianCalendar(2008, 7, 20).getTimeInMillis());
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.8f);
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "Juggling at Mignight";
        msr.setExtension("avi");
        msr.setResultType(Category.VIDEO);
        msr.setSize(5678L);
        msr.addSource("123.132.1.21");
        msr.setUrn("similar-www.juggling.com");
        msr.setProperty(FilePropertyKey.FILE_SIZE, 50.9);
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.OWNER, "Greg Green");
        msr.setProperty(FilePropertyKey.DATE_CREATED,
            new GregorianCalendar(2008, 7, 20).getTimeInMillis());
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.8f);
        handleSearchResult(msr);

        
        msr = new MockSearchResult();
        name = "Ten ball Juggling";
        msr.setExtension("avi");
        msr.setResultType(Category.VIDEO);
        msr.setSize(5678L);
        msr.addSource("123.123.1.21");
        msr.setUrn("www.asdf.com");
        msr.setProperty(FilePropertyKey.FILE_SIZE, 15.9);
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.OWNER, "Michael Madison");
        msr.setProperty(FilePropertyKey.DATE_CREATED,
            new GregorianCalendar(2008, 7, 20).getTimeInMillis());
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.8f);
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "Juggling a busy day";
        msr.setExtension("wav");
        msr.setResultType(Category.AUDIO);
        msr.setSize(5678L);
        msr.addSource("123.18.1.21");
        msr.setUrn("www.asdf3asdf.com");
        msr.setProperty(FilePropertyKey.FILE_SIZE, 15.9);
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.OWNER, "Michael Madison");
        msr.setProperty(FilePropertyKey.DATE_CREATED,
            new GregorianCalendar(2008, 7, 20).getTimeInMillis());
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.8f);
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "How to juggle";
        msr.setExtension("avi");
        msr.setResultType(Category.VIDEO);
        msr.setSize(5678L);
        msr.addSource("123.12.12.21");
        msr.setUrn("www.ddasdrf.net");
        msr.setProperty(FilePropertyKey.FILE_SIZE, 15.9);
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.OWNER, "Michael Crede");
        msr.setProperty(FilePropertyKey.DATE_CREATED,
            new GregorianCalendar(2008, 7, 20).getTimeInMillis());
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.8f);
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "Chainsaw Juggling";
        msr.setExtension("avi");
        msr.setResultType(Category.VIDEO);
        msr.setSize(5678L);
        msr.addSource("123.12.1.21");
        msr.setUrn("www.3asdfNet.com");
        msr.setProperty(FilePropertyKey.FILE_SIZE, 5.9);
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.OWNER, "Greg Green");
        msr.setProperty(FilePropertyKey.DATE_CREATED,
            new GregorianCalendar(2008, 7, 20).getTimeInMillis());
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.8f);
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
        msr.setProperty(FilePropertyKey.FILE_SIZE, 3.9);
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.OWNER, "Fred Teller");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.9f);
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "Proposal at Paris";
        msr.setExtension("ogm");
        msr.setResultType(Category.VIDEO);
        msr.setSize(9876L);
        msr.addSource("127.1.1.21");
        msr.setUrn("www.stlzood.com" + i);
        msr.setProperty(FilePropertyKey.AUTHOR, "I'm engaged");
        msr.setProperty(FilePropertyKey.BITRATE, "5000");
        msr.setProperty(FilePropertyKey.COMMENTS,
            "Who knew they could do that?");
        msr.setProperty(FilePropertyKey.FILE_SIZE, 5.1);
        msr.setProperty(FilePropertyKey.HEIGHT, "480");
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.LENGTH, "0:48");
        msr.setProperty(FilePropertyKey.OWNER, "Chandler Bing");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(FilePropertyKey.RATING, "8");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(FilePropertyKey.QUALITY, "somewhat grainy");
        msr.setProperty(FilePropertyKey.WIDTH, "640");
        msr.setProperty(FilePropertyKey.YEAR, "2008");
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "My Dog Paris";
        msr.setExtension("jpg");
        msr.setResultType(Category.IMAGE);
        msr.setSize(9876L);
        msr.addSource("1.1.1.123");
        msr.setUrn("www.joeswedding.com");
        msr.setProperty(FilePropertyKey.AUTHOR, "Joe's Wedding");
        msr.setProperty(FilePropertyKey.COMMENTS,
            "Live life to live");
        msr.setProperty(FilePropertyKey.FILE_SIZE, 5.1);
        msr.setProperty(FilePropertyKey.OWNER, "Brent Sarah");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(FilePropertyKey.RATING, "8");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(FilePropertyKey.QUALITY, "excellent");
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.YEAR, "2008");
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "Paris Museum";
        msr.setExtension("jpg");
        msr.setResultType(Category.IMAGE);
        msr.setSize(9876L);
        msr.addSource("1.1.1.123");
        msr.setUrn("www.jsdfoeswedding.com");
        msr.setProperty(FilePropertyKey.AUTHOR, "Joe's Wedding");
        msr.setProperty(FilePropertyKey.COMMENTS,
            "Live life to live");
        msr.setProperty(FilePropertyKey.FILE_SIZE, 5.1);
        msr.setProperty(FilePropertyKey.OWNER, "Sarah");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(FilePropertyKey.RATING, "8");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.5f);
        msr.setProperty(FilePropertyKey.QUALITY, "excellent");
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.YEAR, "2008");
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "Paris Trip Plans";
        msr.setExtension("doc");
        msr.setResultType(Category.DOCUMENT);
        msr.setSize(9876L);
        msr.addSource("1.12.13.234");
        msr.setUrn("www.23sola3ddddrsystem.net3");
        msr.setProperty(FilePropertyKey.FILE_SIZE, 3.9);
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.OWNER, "Fred Teller");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.9f);
        handleSearchResult(msr);
        
        msr = new MockSearchResult();
        name = "Paris at night";
        msr.setExtension(".jpg");
        msr.setResultType(Category.IMAGE);
        msr.setSize(9876L);
        msr.setUrn("www.1sdfmizaz.com");
        msr.addSource("1.13.1.12");
        msr.setProperty(FilePropertyKey.FILE_SIZE, 31.9);
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.OWNER, "Sammy Teufle");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.7f);
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "Passport to Paris ";
        msr.setExtension(".doc");
        msr.setResultType(Category.DOCUMENT);
        msr.setSize(9876L);
        msr.addSource("2.1.1.13");
        msr.setUrn("www.tadsf3iza.com");
        msr.setProperty(FilePropertyKey.FILE_SIZE, 11.9);
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.OWNER, "John Bakker");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.7f);
        handleSearchResult(msr);
        msr = new MockSearchResult();

        name = "Notes on Paris";
        msr.setExtension("txt");
        msr.setResultType(Category.DOCUMENT);
        msr.setSize(9876L);
        msr.addSource("81.12.13.32");
        msr.setUrn("www.23sol3324a3rsystem.net3");
        msr.setProperty(FilePropertyKey.FILE_SIZE, 3.9);
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.OWNER, "Fred Teller");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.9f);
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "Ode to Paris ";
        msr.setExtension(".txt");
        msr.setResultType(Category.DOCUMENT);
        msr.setSize(976L);
        msr.addSource("11.1.13.1");
        msr.setUrn("www.figiza.com");
        msr.setProperty(FilePropertyKey.FILE_SIZE, 1.9);
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.OWNER, "John Derrick");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.7f);
        handleSearchResult(msr);

        msr = new MockSearchResult();
        name = "Paris, Texas";
        msr.setExtension(".jpg");
        msr.setResultType(Category.IMAGE);
        msr.setSize(9476L);
        msr.addSource("1.121.1.1");
        msr.setUrn("www.azdfaiza4.com");
        msr.setProperty(FilePropertyKey.FILE_SIZE, 10.9);
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.OWNER, "Eric Johanson");
        msr.setProperty(FilePropertyKey.RELEVANCE, 0.7f);
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