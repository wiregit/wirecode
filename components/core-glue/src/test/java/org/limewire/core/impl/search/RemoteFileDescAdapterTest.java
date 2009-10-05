package org.limewire.core.impl.search;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.Category;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.impl.search.RemoteFileDescAdapter.AltLocRemoteHost;
import org.limewire.core.impl.search.RemoteFileDescAdapter.RfdRemoteHost;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.AddressFeature;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.IpPort;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.xml.LimeXMLDocument;

public class RemoteFileDescAdapterTest extends BaseTestCase {

    public RemoteFileDescAdapterTest(String name) {
        super(name);
    }

    /**
     * Tests the most basic methods of the RemoteFileDescAdapter.
     */
    public void testBasics() throws Exception {
        Mockery context = new Mockery();

        final RemoteFileDesc remoteFileDesc1 = context.mock(RemoteFileDesc.class);
        final CategoryManager categoryManager = context.mock(CategoryManager.class);
        final Set<IpPort> ipPorts = new HashSet<IpPort>();
        final Address address1 = context.mock(Address.class);
        final byte[] guid1 = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
        final String fileName1 = "remote file name 1.txt";
        final long fileSize = 1234L;
        final URN urn1 = URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA1");

        context.checking(new Expectations() {{
            allowing(remoteFileDesc1).getAddress();
            will(returnValue(address1));
            allowing(remoteFileDesc1).getClientGUID();
            will(returnValue(guid1));
            allowing(address1).getAddressDescription();
            will(returnValue("address 1 description"));
            allowing(remoteFileDesc1).getFileName();
                will(returnValue(fileName1));
                allowing(categoryManager).getCategoryForExtension("txt");
                will(returnValue(Category.DOCUMENT));

            allowing(remoteFileDesc1).getSize();
            will(returnValue(fileSize));
            allowing(remoteFileDesc1).getXMLDocument();
            will(returnValue(null));
            allowing(remoteFileDesc1).getCreationTime();
            will(returnValue(5678L));
            allowing(remoteFileDesc1).getSHA1Urn();
            will(returnValue(urn1));
            allowing(remoteFileDesc1).isSpam();
            will(returnValue(false));
        }});

        RemoteFileDescAdapter remoteFileDescAdapter1 =
            new RemoteFileDescAdapter(remoteFileDesc1, ipPorts, categoryManager);

        assertEquals(Category.DOCUMENT, remoteFileDescAdapter1.getCategory());
        assertEquals("txt", remoteFileDescAdapter1.getFileExtension());
        assertEquals(fileName1, remoteFileDescAdapter1.getFileName());
        assertEquals(fileSize, remoteFileDescAdapter1.getSize());
        assertEquals(remoteFileDesc1, remoteFileDescAdapter1.getRfd());
        assertEquals(urn1, remoteFileDescAdapter1.getUrn());
        assertFalse(remoteFileDescAdapter1.isLicensed());
        assertFalse(remoteFileDescAdapter1.isSpam());

        context.assertIsSatisfied();
    }

    /**
     * Tests the getRelevance of the RemoteFileDescAdapter.
     */
    public void testGetRelevance() throws Exception {
        final Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};

        String query = "foo bar";
        RemoteFileDescAdapter a, b;

        // Consistency across executions
        a = createRFDAdapter(context, query, true, true, 10, true);
        b = createRFDAdapter(context, query, true, true, 10, true);
        assertEquals(a.getRelevance(query), b.getRelevance(query));

        // Consistency across same execution
        a = createRFDAdapter(context, query, true, true, 10, true);
        assertEquals(a.getRelevance(query), a.getRelevance(query));

