package com.limegroup.gnutella.spam;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;

public class SpamManagerTest extends BaseTestCase {

    public SpamManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(SpamManagerTest.class);
    }

    /** various names of files - some tokens need to exist in each" */
    private static final String name1 = "badger badger badger";
    private static final String name2 = "badger mushroom mushroom";
    private static final String name3 = "mushroom mushroom snake";
    
    /** addresses */
    private static final String addr1 = "1.1.1.1";
    private static final int port1 = 6346;
    private static final String addr2 = "2.2.2.2";
    private static final int port2 = 6347;
    
    /** urns */
    private static  URN urn1, urn2, urn3;
    
    /** sizes */
    private static final int size1 = 1000;
    private static final int size2 = 2000;
    
    /** xml docs */
    private static final String xml1 = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=" +
        "\"http://www.limewire.com/schemas/audio.xsd\"><audio " +
        "title=\"badger\"" +
        "></audio></audios>";
    
    private static final String xml2 = "<?xml version=\"1.0\"?><videos xsi:noNamespaceSchemaLocation=" +
        "\"http://www.limewire.com/schemas/video.xsd\"><video " +
        "title=\"mushroom\"" +
        "></video></videos>";
    
    static LimeXMLDocument doc1, doc2;
    
    static SpamManager manager = SpamManager.instance();
    static RemoteFileDesc badgers, mushrooms, snake;
    
    public static void globalSetUp() {
        try {
            urn1 = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB");
            urn2 = URN.createSHA1Urn("urn:sha1:ZLSTHIPQGSSZTS5FJUPAKUZWUGZQYPFB");
            urn3 = URN.createSHA1Urn("urn:sha1:YLSTHIPQGSSZTS5FJUPAKUZWUGZQYPFB");
            doc1 = new LimeXMLDocument(xml1);
            doc2 = new LimeXMLDocument(xml2);
        } catch (Exception bad) {
            fail(bad);
        }
    }
    
    public void setUp() {
        SearchSettings.FILTER_SPAM_RESULTS.revertToDefault();
        manager.clearFilterData();
    }
    
    public void tearDown() {
        if (badgers != null)
            badgers.setSpamRating(0f);
        if (mushrooms != null)
            mushrooms.setSpamRating(0f);
        if (snake != null)
            snake.setSpamRating(0f);
    }
    
    /** 
     * tests that when the user sets one result to be spammy, it affects
     * the rating of other results with similar tokens in them
     */
    public void testSetSpam() throws Exception {
        badgers = createRFD(addr1, port1, name1, null, urn1, size1);
        mushrooms = createRFD(addr2, port2, name2, null, urn2, size2);
        snake = createRFD(addr2, port2, name3, null, urn2, size2);
        
        // originally, none of the rfds should be considered spam
        assertFalse(manager.isSpam(badgers));
        assertFalse(manager.isSpam(mushrooms));
        assertFalse(manager.isSpam(snake));
        assertTrue(badgers.getSpamRating() == 0);
        assertTrue(mushrooms.getSpamRating() == 0);
        assertTrue(snake.getSpamRating() == 0);
        
        // lets say the user marks the badgers as spam
        manager.handleUserMarkedSpam(new RemoteFileDesc[]{badgers});
        
        // the badgers should be spam, but the other rfd's not
        assertTrue(manager.isSpam(badgers));
        assertFalse(manager.isSpam(mushrooms));
        assertFalse(manager.isSpam(snake));
        
        // the badger & mushroom rfd should have some spam rating increase
        // but should not be as spammy as the badgers
        assertGreaterThan(0, mushrooms.getSpamRating());
        assertLessThan(badgers.getSpamRating(), mushrooms.getSpamRating());
        
        // the mushroom & snake rfd should still be 0
        assertTrue(0 == snake.getSpamRating());
    }
    
    /** 
     * tests that when the user sets one result to be not spam, it affects
     * the rating of other results with similar tokens in them
     */    
    public void testSetNotSpam() throws Exception {
        SearchSettings.FILTER_SPAM_RESULTS.setValue(0.5f);
        badgers = createRFD(addr1, port1, name1, null, urn1, size1);
        mushrooms = createRFD(addr1, port1, name2, null, urn1, size1);
        snake = createRFD(addr1, port1, name3, null, urn3, size1);
        // rfd2 will be marked as spam, so we work with a very similar rfd
        RemoteFileDesc newMushroom = createRFD(addr1, port1, name2, null, urn2, size1);
        
        // mark the badgers and mushrooms as spam
        manager.handleUserMarkedSpam(new RemoteFileDesc[]{badgers,mushrooms});
        
        // the snake and new rfd should be brought above the spam treshold, but not be 100%
        // furthermore, rfd2new should have higher spam rating than the snake
        assertTrue("not spam? "+badgers.getSpamRating(),manager.isSpam(badgers));
        assertTrue("not spam? "+mushrooms.getSpamRating(),manager.isSpam(mushrooms));
        assertTrue("not spam? "+snake.getSpamRating(),manager.isSpam(snake));
        assertTrue("not spam? "+newMushroom.getSpamRating(),manager.isSpam(newMushroom));
        
        assertTrue("not spam? "+badgers.getSpamRating(),1f == badgers.getSpamRating());
        assertTrue("not spam? "+mushrooms.getSpamRating(),1f == mushrooms.getSpamRating());
        assertLessThan("not spam? "+snake.getSpamRating(),1f,snake.getSpamRating());
        assertLessThan("not spam? "+snake.getSpamRating(),1f,newMushroom.getSpamRating());
        assertLessThan("not spam? "+newMushroom.getSpamRating(),newMushroom.getSpamRating(),snake.getSpamRating());
        
        
        // if the user says the snake is not spammy at all, the mushrooms should drop
        manager.handleUserMarkedGood(new RemoteFileDesc[]{snake});
        assertFalse(manager.isSpam(snake));
        assertFalse(manager.isSpam(newMushroom));
        assertTrue(manager.isSpam(badgers));
        assertTrue(manager.isSpam(mushrooms));
        
        assertTrue(1f == badgers.getSpamRating());
        assertLessThan(1f,newMushroom.getSpamRating());
        assertTrue(0f == snake.getSpamRating());
    }
    
    /**
     * tests that the URN is the most important factor when checking for spam
     */
    public void testUrnOverrides() throws Exception {
        // the badgers and snake have nothing in common but the urn
        badgers = createRFD(addr1, port1, name1, null, urn1, size1);
        snake = createRFD(addr2, port2, name2, null, urn1, size2);
        
        // marking one spam will automatically mark the other.
        manager.handleUserMarkedSpam(new RemoteFileDesc[]{badgers});
        
        assertTrue(manager.isSpam(snake));
        
        // and vice versa
        manager.handleUserMarkedGood(new RemoteFileDesc[]{snake});
        
        assertFalse(manager.isSpam(badgers));
    }
    
    /** 
     * tests that the ratings are lowered for keywords the user searches for
     */
    public void testQueryLowers() throws Exception {
        // make all the players independent, and mark the mushrooms as spam
        // the badgers and snake should have a little higher ratings because of that
        badgers = createRFD(addr1, port1, name1, null, urn1, size1);
        mushrooms = createRFD(addr2, port2, name2, null, urn2, size2);
        snake = createRFD(addr1, port1, name3, null, urn3, size1);
        
        manager.handleUserMarkedSpam(new RemoteFileDesc[]{mushrooms});
        
        assertTrue(manager.isSpam(mushrooms));
        assertFalse(manager.isSpam(badgers));
        assertFalse(manager.isSpam(snake));
        
        float badgerRating = badgers.getSpamRating();
        float snakeRating = snake.getSpamRating();
        assertGreaterThan(0f, badgerRating);
        assertGreaterThan(0f, snakeRating);
        
        // make the user send a query with a badger and a mushroom
        QueryRequest qr = QueryRequest.createQuery("badger mushroom");
        manager.startedQuery(qr);
        
        // nothing should have changed wrt spam or not
        assertFalse(manager.isSpam(badgers));
        assertFalse(manager.isSpam(snake));
        
        // but the badger and snake should have lower spam ratings
        assertLessThan(badgerRating, badgers.getSpamRating());
        assertLessThan(snakeRating, snake.getSpamRating());
    }
    
    private static RemoteFileDesc createRFD(String addr, int port,
            String name, LimeXMLDocument doc, URN urn, int size) {
        Set urns = new HashSet();
        urns.add(urn);
        return new RemoteFileDesc(addr, port, 1, name,
                size, DataUtils.EMPTY_GUID, 3, 
                false, 3, false,
                doc, urns,
                false,false,
                "ALT",0l,
                Collections.EMPTY_SET, 0l);
    }
    
}
