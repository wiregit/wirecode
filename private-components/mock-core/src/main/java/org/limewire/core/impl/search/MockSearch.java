package org.limewire.core.impl.search;

import java.util.GregorianCalendar;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.impl.friend.MockFriend;
import org.limewire.core.impl.friend.MockFriendPresence;


public class MockSearch implements Search {
    public static final String SIMILAR_RESULT_PREFIX = "mock-similar-result-";

    private SearchDetails searchDetails;
    private CopyOnWriteArrayList<SearchListener> listeners =
        new CopyOnWriteArrayList<SearchListener>();

    private int repeatCount = 0;

    public final String[] adjs = { "Almond", "Brass", "Apricot", "Aqua", "Asparagus", "Tangerine",
            "Awesome", "Banana", "Bear", "Bittersweet", "Fast", "Blue", "Bell", "Gray", "Green",
            "Violet", "Red", "Pink", "Orange", "Sienna", "Cool", "Earthy", "Caribbean", "Elder",
            "Pink", "Cerise", "Cerulean", "Chestnut", "Copper", "Better", "Candy", "Cranberry",
            "Dandelion", "Denim", "Gray", "Sand", "Desert", "Eggplant", "Lime", "Electric",
            "Famous", "Fern", "Forest", "Fuchsia", "Fuzzy", "Tree", "Gold", "Apple", "Smith",
            "Magenta", "Indigo", "Jazz", "Berry", "Jam", "Jungle", "Lemon", "Cold", "Lavender",
            "Hot", "New", "Ordinary", "Magenta", "Frowning", "Mint", "Mahogany", "Pretty", "Strange",
            "Grumpy", "Itchy", "Maroon", "Melon", "Midnight", "Clumsy", "Better", "Smiling",
            "Navy", "Neon", "Olive", "Orchid", "Outer", "Tame", "Cheerful", "Peach", "Periwinkle",
            "Pig", "Pine", "Nutty", "Plum", "Purple", "Rose", "Salmon", "Scarlet", "Nice", "Jolly",
            "Great", "Silver", "Sky", "Spring", "Long", "Glow", "Set", "Happy", "Tan", "Thistle",
            "Timber", "Tough", "Torch", "Smart", "Funny", "Tropical", "Tumble", "Ultra", "White",
            "Wild", "Yellow", "Eager", "Joyous", "Jumpy", "Kind", "Lucky", "Meek", "Nifty",
            "Adorable", "Aggressive", "Alert", "Attractive", "Average", "Bright", "Fragile",
            "Graceful", "Handsome", "Light", "Long", "Misty", "Muddy", "Plain", "Poised",
            "Precious", "Shiny", "Sparkling", "Stormy", "Wide", "Alive", "Annoying", "Better",
            "Brainy", "Busy", "Clever", "Clumsy", "Crazy", "Curious", "Easy", "Famous", "Frail",
            "Gifted", "Important", "Innocent", "Modern", "Mushy", "Odd", "Open", "Powerful",
            "Real", "Shy", "Sleepy", "Super", "Tame", "Tough", "Vast", "Wild", "Wrong", "Annoyed",
            "Anxious", "Crazy", "Dizzy", "Dull", "Evil", "Foolish", "Frantic", "Grieving",
            "Grumpy", "Helpful", "Hungry", "Lazy", "Lonely", "Scary", "Tense", "Weary", "Worried",
            "Brave", "Calm", "Charming", "Magic", "Easer", "Elated", "Enchanting", "Excited",
            "Fair", "Fine", "Friendly", "Funny", "Gentle", "Good", "Happy", "Healthy", "Jolly",
            "Kind", "Lovely", "Nice", "Perfect", "Proud", "Silly", "Smiling", "Thankful", "Witty",
            "Zany", "Big", "Fat", "Great", "Huge", "Immense", "Puny", "Scrawny", "Short", "Small",
            "Tall", "Teeny", "Tiny", "Faint", "Harsh", "Loud", "Melodic", "Mute", "Noisy", "Quiet",
            "Raspy", "Soft", "Whispering", "Ancient", "Fast", "Late", "Long", "Modern", "Old",
            "Quick", "Rapid", "Short", "Slow", "Swift", "Bitter", "Fresh", "Ripe", "Rotten",
            "Salty", "Sour", "Spicy" };

