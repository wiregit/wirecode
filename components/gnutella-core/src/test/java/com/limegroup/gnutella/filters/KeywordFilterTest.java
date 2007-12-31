package com.limegroup.gnutella.filters;

import java.util.List;
import java.util.Set;
import java.util.Vector;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.io.IpPort;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.xml.LimeXMLDocument;


/**
 * Unit tests for KeywordFilter
 */
public class KeywordFilterTest extends BaseTestCase {
    
    KeywordFilter filter=new KeywordFilter();
    
    QueryRequest queryRequestMock=null;
    QueryReply queryReplyMock = null;
    PingRequest pingRequestMock = null;
    Mockery context;
    
    
    
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
        
        
        context = new Mockery();
                
        queryRequestMock = context.mock(QueryRequest.class);
        queryReplyMock = context.mock(QueryReply.class);
        pingRequestMock = context.mock(PingRequest.class);
    }
    
    private void keywordContextValue(QueryRequest query, String keyword){
        final QueryRequest localQuery = query;
        final String localKeyword = keyword;
        
        context.checking(new Expectations() {{
            one(localQuery).getQuery();
            will(returnValue(localKeyword));
        }});  
                
    }
    
    public void testAllowKeyword(){
                
        keywordContextValue(queryRequestMock, "pie with rhubarb");
        assertTrue(filter.allow(queryRequestMock));
        filter.disallow("britney spears");
                
        filter.disallow("rhuBarb");
        
        keywordContextValue(queryRequestMock, "rhubar");
        assertTrue(filter.allow(queryRequestMock));
                 
        keywordContextValue(queryRequestMock, "pie with rhubarb");
        assertFalse(filter.allow(queryRequestMock));
        
        keywordContextValue(queryRequestMock, "rhubarb.txt");
        assertFalse(filter.allow(queryRequestMock));
        
        keywordContextValue(queryRequestMock, "Rhubarb*");
        assertFalse(filter.allow(queryRequestMock));
        
        keywordContextValue(queryRequestMock, "Rhubarb#");
        assertFalse(filter.allow(queryRequestMock));
        
        keywordContextValue(queryRequestMock, "Rhubarb***");
        assertFalse(filter.allow(queryRequestMock));
        
        context.assertIsSatisfied();
    }

    public void testAllowFileExt(){      
        keywordContextValue(queryRequestMock, "test.vbs");
        assertTrue(filter.allow(queryRequestMock));

        filter.disallowVbs();
        
        keywordContextValue(queryRequestMock, "test.vbs");
        assertFalse(filter.allow(queryRequestMock));
                
        keywordContextValue(queryRequestMock, "test.htm");
        assertTrue(filter.allow(queryRequestMock));
        
        filter.disallowHtml();
                 
        keywordContextValue(queryRequestMock, "test.htm");
        assertFalse(filter.allow(queryRequestMock));
                
        keywordContextValue(queryRequestMock, "test.wmv");
        assertTrue(filter.allow(queryRequestMock));
               
        keywordContextValue(queryRequestMock, "test.asf");
        assertTrue(filter.allow(queryRequestMock));
                
        filter.disallowWMVASF();
        
        keywordContextValue(queryRequestMock, "test.wmv");
        assertFalse(filter.allow(queryRequestMock));
        
        keywordContextValue(queryRequestMock, "test.asf");
        assertFalse(filter.allow(queryRequestMock));
        
        context.assertIsSatisfied();
        
        }
    
    public void testDisallowAdult() throws Exception {
        KeywordFilter filter = new KeywordFilter();
        
        createResponseList(queryReplyMock, "adult");
        assertTrue(filter.allow(queryReplyMock));
        
        createResponseList(queryReplyMock, "Sex");
        assertTrue(filter.allow(queryReplyMock));

        /*
         * turn filter on
         */
        filter.disallowAdult();
        
        createResponseList(queryReplyMock, "adult");
        assertFalse(filter.allow(queryReplyMock));
        
        createResponseList(queryReplyMock, "Sex");
        assertFalse(filter.allow(queryReplyMock));
                
        createResponseList(queryReplyMock, "innocent");
        assertTrue(filter.allow(queryReplyMock));
        
        context.assertIsSatisfied();
        
    }
    
    public void queryReply(QueryRequest query, String keyword){
        final QueryRequest localQuery = query;
        final String localKeyword = keyword;
        
        context.checking(new Expectations() {{
            one(localQuery).getQuery();
            will(returnValue(localKeyword));
        }});  
                
    }
    
    public void testOtherMessagesAreIgnored() throws Exception{
        context.checking(new Expectations()
        {{ never(pingRequestMock);
        }});
        
        assertTrue(filter.allow(pingRequestMock));
        
        context.assertIsSatisfied();
    }
    
    protected void createResponseList (QueryReply qr, String response)throws BadPacketException{
                
        final QueryReply localqr = qr;
        final List<Response> responseList = new Vector<Response>();
        
        //create Response
        final Response qrResponse;
        long index = 0;
        long size = 0;
        LimeXMLDocument emptyDoc = null;
        Set<URN> emptyurns = null;
        Set<IpPort> alternateLocations = null;
        long creationTime = 0;
        byte[] extensions = null;
        
        qrResponse = new Response(index,size,response, -1, emptyurns, emptyDoc, alternateLocations, creationTime, extensions,null, false);
                     
        /*
         * add Response to Response List
         */
         
        responseList.add(qrResponse);
        
        context.checking(new Expectations() {{
            one(localqr).getResultsAsList();
            will(returnValue(responseList));
        }});  
    
    }
}
