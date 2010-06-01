package com.limegroup.gnutella.spam;

import java.io.File;
import java.net.UnknownHostException;
import java.util.Map;

import junit.framework.Test;

import org.limewire.core.settings.FilterSettings;
import org.limewire.core.settings.SearchSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.io.URN;
import org.limewire.util.Base32;
import org.limewire.util.TestUtils;

import com.google.inject.Injector;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;

public class RatingTableTest extends LimeTestCase {

    private final String addr = "1.1.1.1";
    private final int port = 1111;
    private final String name = "abc.def";
    private final int size = 12345;

    private SpamManager manager;
    private RemoteFileDescFactory rfdFactory;

    public RatingTableTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(RatingTableTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        FilterSettings.USE_NETWORK_FILTER.setValue(false);
        SearchSettings.ENABLE_SPAM_FILTER.setValue(true);
        Injector inject = LimeTestUtils.createInjectorNonEagerly();
        manager = inject.getInstance(SpamManager.class);
        rfdFactory = inject.getInstance(RemoteFileDescFactory.class);
    }

    /**
     * Tests that tokens with default ratings are not stored in the table
     */
    public void testDefaultRatingsAreNotStored() throws Exception {
        RatingTable table = manager.getRatingTable();
        RemoteFileDesc rfd = createRFD(addr, port, name, size);
        assertFalse(rfd.isSpam());
        assertEquals(0f, rfd.getSpamRating());
        assertEquals(0, table.size());
    }

    /**
     * Tests that tokens with non-default ratings are stored in the table
     */
    public void testNonDefaultRatingsAreStored() throws Exception {
        RatingTable table = manager.getRatingTable();
        RemoteFileDesc rfd = createRFD(addr, port, name, size);
        manager.handleUserMarkedSpam(new RemoteFileDesc[]{rfd});
        assertTrue(rfd.isSpam());
        assertGreaterThan(0f, rfd.getSpamRating());
        // There should be six tokens: address, name, ext, size, approx size,
        // client GUID
        assertEquals(6, table.size());        
    }

    /**
     * Tests that the size and least-recently-used order of the rating table are
     * preserved when the ratings are saved and loaded
     */
    public void testSavingAndLoadingPreservesSizeAndOrder() throws Exception {
        RatingTable table = manager.getRatingTable();
        // Create some tokens with non-default ratings
        for(int i = 0; i < 10; i++) {
            String address = "1.2.3." + i;
            String name = "foo" + i + "." + i; // Unique name and extension
            RemoteFileDesc rfd = createRFD(address, i + 1024, name, i * 1000);
            manager.handleUserMarkedSpam(new RemoteFileDesc[]{rfd});
        }
        // There should be six tokens for each RFD: address, name, ext, size,
        // approx size, client GUID
        assertEquals(60, table.size());
        // Get the least-recently-used token
        Token t = table.getLeastRecentlyUsed();
        // Save and load the ratings
        table.stop();
        table.start();
        // Check that the size hasn't changed
        assertEquals(60, table.size());
        // Check that the least-recently-used token hasn't changed
        assertEquals(t, table.getLeastRecentlyUsed());
    }

    public void testInspection() throws Exception {
        RatingTable table = manager.getRatingTable();
        // Create some tokens with non-default ratings
        for(int i = 0; i < 10; i++) {
            String address = "1.2.3." + i;
            String name = "foo" + i + "." + i; // Unique name and extension
            RemoteFileDesc rfd = createRFD(address, i + 1024, name, i * 1000);
            manager.handleUserMarkedSpam(new RemoteFileDesc[]{rfd});
        }
        // There should be six tokens for each RFD: address, name, ext, size,
        // approx size, client GUID
        assertEquals(60, table.size());
        // There should be 10 tokens of each type
        Map<?, ?> m = (Map) table.TOKEN_COUNTS.inspect();
        String[] types = {
                "AddressToken",
                "KeywordToken",
                "FileExtensionToken",
                "SizeToken",
                "ApproximateSizeToken",
                "ClientGUIDToken"
        };
        for(String type : types) {
            Integer count = (Integer)m.get(type);
            assertEquals(10, count.intValue());
        }
    }

