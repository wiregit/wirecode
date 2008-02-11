package com.limegroup.gnutella.search;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.KeyValue;
import org.limewire.collection.NameValue;
import org.limewire.io.IpPort;
import org.limewire.security.SecureMessage;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.limegroup.bittorrent.BTInterval;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.ForMeReplyHandler;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.ResponseFactory;
import com.limegroup.gnutella.ResponseVerifier;
import com.limegroup.gnutella.ResponseVerifierImpl;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;
import com.limegroup.gnutella.xml.LimeXMLDocumentHelper;
import com.limegroup.gnutella.xml.LimeXMLNames;

public class SearchResultHandlerTest extends LimeTestCase {
    
    private LimeXMLDocumentFactory factory;
    private MyActivityCallback callback;
    private LimeXMLDocumentHelper limeXMLDocumentHelper;
    private SearchResultHandler searchResultHandler;
    private ResponseFactory responseFactory;
    private QueryReplyFactory queryReplyFactory;
    private LimeXMLDocumentFactory limeXmlDocumentFactory;
    private RemoteFileDescFactory remoteFileDescFactory;
    
    public SearchResultHandlerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(SearchResultHandlerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjector(MyActivityCallback.class, new AbstractModule() {
            @Override
            protected void configure() {
                bind(ResponseVerifier.class).to(StubVerifier.class);
                bind(SearchResultHandler.class).to(SearchResultHandlerImpl.class);
            }
        });

        callback = (MyActivityCallback) injector.getInstance(ActivityCallback.class);
        
        limeXMLDocumentHelper = injector.getInstance(LimeXMLDocumentHelper.class);
        
        factory = injector.getInstance(LimeXMLDocumentFactory.class);
        
        searchResultHandler = injector.getInstance(SearchResultHandler.class);
        
        responseFactory = injector.getInstance(ResponseFactory.class);
        
        queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        
        limeXmlDocumentFactory = injector.getInstance(LimeXMLDocumentFactory.class);
        
        remoteFileDescFactory = injector.getInstance(RemoteFileDescFactory.class);
    }
    
    public void testSecureActionSent() throws Exception {
        Mockery m = new Mockery();
        final QueryRequest queryRequest = m.mock(QueryRequest.class);
        final byte[] guid = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F };
        
        m.checking(new Expectations() {{
            atLeast(1).of(queryRequest).isBrowseHostQuery();
            atLeast(1).of(queryRequest).isWhatIsNewRequest();
            atLeast(1).of(queryRequest).getGUID();
            will(returnValue(guid));
        }});
        
        searchResultHandler.addQuery(queryRequest);
        