    public final String[] nouns = { "Alligator", "Alpaca", "Antelope", "Badger", "Armadillo",
            "Bat", "Bear", "Bee", "Bird", "Bison", "Buffalo", "Boar", "Butterfly", "Camel", "Cat",
            "Cattle", "Cow", "Chicken", "Clam", "Cockroach", "Codfish", "Coyote", "Crane", "Crow",
            "Deer", "Dinosaur", "Velociraptor", "Dog", "Dolphin", "Donkey", "Dove", "Duck",
            "Eagle", "Eel", "Elephant", "Elk", "Emu", "Falcon", "Ferret", "Fish", "Finch", "Fly",
            "Fox", "Frog", "Gerbil", "Giraffe", "Gnat", "Gnu", "Goat", "Goose", "Gorilla",
            "Grasshopper", "Grouse", "Gull", "Hamster", "Hare", "Hawk", "Hedgehog", "Heron",
            "Hornet", "Hog", "Horse", "Hound", "Hummingbird", "Hyena", "Jay", "Jellyfish",
            "Kangaroo", "Koala", "Lark", "Leopard", "Lion", "Llama", "Mallard", "Mole", "Monkey",
            "Moose", "Mosquito", "Mouse", "Mule", "Nightingale", "Opossum", "Ostrich", "Otter",
            "Owl", "Ox", "Oyster", "Panda", "Parrot", "Peafowl", "Penguin", "Pheasant", "Pig",
            "Pigeon", "Platypus", "Porpoise", "PrarieDog", "Pronghorn", "Quail", "Rabbit",
            "Raccoon", "Rat", "Raven", "Reindeer", "Rhinoceros", "Seal", "Seastar", "Serval",
            "Shark", "Sheep", "Skunk", "Snake", "Snipe", "Sparrow", "Spider", "Squirrel", "Swallow",
            "Swan", "Termite", "Tiger", "Toad", "Trout", "Turkey", "Turtle", "Wallaby", "Walrus",
            "Wasp", "Weasel", "Whale", "Wolf", "Wombat", "Woodpecker", "Wren", "Yak", "Zebra",
            "Ball", "Bed", "Book", "Bun", "Can", "Cake", "Cap", "Car", "Cat", "Day", "Fan", "Feet",
            "Hall", "Hat", "Hen", "Jar", "Kite", "Man", "Map", "Men", "Panda", "Pet", "Pie", "Pig",
            "Pot", "Sun", "Toe", "Apple", "Armadillo", "Banana", "Bike", "Book", "Clam", "Mushroom",
            "Clover", "Club", "Corn", "Crayon", "Crown", "Crib", "Desk", "Dress", "Flower", "Fog",
            "Game", "Hill", "Home", "Hornet", "Hose", "Joke", "Juice", "Mask", "Mice", "Alarm",
            "Bath", "Bean", "Beam", "Camp", "Crook", "Deer", "Dock", "Doctor", "Frog", "Good",
            "Jam", "Face", "Honey", "Kitten", "Fruit", "Fuel", "Cable", "Calculator", "Circle",
            "Guitar", "Bomb", "Border", "Apparel", "Activity", "Desk", "Art", "Colt", "Cyclist",
            "Biker", "Blogger", "Anchoby", "Carp", "Glassfish", "Clownfish", "Barracuda", "Eel",
            "Moray", "Stingray", "Flounder", "Swordfish", "Marlin", "Pipefish", "Grunter",
            "Grunion", "Grouper", "Guppy", "Gulper", "Crab", "Lobster", "Halibut", "Hagfish",
            "Horsefish", "Seahorse", "Jellyfish", "Killifish", "Trout", "Pike", "Ray", "Razorfish",
            "Ragfish", "Hamster", "Gerbil", "Mouse", "Gnome", "Shark", "Snail", "Skilfish" };

    public final String[] verbs = { "abated", "abbreviated", "abolished",
            "absorbed", "accelerated", "accentuated", "accommodated", "accomplished",
            "answered", "anticipated", "appeased", "applied", "appointed", "appraised",
            "approached", "appropriated", "approved", "arbitrated", "aroused", "arranged",
            "assimilated", "assisted", "assured", "attained", "attended", "audited", "augmented",
            "authored", "authorized", "automated", "averted", "avoided", "awarded", "balanced",
            "began", "benchmarked", "benefited", "bid", "billed", "blended", "blocked",
            "bolstered", "boosted", "bought", "branded", "bridged", "broadened", "brought",
            "budgeted", "built", "calculated", "calibrated", "capitalized", "captured",
            "concentrated", "conceptualized", "condensed", "conducted", "conferred", "configured",
            "confirmed", "confronted", "connected", "conserved", "considered", "consolidated",
            "constructed", "consulted", "consummated", "contacted", "continued", "contracted",     
            "evaluated", "examined", "exceeded", "executed", "exercised", "exhibited", "expanded",
            "expedited", "experienced", "experimented", "explained", "explored", "expressed",
            "extended", "extracted", "fabricated", "facilitated", "factored", "familiarized",
            "fashioned", "fielded", "filed", "filled", "finalized", "financed", "fine tuned",
            "finished", "fixed", "focused", "followed", "forecasted", "forged", "formalized",
            "formed", "formulated", "fortified", "forwarded", "fostered", "fought", "found",
            "founded", "framed", "fulfilled", "functioned as", "funded", "furnished", "furthered",
            "installed", "instilled", "instituted", "instructed", "insured", "integrated",
            "intensified", "interacted", "interceded", "interpreted", "intervened", "interviewed",
            "invented", "inventoried", "invested", "investigated", "invigorated", "invited",
            "involved", "isolated", "issued", "itemized", "joined", "judged", "justified",
            "presided", "prevailed", "prevented", "printed", "prioritized", "processed",
            "procured", "produced", "profiled", "programmed", "progressed", "projected",
            "promoted", "proofread", "proposed", "protected", "proved", "provided", "pruned",
            "publicized", "purchased", "pursued", "quadrupled", "qualified", "quantified",
            "queried", "questioned", "quoted", "raised", "rallied", "ranked", "rated", "reached",
            "read", "realigned", "realized", "rearranged", "reasoned", "rebuilt", "received",
            "surpassed", "surveyed", "swayed", "swept", "symbolized", "synthesized", "systemized",
            "tabulated", "tackled", "talked", "tallied", "targeted", "tasted", "taught", "teamed",
            "tempered", "tended", "terminated", "tested", "testified", "tied", "took", "topped",
            "totaled", "traced", "tracked", "trained", "transcribed", "transformed",
            "transitioned", "translated", "transmitted", "traveled", "treated", "trimmed",
            "tripled", "troubleshot", "turned", "tutored", "typed", "uncovered", "underlined",
            "underscored", "undertook", "underwrote", "unearthed", "unified", "united", "updated",
            "upgraded", "upheld", "urged", "used", "utilized", "validated", "valued", "vaulted",
            "verbalized", "verified", "viewed", "visualized", "voiced", "volunteered", "weathered",
            "weighed", "widened", "withstood", "won", "worked", "wove", "wrote", "yielded" };

    private final String[] tracksChemicalAttraction = {"Hopkington", "Super Sillious", "Peabody", "Loquacious Lilly", "Wobbly Wombat", 
            "Spirit of San Quentin", "Sounds of smiles", "Berry Yesterday", "Leaves are a falling", "No where man", "Wiki Wear"};
    