    public void testTokensAreLoadedFromSettings() {
        String template1 = "AAAABBBBCCCCDDDDEEEEFFFFGGGGHHHH";
        String template2 = "BBBBCCCCDDDDEEEEFFFFGGGGHHHHIIII";
        long size1 = 12345, size2 = 23456;

        FilterSettings.USE_NETWORK_FILTER.setValue(true);
        FilterSettings.SPAM_TEMPLATES.set(new String[] {template1, template2});
        FilterSettings.SPAM_SIZES.set(new String[] {
                String.valueOf(size1), String.valueOf(size2)});

        Token t1 = new TemplateHashToken(Base32.decode(template1));
        Token t2 = new TemplateHashToken(Base32.decode(template2));
        Token t3 = new ApproximateSizeToken(size1);
        Token t4 = new ApproximateSizeToken(size2);

        RatingTable table = manager.getRatingTable();
        assertEquals(0, table.size());
        table.loadSpamTokensFromSettings();
        assertEquals(4, table.size());
        assertEquals(1f, table.lookupAndGetRating(t1));
        assertEquals(1f, table.lookupAndGetRating(t2));
        assertEquals(1f, table.lookupAndGetRating(t3));
        assertEquals(1f, table.lookupAndGetRating(t4));
    }

    public void testNewTokensAreLoadedFromSettingsAfterSimppUpdate() {
        // Start by loading 4 tokens as above
        testTokensAreLoadedFromSettings();

        String template3 = "CCCCDDDDEEEEFFFFGGGGHHHHIIIIJJJJ"; // New token
        long size2 = 23456; // Already in table

        FilterSettings.USE_NETWORK_FILTER.setValue(true);
        FilterSettings.SPAM_TEMPLATES.set(new String[] {template3});
        FilterSettings.SPAM_SIZES.set(new String[] {String.valueOf(size2)});

        Token t5 = new TemplateHashToken(Base32.decode(template3));
        Token t6 = new ApproximateSizeToken(size2);

        RatingTable table = manager.getRatingTable();
        assertEquals(4, table.size());
        table.simppUpdated();;
        assertEquals(5, table.size());
        assertEquals(1f, table.lookupAndGetRating(t5));
        assertEquals(1f, table.lookupAndGetRating(t6));
    }

    public void testSimppRatingsDoNotOverwriteUserRatings() throws Exception {
        // Start by loading 4 tokens as above
        testTokensAreLoadedFromSettings();

        String template3 = "CCCCDDDDEEEEFFFFGGGGHHHHIIIIJJJJ"; // New token
        long size2 = 23456; // Already in table

        FilterSettings.USE_NETWORK_FILTER.setValue(true);
        FilterSettings.SPAM_TEMPLATES.set(new String[] {template3});
        FilterSettings.SPAM_SIZES.set(new String[] {String.valueOf(size2)});

        Token t5 = new TemplateHashToken(Base32.decode(template3));
        Token t6 = new ApproximateSizeToken(size2);

        RatingTable table = manager.getRatingTable();
        assertEquals(4, table.size());

        // The user unmarks a result as spam, modifying one of the tokens
        assertEquals(1f, table.lookupAndGetRating(t6));
        RemoteFileDesc rfd = createRFD(addr, port, name, size2);
        manager.handleUserMarkedGood(new RemoteFileDesc[]{rfd});
        assertLessThan(1f, table.lookupAndGetRating(t6));

        // The SIMPP update should not overwrite the user's rating
        table.simppUpdated();
        assertEquals(1f, table.lookupAndGetRating(t5));
        assertLessThan(1f, table.lookupAndGetRating(t6));
    }

    public void testLoadingOldSpamDatSkipsZeroesAndConvertsTemplates() throws Exception {
        // The old spam.dat file contains four tokens: a keyword token with a
        // non-zero rating, a keyword token with a zero rating, a template token
        // with a non-zero rating, and a template token with a zero rating
        File oldSpamDat =
            TestUtils.getResourceFile("com/limegroup/gnutella/resources/spam.dat");
        RatingTable table = manager.getRatingTable();
        assertEquals(0, table.size());
        assertTrue(oldSpamDat.exists());
        table.load(oldSpamDat);
        // The tokens with zero ratings should have been skipped
        assertEquals(2, table.size());
        // The keyword token with a non-zero rating should have been loaded
        Token t1 = new KeywordToken("foo");
        assertGreaterThan(0f, table.lookupAndGetRating(t1));
        // The template token should have been converted to a template hash
        String template = "N7GW4TYFZQAX3F2NXAD4BK6WULXRLXMT";
        Token t2 = new TemplateHashToken(Base32.decode(template));
        assertGreaterThan(0f, table.lookupAndGetRating(t2));
    }

    private RemoteFileDesc createRFD(String addr, int port, String name,
            long size) throws UnknownHostException {
        RemoteFileDesc rfd = rfdFactory.createRemoteFileDesc(
                new ConnectableImpl(addr, port, false), 1, name, size,
                GUID.makeGuid(), 3, 3, false, null, URN.NO_URN_SET, false,
                "ALT", 0L, false, null);
        // This would normally be called by the SearchResultHandler
        manager.calculateSpamRating(rfd);
        return rfd;
    }
}