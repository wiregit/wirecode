package org.limewire.xmpp.client.impl.messages.address;

import java.io.IOException;

import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.packet.IQ;
import org.limewire.net.address.AddressFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class AddressIQProvider implements IQProvider {
        
    AddressFactory factory;

    public AddressIQProvider(AddressFactory factory){
        this.factory = factory;
    }

    public IQ parseIQ(XmlPullParser parser) throws Exception {
        try {
            return new AddressIQ(parser, factory);
        } catch (IOException ie) {
            return new NullAddressIQ(ie); 
        } catch (XmlPullParserException xmpe) {
            return new NullAddressIQ(xmpe);
        }
    }
    
    public static class NullAddressIQ extends AddressIQ {

        private final Exception exception;

        public NullAddressIQ(Exception exception) {
            this.exception = exception;
        }
        
        public Exception getException() {
            return exception;
        }
        
    }
}
