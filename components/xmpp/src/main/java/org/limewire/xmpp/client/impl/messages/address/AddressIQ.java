package org.limewire.xmpp.client.impl.messages.address;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.binary.Base64;
import org.jivesoftware.smack.packet.IQ;
import org.limewire.net.address.Address;
import org.limewire.net.address.AddressFactory;
import org.limewire.net.address.AddressSerializer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class AddressIQ extends IQ {
    private Address address;
    private AddressFactory factory;

    public AddressIQ(XmlPullParser parser, AddressFactory factory) throws IOException, XmlPullParserException {
        this.factory = factory;
        do {
            int eventType = parser.getEventType();
            if(eventType == XmlPullParser.START_TAG) {
                if(parser.getName().equals("address")) {
                    if(!parser.isEmptyElementTag()) {
                        eventType = parser.next();
                        if(eventType == XmlPullParser.START_TAG) {
                            String type = parser.getName();
                            String value = parser.getAttributeValue(null, "value");
                            address = factory.deserialize(type,  Base64.decodeBase64(value.getBytes("UTF-8")));
                            return;
                        } else {
                            return;
                        }
                    } else {
                        return;
                    }
                }
            } else if(eventType == XmlPullParser.END_TAG) {
                if(parser.getName().equals("address")) {
                    return;
                }
            }
        } while (parser.nextTag() != XmlPullParser.END_DOCUMENT);
    }
    
    public AddressIQ() {
        
    }
    
    public AddressIQ(Address address, AddressFactory factory) {
        this.address = address;
        this.factory = factory;
    }

    public Address getAddress() {
        return address;
    }

    public String getChildElementXML() {        
        String pushEndpoint = "<address xmlns=\"jabber:iq:lw-address\">";        
        if(address != null) {
            AddressSerializer addressSerializer = factory.getSerializer(address.getClass());
            pushEndpoint += "<" + addressSerializer.getAddressType();
            try {
                pushEndpoint += " value=\"" + new String(Base64.encodeBase64(addressSerializer.serialize(address)), "UTF-8") + "\"/>";
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        pushEndpoint += "</address>";
        return pushEndpoint;
    }    
}