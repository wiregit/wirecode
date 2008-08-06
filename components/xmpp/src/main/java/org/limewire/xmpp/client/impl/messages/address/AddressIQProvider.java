package org.limewire.xmpp.client.impl.messages.address;

import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.packet.IQ;
import org.limewire.net.address.AddressFactory;
import org.xmlpull.v1.XmlPullParser;

public class AddressIQProvider implements IQProvider {
        
    AddressFactory factory;

    public AddressIQProvider(AddressFactory factory){
        this.factory = factory;
    }

    public IQ parseIQ(XmlPullParser parser) throws Exception {                     
        return new AddressIQ(parser, factory);
    }
}
