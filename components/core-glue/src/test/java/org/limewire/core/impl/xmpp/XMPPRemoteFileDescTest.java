package org.limewire.core.impl.xmpp;

import java.io.IOException;
import java.util.HashSet;

import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.net.address.AddressFactory;
import org.limewire.util.BaseTestCase;
import org.limewire.xmpp.api.client.XMPPAddress;
import org.limewire.xmpp.client.impl.XMPPAddressResolver;

import com.limegroup.gnutella.URN;

public class XMPPRemoteFileDescTest extends BaseTestCase{
    
    public XMPPRemoteFileDescTest(String name) {
        super(name);
    }

    /**
     * Tests the various conditions of XMPPRemoteFileDesc.equals()
     */
    public void testEquals() {

        Mockery context = new Mockery() {
            {   setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        
        XMPPAddress address = context.mock(XMPPAddress.class);
        XMPPAddress otherAddress = context.mock(XMPPAddress.class);
        
        XMPPRemoteFileDesc rfd = createRFD(address, null, null);
        XMPPRemoteFileDesc otherRfd = context.mock(XMPPRemoteFileDesc.class);
        
        assertEquals(rfd, rfd);
        assertNotEquals(rfd, new Integer(-5));
        assertNotEquals(rfd, otherRfd);
        // address not equal
        assertNotEquals(rfd, createRFD(otherAddress, null, null));
        // client guid not equal
        assertNotEquals(rfd, createRFD2(address, null, null));
        // size not equal
        assertNotEquals(rfd, createRFD3(address, null, null));
        // URNs not equal
        assertNotEquals(rfd, createRFD4(address, null, null));
        // filename not equal - should this be true?
        assertEquals(rfd, createRFD5(address, null, null));
    }
    
    /**
     * Tests the hash function for uniqueness and consistency 
     */
    public void testHashCode() {
        
        Mockery context = new Mockery() {
            {   setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        
        XMPPAddress sharedAddress = context.mock(XMPPAddress.class);
        XMPPAddress otherAddress = context.mock(XMPPAddress.class);
        
        XMPPRemoteFileDesc rfdA = createRFDwithGUID1(sharedAddress, null, null);
        XMPPRemoteFileDesc rfdB = createRFDwithGUID1(sharedAddress, null, null);
        XMPPRemoteFileDesc rfdC = createRFDwithGUID1(otherAddress, null, null);
        XMPPRemoteFileDesc rfdD = createRFDwithGUID2(sharedAddress, null, null);
        
        assertEquals(rfdA.hashCode(), rfdA.hashCode());
        assertEquals(rfdA.hashCode(), rfdB.hashCode());
        assertNotEquals(rfdA.hashCode(), rfdC.hashCode());
        assertNotEquals(rfdA.hashCode(), rfdD.hashCode());
        
    }
    
    /*
    public void testGetCredentials() {
        
    }
    
    public void testGetUrlPath() {
        
    }
    
    public void testToMemento() {
        
    }
    
    public void testIsMe() {
        
    }
    
    public void testGetSHA1Urn() {
        
    }
    */
    
    public XMPPRemoteFileDesc createRFD(XMPPAddress address, AddressFactory addressFactory, XMPPAddressResolver addressResolver) {
        return new XMPPRemoteFileDesc(address, Long.MAX_VALUE, null, Long.MAX_VALUE, new byte[] {'x'}, Integer.MIN_VALUE, Integer.MAX_VALUE, null,
                new HashSet<URN>(), null, Long.MIN_VALUE, false, addressFactory, addressResolver);
    }
    
    public XMPPRemoteFileDesc createRFD2(XMPPAddress address, AddressFactory addressFactory, XMPPAddressResolver addressResolver) {
        return new XMPPRemoteFileDesc(address, Long.MAX_VALUE, null, Long.MAX_VALUE, new byte[] {'y'}, Integer.MIN_VALUE, Integer.MAX_VALUE, null,
                new HashSet<URN>(), null, Long.MIN_VALUE, false, addressFactory, addressResolver);
    }
    
    public XMPPRemoteFileDesc createRFD3(XMPPAddress address, AddressFactory addressFactory, XMPPAddressResolver addressResolver) {
        return new XMPPRemoteFileDesc(address, Long.MAX_VALUE, null, -1, new byte[] {'x'}, Integer.MIN_VALUE, Integer.MAX_VALUE, null,
                new HashSet<URN>(), null, Long.MIN_VALUE, false, addressFactory, addressResolver);
    }
    
    public XMPPRemoteFileDesc createRFD4(XMPPAddress address, AddressFactory addressFactory, XMPPAddressResolver addressResolver) {
        
        HashSet<URN> urns = new HashSet<URN>();
        try {
            urns.add(URN.createUrnFromString("urn:sha1:NETZHKEJKTCM74ZQQALJWSLWQHQJ7N6Q"));
        } catch (IOException e) {
        }
        
        return new XMPPRemoteFileDesc(address, Long.MAX_VALUE, null, Long.MAX_VALUE, new byte[] {'x'}, Integer.MIN_VALUE, Integer.MAX_VALUE, null,
                urns, null, Long.MIN_VALUE, false, addressFactory, addressResolver);
    }
    
    public XMPPRemoteFileDesc createRFD5(XMPPAddress address, AddressFactory addressFactory, XMPPAddressResolver addressResolver) {
        return new XMPPRemoteFileDesc(address, Long.MAX_VALUE, "huh", Long.MAX_VALUE, new byte[] {'x'}, Integer.MIN_VALUE, Integer.MAX_VALUE, null,
                new HashSet<URN>(), null, Long.MIN_VALUE, false, addressFactory, addressResolver);
    }
    
    public XMPPRemoteFileDesc createRFDwithGUID1(XMPPAddress address, AddressFactory addressFactory, XMPPAddressResolver addressResolver) {
        return new XMPPRemoteFileDesc(address, Long.MAX_VALUE, null, Long.MAX_VALUE, 
                new byte[] {'0', '1', '2', '3', '4', 5, 6, 7, 8, 9,10, 11,12,13,14,15}, 
                Integer.MIN_VALUE, Integer.MAX_VALUE, null,
                new HashSet<URN>(), null, Long.MIN_VALUE, false, addressFactory, addressResolver);
    }
    
    public XMPPRemoteFileDesc createRFDwithGUID2(XMPPAddress address, AddressFactory addressFactory, XMPPAddressResolver addressResolver) {
        return new XMPPRemoteFileDesc(address, Long.MAX_VALUE, null, Long.MAX_VALUE, 
                new byte[] {'x', '1', '2', '3', '4', 5, 6, 7, 8, 9,10, 11,12,13,14,15}, 
                Integer.MIN_VALUE, Integer.MAX_VALUE, null,
                new HashSet<URN>(), null, Long.MIN_VALUE, false, addressFactory, addressResolver);
    }
    
}
