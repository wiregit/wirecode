package com.limegroup.gnutella.spam;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;

import org.limewire.core.settings.FilterSettings;
import org.limewire.core.settings.SearchSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;

import com.google.inject.Injector;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.ResponseFactory;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.filters.URNFilter;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;

public class SpamManagerTest extends LimeTestCase {

    /** keywords */
    private static final String badger = " badger ";
    private static final String mushroom = " mushroom ";
    private static final String snake = " snake ";
    
    /** addresses */
    private static final String addr1 = "1.1.1.1";
    private static final String addr2 = "2.2.2.2";
    private static final String addr3 = "3.3.3.3";
    private static final String addr4 = "4.4.4.4";
    
    /** ports */
    private static final int port1 = 1, port2 = 2, port3 = 3, port4 = 4;
    
    /** sizes */
    private static final int size1 = 1000;
    private static final int size2 = 2000;
    private static final int size3 = 3000;
    private static final int size4 = 4000;

    /** xml docs */
    private static final String xml1 = "<?xml version='1.0'?>" +
        "<audios xsi:noNamespaceSchemaLocation=" +
        "'http://www.limewire.com/schemas/audio.xsd'>" +
        "<audio title='badger'></audio></audios>";

    private static final String xml2 = "<?xml version='1.0'?>" +
        "<videos xsi:noNamespaceSchemaLocation=" +
        "'http://www.limewire.com/schemas/video.xsd'>" +
        "<video title='mushroom'></video></videos>";

    private static URN urn1, urn2, urn3, urn4, spamUrn;
    private static LimeXMLDocument doc1, doc2;
    
    private Injector injector;
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
        SearchSettings.FILTER_SPAM_RESULTS.setValue(0.5f);
        
        urn1 = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB");
        urn2 = URN.createSHA1Urn("urn:sha1:ZLSTHIPQGSSZTS5FJUPAKUZWUGZQYPFB");
        urn3 = URN.createSHA1Urn("urn:sha1:YLSTHIPQGSSZTS5FJUPAKUZWUGZQYPFB");
        urn4 = URN.createSHA1Urn("urn:sha1:XLSTHIPQGSSZTS5FJUPAKUZWUGZQYPFB");
        
        // Blacklist a URN
        String spam = "WLSTHIPQGSSZTS5FJUPAKUZWUGZQYPFB";
        spamUrn = URN.createSHA1Urn("urn:sha1:" + spam);
        FilterSettings.FILTERED_URNS_LOCAL.set(new String[] { spam });

		injector = LimeTestUtils.createInjector();
		limeXMLDocumentFactory = injector.getInstance(LimeXMLDocumentFactory.class);
		queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
		remoteFileDescFactory = injector.getInstance(RemoteFileDescFactory.class);
		manager = injector.getInstance(SpamManager.class);
        manager.clearFilterData();
        