        // An XMPP friend result should have greater relevance than a Gnutella one
        a = createRFDAdapter(context, query, true, true, 0, true);
        b = createRFDAdapter(context, query, false, true, 0, false);
        assertGreaterThan(a.getRelevance(query), b.getRelevance(query));
        a = createRFDAdapter(context, query, true, true, 0, false);
        b = createRFDAdapter(context, query, false, false, 0, false);
        assertGreaterThan(a.getRelevance(query), b.getRelevance(query));
        a = createRFDAdapter(context, query, true, true, 1, true);
        b = createRFDAdapter(context, query, false, false, 0, false);
        assertGreaterThan(a.getRelevance(query), b.getRelevance(query));

        // Altlocs increase relevance, (currently only one factored in though)
        a = createRFDAdapter(context, query, false, false, 0, true);
        b = createRFDAdapter(context, query, false, false, 1, false);
        assertGreaterThan(a.getRelevance(query), b.getRelevance(query));
        a = createRFDAdapter(context, query, true, true, 0, true);
        b = createRFDAdapter(context, query, true, true, 1, true);
        assertGreaterThan(a.getRelevance(query), b.getRelevance(query));

        // Browseable is better than non browsable
        a = createRFDAdapter(context, query, true, false, 1, true);
        b = createRFDAdapter(context, query, true, true, 0, true);
        assertGreaterThan(a.getRelevance(query), b.getRelevance(query));

        // Try with lots of altlocs, return is irrelevant.
        createRFDAdapter(context, query, true, true, 30, false).getRelevance(query);

        // Test altlocs are not factored into calculation after some point
        a = createRFDAdapter(context, query, true, true, 100, true);
        b = createRFDAdapter(context, query, true, true, 99, true);
        assertEquals(a.getRelevance(query), b.getRelevance(query));

