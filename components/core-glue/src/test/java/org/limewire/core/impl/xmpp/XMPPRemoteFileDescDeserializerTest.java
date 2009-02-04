package org.limewire.core.impl.xmpp;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.net.address.AddressFactory;
import org.limewire.util.BaseTestCase;
import org.limewire.xmpp.client.impl.XMPPAddressResolver;

import com.limegroup.gnutella.downloader.RemoteFileDescFactory;

public class XMPPRemoteFileDescDeserializerTest extends BaseTestCase {
    
    public XMPPRemoteFileDescDeserializerTest(String name) {
        super(name);
    }
    
    public void testRegester() {
        Mockery context = new Mockery() {
            {   setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        
        final AddressFactory addressFactory = context.mock(AddressFactory.class);
        final XMPPAddressResolver addressResolver = context.mock(XMPPAddressResolver.class);
        
        final RemoteFileDescFactory remoteFileDescFactory = context.mock(RemoteFileDescFactory.class);
        
        final XMPPRemoteFileDescDeserializer deserialiser
            = new XMPPRemoteFileDescDeserializer(addressFactory, addressResolver);
        
        context.checking(new Expectations() {
            {
                exactly(1).of(remoteFileDescFactory).register(with(any(String.class)), with(same(deserialiser)));
                
            }});
        
        deserialiser.register(remoteFileDescFactory);
        
        context.assertIsSatisfied();
        
    }
    
}
