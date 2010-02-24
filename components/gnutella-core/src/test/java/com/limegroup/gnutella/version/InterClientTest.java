package com.limegroup.gnutella.version;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.params.HttpParams;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsNot;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.limewire.concurrent.ScheduledListeningFuture;
import org.limewire.concurrent.SimpleTimer;
import org.limewire.core.settings.UpdateSettings;
import org.limewire.gnutella.tests.ActivityCallbackStub;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.io.GGEP;
import org.limewire.io.IOUtils;
import org.limewire.listener.EventListener;
import org.limewire.util.Base32;
import org.limewire.util.FileUtils;
import org.limewire.util.PrivilegedAccessor;
import org.limewire.util.StringUtils;
import org.limewire.util.TestUtils;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.BlockingConnectionUtils;
import com.limegroup.gnutella.PeerTestCase;
import com.limegroup.gnutella.connection.BlockingConnection;
import com.limegroup.gnutella.http.HttpClientListener;
import com.limegroup.gnutella.http.HttpExecutor;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.vendor.AbstractVendorMessage;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMStubHelper;
import com.limegroup.gnutella.messages.vendor.UpdateRequest;
import com.limegroup.gnutella.messages.vendor.UpdateResponse;
import com.limegroup.gnutella.security.CertificateProvider;
import com.limegroup.gnutella.security.CertificateVerifier;
import com.limegroup.gnutella.security.CertificateVerifierImpl;
import com.limegroup.gnutella.security.DefaultDataProvider;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.util.LimeWireUtils;
import com.limegroup.gnutella.util.MockUtils;

/**
 * Tests to make sure updates are sent, requested, etc...
 */
public class InterClientTest extends PeerTestCase {
    
    private BlockingConnection PEER;
    
    private ActivityCallback myActivityCallback;
    
    @Inject
    private UpdateHandler updateHandler;
    
    @Inject
    private @Update CertificateProvider certificateProvider;
    
