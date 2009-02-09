package org.limewire.core.impl.xmpp;

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

    public void testEquals() {

        Mockery context = new Mockery() {
            {   setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        
        XMPPAddress address = context.mock(XMPPAddress.class);
        
        XMPPRemoteFileDesc rfd = createRFD(address, null, null);
        XMPPRemoteFileDesc otherRfd = context.mock(XMPPRemoteFileDesc.class);
        
        assertTrue(rfd.equals(rfd));
        assertFalse(rfd.equals(new Integer(-5)));
        assertFalse(rfd.equals(otherRfd));
        // address not equal
        assertFalse(rfd.equals(createRFD(context, null, null)));
        // client guid not equal
        assertFalse(rfd.equals(createRFD2(address, null, null)));
        // size not equal
        assertFalse(rfd.equals(createRFD3(address, null, null)));
        // URNs not equal -- this should be false, test failing
        assertFalse(rfd.equals(createRFD4(address, null, null)));
        // filename not equal - should this be true?
        assertTrue(rfd.equals(createRFD5(address, null, null)));
    }
    
    public void testHashCode() {
        
    }
    
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
    
    public XMPPRemoteFileDesc createRFD(XMPPAddress address, AddressFactory addressFactory, XMPPAddressResolver addressResolver) {
        return new XMPPRemoteFileDesc(address, Long.MAX_VALUE, null, Long.MAX_VALUE, new byte[] {}, Integer.MIN_VALUE, Integer.MAX_VALUE, null,
                new HashSet<URN>(), null, Long.MIN_VALUE, false, addressFactory, addressResolver);
    }
    
    public XMPPRemoteFileDesc createRFD2(XMPPAddress address, AddressFactory addressFactory, XMPPAddressResolver addressResolver) {
        return new XMPPRemoteFileDesc(address, Long.MAX_VALUE, null, Long.MAX_VALUE, new byte[] {'x'}, Integer.MIN_VALUE, Integer.MAX_VALUE, null,
                new HashSet<URN>(), null, Long.MIN_VALUE, false, addressFactory, addressResolver);
    }
    
    public XMPPRemoteFileDesc createRFD3(XMPPAddress address, AddressFactory addressFactory, XMPPAddressResolver addressResolver) {
        return new XMPPRemoteFileDesc(address, Long.MAX_VALUE, null, -1, new byte[] {}, Integer.MIN_VALUE, Integer.MAX_VALUE, null,
                new HashSet<URN>(), null, Long.MIN_VALUE, false, addressFactory, addressResolver);
    }
    
    public XMPPRemoteFileDesc createRFD4(XMPPAddress address, AddressFactory addressFactory, XMPPAddressResolver addressResolver) {
        return new XMPPRemoteFileDesc(address, Long.MAX_VALUE, null, Long.MAX_VALUE, new byte[] {}, Integer.MIN_VALUE, Integer.MAX_VALUE, null,
                new HashSet<URN>('x'), null, Long.MIN_VALUE, false, addressFactory, addressResolver);
    }
    
    public XMPPRemoteFileDesc createRFD5(XMPPAddress address, AddressFactory addressFactory, XMPPAddressResolver addressResolver) {
        return new XMPPRemoteFileDesc(address, Long.MAX_VALUE, "huh", Long.MAX_VALUE, new byte[] {}, Integer.MIN_VALUE, Integer.MAX_VALUE, null,
                new HashSet<URN>(), null, Long.MIN_VALUE, false, addressFactory, addressResolver);
    }
    
    public XMPPRemoteFileDesc createRFD(Mockery context, AddressFactory addressFactory, XMPPAddressResolver addressResolver) {
        return new XMPPRemoteFileDesc(context.mock(XMPPAddress.class), Long.MAX_VALUE, null, Long.MAX_VALUE, new byte[] {}, Integer.MIN_VALUE, Integer.MAX_VALUE, null,
                new HashSet<URN>(), null, Long.MIN_VALUE, false, addressFactory, addressResolver);
    }
    
}
