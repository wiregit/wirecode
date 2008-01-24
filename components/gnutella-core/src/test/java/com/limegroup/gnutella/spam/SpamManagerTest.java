package com.limegroup.gnutella.spam;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;

import org.limewire.io.IpPortSet;

import com.google.inject.Injector;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;

public class SpamManagerTest extends LimeTestCase {

    /** various names of files - some tokens need to exist in each" */
    private static final String badger = " badger ";
    private static final String mushroom = " mushroom ";
    private static final String snake = " snake ";
    
    /** addresses */
    private static final String addr1 = "1.1.1.1";
    private static final int port1 = 6346;
    private static final String addr2 = "2.2.2.2";
    private static final int port2 = 6347;
    
    /** sizes */
    private static final int size1 = 1000;
    private static final int size2 = 2000;

    /** xml docs */
    private static final String xml1 = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=" +
        "\"http://www.limewire.com/schemas/audio.xsd\"><audio " +
        "title=\"badger\"" +
        "></audio></audios>";

//    private static final String xml2 = "<?xml version=\"1.0\"?><videos xsi:noNamespaceSchemaLocation=" +
//        "\"http://www.limewire.com/schemas/video.xsd\"><video " +
//        "title=\"mushroom\"" +
//        "></video></videos>";

    private static URN urn1, urn2, urn3;
    
    private LimeXMLDocument doc1;
    //private LimeXMLDocument doc2;
    private SpamManager manager;
    private LimeXMLDocumentFactory limeXMLDocumentFactory;
    private QueryRequestFactory queryRequestFactory;
    private RemoteFileDescFactory remoteFileDescFactory;
    
    public SpamManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(SpamManagerTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        SearchSettings.ENABLE_SPAM_FILTER.setValue(true);
        SearchSettings.FILTER_SPAM_RESULTS.revertToDefault();
        
        urn1 = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB");
        urn2 = URN.createSHA1Urn("urn:sha1:ZLSTHIPQGSSZTS5FJUPAKUZWUGZQYPFB");
        urn3 = URN.createSHA1Urn("urn:sha1:YLSTHIPQGSSZTS5FJUPAKUZWUGZQYPFB");

		Injector injector = LimeTestUtils.createInjector();
		limeXMLDocumentFactory = injector.getInstance(LimeXMLDocumentFactory.class);
		queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
		remoteFileDescFactory = injector.getInstance(RemoteFileDescFactory.class);
		manager = injector.getInstance(SpamManager.class);
        manager.clearFilterData();
        
        doc1 = limeXMLDocumentFactory.createLimeXMLDocument(xml1);
    }
    
    /** 
     * tests that when the user sets one result to be spammy, it affects
     * the rating of other results with similar tokens in them
     */
    public void testSetSpam() throws Exception {
        RemoteFileDesc badgers = createRFD(addr1, port1, badger, null, urn1, size1);
        RemoteFileDesc badgerMushroom = createRFD(addr2, port2, badger+mushroom, null, urn2, size2);
        RemoteFileDesc mushroomSnake = createRFD(addr2, port2, mushroom+snake, null, urn2, size2);
        
        // originally, none of the rfds should be considered spam
        assertFalse(manager.isSpam(badgers));
        assertFalse(manager.isSpam(badgerMushroom));
        assertFalse(manager.isSpam(mushroomSnake));
        assertTrue(badgers.getSpamRating() == 0);
        assertTrue(badgerMushroom.getSpamRating() == 0);
        assertTrue(mushroomSnake.getSpamRating() == 0);
        
        // lets say the user marks the badgers as spam
        manager.handleUserMarkedSpam(new RemoteFileDesc[]{badgers});
        
        // the badgers should be spam, but the other rfd's not
        assertTrue(manager.isSpam(badgers));
        assertFalse(manager.isSpam(badgerMushroom));
        assertFalse(manager.isSpam(mushroomSnake));
        
        // the badger & mushroom rfd should have some spam rating increase
        // but should not be as spammy as the badgers
        assertGreaterThan(0, badgerMushroom.getSpamRating());
        assertLessThan(badgers.getSpamRating(), badgerMushroom.getSpamRating());
        
        // the mushroom & snake rfd should still be 0
        assertTrue(0 == mushroomSnake.getSpamRating());
    }
    
