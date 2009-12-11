package com.limegroup.gnutella.filters;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.util.BaseTestCase;

import com.google.common.collect.ImmutableMap;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.xml.LimeXMLDocument;


public class PhraseFilterTest extends BaseTestCase {

    private PhraseFilter filter;
    private QueryRequest queryRequestMock;
    private QueryReply queryReplyMock;
    private PingRequest pingRequestMock;
    private Response responseMock;
    private LimeXMLDocument xmlDoc;
    private Mockery context;

    public PhraseFilterTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PhraseFilterTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        queryRequestMock = context.mock(QueryRequest.class);
        queryReplyMock = context.mock(QueryReply.class);
        pingRequestMock = context.mock(PingRequest.class);
        responseMock = context.mock(Response.class);
        xmlDoc = context.mock(LimeXMLDocument.class);
        
        filter = new PhraseFilter();
    }

    private void keywordContextValue(final QueryRequest query, final String keyword) {
        context.checking(new Expectations() {{
            one(query).getQuery();
            will(returnValue(keyword));
        }});
    }
    
    private void keywordContextValue(final Response response, final String keyword, final boolean xml) {
        context.checking(new Expectations() {{
            one(response).getName();
            will(returnValue(keyword));
            if(xml) {
                one(response).getDocument();
                will(returnValue(null));
            }
        }});   
    }
    
    private void xmlContextValue(final Response response, final String keyword) {
        context.checking(new Expectations() {{
            one(response).getName();
            will(returnValue(""));
            one(response).getDocument();
            will(returnValue(xmlDoc));
            one(xmlDoc).getNameValueSet();
            will(returnValue(ImmutableMap.of("", keyword).entrySet()));
        }});   
    }
    
    public void testEmpty() {
        keywordContextValue(queryRequestMock, "free falling");
        assertTrue(filter.allow(queryRequestMock));
        
        keywordContextValue(responseMock, "free floating", true);
        assertTrue(filter.allow(queryReplyMock, responseMock));
        
        context.assertIsSatisfied();
    }
    
    public void testKeywordPhrase() {
        filter = new PhraseFilter("free falling", "free floating", "fall");
        
        keywordContextValue(queryRequestMock, "free falling");
        assertFalse(filter.allow(queryRequestMock));
        
        keywordContextValue(responseMock, "free floating", false);
        assertFalse(filter.allow(queryReplyMock, responseMock));
        
        keywordContextValue(queryRequestMock, "free");
        assertTrue(filter.allow(queryRequestMock));
        
        keywordContextValue(responseMock, "free", true);
        assertTrue(filter.allow(queryReplyMock, responseMock));
        
        keywordContextValue(queryRequestMock, "falling");
        assertTrue(filter.allow(queryRequestMock));
        
        keywordContextValue(responseMock, "floating", true);
        assertTrue(filter.allow(queryReplyMock, responseMock));
        
        keywordContextValue(queryRequestMock, "i'm free falling");
        assertFalse(filter.allow(queryRequestMock));
        
        keywordContextValue(responseMock, "i'm free floating", false);
        assertFalse(filter.allow(queryReplyMock, responseMock));
        
        keywordContextValue(queryRequestMock, "free falling high");
        assertFalse(filter.allow(queryRequestMock));
        
        keywordContextValue(responseMock, "free floating low", false);
        assertFalse(filter.allow(queryReplyMock, responseMock));
        
        keywordContextValue(queryRequestMock, "i'm free falling high");
        assertFalse(filter.allow(queryRequestMock));
        
        keywordContextValue(responseMock, "i'm free floating low", false);
        assertFalse(filter.allow(queryReplyMock, responseMock));
        
        keywordContextValue(queryRequestMock, "fall");
        assertFalse(filter.allow(queryRequestMock));
        
        keywordContextValue(responseMock, "fall", false);
        assertFalse(filter.allow(queryReplyMock, responseMock));
        
        // single space after should work too.
        keywordContextValue(queryRequestMock, "fall ");
        assertFalse(filter.allow(queryRequestMock));

        // single space after should work too.
        keywordContextValue(responseMock, "fall ", false);
        assertFalse(filter.allow(queryReplyMock, responseMock));
        
        context.assertIsSatisfied();
    }
    
    public void testXmlPhrase() throws Exception {
        filter = new PhraseFilter("fluffy bunnies", "skeezy bunnies", "bun");
        
        xmlContextValue(responseMock, "fluffy bunnies");
        assertFalse(filter.allow(queryReplyMock, responseMock));
        
        xmlContextValue(responseMock, "fluffy");
        assertTrue(filter.allow(queryReplyMock, responseMock));
        
        xmlContextValue(responseMock, "bunnies");
        assertTrue(filter.allow(queryReplyMock, responseMock));
        
        xmlContextValue(responseMock, "happy fluffy bunnies");
        assertFalse(filter.allow(queryReplyMock, responseMock));
        
        xmlContextValue(responseMock, "fluffy bunnies in dirt");
        assertFalse(filter.allow(queryReplyMock, responseMock));
        
        xmlContextValue(responseMock, "happy fluffy bunnies in dirt");
        assertFalse(filter.allow(queryReplyMock, responseMock));
        
        xmlContextValue(responseMock, "bun");
        assertFalse(filter.allow(queryReplyMock, responseMock));
        
        xmlContextValue(responseMock, "bun ");
        assertFalse(filter.allow(queryReplyMock, responseMock));        
        
        context.assertIsSatisfied();
    }

    public void testOtherMessagesAreIgnored() throws Exception{
        context.checking(new Expectations() {{
            never(pingRequestMock);
        }});

        assertTrue(filter.allow(pingRequestMock));

        context.assertIsSatisfied();
    }
}
