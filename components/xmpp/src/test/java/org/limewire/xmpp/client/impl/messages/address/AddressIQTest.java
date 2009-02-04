package org.limewire.xmpp.client.impl.messages.address;

import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.net.address.AddressFactory;
import org.limewire.net.address.AddressFactoryImpl;
import org.limewire.net.address.ConnectableSerializer;
import org.limewire.util.BaseTestCase;
import org.limewire.xmpp.client.impl.messages.IQTestUtils;
import org.limewire.xmpp.client.impl.messages.InvalidIQException;

public class AddressIQTest extends BaseTestCase {

    private AddressFactory addressFactory;
    private ConnectableSerializer serializer;
    
    public AddressIQTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        addressFactory = new AddressFactoryImpl();
        serializer = new ConnectableSerializer();
        addressFactory.registerSerializer(serializer);
    }
    
    public void testParsesOwnOutput() throws Exception {
        Connectable connectable = new ConnectableImpl("129.0.0.1", 4545, true);
        AddressIQ addressIQ = new AddressIQ(connectable, addressFactory);
        
        AddressIQ parsedAddressIQ = new AddressIQ(IQTestUtils.createParser(addressIQ.getChildElementXML()), addressFactory);
        assertEquals(connectable, parsedAddressIQ.getAddress());        
    }
    
    public void testParsesMissingAddressElementGracefully() throws Exception {
        try {
            new AddressIQ(IQTestUtils.createParser("<address xmlns=\"jabber:iq:lw-address\"></address>"), addressFactory);
            fail("invalid iq expected");
        } catch (InvalidIQException iie) {
        }
    }
    
    public void testParsesMissingValueAttributeGracefully() throws Exception {
        try {
            new AddressIQ(IQTestUtils.createParser("<address xmlns=\"jabber:iq:lw-address\"><direct-connect/></address>"), addressFactory);
            fail("invalid iq expected");
        } catch (InvalidIQException iie) {
        }
    }
    
    public void testParsesUnknownAddressTypeGracefully() throws Exception {
        try {
            new AddressIQ(IQTestUtils.createParser("<address xmlns=\"jabber:iq:lw-address\"><street-address value=\"abc\"/></address>"), addressFactory);
            fail("invalid iq expected");
        } catch (InvalidIQException iie) {
        }
    }
    
    public void testParsesMismatchingClosingElementGracefully() throws Exception {
        try {
            new AddressIQ(IQTestUtils.createParser("<address xmlns=\"jabber:iq:lw-address\"><street-address value=\"abc\"/></different-address>"), addressFactory);
            fail("invalid iq expected");
        } catch (InvalidIQException iie) {
        }
    }
}