        List<NameValue<String>> list = new LinkedList<NameValue<String>>();
        list.add(new NameValue<String>("audios__audio__action__", "http://somewhere.com"));
        LimeXMLDocument actionDoc = factory.createLimeXMLDocument(list, "http://www.limewire.com/schemas/audio.xsd");
        Response actionResponse = responseFactory.createResponse(0, 1, "test", actionDoc);
        QueryReply reply = newQueryReply(new Response[] { actionResponse } );
        reply.setSecureStatus(SecureMessage.SECURE);
        assertEquals(0, callback.results.size());
        searchResultHandler.handleQueryReply(reply);
        assertEquals(1, callback.results.size());
        RemoteFileDesc rfd = callback.getRFD();
        assertNotNull(rfd.getXMLDocument());
        assertEquals("http://somewhere.com", rfd.getXMLDocument().getAction());
        assertEquals(SecureMessage.SECURE, rfd.getSecureStatus());
    }
    
    public void testInsecureActionNotSent() throws Exception {
        Response actionResponse = responseFactory.createResponse(0, 1, "test");
        List<NameValue<String>> list = new LinkedList<NameValue<String>>();
        list.add(new NameValue<String>("audios__audio__action__", "http://somewhere.com"));
        LimeXMLDocument actionDoc = factory.createLimeXMLDocument(list, "http://www.limewire.com/schemas/audio.xsd");
        actionResponse.setDocument(actionDoc);
        QueryReply reply = newQueryReply(new Response[] { actionResponse } );
        assertEquals(0, callback.results.size());
        searchResultHandler.handleQueryReply(reply);
        assertEquals(0, callback.results.size());        
    }
    
    public void testInsecureResponseWithoutActionSent() throws Exception {
        Mockery m = new Mockery();
        final QueryRequest queryRequest = m.mock(QueryRequest.class);
        final byte[] guid = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F };
        
        m.checking(new Expectations() {{
            atLeast(1).of(queryRequest).isBrowseHostQuery();
            atLeast(1).of(queryRequest).isWhatIsNewRequest();
            atLeast(1).of(queryRequest).getGUID();
            will(returnValue(guid));
        }});
        
        searchResultHandler.addQuery(queryRequest);
        
        Response actionResponse = responseFactory.createResponse(0, 1, "test");
        List<NameValue<String>> list = new LinkedList<NameValue<String>>();
        list.add(new NameValue<String>("audios__audio__action__", "http://somewhere.com"));
        LimeXMLDocument actionDoc = factory.createLimeXMLDocument(list, "http://www.limewire.com/schemas/audio.xsd");
        actionResponse.setDocument(actionDoc);
        
        Response noDoc = responseFactory.createResponse(1, 2, "other");
        QueryReply reply = newQueryReply(new Response[] { actionResponse, noDoc } );
        assertEquals(0, callback.results.size());
        searchResultHandler.handleQueryReply(reply);
        assertEquals(1, callback.results.size());
        RemoteFileDesc rfd = callback.getRFD();
        assertNull(rfd.getXMLDocument());
        assertEquals("other", rfd.getFileName());
    }
    
    public void testFailedReplyNotForwarded() throws Exception {
        Response actionResponse = responseFactory.createResponse(0, 1, "test");
        List<NameValue<String>> list = new LinkedList<NameValue<String>>();
        list.add(new NameValue<String>("audios__audio__action__", "http://somewhere.com"));
        LimeXMLDocument actionDoc = factory.createLimeXMLDocument(list, "http://www.limewire.com/schemas/audio.xsd");
        actionResponse.setDocument(actionDoc);
        
        Response noDoc = responseFactory.createResponse(1, 2, "other");
        QueryReply reply = newQueryReply(new Response[] { actionResponse, noDoc } );
        reply.setSecureStatus(SecureMessage.FAILED);
        
        assertEquals(0, callback.results.size());
        searchResultHandler.handleQueryReply(reply);
        assertEquals(0, callback.results.size());        
    }
    
    /**
     * Tests the adding of a partial search result (as defined by
     * the presence of an IntervalSet in the QueryResponse), to a
     * SearchResultHandler; verifies that the SearchResultStats
     * indicates the presence of a single available location.
     * 
     * @throws Exception
     */
    public void testAddPartialSearchResult() throws Exception {
       Mockery m = new Mockery();
       
       final QueryRequest queryRequest = m.mock(QueryRequest.class);
       final QueryReply queryReply = m.mock(QueryReply.class);
       final HostData hostData = m.mock(HostData.class);
       final Response response = m.mock(Response.class);
       final List<Response> responses = Collections.singletonList(response);
       final Set<URN> urns = URN.createSHA1AndTTRootUrns(new File("/Users/cjones/Desktop/Equipment.txt"));
       final Set<IpPort> ipPorts = new HashSet<IpPort>();
       final RemoteFileDesc remoteFileDesc = m.mock(RemoteFileDesc.class);
       
       List<KeyValue<String, String>> map = new ArrayList<KeyValue<String, String>>();
       map.add(new KeyValue<String, String>(LimeXMLNames.APPLICATION_NAME, "value"));
       final LimeXMLDocument limeXmlDocument = limeXmlDocumentFactory.createLimeXMLDocument(map, LimeXMLNames.APPLICATION_SCHEMA);
       final byte[] guid = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F };
       final IntervalSet intervalSet = new IntervalSet();
       
       intervalSet.add( new BTInterval(10,15) );
       
       m.checking(new Expectations() {{
           atLeast(1).of(queryRequest).isBrowseHostQuery();
           
           atLeast(1).of(queryRequest).isWhatIsNewRequest();
           
           atLeast(1).of(queryRequest).getGUID();
           will(returnValue(new byte[16]));
           
           atLeast(1).of(queryReply).getGUID();
           will(returnValue(guid));
           
           atLeast(1).of(queryReply).isBrowseHostReply();
           will(returnValue(false));
           
           atLeast(1).of(queryReply).getResultsAsList();
           will(returnValue(responses));
           
           atLeast(1).of(queryReply).getIPBytes();
           will(returnValue(new byte[]{127,0,0,1}));
           
           atLeast(1).of(response).getRanges();
           will(returnValue(intervalSet));
           
           atLeast(1).of(response).getDocument();
           will(returnValue(limeXmlDocument));
           
           atLeast(1).of(response).getUrns();
           will(returnValue(urns));
           
           atLeast(1).of(response).getLocations();
           will(returnValue(ipPorts));
           
           atLeast(1).of(response).toRemoteFileDesc(hostData, remoteFileDescFactory);
           will(returnValue(remoteFileDesc));
           
           atLeast(1).of(remoteFileDesc).setSecureStatus(0);
           
           atLeast(1).of(queryReply).getSecureStatus();
           
           atLeast(1).of(hostData).getMessageGUID();
           will(returnValue(guid));
       }});
       
       SearchResultStats srs = searchResultHandler.addQuery(queryRequest);
       
       assertEquals(0, srs.getNumberOfLocations(urns.iterator().next()));
       srs.addQueryReply(searchResultHandler, queryReply, hostData);
       assertEquals(1, srs.getNumberOfLocations(urns.iterator().next()));
       
       m.assertIsSatisfied();
    }
    
    /**
     * Add a QueryRequest and a QueryReply that represents
     * a whole search result and make sure that it is
     * account for properly.
     * 
     * @throws Exception
     */
    public void testAddWholeSearchResult() throws Exception {
        Mockery m = new Mockery();
        
        final QueryRequest queryRequest = m.mock(QueryRequest.class);
        final QueryReply queryReply = m.mock(QueryReply.class);
        final HostData hostData = m.mock(HostData.class);
        final Response response = m.mock(Response.class);
        final List<Response> responses = Collections.singletonList(response);
        final Set<URN> urns = URN.createSHA1AndTTRootUrns(new File("/Users/cjones/Desktop/Equipment.txt"));
        final Set<IpPort> ipPorts = new HashSet<IpPort>();
        final RemoteFileDesc remoteFileDesc = m.mock(RemoteFileDesc.class);
        
        List<KeyValue<String, String>> map = new ArrayList<KeyValue<String, String>>();
        map.add(new KeyValue<String, String>(LimeXMLNames.APPLICATION_NAME, "value"));
        final LimeXMLDocument limeXmlDocument = limeXmlDocumentFactory.createLimeXMLDocument(map, LimeXMLNames.APPLICATION_SCHEMA);
        final byte[] guid = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F };
        
        m.checking(new Expectations() {{
            atLeast(1).of(queryRequest).isBrowseHostQuery();
            
            atLeast(1).of(queryRequest).isWhatIsNewRequest();
            
            atLeast(1).of(queryRequest).getGUID();
            will(returnValue(new byte[16]));
            
            atLeast(1).of(queryReply).getGUID();
            will(returnValue(guid));
            
            atLeast(1).of(queryReply).isBrowseHostReply();
            will(returnValue(false));
            
            atLeast(1).of(queryReply).getResultsAsList();
            will(returnValue(responses));
            
            atLeast(1).of(queryReply).getIPBytes();
            will(returnValue(new byte[]{127,0,0,1}));
            
            atLeast(1).of(response).getRanges();
            will(returnValue(null));
            
            atLeast(1).of(response).getDocument();
            will(returnValue(limeXmlDocument));
            
            atLeast(1).of(response).getUrns();
            will(returnValue(urns));
            
            atLeast(1).of(response).getLocations();
            will(returnValue(ipPorts));
            
            atLeast(1).of(response).toRemoteFileDesc(hostData, remoteFileDescFactory);
            will(returnValue(remoteFileDesc));
            
            atLeast(1).of(remoteFileDesc).setSecureStatus(0);
            
            atLeast(1).of(queryReply).getSecureStatus();
            
            atLeast(1).of(hostData).getMessageGUID();
            will(returnValue(guid));
        }});
        
        SearchResultStats srs = searchResultHandler.addQuery(queryRequest);
        
        assertEquals(0, srs.getNumberOfLocations(urns.iterator().next()));
        srs.addQueryReply(searchResultHandler, queryReply, hostData);
        assertEquals(1, srs.getNumberOfLocations(urns.iterator().next()));
        
        m.assertIsSatisfied();
    }
    
    /**
     * We will test the receiving of a QueryReply without
     * having received a QueryRequest previous to the QueryReply.
     * This will fail in SearchResultHandler::accountAndUpdateDynamicQueriers(),
     * but not in a fashion that we can directly detect (like quantum state), 
     * other than that of a result count that will remain unchanged (ie, zero 
     * in this case).
     * 
     * (cjones@moo.limewire.com:~/Projects/limewire-psr-2/tests <Mon Feb 11> <16:59:54>)
     * $ [60|560] >> ant -Dclass=search/SearchResultHandlerTest test
     *
     * @throws Exception
     */
    public void testAddReplyWithoutRequest() throws Exception {
        List<NameValue<String>> list = new LinkedList<NameValue<String>>();
        list.add(new NameValue<String>("audios__audio__action__", "http://somewhere.com"));
        LimeXMLDocument actionDoc = factory.createLimeXMLDocument(list, "http://www.limewire.com/schemas/audio.xsd");
        Response actionResponse = responseFactory.createResponse(0, 1, "test", actionDoc);
        QueryReply reply = newQueryReply(new Response[] { actionResponse } );
        reply.setSecureStatus(SecureMessage.SECURE);
        assertEquals(0, callback.results.size());
        searchResultHandler.handleQueryReply(reply);
        assertEquals(0, callback.results.size());
    }
    
    /**
     * 
     * @throws Exception
     */
    public void testRemoveQuery() throws Exception {
        // add a query reply with a specific guid
        // remove the query reply
        // test removing the specific guid, directly (shouldn't work)
    }
    
    
    
    
    private QueryReply newQueryReply(Response[] responses) throws Exception {
        byte[] guid = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F };
        
        QueryReply reply = 
               queryReplyFactory.createQueryReply(guid, (byte)1, 6346,
                new byte[] { (byte)1, (byte)1, (byte)1, (byte)1 }, 1, responses, new byte[16], LimeXMLDocumentHelper.getAggregateString(responses).getBytes(), false, false,
                true, true, false, false, false, IpPort.EMPTY_SET, null);
        ForMeReplyHandler.addXMLToResponses(reply, limeXMLDocumentHelper);
        return reply;
    }
    
    @Singleton
    private static class MyActivityCallback extends ActivityCallbackStub {
        
        List<RemoteFileDesc> results = new LinkedList<RemoteFileDesc>();

        public void handleQueryResult(RemoteFileDesc rfd, HostData data, Set alts) {
            results.add(rfd);
        }
        
        public RemoteFileDesc getRFD() {
            return results.remove(0);
        }
    }
    
    @Singleton
    private static class StubVerifier extends ResponseVerifierImpl {

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