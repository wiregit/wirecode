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

import com.limegroup.gnutella.Constants;
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
    
    /**
     * Tests the clone method with some random inputs
     */
    public void testCreateCloneSimple() {
        
        Mockery context = new Mockery() {
            {   setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        
        final Address addressOrig = context.mock(Address.class);;
        final long index = 10;
        final String fileName = "file.txt";
        final long size = 12;
        final byte[] clientGUID = new byte[] {'h','e', 'y'};
        final int speed = 20;
        final int quality = 9;
        final boolean browseHost = true;
        final LimeXMLDocument xmlDoc = context.mock(LimeXMLDocument.class);
        final Set<URN> urns = new HashSet<URN>();
        final boolean replyToMuticast = true;
        final String vendor = "EGG";
        final long createTime = 13423;
        final boolean http11 = true;
        final XMPPAddress addressNew = context.mock(XMPPAddress.class);
        
        testCreateClone(context, addressOrig, index, fileName, size, clientGUID, speed,
                quality, browseHost, xmlDoc, urns, replyToMuticast, vendor, createTime, http11, addressNew);
    }
    
    /**
     * Tests the clone method with empties, nulls, 0s,
     *  negatives, and falses where applicable.
     *  
     *  
     *  ERROR?  
     *   The http11 param is being lost in the clone
     */
    public void testCreateCloneMinBound() {
        
        Mockery context = new Mockery() {
            {   setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        
        final Address addressOrig = context.mock(Address.class);
        final long index = 0;
        final String fileName = "A";
        final long size = 0;
        final byte[] clientGUID = new byte[] {};
        final int speed = 0;
        final int quality = -32;
        final boolean browseHost = false;
        final LimeXMLDocument xmlDoc = null;
        final Set<? extends URN> urns = new HashSet<URN>();
        final boolean replyToMuticast = false;
        final String vendor = null;
        final long createTime = -132;
        final boolean http11 = false;
        final XMPPAddress addressNew = null;
        
        testCreateClone(context, addressOrig, index, fileName, size, clientGUID, speed,
                quality, browseHost, xmlDoc, urns, replyToMuticast, vendor, createTime, http11, addressNew);
    }
    
    public void testCreateCloneMaxBound() {
        
        Mockery context = new Mockery() {
            {   setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        
        final Address addressOrig = context.mock(XMPPAddress.class);
        final long index = 0xFFFFFFF;
        final String fileName = "Aaaaaaaaasdasfffffffffffffffffffffffffffoiwequrfweiorwe";
        final long size = Constants.MAX_FILE_SIZE;
        final byte[] clientGUID = new byte[] {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1};
        final int speed = Integer.MAX_VALUE;
        final int quality = Integer.MAX_VALUE;
        final boolean browseHost = true;
        final LimeXMLDocument xmlDoc = null;
        final Set<URN> urns = new HashSet<URN>();
        final boolean replyToMuticast = true;
        final String vendor = "sadasjhdsakhfesndvcbjurfgmxcnm,xvnsdkjfhsdjkfnsdjfn";
        final long createTime = Long.MAX_VALUE;
        final boolean http11 = true;
        final XMPPAddress addressNew = context.mock(XMPPAddress.class);
        
        testCreateClone(context, addressOrig, index, fileName, size, clientGUID, speed,
                quality, browseHost, xmlDoc, urns, replyToMuticast, vendor, createTime, http11, addressNew);
    }
    
    private void testCreateClone(Mockery context,
            final Address addressOrig,
            final long index,
            final String fileName,
            final long size,
            final byte[] clientGUID,
            final int speed,
            final int quality,
            final boolean browseHost,
            final LimeXMLDocument xmlDoc,
            final Set<? extends URN> urns,
            final boolean replyToMuticast,
            final String vendor,
            final long createTime,
            final boolean http11,
            final XMPPAddress addressNew) {
        
        final AddressFactory addressFactory = context.mock(AddressFactory.class);
        final XMPPAddressResolver addressResolver = context.mock(XMPPAddressResolver.class);
        
        final XMPPRemoteFileDescDeserializer deserialiser
            = new XMPPRemoteFileDescDeserializer(addressFactory, addressResolver);
                        
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
        assertEquals(browseHost, rfdNew.isBrowseHostEnabled()); // This is failing when browsehost == false
        assertEquals(xmlDoc, rfdNew.getXMLDocument());
        assertEquals(urns, rfdNew.getUrns());
        assertEquals(vendor, rfdNew.getVendor());
        assertEquals(createTime, rfdNew.getCreationTime());
        assertEquals(http11, rfdNew.isHTTP11()); // This is failing with http11 == true
                
        context.assertIsSatisfied();
    }
    
}
