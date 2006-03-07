package com.limegroup.gnutella.search;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import junit.framework.Test;

import com.limegroup.gnutella.ForMeReplyHandler;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.ResponseVerifier;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.SecureMessage;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.NameValue;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentHelper;

public class SearchResultHandlerTest extends BaseTestCase {
    
    private StubCallback callback = new StubCallback();

    public SearchResultHandlerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(SearchResultHandlerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void setUp() throws Exception {
        new RouterService(callback);
        PrivilegedAccessor.setValue(RouterService.class, "VERIFIER", new StubVerifier());
    }
    
    public void testSecureActionSent() throws Exception {
        SearchResultHandler handler = new SearchResultHandler();
        List list = new LinkedList();
        list.add(new NameValue("audios__audio__action__", "http://somewhere.com"));
        LimeXMLDocument actionDoc = new LimeXMLDocument(list, "http://www.limewire.com/schemas/audio.xsd");
        Response actionResponse = new Response(0, 1, "test", actionDoc);
        QueryReply reply = newQueryReply(new Response[] { actionResponse } );
        reply.setSecureStatus(SecureMessage.SECURE);
        assertEquals(0, callback.results.size());
        handler.handleQueryReply(reply);
        assertEquals(1, callback.results.size());
        RemoteFileDesc rfd = callback.getRFD();
        assertNotNull(rfd.getXMLDocument());
        assertEquals("http://somewhere.com", rfd.getXMLDocument().getAction());
        assertEquals(SecureMessage.SECURE, rfd.getSecureStatus());
    }
    
    public void testInsecureActionNotSent() throws Exception {
        SearchResultHandler handler = new SearchResultHandler();
        Response actionResponse = new Response(0, 1, "test");
        List list = new LinkedList();
        list.add(new NameValue("audios__audio__action__", "http://somewhere.com"));
        LimeXMLDocument actionDoc = new LimeXMLDocument(list, "http://www.limewire.com/schemas/audio.xsd");
        actionResponse.setDocument(actionDoc);
        QueryReply reply = newQueryReply(new Response[] { actionResponse } );
        assertEquals(0, callback.results.size());
        handler.handleQueryReply(reply);
        assertEquals(0, callback.results.size());        
    }
    
    public void testInsecureResponseWithoutActionSent() throws Exception {
        SearchResultHandler handler = new SearchResultHandler();
        Response actionResponse = new Response(0, 1, "test");
        List list = new LinkedList();
        list.add(new NameValue("audios__audio__action__", "http://somewhere.com"));
        LimeXMLDocument actionDoc = new LimeXMLDocument(list, "http://www.limewire.com/schemas/audio.xsd");
        actionResponse.setDocument(actionDoc);
        
        Response noDoc = new Response(1, 2, "other");
        QueryReply reply = newQueryReply(new Response[] { actionResponse, noDoc } );
        assertEquals(0, callback.results.size());
        handler.handleQueryReply(reply);
        assertEquals(1, callback.results.size());
        RemoteFileDesc rfd = callback.getRFD();
        assertNull(rfd.getXMLDocument());
        assertEquals("other", rfd.getFileName());
    }
    
    public void testFailedReplyNotForwarded() throws Exception {
        SearchResultHandler handler = new SearchResultHandler();
        Response actionResponse = new Response(0, 1, "test");
        List list = new LinkedList();
        list.add(new NameValue("audios__audio__action__", "http://somewhere.com"));
        LimeXMLDocument actionDoc = new LimeXMLDocument(list, "http://www.limewire.com/schemas/audio.xsd");
        actionResponse.setDocument(actionDoc);
        
        Response noDoc = new Response(1, 2, "other");
        QueryReply reply = newQueryReply(new Response[] { actionResponse, noDoc } );
        reply.setSecureStatus(SecureMessage.FAILED);
        
        assertEquals(0, callback.results.size());
        handler.handleQueryReply(reply);
        assertEquals(0, callback.results.size());        
    }
    
    private QueryReply newQueryReply(Response[] responses) throws Exception {
        QueryReply reply = 
               new QueryReply( new byte[16]
                             , (byte)1
                             , 6346
                             , new byte[] { (byte)1, (byte)1, (byte)1, (byte)1 }
                             , 1
                             , responses
                             , new byte[16]
                             , LimeXMLDocumentHelper.getAggregateString(responses).getBytes()
                             , false
                             , false
                             , true
                             , true
                             , false
                             , false
                             , false
                             , Collections.EMPTY_SET
                             );
        PrivilegedAccessor.invokeMethod(ForMeReplyHandler.instance(), "addXMLToResponses", reply);
        return reply;
    }
    
    private static class StubCallback extends ActivityCallbackStub {
        List results = new LinkedList();

        public void handleQueryResult(RemoteFileDesc rfd, HostData data, Set alts) {
            results.add(rfd);
        }
        
        public RemoteFileDesc getRFD() {
            return (RemoteFileDesc)results.remove(0);
        }
    }
    
    private static class StubVerifier extends ResponseVerifier {

        public synchronized boolean matchesQuery(byte[] guid, Response response) {
            return true;
        }

        public boolean isMandragoreWorm(byte[] guid, Response response) {
            return false;
        }

        public boolean matchesType(byte[] guid, Response response) {
            return true;
        } 
        
    }
}