    private final String[] tracksLightningQueen = {"Cirrus", "Contrail", "Altostratus", "Altocumulus", "Nimbostratus", 
            "Stratocumulus", "Stratus", "Cumulus", "Cumulonimbus", "Puffy", "Ominous"};

    public MockSearch(SearchDetails searchDetails) {
        this.searchDetails = searchDetails;
    }

    @Override
    public void addSearchListener(SearchListener searchListener) {
        listeners.add(searchListener);
    }

    @Override
    public SearchCategory getCategory() {
        return searchDetails.getSearchCategory();
    }
    
    @Override
    public void removeSearchListener(SearchListener searchListener) {
        listeners.remove(searchListener);
    }
    
    @Override
    public void start() {
        for (SearchListener listener : listeners) {
            listener.searchStarted(this);
        }
        addResults("");
    }
    
    @Override
    public void stop() {
        for (SearchListener listener : listeners) {
            listener.searchStopped(this);
        }
    }
    
    private void addResults(final String suffix) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                addRecords(0);
            }
        }).start();
    }
    
    private void handleSearchResult(MockSearchResult mock) {
        for (SearchListener listener : listeners) {
            listener.handleSearchResult(this, mock);
        }
    }
    
//    private void handleSponsoredResults(SponsoredResult... sponsoredResults) {
//        List<SponsoredResult> mockList =  Arrays.asList(sponsoredResults);
//        for (SearchListener listener : listeners) {
//            listener.handleSponsoredResults(this, mockList);
//        }
//    }
    
    @Override
    public void repeat() {
        for (SearchListener listener : listeners) {
            listener.searchStarted(this);
        }
        addResults("rp" + repeatCount++);
    }

    private void addRecords(int i) {
        
        String query = searchDetails.getSearchQuery();
        query = query.toLowerCase();
        
        Random r = new Random();
        int verb = 0;
        int adj = 0;
        int noun = 0;

        String keyword = new String();
        keyword = query;
        if(query.indexOf("monkey") > -1){
            keyword = "monkey";
            if (isValidCategory(SearchCategory.VIDEO)) createSearchResultVideo("monkey on a skateboard", 256, 256, 256);
            if (isValidCategory(SearchCategory.DOCUMENT)) createSearchResultDocument("monkey on a skateboard", 256, 256, 256);
            if (isValidCategory(SearchCategory.AUDIO)) createSearchResultAudio("monkey on a skateboard", 256, 256, 256);
            if (isValidCategory(SearchCategory.IMAGE)) createSearchResultImages("monkey on a skateboard", 256, 256, 256);
            if (isValidCategory(SearchCategory.PROGRAM)) createSearchResultProgram("monkey on a skateboard", 256, 256, 256);
        }

        if(query.indexOf("game") > -1 || query.indexOf("design") > -1 || query.indexOf("patterns") > -1){
            keyword = "game";
            if (isValidCategory(SearchCategory.DOCUMENT)) createSearchResultAdobe();
            if (isValidCategory(SearchCategory.DOCUMENT)) createSearchResultDocument("game design patterns", 256, 256, 256);
            if (isValidCategory(SearchCategory.AUDIO)) createSearchResultAudio("game design patterns", 256, 256, 256);
            if (isValidCategory(SearchCategory.VIDEO)) createSearchResultVideo("game design patterns", 256, 256, 256);
            if (isValidCategory(SearchCategory.PROGRAM)) createSearchResultProgram("game design patterns", 256, 256, 256);
            if (isValidCategory(SearchCategory.IMAGE)) createSearchResultImages("game design patterns", 256, 256, 256);

        }
        if(query.indexOf("giant") > -1 || query.indexOf("guitar") > -1){
            keyword = "guitar";
            if (isValidCategory(SearchCategory.DOCUMENT)) createSearchResultDocument("giant guitar", 256, 256, 256);
            if (isValidCategory(SearchCategory.AUDIO)) createSearchResultAudio("giant guitar", 256, 256, 256);
            if (isValidCategory(SearchCategory.VIDEO)) createSearchResultVideo("giant guitar", 256, 256, 256);
            if (isValidCategory(SearchCategory.PROGRAM)) createSearchResultProgram("giant guitar", 256, 256, 256);            
            if (isValidCategory(SearchCategory.IMAGE)) createSearchResultImages("giant guitar", 256, 256, 256);            
        }
        if(query.indexOf("frank") > -1){
            keyword = "franks";
            if (isValidCategory(SearchCategory.AUDIO)) createSearchResultTheFranks();
        }
        if(query.indexOf("lightning") > -1 && isValidCategory(SearchCategory.AUDIO)){
            keyword = "lightning";
            String albumName = "Lightning Queen";                
            for(int tracks = 0; tracks < 11; tracks++) {
                handleSearchResult(createAlbumTrack(albumName, tracksLightningQueen[tracks]));
            }

            createSearchResultTheFranks();
        }

        
        if(query.indexOf(query) > -1){            
            
            for(int j = 0; j < 64; j++) {
                if (isValidCategory(SearchCategory.DOCUMENT)) {
                    verb = Math.abs(r.nextInt()) % 256;
                    adj = Math.abs(r.nextInt()) % 256;
                    noun = Math.abs(r.nextInt()) % 256;
                    createSearchResultDocument(keyword, verb, adj, noun);
                    try { Thread.sleep(50); } catch (InterruptedException e) {}
                }

                if (isValidCategory(SearchCategory.PROGRAM)) {
                    verb = Math.abs(r.nextInt()) % 256;
                    adj = Math.abs(r.nextInt()) % 256;
                    noun = Math.abs(r.nextInt()) % 256;
                    createSearchResultProgram(keyword, verb, adj, noun);
                    try { Thread.sleep(50); } catch (InterruptedException e) {}
                }

                if (isValidCategory(SearchCategory.AUDIO)) {
                    verb = Math.abs(r.nextInt()) % 256;
                    adj = Math.abs(r.nextInt()) % 256;
                    noun = Math.abs(r.nextInt()) % 256;
                    createSearchResultAudio2(keyword, verb, adj, noun);
                }

                if (isValidCategory(SearchCategory.DOCUMENT)) {
                    try { Thread.sleep(50); } catch (InterruptedException e) {}
                    verb = Math.abs(r.nextInt()) % 256;
                    adj = Math.abs(r.nextInt()) % 256;
                    noun = Math.abs(r.nextInt()) % 256;
                    createSearchResultDocument2(keyword, verb, adj, noun);
                }

                if (isValidCategory(SearchCategory.AUDIO)) {
                    try { Thread.sleep(50); } catch (InterruptedException e) {}
                    verb = Math.abs(r.nextInt()) % 256;
                    adj = Math.abs(r.nextInt()) % 256;
                    noun = Math.abs(r.nextInt()) % 256;
                    createSearchResultAudio(keyword, verb, adj, noun);
                    try { Thread.sleep(50); } catch (InterruptedException e) {}
                }

                if (isValidCategory(SearchCategory.VIDEO)) {
                    verb = Math.abs(r.nextInt()) % 256;
                    adj = Math.abs(r.nextInt()) % 256;
                    noun = Math.abs(r.nextInt()) % 256;
                    createSearchResultVideo(keyword, verb, adj, noun);
                    try { Thread.sleep(50); } catch (InterruptedException e) {}
                }

                if (isValidCategory(SearchCategory.VIDEO)) {
                    verb = Math.abs(r.nextInt()) % 256;
                    adj = Math.abs(r.nextInt()) % 256;
                    noun = Math.abs(r.nextInt()) % 256;
                    createSearchResultVideo2(keyword, verb, adj, noun);
                    try { Thread.sleep(50); } catch (InterruptedException e) {}
                }

                if (isValidCategory(SearchCategory.PROGRAM)) {
                    verb = Math.abs(r.nextInt()) % 256;
                    adj = Math.abs(r.nextInt()) % 256;
                    noun = Math.abs(r.nextInt()) % 256;
                    createSearchResultProgram2(keyword, verb, adj, noun);
                    try { Thread.sleep(50); } catch (InterruptedException e) {}
                }

                if (isValidCategory(SearchCategory.IMAGE)) {
                    verb = Math.abs(r.nextInt()) % 256;
                    adj = Math.abs(r.nextInt()) % 256;
                    noun = Math.abs(r.nextInt()) % 256;
                    createSearchResultImages(keyword, verb, adj, noun);
                    try { Thread.sleep(50); } catch (InterruptedException e) {}
                }

                if (isValidCategory(SearchCategory.IMAGE)) {
                    verb = Math.abs(r.nextInt()) % 256;
                    adj = Math.abs(r.nextInt()) % 256;
                    noun = Math.abs(r.nextInt()) % 256;
                    createSearchResultImages2(keyword, verb, adj, noun);
                }
            }

        }
        
    }
        


    /**
     * Produces a 'The (keyword) (verb) the (adjective) (noun)' 
     * @param keyword
     * @param verb if = 256, is result with only keyword
     * @param adj
     * @param noun
     */
    
    
    private void createSearchResultDocument(String keyword, int verb, int adj, int noun) {
        MockSearchResult msr = new MockSearchResult();
        msr.setResultType(Category.DOCUMENT);
        String name = null;
        if(verb == 256) { 
            name = keyword;
        } else {
            name = "The " + keyword + " " + verbs[verb] + " the " + adjs[adj] + " " + nouns[noun];
        }

        Random r = new Random();
        int num = r.nextInt() % 100 ;
        if (num < 80 ) { 
            msr.setSize(945367L);
            msr.setExtension("doc");
        } else {
            msr.setSize(67L);
            msr.setExtension("txt");
        }

        String ip = new String();
        ip = Math.abs(r.nextInt()) % 256 + "."+ Math.abs(r.nextInt()) % 256 + "." + Math.abs(r.nextInt()) % 256 + "." + Math.abs(r.nextInt()) % 256;
        
        msr.addSource(ip);

        msr.setUrn(Math.abs(r.nextInt()) + "Blah" + Math.abs(r.nextInt()));
        msr.setProperty(FilePropertyKey.AUTHOR, "Dr. Java");
        msr.setProperty(FilePropertyKey.DATE_CREATED,
            new GregorianCalendar(2008, 7, 27).getTimeInMillis());
        msr.setProperty(FilePropertyKey.FILE_SIZE, 221.7);        
        msr.setProperty(FilePropertyKey.NAME, name);

        handleSearchResult(msr);
        
    }

    /**
     * Produces a 'The (noun) (verb) the (adjective) (keyword)' 
     * @param keyword 
     * @param verb if == 256, result only has keyword
     * @param adj
     * @param noun
     */
    
    /**
     * @param verb if == 256, result only has keyword
     */
            
    private void createSearchResultDocument2(String keyword, int verb, int adj, int noun) {
        MockSearchResult msr = new MockSearchResult();
        msr.setResultType(Category.DOCUMENT);
        String name = null;
        if(verb == 256) { 
            name = keyword;
        } else {            
            name = "The " + nouns[noun] + " " + verbs[verb] + " the " + adjs[adj] + " " + keyword;
        }
        
        Random r = new Random();
        int num = r.nextInt() % 100 ;
        if (num < 80 ) { 
            msr.setExtension("pdf");
            msr.setSize(45367L);
            msr.setProperty(FilePropertyKey.FILE_SIZE, 45367);
        } else {
            msr.setExtension("ppt");
            msr.setSize(367L);
            msr.setProperty(FilePropertyKey.FILE_SIZE, 367);

        }

        String ip = new String();
        ip = Math.abs(r.nextInt()) % 256 + "."+ Math.abs(r.nextInt()) % 256 + "." + Math.abs(r.nextInt()) % 256 + "." + Math.abs(r.nextInt()) % 256;
        
        msr.addSource(ip);

        msr.setUrn(Math.abs(r.nextInt()) + "Blah" + Math.abs(r.nextInt()));
        msr.setProperty(FilePropertyKey.AUTHOR, "Dr. Java");
        msr.setProperty(FilePropertyKey.DATE_CREATED,
            new GregorianCalendar(2008, 7, 27).getTimeInMillis());
        msr.setProperty(FilePropertyKey.NAME, name);

        handleSearchResult(msr);
        
    }

    /**
     * Specific search result for Game Design patterns which is a pdf file.
     */
    private void createSearchResultAdobe() {
        MockSearchResult msr = new MockSearchResult();
        msr.setExtension("pdf");
        msr.setResultType(Category.DOCUMENT);
        msr.setSize(12L);
        String name = "Game design patterns";

        Random r = new Random();
        String ip = new String();
        ip = Math.abs(r.nextInt()) % 256 + "."+ Math.abs(r.nextInt()) % 256 + "." + Math.abs(r.nextInt()) % 256 + "." + Math.abs(r.nextInt()) % 256;
        
        msr.addSource(ip);

        msr.setUrn(Math.abs(r.nextInt()) + "Blah" + Math.abs(r.nextInt()));
        msr.setProperty(FilePropertyKey.AUTHOR, "Tim Burr");
        msr.setProperty(FilePropertyKey.DATE_CREATED,
            new GregorianCalendar(2003, 2, 7).getTimeInMillis());
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.FILE_SIZE, 12.7);

        handleSearchResult(msr);
        
    }

    /**
     * Creates search results for two albums by The Franks. Specific results we want for the usability
     * test, as opposed to the random results with the keyword 'frank'.
     */
    private void createSearchResultTheFranks() {
        // Create a search result that will be categorized as "Audio".
        
        handleSearchResult(createAlbumTrack("Lightning Queen", "Rocket me to the moon", 591224));
        handleSearchResult(createAlbumTrack("Lightning Queen", "Gerry Me Ander", 5324));
        handleSearchResult(createAlbumTrack("Lightning Queen", "Smelly Shoes", 124));
        handleSearchResult(createAlbumTrack("Chemical Attraction", "Business Casual", 100));
        for(int albums = 0; albums < 2; albums++) { 
            String albumName = new String();
            for(int tracks = 0; tracks < 11; tracks++) {
                if(albums == 0) {
                    albumName = "Chemical Attraction";
                    handleSearchResult(createAlbumTrack(albumName, tracksChemicalAttraction[tracks]));

                } else {
                    albumName = "Lightning Queen";                
                    handleSearchResult(createAlbumTrack(albumName, tracksLightningQueen[tracks]));
                }
            }            
        }        
    }
    
    private MockSearchResult createAlbumTrack(String albumName, String trackName) {
        MockSearchResult msr = new MockSearchResult();
        Random r = new Random();

        if (trackName.indexOf("Puffy") > -1 || trackName.indexOf("Ominous") > -1) {
            msr.setExtension("wav");            
        } else {
            msr.setExtension("mp3");
        }

        msr.setResultType(Category.AUDIO);
        String ip = new String();
        
        int size =Math.abs(r.nextInt() % 10000000);
        msr.setSize(size);
        ip = Math.abs(r.nextInt()) % 256 + "."+ Math.abs(r.nextInt()) % 256 + "." + Math.abs(r.nextInt()) % 256 + "." + Math.abs(r.nextInt()) % 256;
        
        msr.addSource(ip);
        msr.setUrn(Math.abs(r.nextInt()) + "Blah" + Math.abs(r.nextInt()));

        msr.setProperty(FilePropertyKey.ALBUM, albumName);
        String author = new String();
        author = "The Franks";
        msr.setProperty(FilePropertyKey.AUTHOR, author);
        msr.setProperty(FilePropertyKey.BITRATE, "192");
        msr.setProperty(FilePropertyKey.DESCRIPTION, "very jazzy km lkn nans jaskj asjkbcjkbs bbja " +
                "scb bjk asc bjkajbsc kbk asbc b bascbbasc b " +
                "ascb bascb asb cbascbab cb ascbbas cba scbasc");
        msr.setProperty(FilePropertyKey.TITLE, trackName);
        msr.setProperty(FilePropertyKey.FILE_SIZE, size);
        msr.setProperty(FilePropertyKey.GENRE, "Jazz");
        msr.setProperty(FilePropertyKey.NAME, trackName);
        msr.setProperty(FilePropertyKey.LENGTH, "4:31");
        msr.setProperty(FilePropertyKey.QUALITY, "good quality");
        msr.setProperty(FilePropertyKey.TRACK_NUMBER, "3");
        msr.setProperty(FilePropertyKey.YEAR, "2008");
        return msr;

    }

    private MockSearchResult createAlbumTrack(String albumName, String trackName, int size) {
        MockSearchResult msr = new MockSearchResult();
        msr.setExtension("mp3");
        msr.setResultType(Category.AUDIO);
        Random r = new Random();
        String ip = new String();
        
        msr.setSize(size);
        ip = Math.abs(r.nextInt()) % 256 + "."+ Math.abs(r.nextInt()) % 256 + "." + Math.abs(r.nextInt()) % 256 + "." + Math.abs(r.nextInt()) % 256;
        
        msr.addSource(ip);
        msr.setUrn(Math.abs(r.nextInt()) + "Blah" + Math.abs(r.nextInt()));

        msr.setProperty(FilePropertyKey.ALBUM, albumName);
        String author = new String();
        author = "The Franks";
        msr.setProperty(FilePropertyKey.AUTHOR, author);
        msr.setProperty(FilePropertyKey.DESCRIPTION, "very jazzy km lkn nans jaskj asjkbcjkbs bbja " +
                "scb bjk asc bjkajbsc kbk asbc b bascbbasc b " +
                "ascb bascb asb cbascbab cb ascbbas cba scbasc");
        msr.setProperty(FilePropertyKey.TITLE, trackName);
        msr.setProperty(FilePropertyKey.FILE_SIZE, size);
        msr.setProperty(FilePropertyKey.GENRE, "Jazz");
        msr.setProperty(FilePropertyKey.NAME, trackName);
        msr.setProperty(FilePropertyKey.TRACK_NUMBER, "3");
        msr.setProperty(FilePropertyKey.YEAR, "2008");

        int num = r.nextInt() % 100 ;
        if (num < 80) {
            
            msr.setProperty(FilePropertyKey.BITRATE, 160);
            msr.setProperty(FilePropertyKey.QUALITY, "good quality");
            msr.setProperty(FilePropertyKey.LENGTH, 232);
            
        } else {
            msr.setProperty(FilePropertyKey.QUALITY, "excellent quality");
            msr.setProperty(FilePropertyKey.BITRATE, 192);
            msr.setProperty(FilePropertyKey.LENGTH, 151);
        }

        return msr;

    }

    
    /**
     * Makes a search result using 'the [noun] [a verb] the [adjective] [keyword]'. 
     * If you pass in 256, the keyword used is only keyword, instead of using the verb, adjective
     * and the noun.

     * @param verb if == 256, result only has keyword
     */
    private void createSearchResultAudio(String keyword, int verb, int adj, int noun) {
        // Create a search result that will be categorized as "Audio".
        MockSearchResult msr = new MockSearchResult();
        String name = null;
        String author = new String();
        
        if(verb == 256) { 
            name = keyword;
            author = "The " + keyword;
        } else {
            author = "The " + nouns[noun];
            name = "The " + keyword + " " + verbs[verb] + " the " + adjs[adj] + " " + nouns[noun];
        }
        msr.setExtension("mp3");
        msr.setResultType(Category.AUDIO);
        Random r = new Random();
        String ip = new String();
        ip = Math.abs(r.nextInt()) % 256 + "."+ Math.abs(r.nextInt()) % 256 + "." + Math.abs(r.nextInt()) % 256 + "." + Math.abs(r.nextInt()) % 256;
        
        msr.addSource(ip);
        msr.setUrn(Math.abs(r.nextInt()) + "Blah" + Math.abs(r.nextInt()));
        String album = new String();
        int num = 0;
        num = r.nextInt() % 100 ;
        if (num < 80) {
            if(adj == 256) {
                album = "Gigantic Orange Banana";
            } else {
               album = "The " + adjs[adj] + " " + nouns[noun];
            }
            msr.setExtension("mp3");
            msr.setSize(4234L);
            msr.setProperty(FilePropertyKey.QUALITY, "good quality");            
            msr.setProperty(FilePropertyKey.FILE_SIZE, 4234);
            msr.setProperty(FilePropertyKey.BITRATE, 128);
            msr.setProperty(FilePropertyKey.LENGTH, 91);

        } else {
            msr.setExtension("wav");
            msr.setSize(133L);
            msr.setProperty(FilePropertyKey.QUALITY, "excellent quality");            
            msr.setProperty(FilePropertyKey.FILE_SIZE, 133);
            msr.setProperty(FilePropertyKey.LENGTH, ":31");
            msr.setProperty(FilePropertyKey.BITRATE, "192");
            msr.setProperty(FilePropertyKey.BITRATE, 192);
            msr.setProperty(FilePropertyKey.LENGTH, 31);

        }

        msr.setProperty(FilePropertyKey.ALBUM, album);
        num = r.nextInt() % 100 ;
        if (num < 80) { 
            msr.setProperty(FilePropertyKey.AUTHOR, author);
        }
        msr.setProperty(FilePropertyKey.DESCRIPTION, "very jazzy km lkn nans jaskj asjkbcjkbs bbja " +
                "scb bjk asc bjkajbsc kbk asbc b bascbbasc b " +
                "ascb bascb asb cbascbab cb ascbbas cba scbasc");
        msr.setProperty(FilePropertyKey.TITLE, name);
        msr.setProperty(FilePropertyKey.GENRE, "Jazz");
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.TRACK_NUMBER, "3");
        msr.setProperty(FilePropertyKey.YEAR, "2008");
        handleSearchResult(msr);
    }

    /**
     * Makes a search result using 'the [keyword] [a verb] the [adjective] [noun].
     */
    private void createSearchResultAudio2(String keyword, int verb, int adj, int noun) {
        // Create a search result that will be categorized as "Audio".
        MockSearchResult msr = new MockSearchResult();
        String name = null;
        if(verb == 256) { 
            name = keyword;
        } else {
            name = "The " + nouns[noun] + " " + verbs[verb] + " the " + adjs[adj] + " " + keyword;
        }
        msr.setResultType(Category.AUDIO);
        Random r = new Random();
        String ip = new String();
        ip = Math.abs(r.nextInt()) % 256 + "."+ Math.abs(r.nextInt()) % 256 + "." + Math.abs(r.nextInt()) % 256 + "." + Math.abs(r.nextInt()) % 256;
        
        msr.addSource(ip);
        msr.setUrn(Math.abs(r.nextInt()) + "Blah" + Math.abs(r.nextInt()));
        String album = new String();
        int num = 0;
        num = r.nextInt() % 100 ;
        if (num < 80 ) { 
            album = "The " + adjs[adj] + " " + nouns[noun];
        }
        msr.setProperty(FilePropertyKey.ALBUM, album);
        String author = new String();
        num = r.nextInt() % 100 ;
        if (num < 80 ) { 
            author = "The " + nouns[noun];
            
            msr.setExtension("mp3");
            msr.setSize(30000L);
            msr.setProperty(FilePropertyKey.QUALITY, "good quality");            
            msr.setProperty(FilePropertyKey.FILE_SIZE, 30000.9);
            msr.setProperty(FilePropertyKey.LENGTH, "4:31");
            msr.setProperty(FilePropertyKey.BITRATE, "160");
            msr.setProperty(FilePropertyKey.BITRATE, 160);
            msr.setProperty(FilePropertyKey.LENGTH, 271);            

        } else {
            msr.setExtension("aac");
            msr.setSize(423455L);
            
            msr.setProperty(FilePropertyKey.QUALITY, "poor quality");            
            msr.setProperty(FilePropertyKey.FILE_SIZE, 423455);
            msr.setProperty(FilePropertyKey.LENGTH, "44:31");
            msr.setProperty(FilePropertyKey.BITRATE, "63");
            msr.setProperty(FilePropertyKey.BITRATE, 63);
            msr.setProperty(FilePropertyKey.LENGTH, 44*60);

        }
        msr.setProperty(FilePropertyKey.AUTHOR, author);
        msr.setProperty(FilePropertyKey.DESCRIPTION, "very jazzy km lkn nans jaskj asjkbcjkbs bbja " +
                "scb bjk asc bjkajbsc kbk asbc b bascbbasc b " +
                "ascb bascb asb cbascbab cb ascbbas cba scbasc");
        msr.setProperty(FilePropertyKey.GENRE, "Jazz");
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.TRACK_NUMBER, "3");
        msr.setProperty(FilePropertyKey.YEAR, "2008");
        handleSearchResult(msr);
    }
    
    /**
     * @param verb if == 256, result only has keyword
     */
    private void createSearchResultVideo(String keyword, int verb, int adj, int noun) {
        MockSearchResult msr;

        String name = null;
        if(verb == 256) { 
            name = keyword;
        } else {
            name = "The " + keyword + " " + verbs[verb] + " the " + adjs[adj] + " " + nouns[noun];
        }

        msr = new MockSearchResult();
        msr.setResultType(Category.VIDEO);
        msr.setSize(9876L);

        Random r = new Random();
        String ip = new String();
        ip = Math.abs(r.nextInt()) % 256 + "."+ Math.abs(r.nextInt()) % 256 + "." + Math.abs(r.nextInt()) % 256 + "." + Math.abs(r.nextInt()) % 256;
        
        msr.addSource(ip);

        msr.setUrn(Math.abs(r.nextInt()) + "BlahBlah" + Math.abs(r.nextInt()));

        msr.setProperty(FilePropertyKey.AUTHOR, "Morristown Zoo");
        msr.setProperty(FilePropertyKey.BITRATE, "5000");
        msr.setProperty(FilePropertyKey.DESCRIPTION,
            "Who knew they could do that?");
        msr.setProperty(FilePropertyKey.FILE_SIZE, 44161);
        msr.setProperty(FilePropertyKey.HEIGHT, "480");
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.LENGTH, "0:48");
        msr.setProperty(FilePropertyKey.RATING, "8");
        msr.setProperty(FilePropertyKey.QUALITY, "somewhat grainy");
        msr.setProperty(FilePropertyKey.WIDTH, "640");
        
        String year = new String();
        int num = 0;
        num = r.nextInt() % 100 ;
        if (num < 80 ) { 
            msr.setExtension("mov");

            if(adj == 256) {
                year = "The " + keyword;
            } else {
                year = "The " + adjs[adj] + " " + nouns[noun];                
            }
        } else {
            msr.setExtension("ogm");
        }

        msr.setProperty(FilePropertyKey.YEAR, year);
        handleSearchResult(msr);

    }

    private void createSearchResultVideo2(String keyword, int verb, int adj, int noun) {
        MockSearchResult msr;

        String name = "The " + nouns[noun] + " " + verbs[verb] + " the " + adjs[adj] + " " + keyword;

        msr = new MockSearchResult();
        msr.setResultType(Category.VIDEO);
        msr.setSize(9876L);

        Random r = new Random();
        String ip = new String();
        ip = Math.abs(r.nextInt()) % 256 + "."+ Math.abs(r.nextInt()) % 256 + "." + Math.abs(r.nextInt()) % 256 + "." + Math.abs(r.nextInt()) % 256;
        
        msr.addSource(ip);

        msr.setUrn(Math.abs(r.nextInt()) + "BlahBlah" + Math.abs(r.nextInt()));

        msr.setProperty(FilePropertyKey.AUTHOR, "Morristown Zoo");
        msr.setProperty(FilePropertyKey.BITRATE, "5000");
        msr.setProperty(FilePropertyKey.DESCRIPTION,
            "Who knew they could do that?");
        msr.setProperty(FilePropertyKey.FILE_SIZE, 5.1);
        msr.setProperty(FilePropertyKey.HEIGHT, "480");
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.LENGTH, "0:48");
        msr.setProperty(FilePropertyKey.RATING, "8");
        msr.setProperty(FilePropertyKey.QUALITY, "somewhat grainy");
        msr.setProperty(FilePropertyKey.WIDTH, "640");
        
        String year = new String();
        int num = 0;
        num = r.nextInt() % 100 ;
        if (num < 80 ) { 
            msr.setExtension("avi");

            year = "The " + adjs[adj] + " " + nouns[noun];
        } else {
            msr.setExtension("mpeg");

        }

        msr.setProperty(FilePropertyKey.YEAR, year);
        handleSearchResult(msr);

    }
    
    /**
     * @param verb if == 256, result only has keyword
     */
    private void createSearchResultProgram(String keyword, int verb, int adj, int noun) {
        MockSearchResult msr;

        String name = null;
        if(verb == 256) { 
            name = keyword;
        } else {
            name = "The " + keyword + " " + verbs[verb] + " the " + adjs[adj] + " " + nouns[noun];
        }

        msr = new MockSearchResult();
        msr.setResultType(Category.PROGRAM);
        msr.setSize(8765L);
        Random r = new Random();
        String ip = new String();
        ip = Math.abs(r.nextInt()) % 256 + "."+ Math.abs(r.nextInt()) % 256 + "." + Math.abs(r.nextInt()) % 256 + "." + Math.abs(r.nextInt()) % 256;
        
        msr.addSource(ip);

        msr.setUrn(Math.abs(r.nextInt()) + "BlahBlah" + Math.abs(r.nextInt()));
        msr.setProperty(FilePropertyKey.AUTHOR, "James Vanderbing");
        String company = new String();
        int num = 0;
        num = r.nextInt() % 100 ;
        if (num < 80 ) { 
            msr.setExtension("exe");

            if(adj == 256) {
                company = "The " + keyword;                
            } else {
                company = "The " + adjs[adj] + " " + nouns[noun];
            }                
        } else {
            msr.setExtension("jar");
        }
        msr.setProperty(FilePropertyKey.COMPANY, company);
        msr.setProperty(FilePropertyKey.FILE_SIZE, 3.4);
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.PLATFORM, "Mac OS X");
        msr.setProperty(FilePropertyKey.DATE_CREATED,
            new GregorianCalendar(2008, 9, 2).getTimeInMillis());
        handleSearchResult(msr);
    }

    private void createSearchResultProgram2(String keyword, int verb, int adj, int noun) {
        MockSearchResult msr;

        String name = "The " + nouns[noun] + " " + verbs[verb] + " the " + adjs[adj] + " " + keyword;        

        msr = new MockSearchResult();
        msr.setExtension("exe");
        msr.setResultType(Category.PROGRAM);
        msr.setSize(8765L);
        Random r = new Random();
        String ip = new String();
        ip = Math.abs(r.nextInt()) % 256 + "."+ Math.abs(r.nextInt()) % 256 + "." + Math.abs(r.nextInt()) % 256 + "." + Math.abs(r.nextInt()) % 256;
        
        msr.addSource(ip);

        msr.setUrn(Math.abs(r.nextInt()) + "BlahBlah" + Math.abs(r.nextInt()));
        msr.setProperty(FilePropertyKey.AUTHOR, "James Vanderbing");
        String company = new String();
        int num = 0;
        num = r.nextInt() % 100 ;
        if (num < 80 ) { 
            company = "The " + adjs[adj] + " " + nouns[noun];
        }
        msr.setProperty(FilePropertyKey.COMPANY, company);
        msr.setProperty(FilePropertyKey.FILE_SIZE, 3.4);
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.PLATFORM, "Mac OS X");
        msr.setProperty(FilePropertyKey.DATE_CREATED,
            new GregorianCalendar(2008, 9, 2).getTimeInMillis());
        handleSearchResult(msr);
    }
    
    /**
     * @param verb if == 256, result only has keyword
     */
    private void createSearchResultImages(String keyword, int verb, int adj, int noun) {
        MockSearchResult msr;
        String name = null;
        if(verb == 256) { 
            name = keyword;
        } else {
            name = "The " + keyword + " " + verbs[verb] + " the " + adjs[adj] + " " + nouns[noun];
        }
        msr = new MockSearchResult();
        int num = 0;
        Random r = new Random();

        num = r.nextInt() % 100 ;
        if (num < 80 ) { 
            msr.setExtension("jpg");
        } else {
            msr.setExtension("png");
        }

        msr.setResultType(Category.IMAGE);
        msr.setSize(5678L);

        String ip = new String();
        ip = Math.abs(r.nextInt()) % 256 + "."+ Math.abs(r.nextInt()) % 256 + "." + Math.abs(r.nextInt()) % 256 + "." + Math.abs(r.nextInt()) % 256;
        
        msr.addSource(ip);

        msr.setUrn(Math.abs(r.nextInt()) + "BlahBlah" + Math.abs(r.nextInt()));

        msr.setProperty(FilePropertyKey.FILE_SIZE, 20.9);
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.DATE_CREATED,
            new GregorianCalendar(2008, 7, 20).getTimeInMillis());
        handleSearchResult(msr);

    }

    private void createSearchResultImages2(String keyword, int verb, int adj, int noun) {
        MockSearchResult msr;
        String name = "The " + nouns[noun] + " " + verbs[verb] + " the " + adjs[adj] + " " + keyword;

        msr = new MockSearchResult();
        msr.setResultType(Category.IMAGE);
        msr.setSize(5678L);

        Random r = new Random();
        int num = r.nextInt() % 100 ;
        if (num < 80 ) { 
            msr.setExtension("bmp");
        } else {
            msr.setExtension("gif");
        }

        String ip = new String();
        ip = Math.abs(r.nextInt()) % 256 + "."+ Math.abs(r.nextInt()) % 256 + "." + Math.abs(r.nextInt()) % 256 + "." + Math.abs(r.nextInt()) % 256;
        
        msr.addSource(ip);

        msr.setUrn(Math.abs(r.nextInt()) + "BlahBlah" + Math.abs(r.nextInt()));

        msr.setProperty(FilePropertyKey.FILE_SIZE, 0.9);
        msr.setProperty(FilePropertyKey.NAME, name);
        msr.setProperty(FilePropertyKey.DATE_CREATED,
            new GregorianCalendar(2008, 7, 20).getTimeInMillis());
        handleSearchResult(msr);

    }
    
    /**
     * Returns true if the specified category is valid for this search.
     */
    private boolean isValidCategory(SearchCategory searchCategory) {
        return (getCategory() == SearchCategory.ALL) || (getCategory() == searchCategory);
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
            return new MockFriendPresence(new MockFriend(description));
        }
    }
}