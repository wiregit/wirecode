package com.limegroup.gnutella;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Set;

import junit.framework.Test;

import com.limegroup.gnutella.filters.XMLDocFilterTest;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.xml.LimeXMLDocumentHelper;
import com.limegroup.gnutella.xml.LimeXMLNames;
import com.limegroup.gnutella.xml.LimeXMLUtils;

/*
 * import statements for the createReply methods - will delete once ClientSideWhatisNewSearchTest is refactored
 */
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.ProviderHacks;

/**
 * Tests the client's WhatIsNewSearch behavior.
 */
public class ClientSideWhatIsNewSearchTest extends ClientSideTestCase {

    private static StoreRepliesActivityCallback callback = new StoreRepliesActivityCallback();
    protected byte[] address;
    
    public ClientSideWhatIsNewSearchTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ClientSideWhatIsNewSearchTest.class);
    }

    @SuppressWarnings("unused")
    private static void doSettings() {
        
    }
    
    public static Integer numUPs() {
        return new Integer(1);
    }

    public static ActivityCallback getActivityCallback() {
        return callback;
    }
 
    /**
     * Ensure all results come through if filtering is disabled. 
     */
    public void testAdultFilterDisabled() throws Exception {
        testAdultFilter(false);
    }
    
    /**
     * Ensure appropiate results are filtered when adult filtering is turned on
     * and valid results still get through.  
     */
    public void testAdultFilterEnabled() throws Exception {
        testAdultFilter(true);
    }
    
    public void testAdultFilter(boolean enabled) throws Exception {
        FilterSettings.FILTER_WHATS_NEW_ADULT.setValue(enabled);
        
        drainAll();
        
        GUID guid = new GUID();
        ProviderHacks.getSearchServices().queryWhatIsNew(guid.bytes(), MediaType.getAnyTypeMediaType());
        
        assertQuery(testUP[0], guid);
        
        // send back a result that will be filtered
        Response resp = ProviderHacks.getResponseFactory().createResponse(101, 1019, "sex");
        sendAndAssertResponse(resp, guid, enabled);
        
        resp = XMLDocFilterTest.createXMLResponse("filename", LimeXMLNames.VIDEO_TYPE, "adult");
        sendAndAssertResponse(resp, guid, enabled);
        
        resp = XMLDocFilterTest.createXMLResponse("filename", LimeXMLNames.VIDEO_RATING, "NC-17");
        sendAndAssertResponse(resp, guid, enabled);
        
        resp = ProviderHacks.getResponseFactory().createResponse(105, 345, "harmless");
        sendAndAssertResponse(resp, guid, false);
    }
    
    private void sendAndAssertResponse(Response resp, GUID guid, boolean expectedToBeFiltered) throws Exception {
        QueryReply reply = createReply(resp, guid, testUP[0].getPort(), testUP[0].getInetAddress().getAddress());
        callback.guid = reply.getClientGUID();
        callback.desc = null;
        
        testUP[0].send(reply);
        testUP[0].flush();
        
        RemoteFileDesc desc = callback.waitForRFD(1000);
        if (expectedToBeFiltered) {
            assertNull(desc);
        }
        else {
            assertNotNull(desc);
            assertEquals(resp.getName(), desc.getFileName());
        }
    }

    private QueryRequest assertQuery(Connection c, GUID guid) throws BadPacketException {
        QueryRequest qr = getFirstInstanceOfMessageType(c, QueryRequest.class);
        assertNotNull(qr);
        assertEquals(guid.bytes(), qr.getGUID());
        return qr;
    }
    
    private static class StoreRepliesActivityCallback extends ActivityCallbackStub {
        
        RemoteFileDesc desc = null;
        
        byte[] guid = null;
        
        @Override
        public synchronized void handleQueryResult(RemoteFileDesc rfd, HostData data, Set alts) {
            if (Arrays.equals(data.getClientGUID(), guid)) {
                desc = rfd;
                notify();
            }
        }
        
        public synchronized RemoteFileDesc waitForRFD(long timeout) throws Exception {
            if (desc == null) {
                wait(timeout);
            }
            return desc;
        }
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
        if (!xmlCollectionString.equals(""))
            xmlCompressed = LimeXMLUtils.compress(xmlBytes);
        else //there is no XML
            xmlCompressed = DataUtils.EMPTY_BYTE_ARRAY;
        
        return ProviderHacks.getQueryReplyFactory().createQueryReply(guid.bytes(), (byte)1,
                port, address, 0, new Response[] { resp }, GUID.makeGuid(), xmlCompressed, false,
                false, true, true, true, false);
    }   
    
    
}