    /** 
     * tests that when the user sets one result to be not spam, it affects
     * the rating of other results with similar tokens in them
     */    
    public void testSetNotSpam() throws Exception {
        SearchSettings.FILTER_SPAM_RESULTS.setValue(0.5f);
        RemoteFileDesc badgers = createRFD(addr1, port1, badger, null, urn1, size1);
        RemoteFileDesc badgerMushroom = createRFD(addr1, port1, badger + mushroom, null, urn1, size1);
        RemoteFileDesc mushroomSnake = createRFD(addr1, port1, mushroom + snake, null, urn3, size1);
        // badgers & mushroom will be marked as spam, so we work with a very similar rfd
        RemoteFileDesc newBadgerMushroom = createRFD(addr1, port1, badger + mushroom, null, urn2, size1);
        
        // mark the badgers and mushrooms as spam
        manager.handleUserMarkedSpam(new RemoteFileDesc[]{badgers,badgerMushroom});
        
        // the snake and new rfd should be brought above the spam treshold, but not be 100%
        // furthermore, rfd2new should have higher spam rating than the snake
        assertTrue("not spam? "+badgers.getSpamRating(),manager.isSpam(badgers));
        assertTrue("not spam? "+badgerMushroom.getSpamRating(),manager.isSpam(badgerMushroom));
        assertTrue("not spam? "+mushroomSnake.getSpamRating(),manager.isSpam(mushroomSnake));
        assertTrue("not spam? "+newBadgerMushroom.getSpamRating(),manager.isSpam(newBadgerMushroom));
        
        assertTrue("not spam? "+badgers.getSpamRating(),1f == badgers.getSpamRating());
        assertTrue("not spam? "+badgerMushroom.getSpamRating(),1f == badgerMushroom.getSpamRating());
        assertLessThan("not spam? "+mushroomSnake.getSpamRating(),1f,mushroomSnake.getSpamRating());
        assertLessThan("not spam? "+mushroomSnake.getSpamRating(),1f,newBadgerMushroom.getSpamRating());
        assertLessThan("not spam? "+newBadgerMushroom.getSpamRating(),
                newBadgerMushroom.getSpamRating(),mushroomSnake.getSpamRating());
        
        
        // if the user says the mushroomSnake is not spammy at all, the badgerMushroom should drop
        manager.handleUserMarkedGood(new RemoteFileDesc[]{mushroomSnake});
        assertFalse(manager.isSpam(mushroomSnake));
        assertFalse(manager.isSpam(newBadgerMushroom));
        assertTrue(manager.isSpam(badgers));
        assertTrue(manager.isSpam(badgerMushroom));
        
        assertTrue(1f == badgers.getSpamRating());
        assertLessThan(1f,newBadgerMushroom.getSpamRating());
        assertTrue(0f == mushroomSnake.getSpamRating());
    }
    
    /**
     * tests that the URN is the most important factor when checking for spam
     */
    public void testUrnOverrides() throws Exception {
        // the badgers and snake have nothing in common but the urn
        RemoteFileDesc badgers = createRFD(addr1, port1, badger, null, urn1, size1);
        RemoteFileDesc snakes = createRFD(addr2, port2, snake, null, urn1, size2);
        
        // marking one spam will automatically mark the other.
        manager.handleUserMarkedSpam(new RemoteFileDesc[]{badgers});
        
        assertTrue(manager.isSpam(snakes));
        
        // and vice versa
        manager.handleUserMarkedGood(new RemoteFileDesc[]{snakes});
        
        assertFalse(manager.isSpam(badgers));
    }
    
