package org.limewire.xmpp.client.impl.messages.connectrequest;

import java.io.IOException;
import java.text.MessageFormat;

import org.apache.commons.codec.binary.Base64;
import org.jivesoftware.smack.packet.IQ;
import org.limewire.io.Connectable;
import org.limewire.io.NetworkUtils;
import org.limewire.net.address.ConnectableSerializer;
import org.limewire.util.Objects;
import org.limewire.util.StringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.limegroup.gnutella.GUID;

/**
 * IQ to be send to request the other peer to open a connection to this peer.
 * 
 * The connection can be of two types:
 * 
 * 1) a regular TCP connection, in this case {@link #getSupportedFWTVersion()} is 0
 * 2) a reliable udp connection, in this case {@link #getSupportedFWTVersion()} conveys 
 *    the supported protocol version
 * In both cases a valid address for connecting needs to be provided
 */
public class ConnectRequestIQ extends IQ {

    private final Connectable address;
    private final int supportedfwtVersion;
    private final GUID clientGuid;
    
    /**
     * Only constructs valid connect request iqs, otherwise throws {@link IOException}. 
     */
    public ConnectRequestIQ(XmlPullParser parser) throws IOException, XmlPullParserException {
       int eventType = parser.getEventType();
       GUID guid = null;
       int fwtVersion = -1;
       Connectable connectable = null;
       for (; eventType != XmlPullParser.END_DOCUMENT; eventType = parser.next()) {
           if (eventType == XmlPullParser.START_TAG) {
               if (parser.getName().equals("connect-request")) {
                   String value = parser.getAttributeValue(null, "client-guid");
                   if (value == null) {
                       throw new IOException("no guid provided");
                   }
                   try { 
                       guid = new GUID(value);
                   } catch (IllegalArgumentException iae) {
                       throw new IOException("invalid guid: " + value, iae);
                   }
                   value = parser.getAttributeValue(null, "supported-fwt-version");
                   if (value == null) {
                       throw new IOException("no fwt version provided");
                   }
                   try {
                       fwtVersion = Integer.parseInt(value);
                   } catch (NumberFormatException nfe) {
                       throw new IOException("fwt version no a valid number: " + value, nfe);
                   }
               } else if (parser.getName().equals("address")) {
                   String type = parser.getAttributeValue(null, "type");
                   ConnectableSerializer serializer = new ConnectableSerializer();
                   if (type == null || !type.equals(serializer.getAddressType())) {
                       throw new IOException("no address type provided or invalid: " + type);
                   }
                   String value = parser.getAttributeValue(null, "value");
                   if (value == null) {
                       throw new IOException("no address value found");
                   }
                   connectable = serializer.deserialize(Base64.decodeBase64(StringUtils.toUTF8Bytes(value)));
                   if (!NetworkUtils.isValidIpPort(connectable)) {
                       throw new IOException("invalid address: " + connectable);
                   }
               }
           }
       }
       if (guid == null || fwtVersion == -1 || connectable == null) {
           throw new IOException(MessageFormat.format("incomplete connect request, {0}, {1}, {2}", guid, fwtVersion, connectable));
       }
       clientGuid = guid;
       supportedfwtVersion = fwtVersion;
       address = connectable;
    }
    
    /**
     * 
     * @param address needs to be a valid address, otherwise will throw {@link IllegalArgumentException}
     * @param clientGuid
     * @param supportedFWTVersion 0 if fwt is not supported
     */
    public ConnectRequestIQ(Connectable address, GUID clientGuid, int supportedFWTVersion) {
        this.address = address;
        this.clientGuid = Objects.nonNull(clientGuid, "clientGuid");
        this.supportedfwtVersion = supportedFWTVersion;
        if (!NetworkUtils.isValidIpPort(address)) {
            throw new IllegalArgumentException("invalid address: " + address);
        }
    }

    @Override
    public String getChildElementXML() {
        ConnectableSerializer serializer = new ConnectableSerializer();
        String message = "<connect-request xmlns=\"jabber:iq:lw-connect-request\" client-guid=\"{0}\" supported-fwt-version=\"{1}\"><address type=\"{2}\" value=\"{3}\"/></connect-request>";
        try {
            return MessageFormat.format(message, clientGuid.toHexString(), String.valueOf(supportedfwtVersion), serializer.getAddressType(), StringUtils.getUTF8String(Base64.encodeBase64(serializer.serialize(address))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } 
    }
    
    public Connectable getAddress() {
        return address;
    }

    public int getSupportedFWTVersion() {
        return supportedfwtVersion;
    }

    public GUID getClientGuid() {
        return clientGuid;
    }
}
