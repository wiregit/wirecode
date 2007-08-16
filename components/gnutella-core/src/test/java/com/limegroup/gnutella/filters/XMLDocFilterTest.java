package com.limegroup.gnutella.filters;

import java.io.ByteArrayOutputStream;
import java.util.Collections;

import junit.framework.Test;

import org.limewire.collection.NameValue;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;

public class XMLDocFilterTest extends KeywordFilterTest {

    public XMLDocFilterTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(XMLDocFilterTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testDisallowAdultXML() throws Exception {
        XMLDocFilter filter = new XMLDocFilter();
        
        
        // test filter switched off
        QueryReply qr = createXMLReply(LimeXMLNames.VIDEO_TYPE, "Adult");
        assertTrue(filter.allow(qr));
        
        qr = createXMLReply(LimeXMLNames.VIDEO_RATING, "R");
        assertTrue(filter.allow(qr));
        
        qr = createXMLReply(LimeXMLNames.VIDEO_RATING, "NC-17");
        assertTrue(filter.allow(qr));
        
        filter.disallowAdult();
        
        qr = createXMLReply(LimeXMLNames.VIDEO_RATING, "R");
        assertFalse(filter.allow(qr));
        
        qr = createXMLReply(LimeXMLNames.VIDEO_RATING, "NC-17");
        assertFalse(filter.allow(qr));
        
        qr = createXMLReply(LimeXMLNames.VIDEO_TYPE, "Adult");
        assertFalse(filter.allow(qr));

        qr = createXMLReply(LimeXMLNames.VIDEO_TYPE, "Adult Film");
        assertFalse(filter.allow(qr));

        qr = createXMLReply(LimeXMLNames.VIDEO_TYPE, "Adult Film Clip");
        assertFalse(filter.allow(qr));
        
        qr = createXMLReply(LimeXMLNames.VIDEO_TYPE, "adul");
        assertTrue(filter.allow(qr));
        
        qr = createXMLReply(LimeXMLNames.VIDEO_RATING, "NR");
        assertTrue(filter.allow(qr));
        
        qr = createReply("Adult Film");
        assertFalse(filter.allow(qr));
        
        qr = createXMLReply(LimeXMLNames.VIDEO_RATING, "Adults Only");
        assertFalse(filter.allow(qr));
    }
    
    private QueryReply createXMLReply(String field, String values) throws Exception {
        return createXMLReply(field, values, 5555, address);
    }
    
    public static QueryReply createXMLReply(String field, String values, int port, byte[] address) throws Exception {
        
        Response resp = createXMLResponse("filename", field, values);
        
        return createReply(resp, port, address);
    }
    
    public static QueryReply createReply(Response resp, int port, byte[] address) throws Exception {
        QueryReply qr = createReply(resp, new GUID(), port, address);

        // write out to network so it is parsed again and xml docs are constructed
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        qr.writePayload(out);
        qr = ProviderHacks.getQueryReplyFactory().createFromNetwork(GUID.makeGuid(), (byte) 1,
                (byte) 1, out.toByteArray());
        
        // hack, query replies have to be equipped with xml again
        PrivilegedAccessor.invokeMethod(ProviderHacks.getForMeReplyHandler(), "addXMLToResponses", qr);
        return qr;
    }
    
    public static Response createXMLResponse(String fileName, String field, String values) throws Exception {
        NameValue<String> nameValue = new NameValue<String>(field, values);
        LimeXMLDocument doc = ProviderHacks.getLimeXMLDocumentFactory().createLimeXMLDocument(Collections.singletonList(nameValue),
                LimeXMLNames.VIDEO_SCHEMA);
        return ProviderHacks.getResponseFactory().createResponse(101, 1340, fileName, doc);
    }
}