        doc1 = limeXMLDocumentFactory.createLimeXMLDocument(xml1);
        doc2 = limeXMLDocumentFactory.createLimeXMLDocument(xml2);
    }
    
    /**
     * Tests that incoming results start with a zero spam rating
     */
    public void testStartsAtZero() throws Exception {
        RemoteFileDesc result = createRFD(addr1, port1, badger, null, urn1, size1);
        assertFalse(result.isSpam());
        assertEquals(0f, result.getSpamRating());
    }
    
    /** 
     * Tests that when the user marks one result as spam, it affects the
     * rating of subsequent related results, but not existing results or
     * subsequent unrelated results
     */
    public void testSetSpam() throws Exception {
        // Two results arrive
        RemoteFileDesc marked = createRFD(addr1, port1, badger, null, urn1, size1);
        RemoteFileDesc related1 = createRFD(addr2, port2, badger+mushroom, null, urn2, size2);
        
        // Save the spam ratings before the user's action
        float markedRating = marked.getSpamRating();
        float related1Rating = related1.getSpamRating();
        
        // The user marks the first result as spam - its rating should increase
        // but the rating of the related result should not
        manager.handleUserMarkedSpam(new RemoteFileDesc[]{marked});
        assertTrue(marked.isSpam());
        assertGreaterThan(markedRating, marked.getSpamRating());
        assertFalse(related1.isSpam());
        assertEquals(related1Rating, related1.getSpamRating());
        
        // Another related result arrives after the user's action - it should
        // receive a spam rating greater than zero but less than the rating
        // of the spam result
        RemoteFileDesc related2 = createRFD(addr3, port3, badger+snake, null, urn3, size3);
        assertGreaterThan(0f, related2.getSpamRating());
        assertLessThan(marked.getSpamRating(), related2.getSpamRating());
        
        // An unrelated result - it should receive a zero spam rating
        RemoteFileDesc unrelated = createRFD(addr4, port4, mushroom+snake, null, urn4, size4);
        assertFalse(unrelated.isSpam());
        assertEquals(0f, unrelated.getSpamRating());
    }
    
    /** 
     * Tests that when the user marks one result as not spam, it affects the
     * rating of subsequent related results, but not existing results
     */    
    public void testSetNotSpam() throws Exception {
        // A result arrives and the user marks it as spam
        RemoteFileDesc marked = createRFD(addr1, port1, badger, null, urn1, size1);
        manager.handleUserMarkedSpam(new RemoteFileDesc[]{marked});
        assertTrue(marked.isSpam());
        float markedRating = marked.getSpamRating();
        assertGreaterThan(0f, markedRating);
        
        // A related result arrives - it should receive a non-zero spam rating
        RemoteFileDesc related1 = createRFD(addr2, port2, badger+mushroom, null, urn2, size2);
        float related1Rating = related1.getSpamRating();
        assertGreaterThan(0f, related1Rating);
        assertLessThan(markedRating, related1Rating);
        
        // Now the user marks the first result as NOT spam - its rating should
        // decrease but the rating of the related result should not
        manager.handleUserMarkedGood(new RemoteFileDesc[]{marked});
        assertFalse(marked.isSpam());
        assertLessThan(markedRating, marked.getSpamRating());
        assertEquals(related1Rating, related1.getSpamRating());
        
        // Another related result arrives after the user's action - it should
        // receive a lower rating than the first related result
        RemoteFileDesc related2 = createRFD(addr3, port3, badger+snake, null, urn3, size3);
        assertLessThan(related1Rating, related2.getSpamRating());
    }
    
    /**
     * Tests that the URN is sufficient to identify spam
     */
    public void testUrnIsEnough() throws Exception {
        // A result arrives and the user marks it as spam
        RemoteFileDesc marked = createRFD(addr1, port1, badger, null, urn1, size1);
        manager.handleUserMarkedSpam(new RemoteFileDesc[]{marked});
        assertTrue(marked.isSpam());
        assertGreaterThan(0f, marked.getSpamRating());
        
        // Another result arrives, with the same URN but nothing else in
        // common - it should be marked as spam
        RemoteFileDesc sameURN = createRFD(addr2, port2, mushroom, null, urn1, size2);
        assertTrue(sameURN.isSpam());
        assertGreaterThan(0f, sameURN.getSpamRating());
    }
    
    /** 
     * Tests that the ratings are lowered for keywords the user searches for
     */
    public void testQueryLowers() throws Exception {
        // A result arrives and the user marks it as spam
        RemoteFileDesc marked = createRFD(addr1, port1, badger, null, urn1, size1);
        manager.handleUserMarkedSpam(new RemoteFileDesc[]{marked});
        assertTrue(marked.isSpam());
        assertGreaterThan(0f, marked.getSpamRating());
        
        // A related result arrives - it should receive a non-zero spam rating
        RemoteFileDesc related1 = createRFD(addr2, port2, badger+mushroom, null, urn2, size2);
        assertGreaterThan(0f, related1.getSpamRating());
        assertLessThan(marked.getSpamRating(), related1.getSpamRating());
        
        // Now the user issues a query with related keywords
        QueryRequest qr = queryRequestFactory.createQuery(badger+snake);
        manager.startedQuery(qr);
        
        // Another related result arrives - it should receive a lower rating
        // than the first related result
        RemoteFileDesc related2 = createRFD(addr3, port3, badger+mushroom, null, urn3, size3);
        assertLessThan(related1.getSpamRating(), related2.getSpamRating());
    }
    
    /**
     * Tests that the address (not port) of the result affects the spam rating
     */
    public void testAddressAffects() throws Exception {
        // A result arrives and the user marks it as spam
        RemoteFileDesc marked = createRFD(addr1, port1, badger, null, urn1, size1);
        manager.handleUserMarkedSpam(new RemoteFileDesc[]{marked});
        assertTrue(marked.isSpam());
        assertGreaterThan(0f, marked.getSpamRating());
        
        // A result with nothing in common but the address should receive a
        // non-zero rating
        RemoteFileDesc sameAddress = createRFD(addr1, port2, mushroom, null, urn2, size2);
        assertGreaterThan(0f, sameAddress.getSpamRating());
        assertLessThan(marked.getSpamRating(), sameAddress.getSpamRating());
        
        // A result with nothing in common should receive a zero spam rating
        RemoteFileDesc unrelated = createRFD(addr3, port3, snake, null, urn3, size3);
        assertFalse(unrelated.isSpam());
        assertEquals(0f, unrelated.getSpamRating());
    }

    /**
     * Tests that the size of the result affects the spam rating
     */
    public void testSizeAffects() throws Exception {
        // A result arrives and the user marks it as spam
        RemoteFileDesc marked = createRFD(addr1, port1, badger, null, urn1, size1);
        manager.handleUserMarkedSpam(new RemoteFileDesc[]{marked});
        assertTrue(marked.isSpam());
        assertGreaterThan(0f, marked.getSpamRating());
        
        // A result with nothing in common but the size should receive a
        // non-zero rating
        RemoteFileDesc sameAddress = createRFD(addr2, port2, mushroom, null, urn2, size1);
        assertGreaterThan(0f, sameAddress.getSpamRating());
        assertLessThan(marked.getSpamRating(), sameAddress.getSpamRating());
        
        // A result with nothing in common should receive a zero spam rating
        RemoteFileDesc unrelated = createRFD(addr3, port3, snake, null, urn3, size3);
        assertFalse(unrelated.isSpam());
        assertEquals(0f, unrelated.getSpamRating());
    }
    
    /**
     * Tests that the approximate size of the result affects the spam rating
     */
    public void testApproximateSizeAffects() throws Exception {
        int size5 = 1024, size6 = 1025, size7 = 2048;
        
        // A result arrives and the user marks it as spam
        RemoteFileDesc marked = createRFD(addr1, port1, badger, null, urn1, size5);
        manager.handleUserMarkedSpam(new RemoteFileDesc[]{marked});
        assertTrue(marked.isSpam());
        assertGreaterThan(0f, marked.getSpamRating());
        
        // A result with nothing in common but similar in size should receive
        // a non-zero rating
        RemoteFileDesc sameAddress = createRFD(addr2, port2, mushroom, null, urn2, size6);
        assertGreaterThan(0f, sameAddress.getSpamRating());
        assertLessThan(marked.getSpamRating(), sameAddress.getSpamRating());
        
        // A result with nothing in common should receive a zero spam rating
        RemoteFileDesc unrelated = createRFD(addr3, port3, snake, null, urn3, size7);
        assertFalse(unrelated.isSpam());
        assertEquals(0f, unrelated.getSpamRating());
    }
    
    /**
     * Tests that the client GUID of the result affects the spam rating
     */
    public void testClientGUIDAffects() throws Exception {
        byte[] guid1 = GUID.makeGuid();
        byte[] guid2 = GUID.makeGuid();
        
        // A result arrives and the user marks it as spam
        RemoteFileDesc marked =
            createRFD(addr1, port1, badger, null, urn1, size1, guid1);
        manager.handleUserMarkedSpam(new RemoteFileDesc[]{marked});
        assertTrue(marked.isSpam());
        assertGreaterThan(0f, marked.getSpamRating());
        
        // A result with nothing in common but the client GUID should receive
        // a non-zero rating
        RemoteFileDesc sameGUID =
            createRFD(addr2, port2, mushroom, null, urn2, size2, guid1);
        assertGreaterThan(0f, sameGUID.getSpamRating());
        assertLessThan(marked.getSpamRating(), sameGUID.getSpamRating());
        
        // A result with nothing in common should receive a zero spam rating
        RemoteFileDesc unrelated =
            createRFD(addr3, port3, snake, null, urn3, size3, guid2);
        assertFalse(unrelated.isSpam());
        assertEquals(0f, unrelated.getSpamRating());
    }
    
    /**
     * Tests that any XML documents in the result affect the spam rating
     */
    public void testXMLAffects() throws Exception {
        // A result arrives and the user marks it as spam
        RemoteFileDesc marked = createRFD(addr1, port1, badger, doc1, urn1, size1);
        manager.handleUserMarkedSpam(new RemoteFileDesc[]{marked});
        assertTrue(marked.isSpam());
        assertGreaterThan(0f, marked.getSpamRating());
        
        // A result with nothing in common but the XML should receive a
        // non-zero rating
        RemoteFileDesc sameAddress = createRFD(addr2, port2, mushroom, doc1, urn2, size2);
        assertGreaterThan(0f, sameAddress.getSpamRating());
        assertLessThan(marked.getSpamRating(), sameAddress.getSpamRating());
        
        // A result with nothing in common should receive a zero spam rating
        RemoteFileDesc unrelated = createRFD(addr3, port3, snake, doc2, urn3, size3);
        assertFalse(unrelated.isSpam());
        assertEquals(0f, unrelated.getSpamRating());
    }
    
    /**
     * Tests that the extension of the result affects the spam rating less
     * than an ordinary keyword 
     */
    public void testKeywordBeatsExtension() throws Exception {
        String ext1 = ".foo", ext2 = ".bar";
        
        // A result arrives and the user marks it as spam
        RemoteFileDesc marked = createRFD(addr1, port1, badger+ext1, null, urn1, size1);
        manager.handleUserMarkedSpam(new RemoteFileDesc[]{marked});
        assertTrue(marked.isSpam());
        assertGreaterThan(0f, marked.getSpamRating());
        
        // A result with nothing in common but the extension should receive a
        // non-zero rating
        RemoteFileDesc sameExt = createRFD(addr2, port2, mushroom+ext1, null, urn2, size2);
        assertGreaterThan(0f, sameExt.getSpamRating());
        assertLessThan(marked.getSpamRating(), sameExt.getSpamRating());
        
        // A result with nothing in common but an ordinary keyword should
        // receive a non-zero rating
        RemoteFileDesc sameWord = createRFD(addr3, port3, badger+ext2, null, urn3, size3);
        assertGreaterThan(0f, sameWord.getSpamRating());
        assertLessThan(marked.getSpamRating(), sameWord.getSpamRating());
        
        // The extension should have less effect than the keyword
        assertLessThan(sameWord.getSpamRating(), sameExt.getSpamRating());
        
        // A result with nothing in common should receive a zero spam rating
        RemoteFileDesc unrelated = createRFD(addr4, port4, snake, null, urn4, size4);
        assertFalse(unrelated.isSpam());
        assertEquals(0f, unrelated.getSpamRating());
    }
    
    /**
     * Tests that private (LAN) addresses are ignored
     */
    public void testPrivateAddressIgnored() throws Exception {
        String privateAddress = "192.168.0.1";
        
        // A result arrives and the user marks it as spam
        RemoteFileDesc marked = createRFD(privateAddress, port1, badger, null, urn1, size1);
        manager.handleUserMarkedSpam(new RemoteFileDesc[]{marked});
        assertTrue(marked.isSpam());
        assertGreaterThan(0f, marked.getSpamRating());
        
        // A result with nothing in common but the private address should
        // receive a zero rating
        RemoteFileDesc unrelated = createRFD(privateAddress, port2, mushroom, null, urn2, size2);
        assertFalse(unrelated.isSpam());
        assertEquals(0f, unrelated.getSpamRating());
    }
    
    /**
     * Tests that the IP blacklist no longer affects the spam rating - results
     * from blacklisted IPs should be dropped before reaching this stage
     */
    public void testAddressBlacklistDoesNotAffect() throws Exception {
        FilterSettings.HOSTILE_IPS.set(new String[] {addr1});
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.set(new String[] {addr2});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.set(new String[] {addr3});

        // A reply from a hostile address should receive a zero rating
        RemoteFileDesc hostile = createRFD(addr1, port1, badger, null, urn1, size1);
        assertFalse(hostile.isSpam());
        assertEquals(0f, hostile.getSpamRating());

        // A reply from a blacklisted address should receive a zero rating
        RemoteFileDesc black = createRFD(addr2, port2, badger, null, urn2, size2);
        assertFalse(black.isSpam());
        assertEquals(0f, black.getSpamRating());

        // A reply from a whitelisted address should receive a zero rating
        RemoteFileDesc white = createRFD(addr3, port3, badger, null, urn3, size3);
        assertFalse(white.isSpam());
        assertEquals(0f, white.getSpamRating());

        FilterSettings.HOSTILE_IPS.revertToDefault();
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.revertToDefault();
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.revertToDefault();
    }

    /** 
     * Tests that the URN blacklist affects the spam rating
     */
    public void testUrnBlacklistAffects() throws Exception {
        // A query reply with a blacklisted URN arrives
        ResponseFactory responseFactory =
            injector.getInstance(ResponseFactory.class);
        Response response =
            responseFactory.createResponse(0, size1, badger+snake, spamUrn);
        QueryReplyFactory queryReplyFactory =
            injector.getInstance(QueryReplyFactory.class);
        QueryReply reply = queryReplyFactory.createQueryReply(GUID.makeGuid(),
                (byte)0, port1, new byte[] {1, 1, 1, 1}, 0,
                new Response[] {response}, GUID.makeGuid(), false, false,
                false, false, false, false);

        // The URN filter should block the result and mark it as spam
        URNFilter urnFilter = injector.getInstance(URNFilter.class);
        urnFilter.refreshURNs();
        assertEquals(0, manager.getRatingTable().size());
        assertFalse(urnFilter.allow(reply));
        assertGreaterThan(0, manager.getRatingTable().size());

        // A result with the same keywords as the spam result should receive a
        // zero spam rating
        RemoteFileDesc sameName =
            createRFD(addr2, port2, badger+snake, null, urn2, size2);
        assertFalse(sameName.isSpam());
        assertEquals(0f, sameName.getSpamRating());

        // A result from the same address as the spam result should received a
        // non-zero spam rating
        RemoteFileDesc sameAddr =
            createRFD(addr1, port3, badger+snake, null, urn3, size3);
        assertGreaterThan(0f, sameAddr.getSpamRating());

        // A result with the same size as the spam result should receive a
        // non-zero spam rating
        RemoteFileDesc sameSize =
            createRFD(addr4, port4, badger+snake, null, urn4, size1);
        assertGreaterThan(0f, sameSize.getSpamRating());

        // An unrelated result should receive a zero spam rating
        RemoteFileDesc unrelated =
            createRFD(addr4, port4, badger+snake, null, urn4, size4);
        assertFalse(unrelated.isSpam());
        assertEquals(0f, unrelated.getSpamRating());
    }
    
    private RemoteFileDesc createRFD(String addr, int port, String name,
            LimeXMLDocument doc, URN urn, int size) throws Exception {
        return createRFD(addr, port, name, doc, urn, size, GUID.makeGuid());
    }
    
    private RemoteFileDesc createRFD(String addr, int port, String name,
            LimeXMLDocument doc, URN urn, int size, byte[] guid)
    throws Exception {
        Set<URN> urns = new HashSet<URN>();
        urns.add(urn);
        RemoteFileDesc rfd =
                remoteFileDescFactory.createRemoteFileDesc(
                        new ConnectableImpl(addr, port, false), 1, name, size,
                        guid, 3, 3, false, doc, urns, false, "ALT", 0);
        // This would normally be called by the SearchResultHandler
        manager.calculateSpamRating(rfd);
        return rfd;    
    }
}