        context.assertIsSatisfied();
    }
    
    private RemoteFileDescAdapter createRFDAdapter(final Mockery context,
            final String filename, final boolean anonymous,
            final boolean canBrowseHost, final int altlocs,
            final boolean firstAltLocConnectable) {

        final RemoteFileDesc rfd = context.mock(RemoteFileDesc.class);
        final CategoryManager categoryManager = context.mock(CategoryManager.class);
        final Set<IpPort> locs = new HashSet<IpPort>();

        final FriendPresence friendPresence;
        if(anonymous) {
            friendPresence = null;
        } else {
            friendPresence = context.mock(FriendPresence.class);
        }

        context.checking(new Expectations() {{
            // Fill the locs list with alternating IpPort and Connectable instances 
            for(int i = 0; i < altlocs; i++) {
                IpPort loc;
                if((i % 1 == 0) == firstAltLocConnectable) {
                    loc = context.mock(Connectable.class);
                } else {
                    loc = context.mock(IpPort.class);
                }
                InetAddress addr = context.mock(InetAddress.class);
                allowing(loc).getInetAddress();
                will(returnValue(addr));
                allowing(addr).getAddress();
                will(returnValue(new byte[]{24,101,1,(byte) (i % 255)}));
                allowing(addr).getHostAddress();
                will(returnValue("24.101.1." + (i % 255)));
                allowing(loc);
                locs.add(loc);
                allowing(addr);
            }

            allowing(rfd).getClientGUID();
            will(returnValue(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 }));

            if(!anonymous) {
                Friend friend = context.mock(Friend.class);
                allowing(friendPresence).getFriend();
                will(returnValue(friend));                 
                allowing(friend).isAnonymous();
                will(returnValue(false));
                allowing(friend);
            }

            allowing(rfd).isBrowseHostEnabled();
            will(returnValue(canBrowseHost));

            allowing(rfd).getRelevance(with(equal(filename)));
            will(returnValue(1f));

            allowing(rfd);
            
            allowing(categoryManager).getCategoryForExtension("");
            will(returnValue(Category.OTHER));
        }});

        if(anonymous) {
            return new RemoteFileDescAdapter(rfd, locs, categoryManager);
        } else {
            return new RemoteFileDescAdapter(rfd, locs, friendPresence, categoryManager);
        }
    }
   
    /**
     * Mostly internal based test for testing forward compatibility for 
     *  supporting relevances of new RemoteHost types.
     */
    public void testGetRelevanceWithAlternateHostTypes() {
        final Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};

        final RemoteFileDesc rfd = context.mock(RemoteFileDesc.class);
        final CategoryManager categoryManager = context.mock(CategoryManager.class);
        final Set<IpPort> locs = new HashSet<IpPort>();
        final String filename = "foo bar";

        context.checking(new Expectations() {{
            allowing(rfd).getClientGUID();
            will(returnValue(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 }));

            allowing(rfd).getRelevance(with(equal(filename)));
            will(returnValue(1f));

            allowing(rfd);
            allowing(categoryManager).getCategoryForExtension("");
            will(returnValue(Category.OTHER));
        }});

        RemoteFileDescAdapter rfdAdapter = new RemoteFileDescAdapter(rfd, locs, categoryManager) {
            @Override
            public List<RemoteHost> getSources() {
                List<RemoteHost> list = new LinkedList<RemoteHost>();

                // Host without relevance
                list.add(context.mock(RemoteHost.class));

                final RelevantRemoteHost host = context.mock(RelevantRemoteHost.class);

                context.checking(new Expectations() {{
                    allowing(host).getRelevance();
                    will(returnValue(99));
                }});

                // New host type with other relevance
                list.add(host);

                return list;
            }
        };

        assertEquals(99f, rfdAdapter.getRelevance(filename));

        context.assertIsSatisfied();
    }
    
    /**
     * Tests {@link RemoteFileDescAdapter#getSources()} under various loads.
     */
    public void testGetSources() {
        final Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
    
        final RemoteFileDesc rfd = context.mock(RemoteFileDesc.class);
        final CategoryManager categoryManager = context.mock(CategoryManager.class);
        final FriendPresence friendPresence = context.mock(FriendPresence.class);
        final Set<IpPort> locs = new HashSet<IpPort>();

        final Address address = context.mock(Address.class);
        final Connectable connectable = context.mock(Connectable.class);
        final IpPort ipPort = context.mock(IpPort.class);
        
        context.checking(new Expectations() {{
            allowing(rfd).getClientGUID();
            will(returnValue(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 }));
            
            allowing(friendPresence).getFeature(AddressFeature.ID);
            will(returnValue(new AddressFeature(address)));
            
            allowing(rfd).getAddress();
            will(returnValue(address));
            allowing(address);
            allowing(rfd);
            allowing(categoryManager).getCategoryForExtension("");
            will(returnValue(Category.OTHER));
            
            allowing(connectable);
            
            allowing(ipPort).getAddress();
            will(returnValue("hello"));
            allowing(ipPort);
            
        }});
        
        // Make sure the sources list contains a RemoteHost for the rfd and the single altloc
        //  ensure the address' are correctly set.  Use a Connectable for the altloc.        
        locs.add(connectable);
        RemoteFileDescAdapter anonRfdAdapter = new RemoteFileDescAdapter(rfd, locs, categoryManager);
        Iterable<RemoteHost> hosts1 = anonRfdAdapter.getSources();
        locs.remove(connectable);
        boolean rfdRemoteHostFound = false;
        boolean altLocFound = false;
        for ( RemoteHost host : hosts1 ) {
            if (host instanceof RfdRemoteHost) {
                assertSame(address, host.getFriendPresence().getFeature(AddressFeature.ID).getFeature());
                rfdRemoteHostFound = true;
            } 
            else if (host instanceof AltLocRemoteHost) {
                assertSame(connectable, host.getFriendPresence().getFeature(AddressFeature.ID).getFeature());
                altLocFound = true;
            }
        }
        assertTrue(rfdRemoteHostFound);
        assertTrue(altLocFound);

        // Make sure the sources list contains a RemoteHost for the friend and the single altloc
        //  ensure the address' are correctly set.  Use a regular IpPort instead of Connectable.
        locs.add(ipPort);
        RemoteFileDescAdapter friendRfdAdapter = new RemoteFileDescAdapter(rfd, locs, friendPresence, categoryManager);
        Iterable<RemoteHost> hosts2 = friendRfdAdapter.getSources();
        locs.remove(ipPort);
        rfdRemoteHostFound = false;
        altLocFound = false;
        for ( RemoteHost host : hosts2 ) {
            if (host instanceof RfdRemoteHost) {
                assertSame(address, host.getFriendPresence().getFeature(AddressFeature.ID).getFeature());
                rfdRemoteHostFound = true;
            } 
            else if (host instanceof AltLocRemoteHost) {
                Connectable addressCollected = (Connectable) host.getFriendPresence().getFeature(AddressFeature.ID).getFeature();
                assertSame("hello", addressCollected.getAddress());
                altLocFound = true;
            }
        }
        assertTrue(rfdRemoteHostFound);
        assertTrue(altLocFound);
        
        // Test duplicate getSources() call, ensure they are the same
        Iterable<RemoteHost> hosts2repeat = friendRfdAdapter.getSources();
        rfdRemoteHostFound = false;
        altLocFound = false;
        for ( RemoteHost host : hosts2repeat ) {
            if (host instanceof RfdRemoteHost) {
                assertSame(address, host.getFriendPresence().getFeature(AddressFeature.ID).getFeature());
                rfdRemoteHostFound = true;
            } 
            else if (host instanceof AltLocRemoteHost) {
                Connectable addressCollected = (Connectable) host.getFriendPresence().getFeature(AddressFeature.ID).getFeature();
                assertSame("hello", addressCollected.getAddress());
                altLocFound = true;
            }
        }
        assertTrue(rfdRemoteHostFound);
        assertTrue(altLocFound);
        
        context.assertIsSatisfied();
        
    }
    
    /**
     * Tests that the altlocs are preserved through construction.
     */
    public void testGetAltLocs() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
     
        RemoteFileDescAdapter rfdAdapter = createRFDAdapter(context, "", true, true, 16, true);
        
        List<IpPort> alts = rfdAdapter.getAlts();
        
        assertEquals(16, alts.size());
    }
    
    /**
     * Ensures the extension and the filename are properly retrieved from
     *  the rfd.
     */
    public void testGetFileNameInfo() {
        final Mockery context = new Mockery();
    
        final RemoteFileDesc rfd = context.mock(RemoteFileDesc.class);
        final CategoryManager categoryManager = context.mock(CategoryManager.class);
        final Set<IpPort> locs = new HashSet<IpPort>();

        context.checking(new Expectations() {{
            allowing(rfd).getClientGUID();
            will(returnValue(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 }));
            
            allowing(rfd).getFileName();
            will(returnValue("Giant Guitar.JPG"));
            allowing(categoryManager).getCategoryForExtension("JPG");
            will(returnValue(Category.IMAGE));
            allowing(rfd);
        }});
        
        RemoteFileDescAdapter rfdAdapter = new RemoteFileDescAdapter(rfd, locs, categoryManager);
        
        assertEquals("JPG", rfdAdapter.getFileExtension());
        assertEquals("Giant Guitar.JPG", rfdAdapter.getFileName());
        assertEquals(Category.IMAGE, rfdAdapter.getCategory());
        
        context.assertIsSatisfied();
    }
    
    /**
     * Tests {@link RemoteFileDescAdapter#isLicensed()} under various conditions.
     */
    public void testIsLicensed() {
        final Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final RemoteFileDesc rfdWithNull1 = context.mock(RemoteFileDesc.class);
        final RemoteFileDesc rfdWithNull2 = context.mock(RemoteFileDesc.class);
        final RemoteFileDesc rfdGood = context.mock(RemoteFileDesc.class);
        final CategoryManager categoryManager = context.mock(CategoryManager.class);
        final Set<IpPort> locs = new HashSet<IpPort>();

        context.checking(new Expectations() {{
            allowing(rfdWithNull1).getClientGUID();
            will(returnValue(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 }));
            allowing(rfdWithNull2).getClientGUID();
            will(returnValue(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 }));
            allowing(rfdGood).getClientGUID();
            will(returnValue(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 }));
            
            allowing(rfdWithNull1).getXMLDocument();
            will(returnValue(null));
            LimeXMLDocument document = context.mock(LimeXMLDocument.class);
            allowing(rfdWithNull2).getXMLDocument();
            will(returnValue(document));
            allowing(document).getLicenseString();
            will(returnValue(null));
            LimeXMLDocument document2 = context.mock(LimeXMLDocument.class);
            allowing(rfdGood).getXMLDocument();
            will(returnValue(document2));
            allowing(document2).getLicenseString();
            will(returnValue("GPL"));
            
            allowing(rfdWithNull1);
            allowing(rfdWithNull2);
            allowing(rfdGood);
            allowing(categoryManager).getCategoryForExtension("");
            will(returnValue(Category.OTHER));
        }});
        
        RemoteFileDescAdapter rfdAdapterWithNull1 = new RemoteFileDescAdapter(rfdWithNull1, locs, categoryManager);
        assertFalse(rfdAdapterWithNull1.isLicensed());
        
        RemoteFileDescAdapter rfdAdapterWithNull2 = new RemoteFileDescAdapter(rfdWithNull2, locs, categoryManager);
        assertFalse(rfdAdapterWithNull2.isLicensed());
        
        RemoteFileDescAdapter rfdAdapterGood = new RemoteFileDescAdapter(rfdGood, locs, categoryManager);
        assertTrue(rfdAdapterGood.isLicensed());
        
        context.assertIsSatisfied();
    }
    
    /**
     * Tests a few of the methods that delegate to the rfd and ensure they pass on the correct
     *  values.
     */
    public void testRfdDelegates() {
        Mockery context = new Mockery();
        
        final RemoteFileDesc rfd = context.mock(RemoteFileDesc.class);
        final CategoryManager categoryManager = context.mock(CategoryManager.class);
        final Set<IpPort> locs = new HashSet<IpPort>();

        context.checking(new Expectations() {{
            allowing(rfd).getClientGUID();
            will(returnValue(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 }));
            
            allowing(rfd).getSize();
            will(returnValue(Long.MAX_VALUE-3));          
            
            one(rfd).isSpam();
            will(returnValue(true));
            one(rfd).isSpam();
            will(returnValue(false));
            
            allowing(rfd);
            allowing(categoryManager).getCategoryForExtension("");
            will(returnValue(Category.OTHER));
        }});
        
        RemoteFileDescAdapter rfdAdapter = new RemoteFileDescAdapter(rfd, locs, categoryManager);
        
        assertSame(rfd, rfdAdapter.getRfd());
        assertEquals(Long.MAX_VALUE-3, rfdAdapter.getSize());
        
        assertTrue(rfdAdapter.isSpam());
        assertFalse(rfdAdapter.isSpam());
        
        context.assertIsSatisfied();
    }
    
    /**
     * Tests {@link RemoteFileDescAdapter#getUrn()} and ensures it passes on the URN included with the RFD.
     */
    public void testGetUrn() throws IOException {
        Mockery context = new Mockery();
        
        final RemoteFileDesc rfd = context.mock(RemoteFileDesc.class);
        final CategoryManager categoryManager = context.mock(CategoryManager.class);
        final Set<IpPort> locs = new HashSet<IpPort>();

        final URN urn = URN.createSHA1Urn("urn:sha1:XXSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        
        context.checking(new Expectations() {{
            allowing(rfd).getClientGUID();
            will(returnValue(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 }));
            
            allowing(rfd).getSHA1Urn();
            will(returnValue(urn));
            
            allowing(rfd);
            allowing(categoryManager).getCategoryForExtension("");
            will(returnValue(Category.OTHER));
        }});
        
        RemoteFileDescAdapter rfdAdapter = new RemoteFileDescAdapter(rfd, locs, categoryManager);
        
        assertEquals(urn, rfdAdapter.getUrn());
        
        context.assertIsSatisfied();
    }
    
    /**
     * Tests {@link RemoteFileDescAdapter#getUrn()} and ensures it passes on the URN included with the RFD.
     * @throws IOException 
     */
    public void testGetMagnetUrl() {
        Mockery context = new Mockery();
        
        final RemoteFileDesc rfd = context.mock(RemoteFileDesc.class);
        final CategoryManager categoryManager = context.mock(CategoryManager.class);
        final Set<IpPort> locs = new HashSet<IpPort>();
        context.checking(new Expectations() {{
            allowing(rfd).getClientGUID();
            will(returnValue(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 }));
            
            allowing(rfd);
            allowing(categoryManager).getCategoryForExtension("");
            will(returnValue(Category.OTHER));
        }});
        
        RemoteFileDescAdapter rfdAdapter = new RemoteFileDescAdapter(rfd, locs, categoryManager);
        
        assertNotNull(rfdAdapter.getMagnetURL());
        assertGreaterThan(0, rfdAdapter.getMagnetURL().length());
        assertTrue(rfdAdapter.getMagnetURL().startsWith("magnet"));
        
        context.assertIsSatisfied();
    }
    
    public void testToString() {
        Mockery context = new Mockery();
        
        final RemoteFileDesc rfd = context.mock(RemoteFileDesc.class);
        final CategoryManager categoryManager = context.mock(CategoryManager.class);
        final Set<IpPort> locs = new HashSet<IpPort>();
        context.checking(new Expectations() {{
            allowing(rfd).getClientGUID();
            will(returnValue(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 }));
            
            allowing(rfd);
            allowing(categoryManager).getCategoryForExtension("");
            will(returnValue(Category.OTHER));
        }});
        
        RemoteFileDescAdapter rfdAdapter1 = new RemoteFileDescAdapter(rfd, locs, categoryManager);
        
        assertNotNull(rfdAdapter1.toString());
    }
    
    /**
     * Tests the functionality of {@link RfdRemoteHost}.
     */
    public void testRfdRemoteHost() {
        final Mockery context = new Mockery();
        
        final RemoteFileDesc rfd = context.mock(RemoteFileDesc.class);
        final FriendPresence friendPresence = context.mock(FriendPresence.class);
        final Friend friend = context.mock(Friend.class);
        
        context.checking(new Expectations() {{           
            exactly(3).of(friendPresence).getFriend();
            will(returnValue(friend));
            exactly(3).of(friend).isAnonymous();
            will(returnValue(true));
            exactly(1).of(rfd).isBrowseHostEnabled();
            will(returnValue(true));
            
            allowing(rfd);
        }});
        
        RemoteHost remoteHost = new RfdRemoteHost(friendPresence, rfd);
                
        assertSame(friendPresence, remoteHost.getFriendPresence());
        assertTrue(remoteHost.isBrowseHostEnabled());
        assertFalse(remoteHost.isChatEnabled());
        assertFalse(remoteHost.isSharingEnabled());
        
        context.assertIsSatisfied();
    }
    
    /**
     * Tests the class {@link AltLocRemoteHost} and ensures it can be constructed, 
     *  accessed, and that the returns are sane.
     */
    public void testAltLocRemoteHost() throws UnknownHostException {
        
        IpPort ipPort = new ConnectableImpl("akzp.com", 55, false);
        
        RemoteHost remoteHost = new AltLocRemoteHost(ipPort);
        
        // The outcome of these functions is never determinate for an AltLoc
        //  so it is a matter of app behaviour preference what they return
        //  therefore ignore the result but make sure they do not cause
        //  exceptions.
        remoteHost.isBrowseHostEnabled();
        remoteHost.isChatEnabled();
        remoteHost.isSharingEnabled();
       
        assertNotNull(remoteHost.getFriendPresence());
        assertEquals(55, 
           ((Connectable)remoteHost.getFriendPresence().getFeature(AddressFeature.ID).getFeature()).getPort());
    }
}
