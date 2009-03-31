package org.limewire.core.impl.search;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.Category;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.impl.URNImpl;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.IpPort;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;

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
        final Set<IpPort> ipPorts = new HashSet<IpPort>();
        final Address address1 = context.mock(Address.class);
        final byte[] guid1 = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
        final String fileName1 = "remote file name 1.txt";
        final long fileSize = 1234L;
        final URN urn1 = URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA1");

        context.checking(new Expectations() {
            {
                allowing(remoteFileDesc1).getAddress();
                will(returnValue(address1));
                allowing(remoteFileDesc1).getClientGUID();
                will(returnValue(guid1));
                allowing(address1).getAddressDescription();
                will(returnValue("address 1 description"));
                allowing(remoteFileDesc1).getFileName();

                will(returnValue(fileName1));
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
            }
        });

        RemoteFileDescAdapter remoteFileDescAdapter1 = new RemoteFileDescAdapter(remoteFileDesc1,
                ipPorts);

        assertEquals(Category.DOCUMENT, remoteFileDescAdapter1.getCategory());
        assertEquals("txt", remoteFileDescAdapter1.getFileExtension());
        assertEquals(fileName1, remoteFileDescAdapter1.getFileName());
        assertEquals(fileSize, remoteFileDescAdapter1.getSize());
        assertEquals(remoteFileDesc1, remoteFileDescAdapter1.getRfd());
        assertEquals(new URNImpl(urn1), remoteFileDescAdapter1.getUrn());
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
        
        // Consistency across executions
        assertEquals(createRFDAdapter(context, true, true, 10, true).getRelevance(), 
                createRFDAdapter(context, true, true, 10, true).getRelevance());
        
        // An XMPP friend result should have greater relevance than a Gnutella one
        assertGreaterThan(createRFDAdapter(context, true, true, 0, true).getRelevance(), 
                createRFDAdapter(context, false, true, 0, false).getRelevance());
        assertGreaterThan(createRFDAdapter(context, true, true, 0, false).getRelevance(), 
                createRFDAdapter(context, false, false, 0, false).getRelevance());
        assertGreaterThan(createRFDAdapter(context, true, true, 1, true).getRelevance(), 
                createRFDAdapter(context, false, false, 0, false).getRelevance());
        
        // Altlocs increase relevance, (currently only one factored in though)
        assertGreaterThan(createRFDAdapter(context, false, false, 0, true).getRelevance(), 
                createRFDAdapter(context, false, false, 1, false).getRelevance());
        assertGreaterThan(createRFDAdapter(context, true, true, 0, true).getRelevance(), 
                createRFDAdapter(context, true, true, 1, true).getRelevance());
        
        // Browseable is better than non browsable
        assertGreaterThan(createRFDAdapter(context, true, false, 1, true).getRelevance(), 
                createRFDAdapter(context, true, true, 0, true).getRelevance());
        
        // Try with lots of altlocs, return is irrelevant.
        createRFDAdapter(context, true, true, 30, false).getRelevance();

        // Test altlocs are not factored into calculation after some point
        assertEquals(createRFDAdapter(context, true, true, 100, true).getRelevance(), 
                createRFDAdapter(context, true, true, 99, true).getRelevance());        
        
        context.assertIsSatisfied();
    }
    
    private RemoteFileDescAdapter createRFDAdapter(final Mockery context, final boolean anonymous, 
            final boolean canBrowseHost, final int altlocs, final boolean firstAltLocConnectable) {
        
        final RemoteFileDesc rfd = context.mock(RemoteFileDesc.class);
        final Set<IpPort> locs = new HashSet<IpPort>();

        final FriendPresence friendPresence;
        if (anonymous) {
            friendPresence = null;
        } else {
            friendPresence = context.mock(FriendPresence.class);
        }

        context.checking(new Expectations() {
            {

                // Fill the locs list with alternating IpPort and Connectable instances 
                for ( int i=0 ; i<altlocs ; i++ ) {
                    IpPort loc;
                    if ((i%1 == 0) == firstAltLocConnectable) {
                        loc = context.mock(Connectable.class);
                    } 
                    else {
                        loc = context.mock(IpPort.class);
                    }
                    InetAddress addr = context.mock(InetAddress.class);
                    allowing(loc).getInetAddress();
                    will(returnValue(addr));
                    allowing(addr).getAddress();
                    will(returnValue(new byte[]{24,101,1,(byte) (i % 255)}));
                    allowing(loc);
                    locs.add(loc);
                }

                
                allowing(rfd).getClientGUID();
                will(returnValue(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 }));

                if (!anonymous) {
                    Friend friend = context.mock(Friend.class);
                    allowing(friendPresence).getFriend();
                    will(returnValue(friend));                 
                    allowing(friend).isAnonymous();
                    will(returnValue(false));
                    allowing(friend);
                }
                
                allowing(rfd).isBrowseHostEnabled();
                will(returnValue(canBrowseHost));
                
                allowing(rfd);
            }
        });
        
        RemoteFileDescAdapter rfdAdapter;
        
        if (!anonymous) {
            rfdAdapter = new RemoteFileDescAdapter(rfd,
                locs, friendPresence);
        } 
        else {
            rfdAdapter = new RemoteFileDescAdapter(rfd,
                    locs);
        }
        
        return rfdAdapter;
    }
}
