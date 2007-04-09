package com.limegroup.gnutella.filters;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;

import junit.framework.Test;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.xml.LimeXMLDocumentHelper;
import com.limegroup.gnutella.xml.LimeXMLUtils;

/**
 * Unit tests for KeywordFilter
 */
public class KeywordFilterTest extends LimeTestCase {
    
    KeywordFilter filter=new KeywordFilter();
    QueryRequest qr=null;    
    
    protected byte[] address;
    
	public KeywordFilterTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(KeywordFilterTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
    
    @Override
    protected void setUp() throws Exception {
        address = InetAddress.getLocalHost().getAddress();
    }
	
	public void testLegacy() {        
        qr=QueryRequest.createQuery("Britney", (byte)1);
        assertTrue(filter.allow(qr));
        filter.disallow("britney spears");
        assertTrue(filter.allow(qr));
        
        qr=QueryRequest.createQuery("pie with rhubarb", (byte)1);
        assertTrue(filter.allow(qr));
        filter.disallow("rhuBarb");
        assertTrue(!filter.allow(qr));
        qr=QueryRequest.createQuery("rhubarb.txt", (byte)1);
        assertTrue(!filter.allow(qr));
        qr=QueryRequest.createQuery("Rhubarb*", (byte)1);
        assertTrue(!filter.allow(qr));
        
        filter.disallowVbs();
        qr=QueryRequest.createQuery("test.vbs", (byte)1);
        assertTrue(!filter.allow(qr));
        
        filter.disallowHtml();
        qr=QueryRequest.createQuery("test.htm", (byte)1);
        assertTrue(!filter.allow(qr));
    }
    
    public void testDisallowAdult() throws Exception {
        KeywordFilter filter = new KeywordFilter();
        QueryReply qr = createReply("adult");
        assertTrue(filter.allow(qr));
        qr = createReply("Sex");
        assertTrue(filter.allow(qr));
        
        // turn filter on
        filter.disallowAdult();
        
        assertFalse(filter.allow(qr));
        
        qr = createReply("adult");
        assertFalse(filter.allow(qr));
        
        qr = createReply("innocent");
        assertTrue(filter.allow(qr));
    }
    
    protected QueryReply createReply(String response) {
        return createReply(new Response(5, 5, response), new GUID(), 5555, address); 
    }
    
    public static QueryReply createReply(Response resp, GUID guid, int port, byte[] address) {
        String xmlCollectionString = LimeXMLDocumentHelper.getAggregateString(new Response [] { resp } );
        if (xmlCollectionString == null)
            xmlCollectionString = "";

        byte[] xmlBytes = null;
        try {
            xmlBytes = xmlCollectionString.getBytes("UTF-8");
        } catch(UnsupportedEncodingException ueex) {//no support for utf-8?? 
        }
        byte[] xmlCompressed = null;
        if (xmlCollectionString!=null && !xmlCollectionString.equals(""))
            xmlCompressed = 
                LimeXMLUtils.compress(xmlBytes);
        else //there is no XML
            xmlCompressed = DataUtils.EMPTY_BYTE_ARRAY;
        
        return new QueryReply(guid.bytes(), (byte)1,
                port, address, 0, new Response[] { resp },
                GUID.makeGuid(), xmlCompressed, false, false, true, true, true, false);
    }
}