    public InterClientTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(InterClientTest.class);
    }    

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
    
    private final Matcher<HttpUriRequest> updateRequest = MockUtils.createUriRequestMatcher("limewire.com/v3/update.def");
    
    private final Matcher<HttpUriRequest> nonUpdateRequest = new IsNot<HttpUriRequest>(updateRequest);
    
    private final Matcher<HttpUriRequest> certificateRequest = MockUtils.createUriRequestMatcher("http://certs.limewire.com/update/update.cert");
    
    private final Matcher<HttpUriRequest> nonCertificateRequest = new IsNot<HttpUriRequest>(certificateRequest);
    
    private UpdateRequest dummy = new UpdateRequest();

    private Module[] modules;

    private NotifyingSimpleTimer backgroundExecutor;

    private File certFile;

    private File versionFile;
    
    private byte[] defaultData;

    private Mockery context;

    private LimeHttpClient limeHttpClient;

    private HttpExecutor httpExecutor;
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
        certFile = new File(_settingsDir, "update.cert");
        versionFile = new File(_settingsDir, "version.xml");
        changeCertFile("update.cert");
        changeVersionFile("test_8.xml");
        
        // suppress timeout http failovers by default
        UpdateSettings.LAST_HTTP_FAILOVER.setValue(System.currentTimeMillis());
        
        myActivityCallback = new ActivityCallbackStub();
        backgroundExecutor = new NotifyingSimpleTimer();
        
        context = new Mockery();
        limeHttpClient = context.mock(LimeHttpClient.class);
        httpExecutor = context.mock(HttpExecutor.class);
        
        context.checking(new Expectations() {{
            ignoring(limeHttpClient).execute(with(nonCertificateRequest));
            will(throwException(new IOException()));
            ignoring(limeHttpClient).releaseConnection(with(any(HttpResponse.class)));
            
            ignoring(httpExecutor).execute(with(nonUpdateRequest),
                    with(any(HttpParams.class)), with(any(HttpClientListener.class)));
            will(MockUtils.failUpload());
            ignoring(httpExecutor).releaseResources(with(any(HttpResponse.class)));
        }});
        
        modules = new Module[] { new AbstractModule() {
            @Override
            public void configure() {
                bind(ActivityCallback.class).toInstance(myActivityCallback);
                bind(ScheduledExecutorService.class).annotatedWith(Names.named("backgroundExecutor")).toInstance(backgroundExecutor);
                bind(UpdateMessageVerifier.class).toInstance(new UpdateMessageVerifierImpl("GCBADNZQQIASYBQHFKDERTRYAQATBAQBD4BIDAIA7V7VHAI5OUJCSUW7JKOC53HE473BDN2SHTXUIAGDDY7YBNSREZUUKXKAEJI7WWJ5RVMPVP6F6W5DB5WLTNKWZV4BHOAB2NDP6JTGBN3LTFIKLJE7T7UAI6YQELBE7O5J277LPRQ37A5VPZ6GVCTBKDYE7OB7NU6FD3BQENKUCNNBNEJS6Z27HLRLMHLSV37SEIBRTHORJAA4OAQVACLWAUEPCURQXTFSSK4YFIXLQQF7AWA46UBIDAIA67Q2BBOWTM655S54VNODNOCXXF4ZJL537I5OVAXZK5GAWPIHQJTVCWKXR25NIWKP4ZYQOEEBQC2ESFTREPUEYKAWCO346CJSRTEKNYJ4CZ5IWVD4RUUOBI5ODYV3HJTVSFXKG7YL7IQTKYXR7NRHUAJEHPGKJ4N6VBIZBCNIQPP6CWXFT4DJFC3GL2AHWVJFMQAUYO76Z5ESUA4BQQAAFAMAAIC5BZX4C463D34VXV74JRSGAVQXZK2FPDHEWT7YSLZ5R6AP6KAD4ODGZPVJ5I3NXRTGDEIYRBZAJ4WHMOLNDCJXHJLJPELPLLB6GUWMO5ZEN26KJD3CFEQRJDIPDAWZIISVZCSRCUJ64KKDO4Q32NKG5SZLPJSQM6HX2THS5PBHWHOVVTBXBJAUXMUUULU6SHYCKFC6EVJLW"));
                bind(CertificateVerifier.class).toInstance(new CertificateVerifierImpl("GCBADOBQQIASYBQHFKDERTRYAQATBAQBD4BIDAIA7V7VHAI5OUJCSUW7JKOC53HE473BDN2SHTXUIAGDDY7YBNSREZUUKXKAEJI7WWJ5RVMPVP6F6W5DB5WLTNKWZV4BHOAB2NDP6JTGBN3LTFIKLJE7T7UAI6YQELBE7O5J277LPRQ37A5VPZ6GVCTBKDYE7OB7NU6FD3BQENKUCNNBNEJS6Z27HLRLMHLSV37SEIBRTHORJAA4OAQVACLWAUEPCURQXTFSSK4YFIXLQQF7AWA46UBIDAIA67Q2BBOWTM655S54VNODNOCXXF4ZJL537I5OVAXZK5GAWPIHQJTVCWKXR25NIWKP4ZYQOEEBQC2ESFTREPUEYKAWCO346CJSRTEKNYJ4CZ5IWVD4RUUOBI5ODYV3HJTVSFXKG7YL7IQTKYXR7NRHUAJEHPGKJ4N6VBIZBCNIQPP6CWXFT4DJFC3GL2AHWVJFMQAUYO76Z5ESUA4BQUAAFAMBADU44D3J45TEMETF4ARTYM3GJNGUEPNZADXXFQ6XBWYWXRLGLIHAEIFPPSE4TXLMBJSD7V3ODNIJJRJ4NPBMNJ3B4DPWMZLGAPUWGVT3RCRKYJQSKYT73DHK7Z2XMPYG2NAA3UFRGTK6FWCLEBNL5XUD2Q7KXFH5XXUKYSTTIGSEBN225Q2CY74ED6I6UDYJP6Y35ORWSQHZONI"));
                bind(DefaultDataProvider.class).annotatedWith(Update.class).toInstance(new UpdateDefaultDataProviderImpl() {
                    @Override
                    public byte[] getDefaultData() {
                        return defaultData;
                    }
                });
                bind(LimeHttpClient.class).toInstance(limeHttpClient);
                bind(HttpExecutor.class).toInstance(httpExecutor);
            }
        }, LimeTestUtils.createModule(this) };
    }
    
    private void createUpdateHandler() throws Exception {
        createInjector(modules);
        PEER = connect(true);
        BlockingConnectionUtils.drain(PEER);
    }
    
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        if(PEER != null)
            PEER.close();
    }
    
    /**
     * Simple test to make sure that if someone sends a CapabilitiesVM
     * with a greater version than we have, that we request the new version.
     * And that we don't request if someone doesn't send it.
     */
    public void testRequestIsSent() throws Exception {
        createUpdateHandler();
        assertEquals(8, updateHandler.getNewVersion());
        assertEquals(4, certificateProvider.get().getKeyVersion());
        
        PEER.send(getCVM(9));
        PEER.flush();
        
        // We should get an UpdateRequest.
        UpdateRequest m = BlockingConnectionUtils.getFirstInstanceOfMessageType(PEER, UpdateRequest.class);
        assertNotNull(m);
                
        // send a lower old update version, should not get request
        PEER.send(getCVM(7));
        PEER.flush();
        
        // we shouldn't get a message, since they said they had 7 and we have 8 already
        m = BlockingConnectionUtils.getFirstInstanceOfMessageType(PEER, UpdateRequest.class);
        assertNull(m);
        
        // Now if they send with 11, we'll request.
        PEER.send(getCVM(11));
        PEER.flush();
        m = BlockingConnectionUtils.getFirstInstanceOfMessageType(PEER, UpdateRequest.class);
        assertNotNull(m);
    }
    
    public void testRequestIsSentIfNewVersionIsAdvertised() throws Exception {
        createUpdateHandler();
        assertEquals(8, updateHandler.getNewVersion());
        assertEquals(4, certificateProvider.get().getKeyVersion());
        
        PEER.send(CapabilitiesVMStubHelper.makeCapabilitiesWithUpdate(9, 9, 4));
        PEER.flush();
        
        // We should get an UpdateRequest.
        UpdateRequest m = BlockingConnectionUtils.getFirstInstanceOfMessageType(PEER, UpdateRequest.class);
        assertNotNull(m);
        
        // send same new version as current, but higher old version, should not trigger request
        PEER.send(CapabilitiesVMStubHelper.makeCapabilitiesWithUpdate(9, 8, 4));
        PEER.flush();
        
        // we shouldn't get a message
        m = BlockingConnectionUtils.getFirstInstanceOfMessageType(PEER, UpdateRequest.class);
        assertNull(m);
        
        // advertise lower new version, but higher key version, should trigger request
        PEER.send(CapabilitiesVMStubHelper.makeCapabilitiesWithUpdate(9, 4, 5));
        PEER.flush();
        
        // We should get an UpdateRequest.
        m = BlockingConnectionUtils.getFirstInstanceOfMessageType(PEER, UpdateRequest.class);
        assertNotNull(m);
        
        // advertise lower new version, but same key version, should not trigger request
        PEER.send(CapabilitiesVMStubHelper.makeCapabilitiesWithUpdate(9, 4, 4));
        PEER.flush();
     
        // we shouldn't get a message
        m = BlockingConnectionUtils.getFirstInstanceOfMessageType(PEER, UpdateRequest.class);
        assertNull(m);

        // advertise higher new version, but lower key version, should not trigger request
        PEER.send(CapabilitiesVMStubHelper.makeCapabilitiesWithUpdate(9, 9, 3));
        PEER.flush();
     
        // we shouldn't get a message
        m = BlockingConnectionUtils.getFirstInstanceOfMessageType(PEER, UpdateRequest.class);
        assertNull(m);
    }

    
    /**
     * Tests that a response is sent in response to a rquest.
     */
    public void testNoResponseIsSent() throws Exception {
        assertTrue(versionFile.delete()); // delete version file
        createUpdateHandler();
        
        // We should get no response, since we have no data to give.
        PEER.send(new UpdateRequest());
        PEER.flush();
        Message m = BlockingConnectionUtils.getFirstInstanceOfMessageType(PEER, UpdateResponse.class);
        assertNull(m);
    }
    
    public void testResponseIsSent() throws Exception {
        createUpdateHandler();
        PEER.send(new UpdateRequest());
        PEER.flush();
        UpdateResponse m = BlockingConnectionUtils.getFirstInstanceOfMessageType(PEER, UpdateResponse.class);
        assertNotNull(m);
        byte[] payload = payload(m);
        GGEP ggep = new GGEP(payload, 0);
        assertEquals(readFile("test_8.xml"), ggep.get("U"));
        assertEquals(1, ggep.getHeaders().size());
    }
    
    /**
     * Tests that valid versions are used.
     */
    public void testReceiveNewerUpdateResponse() throws Exception {
        createUpdateHandler();

        byte[] b = readFile("test_10.xml");
        PEER.send(UpdateResponse.createUpdateResponse(b,dummy));
        PEER.flush();
        
        backgroundExecutor.waitForNetworkDataHandled();
        
        assertEquals(10, updateHandler.getNewVersion());
        
        // Make sure we got a new CapabilitiesVM.
        CapabilitiesVM m = BlockingConnectionUtils.getFirstInstanceOfMessageType(PEER, CapabilitiesVM.class);
        assertNotNull(m);
        
        assertEquals(10, m.supportsUpdate());
        assertEquals(10, m.supportsNewUpdateVersion());
        assertEquals(4, m.supportsUpdateKeyVersion());
    }

        
    /**
     * Tests that older versions are ignored.
     */
    public void testOlderUpdateResponsesAreIgnored() throws Exception {
        changeVersionFile("test_10.xml");
        createUpdateHandler();
        assertEquals(10, updateHandler.getNewVersion());
        
        byte[] b = readFile("test_8.xml");
        PEER.send(UpdateResponse.createUpdateResponse(b,dummy));
        PEER.flush();
        
        backgroundExecutor.waitForNetworkDataHandled();
        
        assertEquals(10, updateHandler.getNewVersion());
    }
    
    /**
     * Test that invalid signatures are ignored.
     */
    public void testInvalidSignaturesIgnored() throws Exception {
        createUpdateHandler();
        
        byte[] b = readFile("test_10.xml");
        b[0] = '0'; // break the sig.
        PEER.send(UpdateResponse.createUpdateResponse(b,dummy));
        PEER.flush();
        
        backgroundExecutor.waitForNetworkDataHandled();
        
        assertEquals(8, updateHandler.getNewVersion());
    }
    
    /**
     * Tests that invalid bytes break verification.
     */
    public void testInvalidBytesIgnored() throws Exception {
        createUpdateHandler();
        
        byte[] b = readFile("test_10.xml");
        b[b.length-1] = '0'; // break the data.
        PEER.send(UpdateResponse.createUpdateResponse(b,dummy));
        PEER.flush();
        
        backgroundExecutor.waitForNetworkDataHandled();
        
        assertEquals(8, updateHandler.getNewVersion());
    }
    
    /**
     * Test invalid XML ignored.
     */
    public void testInvalidXMLIgnored() throws Exception {
        createUpdateHandler();
        
        byte[] b = readFile("invalid-xml-v9.xml");
        PEER.send(UpdateResponse.createUpdateResponse(b,dummy));
        PEER.flush();
        
        backgroundExecutor.waitForNetworkDataHandled();
        
        assertEquals(8, updateHandler.getNewVersion());
    }
    
    /**
     * Tests that no updates are sent to the UI because the 
     * version is too old
     */
    public void testUpdatesNotSentToGui() throws Exception {
        createUpdateHandler();
        
        // add listener, should not receive any events
        HandleUpdate update = new HandleUpdate();
        updateHandler.addListener(update);
        
        byte[] b = readFile("test_10.xml");
        PEER.send(UpdateResponse.createUpdateResponse(b,dummy));
        PEER.flush();
        
        backgroundExecutor.waitForNetworkDataHandled();

        updateHandler.removeListener(update);// remove listener
        
        assertEquals(10, updateHandler.getNewVersion());
        assertNull("Should not recieve update event", update.event);
    }
    
    /**
     * Tests that updates are sent out when versions come in.
     */
    public void testUpdatesSentToGUI() throws Exception {
        createUpdateHandler();
        setVersion("3.0.0");

        // Set the update style to zero to ensure the message is not ignored
        UpdateSettings.UPDATE_STYLE.setValue(0);
        
        //add listener, should receive an update event
        HandleUpdate update = new HandleUpdate();
        updateHandler.addListener(update);
        
        byte[] b = readFile("test_10.xml");
        PEER.send(UpdateResponse.createUpdateResponse(b,dummy));
        PEER.flush();
        
        backgroundExecutor.waitForNetworkDataHandled();

        updateHandler.removeListener(update); //remove listener
        
        assertEquals(10, updateHandler.getNewVersion());
        assertEquals(update.event.getType(), com.limegroup.gnutella.version.UpdateEvent.Type.UPDATE);
    }    
    
    private class HandleUpdate implements EventListener<UpdateEvent> {
        UpdateEvent event = null;

        @Override
        public void handleEvent(UpdateEvent event) {
            this.event = event;
        }
    }
    
    public void testCompressedResponse() throws Exception {
        assertTrue(versionFile.delete());
        createUpdateHandler();
        
        // We should get no response, since we have no data to give.
        UpdateRequestStub request = new UpdateRequestStub(2,true,true);
        PEER.send(new UpdateRequest());
        PEER.flush();
        UpdateResponse m = BlockingConnectionUtils.getFirstInstanceOfMessageType(PEER, UpdateResponse.class);
        assertNull(m);
        
        updateHandler.handleNewData(readFile("test_8.xml"), null);
        
        backgroundExecutor.waitForNetworkDataHandled();
        
        PEER.send(request);
        PEER.flush();
        m = BlockingConnectionUtils.getFirstInstanceOfMessageType(PEER, UpdateResponse.class);
        assertNotNull(m);

        byte [] payload = payload(m);
        GGEP g = new GGEP(payload,0,null);
        assertEquals(readFile("test_8.xml"), g.getBytes("C"));
        assertFalse(g.hasKey("U"));
    }
    
    public void testUncompressedGGEPResponse() throws Exception {
        createUpdateHandler();
        
        UpdateRequestStub request = new UpdateRequestStub(2,true,false);

        PEER.send(request);
        PEER.flush();
        UpdateResponse m = BlockingConnectionUtils.getFirstInstanceOfMessageType(PEER, UpdateResponse.class);
        assertNotNull(m);
        
        byte [] payload = payload(m);
        GGEP g = new GGEP(payload,0,null);
        assertEquals(readFile("test_8.xml"), g.getBytes("U"));
        assertFalse(g.hasKey("C"));
    }
    
    /**
     * tests that a request with higher version w/o GGEP gets responded to properly
     */
    public void testHigherVersionNoGGEP() throws Exception {
        createUpdateHandler();
        
        UpdateRequestStub request = new UpdateRequestStub(UpdateRequest.VERSION+1,false,false);
        PEER.send(request);
        PEER.flush();
        UpdateResponse m = BlockingConnectionUtils.getFirstInstanceOfMessageType(PEER, UpdateResponse.class);
        assertNotNull(m);
        byte [] payload = payload(m);
        GGEP g = new GGEP(payload,0);
        assertEquals(readFile("test_8.xml"), g.getBytes("U"));
        assertFalse(g.hasKey("C"));
    }
    
    /** Tests that requests from older versions get responded to properly. */
    public void testOldRequestGetsMaxVersionNoGGEP() throws Exception {
        createUpdateHandler();
        
        PEER.send(new UpdateRequestStub(1, false, false));
        PEER.flush();
        UpdateResponse m = BlockingConnectionUtils.getFirstInstanceOfMessageType(PEER, UpdateResponse.class);
        assertNotNull(m);
        
        byte[] payload = payload(m);
        assertEquals("I5AVOQKFIZCE4Q2RKFATKVBWKBKVEWSOJRFU6WS2JVIUCR2QGRJESNBVIE3UESKDINIUCSSWIZKE2WCHGZKFMMS2GJAVSTKCGQ2TINSLIRFEQWCRG5KEITL4PQ6HK4DEMF2GKIDJMQ6SEMRRGQ3TIOBTGY2DOIRAORUW2ZLTORQW24B5EIYSEPQKEAQCAPDNONTSAZTSN5WT2IRXGYXDONZOG42SEIDGN5ZD2IRYGYXDQOBOHA2SEIDUN46SEOBWFY4DSLRYGURCA5LSNQ6SE2DUORYDULZPO53XOLTMNFWWK53JOJSS4Y3PNUXXK4DEMF2GKIRAON2HS3DFHURDAIRAN5ZT2ISXNFXGI33XOMRCA5LSNY6SE5LSNY5GE2LUOBZGS3TUHJIEYUCSKRIEET2BKJBE6U2BJNIECTKHKZJTEU2MGU3VGM2HIRGFCLRXIZIEGR2NG43VGSCPKFGVAUSQJU2UGNKMJ5NEKT2EG43EGRK2IQ2E2USBIVGESIRAOVRW63LNMFXGIPJHEISCKIRAF5JSOIDVNZQW2ZJ5EJGGS3LFK5UXEZKXNFXDILRRGYXDMLTFPBSSEIDTNF5GKPJCGQ2TANRSGU3CEPQKEAQCAIBAEA6GYYLOM4QGSZB5E5SW4JZ6BIQCAIBAEAQCAIB4EFNUGRCBKRAVWNBOGE3C4NRAKVJE4XK5HYFCAIBAEAQCAPBPNRQW4ZZ6BIQCAIB4F5WXGZZ6BIQCAIBAEAQDY3LTM4QGM4TPNU6SENBOHAXDCIRAMZXXEPJCGQXDCNROGYRCA5LSNQ6SE2DUORYDULZPO53XOLTMNFWWK53JOJSS4Y3PNUXXK4DEMF2GKIRAMZZGKZJ5EJ2HE5LFEIQG64Z5EJLWS3TEN53XGIRAON2HS3DFHURDIIRAOVZG4PJCOVZG4OTCNF2HA4TJNZ2DUUCMKBJFIUCCJ5AVEQSPKNAUWUCBJVDVMUZSKNGDKN2TGNDUITCRFY3UMUCDI5GTON2TJBHVCTKQKJIE2NKDGVGE6WSFJ5CDONSDIVNEINCNKJAUKTCJEIQHKY3PNVWWC3TEHUTSEJBFEIQC6UZHEB2W4YLNMU6SETDJNVSVO2LSMVLWS3RUFYYTMLRWFZSXQZJCEBZWS6TFHURDINJQGYZDKNRCHYFCAIBAEAQCAPDMMFXGOIDJMQ6SOZLOE47AUIBAEAQCAIB4EFNUGRCBKRAVWCRAEAQCAIBAHR2GCYTMMUQGC3DJM5XD2Y3FNZ2GK4RAOZQWY2LHNY6WGZLOORSXEPR4ORZD4PDUMQ7AUPDDMVXHIZLSHY6GEPSVOJTWK3TUEBGGS3LFK5UXEZJAKNSWG5LSNF2HSICVOBSGC5DFEBAXMYLJNRQWE3DFFY6GE4R6BJIGYZLBONSSAVLQMRQXIZJAJFWW2ZLENFQXIZLMPEXDYYTSHY6GE4R6HQXWEPQKJFTCA5DIMUQHK4DEMF2GKIDEN5SXGIDON52CA53POJVSYIDWNFZWS5B4MJZD4CTIOR2HAORPF53XO5ZONRUW2ZLXNFZGKLTDN5WS6ZDPO5XGY33BMQ6GE4R6EBTG64RAORUGKIDMMF2GK43UEB3GK4TTNFXW4IDPMYQEY2LNMVLWS4TFFY6C6YR6HQXWGZLOORSXEPR4F52GIPR4F52HEPR4F52GCYTMMU7AUIBAEAQCAIC5LU7AUIBAEAQCAIB4F5WGC3THHYFCAIBAEAQCAPBPNVZWOPQKEAQCAIBAEAFCAIBAEAQCAPDNONTSAZTSN5WT2IRUFY4C4MJCEBTG64R5EI2C4MJWFY3CEIDVOJWD2ITIOR2HAORPF53XO5ZONRUW2ZLXNFZGKLTDN5WS65LQMRQXIZJCEBZXI6LMMU6SENBCHYFCAIBAEAQCAPDMMFXGOIDJMQ6SOZLOE47AUIBAEAQCAIB4EFNUGRCBKRAVWCRAEAQCAIBAHR2GCYTMMUQGC3DJM5XD2Y3FNZ2GK4RAOZQWY2LHNY6WGZLOORSXEPR4ORZD4PDUMQ7AUPDDMVXHIZLSHY6GEPSVOJTWK3TUEBGGS3LFK5UXEZJAKNSWG5LSNF2HSICVOBSGC5DFEBAXMYLJNRQWE3DFFY6GE4R6BJIGYZLBONSSAVLQMRQXIZJAJFWW2ZLENFQXIZLMPEXDYYTSHY6GE4R6HQXWEPQKJFTCA5DIMUQHK4DEMF2GKIDEN5SXGIDON52CA53POJVSYIDWNFZWS5B4MJZD4CTIOR2HAORPF53XO5ZONRUW2ZLXNFZGKLTDN5WS6ZDPO5XGY33BMQ6GE4R6EBTG64RAORUGKIDMMF2GK43UEB3GK4TTNFXW4IDPMYQEY2LNMVLWS4TFFY6C6YR6HQXWGZLOORSXEPR4F52GIPR4F52HEPR4F52GCYTMMU7AUIBAEAQCAIC5LU7AUIBAEAQCAIB4F5WGC3THHYFCAIBAEAQCAPBPNVZWOPQKHQXXK4DEMF2GKPQK",
                Base32.encode(payload));
    }
    
    /////////////////////When LimeWire starts///////////////////////////////////

    /**
     * Tests that the default data is loaded if there is no local version.xml
     * file available.
     */
    public void testStartLoadMissingUpdate() throws Exception {
        assertTrue(certFile.delete());
        assertTrue(versionFile.delete());
        defaultData = readFile("update_4_4_4_cert.xml");
        createUpdateHandler();
        assertEquals(4, updateHandler.getNewVersion());
        assertEquals(4, updateHandler.getLatestId());
        assertEquals(4, updateHandler.getKeyVersion());
        assertEquals(StringUtils.toUTF8String(readFile("update.cert")), certificateProvider.get().getCertificateString());
    }
    
    /**
     * Tests that the default data is used if the signature of the local
     * version.xml file is corrupted. 
     */
    public void testStartLoadUpdateBadOldSig() throws Exception {
        byte[] versionXML = readFile("test_8.xml");
        // change signature
        versionXML[0] = (byte)(versionXML[0] + 1);
        FileUtils.verySafeSave(versionFile.getParentFile(), versionFile.getName(), versionXML);
        defaultData = readFile("update_4_4_4_cert.xml");
        createUpdateHandler();
        assertEquals(4, updateHandler.getNewVersion());
        assertEquals(4, updateHandler.getLatestId());
        assertEquals(4, updateHandler.getKeyVersion());
        assertEquals(StringUtils.toUTF8String(readFile("update.cert")), certificateProvider.get().getCertificateString());
    }
    
    /**
     * Tests that certificate is downloaded if missing at start up and local
     * version.xml is accepted. Also ensure that new update messages are accepted
     * in that session later on.
     */
    public void testStartLoadMissingCertAndDownloadOK() throws Exception {
        assertTrue(certFile.delete());
        
        context.checking(new Expectations() {{
            one(limeHttpClient).execute(with(certificateRequest));
            will(uploadCertificate("update.cert"));
        }});
        
        createUpdateHandler();
        
        assertEquals(8, updateHandler.getNewVersion());
        assertEquals(8, updateHandler.getLatestId());
        assertEquals(4, updateHandler.getKeyVersion());
        assertFilesEqual("update.cert", certFile);
        
        // send newer update message
        byte[] b = readFile("test_10.xml");
        PEER.send(UpdateResponse.createUpdateResponse(b,dummy));
        PEER.flush();
        
        backgroundExecutor.waitForNetworkDataHandled();
        
        assertEquals(10, updateHandler.getNewVersion());
        assertEquals(10, updateHandler.getLatestId());
        assertEquals(4, updateHandler.getKeyVersion());

        context.assertIsSatisfied();
    }
    
    /**
     * Tests that only one http certificate request is made per session. If it 
     * fails, the default certificate in the default update data will be used.
     * Update responses with newer key versions and without cert will be ignored.
     * Update responses with newer key version and with cert will be accepted.
     */
    public void testStartLoadMissingCertDownloadFailedIgnoresHigherKeyVersions() throws Exception {
        assertTrue(certFile.delete());
        defaultData = readFile("update_4_4_4_cert.xml");
                
        context.checking(new Expectations() {{
            one(limeHttpClient).execute(with(certificateRequest));
            will(throwException(new IOException()));
        }});
        
        createUpdateHandler();
        assertEquals(4, updateHandler.getNewVersion());
        assertEquals(4, updateHandler.getLatestId());
        assertEquals(4, updateHandler.getKeyVersion());
        assertFilesEqual("update.cert", certFile);
        
        // send update response with newer key version, but no certificate
        PEER.send(UpdateResponse.createUpdateResponse(readFile("update_10_30_20_noCert.xml"), dummy));
        PEER.flush();
        
        backgroundExecutor.waitForNetworkDataHandled();
        
        assertEquals(4, updateHandler.getNewVersion());
        assertEquals(4, updateHandler.getLatestId());
        assertEquals(4, updateHandler.getKeyVersion());
        assertFilesEqual("update.cert", certFile);
        
        context.assertIsSatisfied();
    }
    
    /**
     * Tests that only one http certificate request is made per session. If it 
     * fails, the default certificate in the default update data will be used.
     * Update responses with newer key version and with cert will be accepted.
     */
    public void testStartLoadMissingCertDownloadFailedAcceptHigherCerts() throws Exception {
        assertTrue(certFile.delete());
        defaultData = readFile("update_4_4_4_cert.xml");
                
        context.checking(new Expectations() {{
            one(limeHttpClient).execute(with(certificateRequest));
            will(throwException(new IOException()));
        }});
        
        createUpdateHandler();
        assertEquals(4, updateHandler.getNewVersion());
        assertEquals(4, updateHandler.getLatestId());
        assertEquals(4, updateHandler.getKeyVersion());
        assertFilesEqual("update.cert", certFile);
        
        // send update response with newer key version, but no certificate
        PEER.send(UpdateResponse.createUpdateResponse(readFile("update_10_30_20_Cert.xml"), dummy));
        PEER.flush();
        
        backgroundExecutor.waitForNetworkDataHandled();
        
        assertEquals(10, updateHandler.getLatestId());
        assertEquals(30, updateHandler.getNewVersion());
        assertEquals(20, updateHandler.getKeyVersion());
        assertFilesEqual("slave_20.cert", certFile);
        
        context.assertIsSatisfied();
    }

    /**
     * Tests http download are triggered if there hasn't been a new update
     * message in a month
     */
    public void testStartOldLocalUpdateAndDownloadOK() throws Exception {
        UpdateSettings.LAST_UPDATE_TIMESTAMP.setValue(0);
        UpdateSettings.LAST_HTTP_FAILOVER.setValue(0);
        
        context.checking(new Expectations() {{
            one(httpExecutor).execute(with(updateRequest), with(any(HttpParams.class)), with(any(HttpClientListener.class)));
            will(MockUtils.upload(IOUtils.deflate(readFile("update_10_30_20_Cert.xml"))));
        }});
        
        createUpdateHandler();
        
        backgroundExecutor.waitForHttpUpdate();
        
        assertGreaterThan(0, UpdateSettings.LAST_UPDATE_TIMESTAMP.getValue());
        assertGreaterThan(0, UpdateSettings.LAST_HTTP_FAILOVER.getValue());
        
        context.assertIsSatisfied();
    }
    
    /////////////////////Verify a Update from a peer/////////////////////////////
    
    // reject.   
    public void testKvLocalGreater() throws Exception {
    }
    
    // reject.   
    public void testOvGreaterKvEqualNvequal() throws Exception {
    }    
    
    // reject. 
    public void testBadNewSig() throws Exception {
    }
    
    // reject.   
    public void testKvGreater_CertInUpdateBadCertSig() throws Exception {
    }
    
    // reject. 
    public void testKvGreater_CertInUpdateKvNotEqual() throws Exception {
    }
    
    // reject. 
    public void testKvGreater_CertInUpdateKvEqualBadNewSig() throws Exception {
    }
    
    // accept. New Cert stored
    public void testKvGreater_CertInUpdateKvEqualGoodNewSig() throws Exception {
    }
        
    // reject.   
    public void testKvGreaterCertNotInUpdate_DownloadFailed() throws Exception {
    }
    // accept. New Cert stored.   
    public void testKvGreaterCertNotInUpdate_DownloadOK() throws Exception {
    }
    // reject.   
    public void testKvGreaterCertNotInUpdate_DownloadOKKvUpdateNotEqualCert() throws Exception {
    }
    

    // reject.  
    public void testKvLocalIGIDNetworkIGID() throws Exception {
    }
    
    ////////////////////////////////http download Update ///////////////////////////
    
    // accept
    public void testKvIGIDDownloadFirstTimeOK() throws Exception {
    }
    // accept
    public void testKvIGIDDownloadSecondTimeOK() throws Exception {
    }
        
    ////////////////////////////////private methods///////////////////////////
    private static File locateFile(String fileName) {
        File f = TestUtils.getResourceFile("com/limegroup/gnutella/version/" + fileName);
        assertTrue(f.exists());
        assertTrue(f.isFile());
        return f;
    }
        
    private static byte[] readFile(String fileName) throws Exception {
        return FileUtils.readFileFully(locateFile(fileName));
    }
    
    private static void setVersion(String v) throws Exception {
        PrivilegedAccessor.setValue(LimeWireUtils.class, "testVersion", v);
    }
    
    private static byte[] payload(UpdateResponse m) throws Exception {
        return (byte[])PrivilegedAccessor.invokeMethod(m, "getPayload");
    }
    
    private static Message getCVM(int i) throws Exception {
        return CapabilitiesVMStubHelper.makeCapabilitiesWithUpdate(i);
    }
    
    /* Required for PeerTestCase. */
    @Override
    protected ActivityCallback getActivityCallback() {
        return new ActivityCallbackStub();
    }
    
    private void changeCertFile(String fileName) {
        FileUtils.copy(locateFile(fileName), certFile);
    }
    
    private void changeVersionFile(String fileName) {
        FileUtils.copy(locateFile(fileName), versionFile);
    }

    private static byte[] derivePayload(boolean hasGGEP, boolean requestsCompressed) throws Exception {
        if (!hasGGEP)
            return DataUtils.EMPTY_BYTE_ARRAY;
        
        GGEP g = new GGEP();
        if (requestsCompressed)
            g.put("C");
        else
            g.put("X"); //put something else
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        g.write(baos);
        return baos.toByteArray();
    }
    
    private void assertFilesEqual(String expectedFile, File actualFile) throws Exception {
        byte[] expectedBytes = readFile(expectedFile);
        byte[] actualBytes = FileUtils.readFileFully(actualFile); 
        assertEquals(StringUtils.toUTF8String(expectedBytes), StringUtils.toUTF8String(actualBytes));
    }
    
    private static class UpdateRequestStub extends AbstractVendorMessage {

        public int version;
//        public boolean hasGGEP,requestsCompressed;
        
        public UpdateRequestStub(int version, boolean hasGGEP, boolean requestsCompressed) 
        throws Exception {
            super(F_LIME_VENDOR_ID, F_UPDATE_REQ, version, derivePayload(hasGGEP, requestsCompressed));
            this.version = version;
//            this.hasGGEP = hasGGEP;
//            this.requestsCompressed = requestsCompressed;
        }
        
        @Override
        public int getVersion() {
            return version;
        }

//        public boolean hasGGEP() {
//            return hasGGEP;
//        }
//
//        public boolean requestsCompressed() {
//            return requestsCompressed;
//        }
        
    }
    
    private CustomAction uploadCertificate(final String fileName) {
        return new CustomAction("upload certificate") {
            @Override
            public Object invoke(Invocation invocation) throws Throwable {
                HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
                ByteArrayEntity entity = new ByteArrayEntity(readFile(fileName));
                entity.setContentEncoding("utf-8");
                response.setEntity(entity);
                return response;
            }
        };
    }

    /**
     * Executor that lowers a countdown latch when the SimppManager has
     * received a new message.
     */
    private class NotifyingSimpleTimer extends SimpleTimer {
        
        public NotifyingSimpleTimer() {
            super(true);
        }

        final CountDownLatch handleNetworkDataLatch = new CountDownLatch(1);
        final CountDownLatch handleHttpUpdateLatch = new CountDownLatch(1);
        
        final Map<String, CountDownLatch> latches = ImmutableMap.of("UpdatehandlerImpl$4", handleNetworkDataLatch,
                "UpdateHandlerImpl$RequestHandler$1", handleHttpUpdateLatch);
        
        @Override
        public void execute(Runnable command) {
            super.execute(command);
            for (final Entry<String, CountDownLatch> entry : latches.entrySet()) {
                if (command.getClass().getName().endsWith(entry.getKey())) {
                    super.execute(new Runnable() {
                        @Override
                        public void run() {
                            entry.getValue().countDown();
                        }
                    });
                }
            }
        }
        
        @Override
        public ScheduledListeningFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            // execute http immediately
            if (command.getClass().getName().endsWith("UpdateHandlerImpl$5")) {
                command.run();
                return null;
            } else {
                return super.schedule(command, delay, unit);
            }
        }
        
        public void waitForNetworkDataHandled() throws InterruptedException {
            assertTrue(handleNetworkDataLatch.await(2, TimeUnit.SECONDS));
        }

        public void waitForHttpUpdate() throws InterruptedException {
            assertTrue(handleHttpUpdateLatch.await(4, TimeUnit.SECONDS));
        }
    }
}