    /** 
     * tests that the ratings are lowered for keywords the user searches for
     */
    public void testQueryLowers() throws Exception {
        // make all the players slightly dependent, and mark the mushrooms as spam
        // the badgers and snake should have a little higher ratings because of that
        RemoteFileDesc mushroomBadgers = createRFD(addr1, port1, mushroom+badger, null, urn1, size1);
        RemoteFileDesc mushrooms = createRFD(addr2, port2, mushroom, null, urn2, size2);
        RemoteFileDesc mushroomSnakes = createRFD(addr1, port1, mushroom+snake, null, urn3, size1);
        
        manager.handleUserMarkedSpam(new RemoteFileDesc[]{mushrooms});
        
        assertTrue(manager.isSpam(mushrooms));
        assertFalse(manager.isSpam(mushroomBadgers));
        assertFalse(manager.isSpam(mushroomSnakes));
        
        float badgerRating = mushroomBadgers.getSpamRating();
        float snakeRating = mushroomSnakes.getSpamRating();
        assertGreaterThan(0f, badgerRating);
        assertGreaterThan(0f, snakeRating);
        
        // make the user send a query with a badger and a mushroom
        QueryRequest qr = queryRequestFactory.createQuery(mushroom);
        manager.startedQuery(qr);
        
        // if we receive results containing badgers and snakes their rating
        // should be lower than before
        RemoteFileDesc newBadger = createRFD(addr1,port1,mushroom + badger,null,urn1,size1);
        RemoteFileDesc newSnake = createRFD(addr1,port1,mushroom + snake,null,urn1,size1);
        
        assertFalse(manager.isSpam(newBadger));
        assertFalse(manager.isSpam(newSnake));
        
        assertLessThan(badgerRating, newBadger.getSpamRating());
        assertLessThan(snakeRating, newSnake.getSpamRating());
        
    }
    
    /**
     * tests that the address of the result is coming from affects the spam rating
     */
    public void testAddressAffects() throws Exception {
        // create two completely different rfds except for the address
        RemoteFileDesc badgers = createRFD(addr1,port1,badger,null,urn1,size1);
        RemoteFileDesc snakes = createRFD(addr1,port1,mushroom,null,urn2,size2);
        
        assertFalse(manager.isSpam(badgers));
        assertFalse(manager.isSpam(snakes));
        
        assertTrue(0f == badgers.getSpamRating());
        assertTrue(0f == snakes.getSpamRating());
        
        // mark one of them as bad, the rating of the other should rise a little bit,
        // but not enough to consider it spam
        manager.handleUserMarkedSpam(new RemoteFileDesc[]{badgers});
        
        assertFalse(manager.isSpam(snakes));
        assertGreaterThan(0f, snakes.getSpamRating());
        
    }

    /**
     * tests that the size of the result affects the spam rating
     */
    public void testSizeAffects() throws Exception {
        // create two completely different rfds except for the size
        RemoteFileDesc badgers = createRFD(addr1,port1,badger,null,urn1,size1);
        RemoteFileDesc snakes = createRFD(addr2,port2,mushroom,null,urn2,size1);
        
        assertFalse(manager.isSpam(badgers));
        assertFalse(manager.isSpam(snakes));
        
        assertTrue(0f == badgers.getSpamRating());
        assertTrue(0f == snakes.getSpamRating());
        
        // mark one of them as bad, the rating of the other should rise a little bit,
        // but not enough to consider it spam
        manager.handleUserMarkedSpam(new RemoteFileDesc[]{badgers});
        
        assertFalse(manager.isSpam(snakes));
        assertGreaterThan(0f, snakes.getSpamRating());
    }
    
    /**
     * tests that any xml documents in the result is affect the spam rating
     */
    public void testXMLAffects() throws Exception {
        // create two completely different rfds except for the address
        RemoteFileDesc badgers = createRFD(addr1,port1,badger,doc1,urn1,size1);
        RemoteFileDesc snakes = createRFD(addr2,port2,mushroom,doc1,urn2,size2);
        
        assertFalse(manager.isSpam(badgers));
        assertFalse(manager.isSpam(snakes));
        
        assertTrue(0f == badgers.getSpamRating());
        assertTrue(0f == snakes.getSpamRating());
        
        // mark one of them as bad, the rating of the other should rise a little bit,
        // but not enough to consider it spam
        manager.handleUserMarkedSpam(new RemoteFileDesc[]{badgers});
        
        assertFalse(manager.isSpam(snakes));
        assertGreaterThan(0f, snakes.getSpamRating());
        
    }

    private RemoteFileDesc createRFD(String addr, int port,
            String name, LimeXMLDocument doc, URN urn, int size) {
        Set<URN> urns = new HashSet<URN>();
        urns.add(urn);
        return remoteFileDescFactory.createRemoteFileDesc(addr, port, 1, name, size,
                DataUtils.EMPTY_GUID, 3, false, 3, false, doc, urns, false, false, "ALT",
                new IpPortSet(), 0l, false);
    }
    
}
