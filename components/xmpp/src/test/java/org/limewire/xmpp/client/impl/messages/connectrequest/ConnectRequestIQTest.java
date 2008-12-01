package org.limewire.xmpp.client.impl.messages.connectrequest;

import java.io.IOException;
import java.io.StringReader;

import org.limewire.io.ConnectableImpl;
import org.limewire.util.BaseTestCase;
import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.limegroup.gnutella.GUID;

public class ConnectRequestIQTest extends BaseTestCase {

    public ConnectRequestIQTest(String name) {
        super(name);
    }

    
    public void testParsesItsOwnOutput() throws Exception {
        GUID guid = new GUID();
        ConnectRequestIQ connectRequest = new ConnectRequestIQ(new ConnectableImpl("129.0.0.1", 5000, true), guid, 1);

        ConnectRequestIQ parsedRequest = new ConnectRequestIQ(createParser(connectRequest.getChildElementXML()));
        
        assertEquals(connectRequest.getClientGuid(), parsedRequest.getClientGuid());
        assertEquals(connectRequest.getAddress(), parsedRequest.getAddress());
        assertEquals(connectRequest.getSupportedFWTVersion(), parsedRequest.getSupportedFWTVersion());
    }
    
    private XmlPullParser createParser(String input) throws Exception {
        XmlPullParser parser = new MXParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        parser.setInput(new StringReader(input));
        return parser;
    }
    
    public void testThrowsOnMissingAddress() throws Exception {
        try {
            new ConnectRequestIQ(createParser("<connect-request xmlns=\"jabber:iq:lw-connect-request\" client-guid=\"" + new GUID() + "\" supported-fwt-version=\"0\"></connect-request>"));
            fail("io exception expected");
        } catch (XmlPullParserException e) {
            fail(e);
        } catch (IOException e) {
        }
    }
    
    public void testThrowsOnInvalidGuid() throws Exception {
        try {
            new ConnectRequestIQ(createParser("<connect-request xmlns=\"jabber:iq:lw-connect-request\" client-guid=\"ffdfdd\" supported-fwt-version=\"0\"><address type=\"direct-connect\" value=\"AIEAAAETiAE=\"/></connect-request>"));
            fail("io exception expected");
        } catch (XmlPullParserException e) {
            fail(e);
        } catch (IOException e) {
        }
    }
    
    public void testThrowsOnMissingGuid() throws Exception {
        try {
            new ConnectRequestIQ(createParser("<connect-request xmlns=\"jabber:iq:lw-connect-request\" supported-fwt-version=\"0\"><address type=\"direct-connect\" value=\"AIEAAAETiAE=\"/></connect-request>"));
            fail("io exception expected");
        } catch (XmlPullParserException e) {
            fail(e);
        } catch (IOException e) {
        }
    }
    
    public void testThrowsOnMissingFWTVersion() throws Exception {
        try {
            new ConnectRequestIQ(createParser("<connect-request xmlns=\"jabber:iq:lw-connect-request\" client-guid=\"BD2BF8AA3D262F0957AF5F96B7F16600\"><address type=\"direct-connect\" value=\"AIEAAAETiAE=\"/></connect-request>"));
            fail("io exception expected");
        } catch (XmlPullParserException e) {
            fail(e);
        } catch (IOException e) {
        }
    }
    
    public void testThrowsOnInvalidFWTVersion() throws Exception {
        try {
            new ConnectRequestIQ(createParser("<connect-request xmlns=\"jabber:iq:lw-connect-request\" client-guid=\"BD2BF8AA3D262F0957AF5F96B7F16600\"  supported-fwt-version=\"A\"><address type=\"direct-connect\" value=\"AIEAAAETiAE=\"/></connect-request>"));
            fail("io exception expected");
        } catch (XmlPullParserException e) {
            fail(e);
        } catch (IOException e) {
        }
    }
}
