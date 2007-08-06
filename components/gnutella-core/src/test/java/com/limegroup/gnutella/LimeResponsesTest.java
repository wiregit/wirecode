package com.limegroup.gnutella;

import java.util.List;

import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactoryImpl;
import com.limegroup.gnutella.messages.StaticMessages;
import com.limegroup.gnutella.routing.PatchTableMessage;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.simpp.SimppListener;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;

import junit.framework.Test;

@SuppressWarnings("all")
public class LimeResponsesTest extends ClientSideTestCase {

    public LimeResponsesTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(LimeResponsesTest.class);
    }
    
    public static ActivityCallback getActivityCallback() {
        return new ActivityCallbackStub();
    }
    
    public static Integer numUPs() {
        return new Integer(1);
    }
    
    private static void doSettings() throws Exception {
        PrivilegedAccessor.setValue(FileManager.class, "QRP_DELAY", 1000);
        SearchSettings.LIME_QRP_ENTRIES.setValue(new String[]{"badger"});
        SearchSettings.LIME_SEARCH_TERMS.setValue(new String[]{"badger"});
        SearchSettings.SEND_LIME_RESPONSES.setValue(1f);
    }
    
    public void testResponse() throws Exception {
        QueryRequest qr = ProviderHacks.getQueryRequestFactory().createNonFirewalledQuery("badger", (byte)1);
        testUP[0].send(qr);
        testUP[0].flush();
        Thread.sleep(1000);
        QueryReply r = getFirstQueryReply(testUP[0]);
        assertNotNull(r);
        QueryReply expected = ProviderHacks.getStaticMessages().getLimeReply();
        assertTrue(expected.getResultsAsList().containsAll(r.getResultsAsList()));
        assertTrue(r.getResultsAsList().containsAll(expected.getResultsAsList()));
        
        // change the words to something else
        SearchSettings.LIME_SEARCH_TERMS.setValue(new String[]{"mushroom"});
        qr = ProviderHacks.getQueryRequestFactory().createNonFirewalledQuery("badger", (byte)1);
        testUP[0].send(qr);
        testUP[0].flush();
        Thread.sleep(1000);
        r = getFirstQueryReply(testUP[0]);
        assertNull(r);
        
        qr = ProviderHacks.getQueryRequestFactory().createNonFirewalledQuery("mushroom", (byte)1);
        testUP[0].send(qr);
        testUP[0].flush();
        Thread.sleep(1000);
        r = getFirstQueryReply(testUP[0]);
        assertNotNull(r);
        assertTrue(expected.getResultsAsList().containsAll(r.getResultsAsList()));
        assertTrue(r.getResultsAsList().containsAll(expected.getResultsAsList()));
        
        // turn off responding completely
        SearchSettings.SEND_LIME_RESPONSES.setValue(0);
        qr = ProviderHacks.getQueryRequestFactory().createNonFirewalledQuery("mushroom", (byte)1);
        testUP[0].send(qr);
        testUP[0].flush();
        Thread.sleep(1000);
        r = getFirstQueryReply(testUP[0]);
        assertNull(r);
    }
    
    public void testQRP() throws Exception {
        ManagedConnection c = ProviderHacks.getConnectionManager().getInitializedConnections().get(0);
        c.incrementNextQRPForwardTime(0);
        PatchTableMessage ptm = getFirstInstanceOfMessageType(testUP[0], PatchTableMessage.class, 22000);
        assertNotNull(ptm);
        QueryRouteTable qrt = new QueryRouteTable();
        qrt.patch(ptm);
        
        // initially, the qrp words should be included
        assertTrue(qrt.contains(ProviderHacks.getQueryRequestFactory().createQuery("badger")));
        
        // change some words, an updated qrp should be sent shortly
        SearchSettings.LIME_QRP_ENTRIES.setValue(new String[]{"mushroom"});
        c.incrementNextQRPForwardTime(0);
        triggerSimppUpdate();
        ptm = getFirstInstanceOfMessageType(testUP[0], PatchTableMessage.class, 12000);
        assertNotNull(ptm);
        qrt.patch(ptm);
        
        // the new word should be there, the old one gone.
        assertTrue(qrt.contains(ProviderHacks.getQueryRequestFactory().createQuery("mushroom")));
        assertFalse(qrt.contains(ProviderHacks.getQueryRequestFactory().createQuery("badger")));
    }
    
    private static int simppVersion;
    private void triggerSimppUpdate() throws Exception {
        List<SimppListener> l = (List<SimppListener>) 
            PrivilegedAccessor.getValue(SimppManager.instance(), "listeners");
        for (SimppListener s : l)
            s.simppUpdated(simppVersion++);
    }
}
