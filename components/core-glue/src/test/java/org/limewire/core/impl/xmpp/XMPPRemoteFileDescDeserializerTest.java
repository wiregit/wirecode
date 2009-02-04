package org.limewire.core.impl.xmpp;

import java.util.HashSet;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.io.Address;
import org.limewire.net.address.AddressFactory;
import org.limewire.util.BaseTestCase;
import org.limewire.xmpp.api.client.XMPPAddress;
import org.limewire.xmpp.client.impl.XMPPAddressResolver;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.downloader.RemoteFileDescImpl;
import com.limegroup.gnutella.xml.LimeXMLDocument;

public class XMPPRemoteFileDescDeserializerTest extends BaseTestCase {
    
    public XMPPRemoteFileDescDeserializerTest(String name) {
        super(name);
    }
    
    public void testRegester() {
        Mockery context = new Mockery();

        final RemoteFileDescFactory remoteFileDescFactory = context.mock(RemoteFileDescFactory.class);
        
        final XMPPRemoteFileDescDeserializer deserialiser
            = new XMPPRemoteFileDescDeserializer(null, null);
        
        context.checking(new Expectations() {
            {   exactly(1).of(remoteFileDescFactory).register(with(any(String.class)), with(same(deserialiser)));
            }});
        
        deserialiser.register(remoteFileDescFactory);
        
        context.assertIsSatisfied();
        
    }
    
    public void testCreateClone() {
        Mockery context = new Mockery() {
            {   setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        
        final AddressFactory addressFactory = context.mock(AddressFactory.class);
        final XMPPAddressResolver addressResolver = context.mock(XMPPAddressResolver.class);
        
        final XMPPRemoteFileDescDeserializer deserialiser
            = new XMPPRemoteFileDescDeserializer(addressFactory, addressResolver);
        
        final Address addressOrig = context.mock(Address.class);;
        final long index = 10;
        final String fileName = "file.txt";
        final long size = 12;
        final byte[] clientGUID = new byte[] {'h','e', 'y'};
        final int speed = 20;
        final int quality = 9;
        final boolean browseHost = true;
        final LimeXMLDocument xmlDoc = context.mock(LimeXMLDocument.class);
        final Set<? extends URN> urns = new HashSet<URN>();
        final boolean replyToMuticast = false;
        final String vendor = "EGG";
        final long createTime = 13423;
        final boolean http11 = true;
        final XMPPAddress addressNew = context.mock(XMPPAddress.class);
                
        context.checking(new Expectations() {
            {  // Nothing internal to the mocks should be touched.
            }});
        
        RemoteFileDesc rfdOrig = new RemoteFileDescImpl(addressOrig, index, fileName, size,
                clientGUID, speed, quality, browseHost ,
                xmlDoc, urns, replyToMuticast, vendor, 
                createTime, http11, addressFactory);
                
        RemoteFileDesc rfdNew = deserialiser.createClone(rfdOrig, addressNew);
        
        assertEquals(addressNew, rfdNew.getAddress());
        assertEquals(index, rfdNew.getIndex());
        assertEquals(fileName, rfdNew.getFileName());
        assertEquals(size, rfdNew.getSize());
        assertEquals(clientGUID, rfdNew.getClientGUID());
        assertEquals(speed, rfdNew.getSpeed());
        assertEquals(quality, rfdNew.getQuality());;
        assertEquals(browseHost, rfdNew.isBrowseHostEnabled());
        assertEquals(xmlDoc, rfdNew.getXMLDocument());
        assertEquals(urns, rfdNew.getUrns());
        assertEquals(vendor, rfdNew.getVendor());
        assertEquals(createTime, rfdNew.getCreationTime());
        assertEquals(http11, rfdNew.isHTTP11());
                
        context.assertIsSatisfied();
    }
    
}
