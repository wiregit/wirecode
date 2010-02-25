package com.limegroup.gnutella.simpp;

import java.io.File;
import java.io.IOException;
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
import org.limewire.core.settings.ApplicationSettings;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.FilterSettings;
import org.limewire.core.settings.MessageSettings;
import org.limewire.core.settings.NetworkSettings;
import org.limewire.core.settings.UltrapeerSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.io.IOUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.PrivilegedAccessor;
import org.limewire.util.StringUtils;
import org.limewire.util.TestUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.http.HttpClientListener;
import com.limegroup.gnutella.http.HttpExecutor;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMStubHelper;
import com.limegroup.gnutella.security.CertificateProvider;
import com.limegroup.gnutella.security.CertificateVerifier;
import com.limegroup.gnutella.security.CertificateVerifierImpl;
import com.limegroup.gnutella.security.DefaultDataProvider;
import com.limegroup.gnutella.util.MockUtils;

public class SimppManagerTest extends LimeTestCase {
    // tools and keys used to generate simpp.xml files, certificate files, etc are in 
    // tools_and_keys_for_generating_simpps_used_in_SimppManagerTest.tgz which is in
    // the resources folder
    static final int OLD = 1;
    
    static final int MIDDLE = 2;

    static final int NEW = 3;
    
    static final int DEF_SIGNATURE = 4;

    static final int DEF_MESSAGE = 5;

    static final int BAD_XML = 6;
    
    static final int RANDOM_BYTES = 7;

    static final int ABOVE_MAX = 8;

    static final int BELOW_MIN = 9;

    private static File OLD_SIMPP_FILE;
    
    private static File MIDDLE_SIMPP_FILE;

    private static File NEW_SIMPP_FILE;

    private static File DEF_SIG_FILE;
    
    private static File DEF_MESSAGE_FILE;

    private static File BAD_XML_FILE;

    private static File RANDOM_BYTES_FILE;

    static final int PORT = 6346;

    private static File _simppFile;

    private CapabilitiesVMFactory capabilitiesVMFactory;

    private ConnectionServices connectionServices;

    private SimppManagerImpl simppManager;

    private LifecycleManager lifecycleManager;
    
    private MessageFactory messageFactory;

    private File CERT_FILE_4;

    private File certFile;

    private File ABOVE_MAX_FILE;

    private File BELOW_MIN_FILE;

    private NotifyingSimpleTimer backgroundExecutor = new NotifyingSimpleTimer();

    private Mockery context;

    private HttpExecutor httpExecutor;

    private LimeHttpClient limeHttpClient;
    
    private byte[] defaultSimppData = null;

    private CertificateProvider certificateProvider;
    
    private final Matcher<HttpUriRequest> certificateRequest = MockUtils.createUriRequestMatcher("http://certs.limewire.com/simpp/simpp.cert");
    
    private final Matcher<HttpUriRequest> nonCertificateRequest = new IsNot<HttpUriRequest>(certificateRequest);
    
    private final Matcher<HttpUriRequest> simppRequest = MockUtils.createUriRequestMatcher("http://simpp");
    
    private final Matcher<HttpUriRequest> nonSimppRequest = new IsNot<HttpUriRequest>(simppRequest);
    
    public SimppManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(SimppManagerTest.class);
    }

    @Override
    public void setUp() throws Exception {
        context = new Mockery();
        httpExecutor = context.mock(HttpExecutor.class);
        limeHttpClient = context.mock(LimeHttpClient.class);
        
        context.checking(new Expectations() {{
            ignoring(limeHttpClient).execute(with(nonCertificateRequest));
            will(throwException(new IOException()));
            ignoring(limeHttpClient).releaseConnection(with(any(HttpResponse.class)));
            
            ignoring(httpExecutor).execute(with(nonSimppRequest),
                    with(any(HttpParams.class)), with(any(HttpClientListener.class)));
            will(MockUtils.failUpload());
            ignoring(httpExecutor).releaseResources(with(any(HttpResponse.class)));
        }});
        
        setSettings();
    }
    
    public void createSimppManager() throws Exception {
        Injector injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(DefaultDataProvider.class).annotatedWith(Simpp.class).toInstance(new DefaultDataProvider() {
                    public byte[] getDefaultData() {
                        return defaultSimppData;
                    }
                    public byte[] getOldDefaultData() {
                        return new SimppDataProviderImpl().getOldDefaultData();
                    }
                });
                bind(SimppDataVerifier.class).toInstance(new SimppDataVerifierImpl("GCBADNZQQIASYBQHFKDERTRYAQATBAQBD4BIDAIA7V7VHAI5OUJCSUW7JKOC53HE473BDN2SHTXUIAGDDY7YBNSREZUUKXKAEJI7WWJ5RVMPVP6F6W5DB5WLTNKWZV4BHOAB2NDP6JTGBN3LTFIKLJE7T7UAI6YQELBE7O5J277LPRQ37A5VPZ6GVCTBKDYE7OB7NU6FD3BQENKUCNNBNEJS6Z27HLRLMHLSV37SEIBRTHORJAA4OAQVACLWAUEPCURQXTFSSK4YFIXLQQF7AWA46UBIDAIA67Q2BBOWTM655S54VNODNOCXXF4ZJL537I5OVAXZK5GAWPIHQJTVCWKXR25NIWKP4ZYQOEEBQC2ESFTREPUEYKAWCO346CJSRTEKNYJ4CZ5IWVD4RUUOBI5ODYV3HJTVSFXKG7YL7IQTKYXR7NRHUAJEHPGKJ4N6VBIZBCNIQPP6CWXFT4DJFC3GL2AHWVJFMQAUYO76Z5ESUA4BQQAAFAMAAHNFDNZU6UKXDJP5N7NGWAD2YQMOU23C5IRAJHNHHSDJQITAY3BRZGMUONFNOJFR74VMICCOS4UNEPZMDA46ACY5BCGRSRLPGU3XIIXZATSCOL5KFHWGOJZCZUAVFHHQHENYOIJVGFSFULPIXRK2AS45PHNNFCYCDLHZ4SQNFLZN43UIVR4DOO6EYGYP2QYCPLVU2LJXW745S"));
                bind(CertificateVerifier.class).toInstance(new CertificateVerifierImpl("GCBADOBQQIASYBQHFKDERTRYAQATBAQBD4BIDAIA7V7VHAI5OUJCSUW7JKOC53HE473BDN2SHTXUIAGDDY7YBNSREZUUKXKAEJI7WWJ5RVMPVP6F6W5DB5WLTNKWZV4BHOAB2NDP6JTGBN3LTFIKLJE7T7UAI6YQELBE7O5J277LPRQ37A5VPZ6GVCTBKDYE7OB7NU6FD3BQENKUCNNBNEJS6Z27HLRLMHLSV37SEIBRTHORJAA4OAQVACLWAUEPCURQXTFSSK4YFIXLQQF7AWA46UBIDAIA67Q2BBOWTM655S54VNODNOCXXF4ZJL537I5OVAXZK5GAWPIHQJTVCWKXR25NIWKP4ZYQOEEBQC2ESFTREPUEYKAWCO346CJSRTEKNYJ4CZ5IWVD4RUUOBI5ODYV3HJTVSFXKG7YL7IQTKYXR7NRHUAJEHPGKJ4N6VBIZBCNIQPP6CWXFT4DJFC3GL2AHWVJFMQAUYO76Z5ESUA4BQUAAFAMBACNULEAYQO6RSRCIXXFMWETUCS3PF6PSC3ZQAZW6ZJQGXI7NBXGTPWMKZW7YTNOTXWMC7XBVVBMNXZKRT4RXAP265E67VPGE747CMU7TLAMLGKREXDKX3ZO5ODPVDZRGZXFYKLNTSLLHWHRMXJFC67AKWL4KTXHTNVQXU3U2L4ZNF4HRBT3V2MK655WCREAXBDALGSBAMS7JVUY"));
                bind(ScheduledExecutorService.class).annotatedWith(Names.named("backgroundExecutor")).toInstance(backgroundExecutor);
                bind(HttpExecutor.class).toInstance(httpExecutor);
                bind(LimeHttpClient.class).toInstance(limeHttpClient);
            }
        });
		capabilitiesVMFactory = injector.getInstance(CapabilitiesVMFactory.class);
		connectionServices = injector.getInstance(ConnectionServices.class);
		simppManager = (SimppManagerImpl) injector.getInstance(SimppManager.class);
		lifecycleManager = injector.getInstance(LifecycleManager.class);
        messageFactory = injector.getInstance(MessageFactory.class);
        certificateProvider = injector.getInstance(Key.get(CertificateProvider.class, Simpp.class));
		
        lifecycleManager.start();
        
        simppManager.setMaxHttpRequestUpdateDelayForMaxFailover(1);
        simppManager.setMinHttpRequestUpdateDelayForMaxFailover(0);
        simppManager.setSilentPeriodForMaxHttpRequest(0);
    }
    
    @Override
    protected void tearDown() throws Exception {
        connectionServices.disconnect();
        lifecycleManager.shutdown();
    }
    
    private void setSettings() throws Exception {

        OLD_SIMPP_FILE    = TestUtils.getResourceInPackage("oldFile.xml", SimppManagerTest.class);
        MIDDLE_SIMPP_FILE = TestUtils.getResourceInPackage("middleFile.xml", SimppManagerTest.class);
        NEW_SIMPP_FILE    = TestUtils.getResourceInPackage("newFile.xml", SimppManagerTest.class);
        DEF_SIG_FILE      = TestUtils.getResourceInPackage("defSigFile.xml", SimppManagerTest.class);
        DEF_MESSAGE_FILE  = TestUtils.getResourceInPackage("defMessageFile.xml",SimppManagerTest.class);
        BAD_XML_FILE      = TestUtils.getResourceInPackage("badXmlFile.xml", SimppManagerTest.class);
        RANDOM_BYTES_FILE = TestUtils.getResourceInPackage("randFile.xml", SimppManagerTest.class);
        ABOVE_MAX_FILE = TestUtils.getResourceInPackage("aboveMaxFile.xml", SimppManagerTest.class);
        BELOW_MIN_FILE = TestUtils.getResourceInPackage("belowMinFile.xml", SimppManagerTest.class);
        
        CERT_FILE_4 = TestUtils.getResourceInPackage("simpp.cert.4", SimppManagerTest.class);

        assertTrue(OLD_SIMPP_FILE.exists());
        assertTrue(MIDDLE_SIMPP_FILE.exists());
        assertTrue(NEW_SIMPP_FILE.exists());
        assertTrue(DEF_SIG_FILE.exists());
        assertTrue(BAD_XML_FILE.exists());
        assertTrue(RANDOM_BYTES_FILE.exists());
        assertTrue(CERT_FILE_4.exists());
        
        _simppFile = new File(_settingsDir, "simpp.xml");
        certFile = new File(_settingsDir, "simpp.cert");

        //set up the correct simpp version
        //_simppMessageNumber = OLD;
        changeSimppFile(OLD_SIMPP_FILE);
        changeCertFile(CERT_FILE_4);

        if (SimppManagerTestSettings.TEST_UPLOAD_SETTING.getValue() != 
            SimppManagerTestSettings.DEFAULT_SETTING) {
        
            Thread.sleep(2000);
            
            assertEquals("base case did not revert to defaults",
                SimppManagerTestSettings.DEFAULT_SETTING, 
                SimppManagerTestSettings.TEST_UPLOAD_SETTING.getValue());
        }

        FilterSettings.BLACK_LISTED_IP_ADDRESSES.set(
                                                   new String[] {"*.*.*.*"} );
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.set(
                                                   new String[] {"127.*.*.*"});
        
        ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
        NetworkSettings.PORT.setValue(PORT);
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(true);
        UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
        UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(false);
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
        UltrapeerSettings.MAX_LEAVES.setValue(5);
        ConnectionSettings.NUM_CONNECTIONS.setValue(5);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        
        ConnectionSettings.DISABLE_UPNP.setValue(true);
        NetworkSettings.PORT.setValue(PORT);
        
        ConnectionSettings.FORCE_IP_ADDRESS.setValue(true);
        ConnectionSettings.FORCED_IP_ADDRESS_STRING.set("127.0.0.1");
        ConnectionSettings.FORCED_PORT.setValue(PORT);
        
        UltrapeerSettings.NEED_MIN_CONNECT_TIME.setValue(false);
        
        ApplicationSettings.INITIALIZE_SIMPP.setValue(true);
        MessageSettings.REREQUEST_SIGNED_MESSAGE.set(0.0f);
    }
    
    ////////////////////////////////tests/////////////////////////////////////

    public void testOldVersion() throws Exception{
        //Note: we have already set the version to be old in setSettings
        createSimppManager();
        SimppManager man = simppManager;
        assertEquals("problem reading/verifying old version file", 1,
                                                              man.getVersion());
    }
    
    public void testMiddleVersion() throws Exception {
        changeSimppFile(MIDDLE_SIMPP_FILE);
        createSimppManager();
        SimppManager man = simppManager;
        assertEquals("problem reading/verifying middle version file", 2,
                                                             man.getVersion());
    }
    
    public void testNewVersion() throws Exception {
        changeSimppFile(NEW_SIMPP_FILE);
        createSimppManager();
        SimppManager man = simppManager;
        assertEquals("problem reading/verifying new version file", 3,
                                                            man.getVersion());
    }
    
    public void testBadSignatureFails() throws Exception {
        changeSimppFile(DEF_SIG_FILE);
        createSimppManager();
        SimppManager man = simppManager;
        assertEquals("bad signature accepted", 0, man.getVersion());
    }
    
    public void testBadMessageFails() throws Exception {
        changeSimppFile(DEF_MESSAGE_FILE);
        createSimppManager();
        SimppManager man = simppManager;
        assertEquals("tampered message accepted", 0, man.getVersion());
    }
    
    public void testBadXMLFails() throws Exception {
        changeSimppFile(BAD_XML_FILE);
        createSimppManager();
        SimppManager man = simppManager;
        assertEquals("malformed xml accepted", 0, man.getVersion());
    }
    
    public void testRandomBytesFails() throws Exception {
        changeSimppFile(RANDOM_BYTES_FILE);
        createSimppManager();
        SimppManager man = simppManager;
        assertEquals("garbage bytes accepted", 0, man.getVersion());
    }

    public void testOlderSimppNotRequested() throws Exception {
        //1. Set up LimeWire
        changeSimppFile(MIDDLE_SIMPP_FILE);
        createSimppManager();
        //2. Set up the TestConnection to have the old version, and expect to
        //not receive a simpprequest
        TestConnection conn = new TestConnection(OLD_SIMPP_FILE, OLD, false, false, messageFactory);//!expect, !respond
        try {
            conn.start();
        
            //6s = 2s * 3 (timeout in TestConnection == 2s)
            Thread.sleep(6000);//let messages be exchanged, 
            
            //3. let the test run and make sure state is OK, for this test this part
            //is just a formality, the real testing is on the TestConnection in step
            //2 above.
            SimppManager man = simppManager;
            assertEquals("SimppManager should not have updated", MIDDLE, 
                    man.getVersion());
        } finally {
            conn.killConnection();
        }
    }
    
    public void testOlderSimppNotRequestedUnsolicitedAccepted() throws Exception {
        //1. Set up LimeWire 
        changeSimppFile(MIDDLE_SIMPP_FILE);
        createSimppManager();
        
        //2. Set up the TestConnection to advertise same version, not expect a
        //SimppReq, and to send an unsolicited newer SimppResponse
        TestConnection conn = new TestConnection(NEW_SIMPP_FILE, false, true, CapabilitiesVMStubHelper.makeCapabilitiesWithSimpp(OLD), messageFactory);
        conn.start();
        //6s = 2s * 3 (timeout in TestConnection == 2s)
        Thread.sleep(6000);//let messages be exchanged, 
        
        //3. let the test run and make sure state is OK, 
        SimppManager man = simppManager;
        assertEquals("SimppManager should not have updated", NEW, 
                                                              man.getVersion());
        conn.killConnection();
    }

    public void testSameSimppNotRequested() throws Exception {
        //1. Set up LimeWire 
        changeSimppFile(MIDDLE_SIMPP_FILE);
        createSimppManager();

        //2. Set up the TestConnection to advertise same version, not expect a
        //SimppReq, and to send an unsolicited same SimppResponse
        TestConnection conn = new TestConnection(MIDDLE_SIMPP_FILE, MIDDLE, false, true, messageFactory);
        try {
            conn.start();
            //6s = 2s * 3 (timeout in TestConnection == 2s)
            Thread.sleep(6000);//let messages be exchanged, 
            
            //3. let the test run and make sure state is OK, 
            SimppManager man = simppManager;
            assertEquals("SimppManager should not have updated", MIDDLE, 
                    man.getVersion());
        } finally {
            conn.killConnection();
        }
    }
    
    public void testHigherKeyVersionLowerNewVersionIsAccepted() throws Exception {
        changeSimppFile(TestUtils.getResourceInPackage("simpp.xml.Ov11_Kv15_Nv25_NoCert", getClass()));
        changeCertFile(TestUtils.getResourceInPackage("slave.cert.15", getClass()));
        createSimppManager();
        assertVersionNumbers(11, 15, 25);
        TestConnection conn = new TestConnection(TestUtils.getResourceInPackage("simp.xml.Ov10_Kv_19_Nv_10_Cert", getClass()), 10, 19, 10, true, true, messageFactory);//expect, respond
        try {
            conn.start();
            
            waitForUpdateRun();
            
            assertVersionNumbers(10, 19, 10);
        } finally {
            conn.killConnection();
        }
    }
    
    public void testNewSimppAdvOldActualRejected() throws Exception {
        //1. Set up LimeWire 
        changeSimppFile(MIDDLE_SIMPP_FILE);
        createSimppManager();

        //2. Set up the TestConnection to advertise same version, not expect a
        //SimppReq, and to send an unsolicited older SimppResponse
        TestConnection conn = new TestConnection(MIDDLE_SIMPP_FILE, false, true, CapabilitiesVMStubHelper.makeCapabilitiesWithSimpp(OLD), messageFactory);
        try {
            conn.start();
            
            //6s = 2s * 3 (timeout in TestConnection == 2s)
            Thread.sleep(6000);//let messages be exchanged, 
            
            //3. let the test run and make sure state is OK, 
            SimppManager man = simppManager;
            assertEquals("SimppManager should not have updated", MIDDLE, 
                    man.getVersion());
        } finally {
            conn.killConnection();
        }
    }


    public void testNewerSimppRequested() throws Exception {
        //1. Set up limewire correctly
        changeSimppFile(MIDDLE_SIMPP_FILE);
        createSimppManager();

        //2. Set up the test connection, to have the new version, and to expect
        //a simpp request from limewire
        TestConnection conn = new TestConnection(NEW_SIMPP_FILE, NEW, true, true, messageFactory);//expect, respond
        try {
            conn.start();
            
            waitForUpdateRun();
            
            //3. OK. LimeWire should have upgraded now. 
            SimppManager man = simppManager;
            assertEquals("Simpp manager did not update simpp version", 
                    NEW, man.getVersion());
        } finally {
            conn.killConnection();
        }
    }

    public void testTamperedSimppSigRejected() throws Exception {
        //1. Set up limewire correctly
        changeSimppFile(MIDDLE_SIMPP_FILE);
        createSimppManager();

        //2. Set up the test connection, to advertise the new version, and to
        //expect a simpp request from limewire and send a defective signature
        //msg
        TestConnection conn = new TestConnection(DEF_MESSAGE_FILE, true, true, CapabilitiesVMStubHelper.makeCapabilitiesWithSimpp(NEW), messageFactory);
        conn.start();
        
        waitForUpdateRun();

        //3. OK. LimeWire should have upgraded now. 
        SimppManager man = simppManager;
        assertEquals("Simpp manager did not update simpp version", 
                                                     MIDDLE, man.getVersion());
        
    }

    public void testTamperedSimppDataRejected() throws Exception  {
       //1. Set up limewire correctly
        changeSimppFile(MIDDLE_SIMPP_FILE);
        createSimppManager();

        //2. Set up the test connection, to advertise the new version, and to
        //expect a simpp request from limewire and send a defective message msg
        TestConnection conn = new TestConnection(DEF_MESSAGE_FILE, true, true, CapabilitiesVMStubHelper.makeCapabilitiesWithSimpp(NEW), messageFactory);
        conn.start();
        
        waitForUpdateRun();

        //3. OK. LimeWire should have upgraded now. 
        SimppManager man = simppManager;
        assertEquals("Simpp manager did not update simpp version", 
                                                     MIDDLE, man.getVersion());
        
    }

    public void testBadSimppXMLRejected() throws Exception  {
        //1. Set up limewire correctly
        changeSimppFile(MIDDLE_SIMPP_FILE);
        createSimppManager();

        //2. Set up the test connection, to advertise the new version, and to
        //expect a simpp request from limewire and send a bad_xml msg
        TestConnection conn = new TestConnection(BAD_XML_FILE, true, true, CapabilitiesVMStubHelper.makeCapabilitiesWithSimpp(NEW), messageFactory);
        conn.start();
        
        waitForUpdateRun();

        //3. OK. LimeWire should have upgraded now. 
        SimppManager man = simppManager;
        assertEquals("Simpp manager did not update simpp version", 
                                                     MIDDLE, man.getVersion());

        conn.killConnection();
    }

    public void testGargabeDataRejected() throws Exception {
        //1. Set up limewire correctly
        changeSimppFile(MIDDLE_SIMPP_FILE);
        createSimppManager();

        //2. Set up the test connection, to advertise the new version, and to
        //expect a simpp request from limewire and send a garbage msg
        TestConnection conn = new TestConnection(RANDOM_BYTES_FILE, true, true, CapabilitiesVMStubHelper.makeCapabilitiesWithSimpp(NEW), messageFactory);
        conn.start();
        
        waitForUpdateRun();

        //3. OK. LimeWire should have upgraded now. 
        SimppManager man = simppManager;
        assertEquals("Simpp manager did not update simpp version", 
                                                     MIDDLE, man.getVersion());
    }

    public void testSimppTakesEffect() throws Exception {
        //1. Test that Simpp files read off disk take effect. 
        changeSimppFile(OLD_SIMPP_FILE);
        createSimppManager();

        assertEquals("base case did not revert to defaults",12, 
                     SimppManagerTestSettings.TEST_UPLOAD_SETTING.getValue());
        //2. Test that simpp messages read off the network take effect
        //Get a new message from a connection and make sure the value is changed
        TestConnection conn = new TestConnection(NEW_SIMPP_FILE, NEW, true, true, messageFactory);
        conn.start();

        waitForUpdateRun();

        assertEquals("test_upload setting not changed to simpp value", 15,
                     SimppManagerTestSettings.TEST_UPLOAD_SETTING.getValue());
        
        conn.killConnection();
    }

    public void testSimppSettingObeysMax() throws Exception {
        changeSimppFile(OLD_SIMPP_FILE);
        createSimppManager();
        
        assertEquals("base case did not revert to defaults",12, 
                     SimppManagerTestSettings.TEST_UPLOAD_SETTING.getValue());
        //2. Test that simpp messages read off the network take effect
        //Get a new message from a connection and make sure the value is changed
        TestConnection conn = new TestConnection(ABOVE_MAX_FILE, true, true, CapabilitiesVMStubHelper.makeCapabilitiesWithSimpp(NEW), messageFactory);
        conn.start();
        
        waitForUpdateRun();

        assertEquals("test_upload setting not changed to simpp value",
                     SimppManagerTestSettings.MAX_SETTING,
                     SimppManagerTestSettings.TEST_UPLOAD_SETTING.getValue());
        
        conn.killConnection();
    }

    public void testSimppSettingObeysMin() throws Exception {
        changeSimppFile(OLD_SIMPP_FILE);
        createSimppManager();
        
        assertEquals("base case did not revert to defaults",12, 
               SimppManagerTestSettings.TEST_UPLOAD_SETTING.getValue());
        //2. Test that simpp messages read off the network take effect
        //Get a new message from a connection and make sure the value is changed
        TestConnection conn = new TestConnection(BELOW_MIN_FILE, true, true, CapabilitiesVMStubHelper.makeCapabilitiesWithSimpp(NEW), messageFactory);
        conn.start();
        
        waitForUpdateRun();
        
        assertEquals("test_upload settting didn't obey min value",
               SimppManagerTestSettings.MIN_SETTING,
               SimppManagerTestSettings.TEST_UPLOAD_SETTING.getValue());
        
        conn.killConnection();
    }

    public void testIOXLeavesSimppUnchanged() throws Exception {
        //1. Set up limewire correctly
        changeSimppFile(MIDDLE_SIMPP_FILE);
        createSimppManager();

        //2. Set up the test connection, to have the new version, and to expect
        //a simpp request from limewire, but then close the connection while
        //uploading the simpp message
        TestConnection conn = new TestConnection(NEW_SIMPP_FILE, NEW, true, true, messageFactory);
        conn.setCauseError(true);
        conn.start();

        Thread.sleep(6000);

        SimppManager man = simppManager;
        assertEquals("Simpp manager has wrong simpp version", 
                                                     MIDDLE, man.getVersion());
        conn.killConnection();
    }

    
    /////////////////////When LimeWire starts///////////////////////////////////
    // accept
    public void testStartLoadGoodCertAndGoodSimpp() throws Exception {
        loadCertAndSimpp("slave.cert.15", "simpp.xml.Ov5_Kv_15_Nv25_NoCert");
        createSimppManager();
        assertVersionNumbers(5, 15, 25);
    }   

    // use default simpp values
    public void testStartLoadSimppBadOldSig() throws Exception {
        loadCertAndSimpp("slave.cert.15", "simpp.xml.Ov10_Kv_15_Nv30_badOldSig");
        createSimppManager();
        assertVersionNumbers(0, 15, 0);
    }
    // use default simpp values
    public void testStartLoadGoodCertAndLoadSimppBadNewSig() throws Exception {
        loadCertAndSimpp("slave.cert.15", "simpp.xml.Ov6_Kv15_Nv26_NoCert_badNewSig");
        createSimppManager();
        assertVersionNumbers(0, 15, 0);
    }
    
    
    // download cert from newCertURL, and load and accept simpp
    public void testStartLoadMissingCertAndDownloadOKAndGoodSimpp() throws Exception {
        changeSimppFile(TestUtils.getResourceInPackage("simpp.xml.Ov5_Kv_15_Nv25_NoCert", getClass()));
        assertTrue(certFile.delete());
        
        context.checking(new Expectations() {{
            one(limeHttpClient).execute(with(certificateRequest));
            will(returnValue(createCertificateHttpResponse("slave.cert.15")));
        }});
        
        createSimppManager();
        assertVersionNumbers(5, 15, 25);
        
        context.assertIsSatisfied();
    }
    
    public void testStartLoadMissingSimppUsesDefaultData() throws Exception {
        assertTrue(_simppFile.delete());
        assertTrue(certFile.delete());
        
        File defaultDataFile = TestUtils.getResourceInPackage("simpp.xml.ov656_kv4_nv4_cert", getClass());
        defaultSimppData = FileUtils.readFileFully(defaultDataFile);
        assertNotNull(defaultSimppData);
        
        createSimppManager();
        assertVersionNumbers(656, 4, 4);
        assertTrue(certFile.exists());
    }


    public void testStartSimppGoodLoadBadSigCertAndDownloadFailedUsesDefaultData() throws Exception {
        loadCertAndSimpp("slave.cert.20_badSig", "simpp.xml.Ov10_Kv_20_Nv30_NoCert");
        defaultSimppData = FileUtils.readFileFully(TestUtils.getResourceInPackage("simpp.xml.ov656_kv4_nv4_cert", getClass()));
        assertNotNull(defaultSimppData);
        
        context.checking(new Expectations() {{
            one(limeHttpClient).execute(with(certificateRequest));
            will(throwException(new IOException()));
        }});
        
        createSimppManager();
        assertVersionNumbers(656, 4, 4);
        
        // testing "do not accept simpp in this session"
        File newSimppFile = TestUtils.getResourceInPackage("simpp.xml.Ov10_Kv_20_Nv30_Cert", getClass());        
        TestConnection conn = new TestConnection(newSimppFile, 10, 20, 30, true, true, messageFactory);
        conn.start();
        
        waitForUpdateRun();

        assertVersionNumbers(10, 20, 30);
        context.assertIsSatisfied();
    }
    
    // do not accept simpp in this session
    public void testStartSimppGoodLoadMissingCertAndDownloadFailedUsesDefaultData() throws Exception {
        changeSimppFile(TestUtils.getResourceInPackage("simpp.xml.Ov10_Kv_20_Nv30_NoCert", getClass()));
        assertTrue(certFile.delete());
        defaultSimppData = FileUtils.readFileFully(TestUtils.getResourceInPackage("simpp.xml.ov656_kv4_nv4_cert", getClass()));
        assertNotNull(defaultSimppData);

        context.checking(new Expectations() {{
            one(limeHttpClient).execute(with(certificateRequest));
            will(throwException(new IOException()));
        }});
        
        createSimppManager();
        assertVersionNumbers(656, 4, 4);
        context.assertIsSatisfied();
    }
    
    // do not accept simpp in this session
    public void testStartSimppGoodLoadBadSigCertAndDownloadBadSigCertUsesDefaultData() throws Exception {
        loadCertAndSimpp("slave.cert.20_badSig", "simpp.xml.Ov10_Kv_20_Nv30_NoCert");
        defaultSimppData = FileUtils.readFileFully(TestUtils.getResourceInPackage("simpp.xml.ov656_kv4_nv4_cert", getClass()));
        assertNotNull(defaultSimppData);
        
        context.checking(new Expectations() {{
            one(limeHttpClient).execute(with(certificateRequest));
            will(returnValue(createCertificateHttpResponse("slave.cert.20_badSig")));
        }});

        createSimppManager();
        assertVersionNumbers(656, 4, 4);
        context.assertIsSatisfied();
    }
    
    // default simpp, store good cert, accepting later simpp
    public void testStartSimppBadSigLoadBadSigCertAndDownloadOkUsesDefaultData() throws Exception {
        loadCertAndSimpp("slave.cert.20_badSig", "simpp.xml.Ov10_Kv_20_Nv30_NoCert_badOldSig");
        defaultSimppData = FileUtils.readFileFully(TestUtils.getResourceInPackage("simpp.xml.ov656_kv4_nv4_cert", getClass()));
        assertNotNull(defaultSimppData);
        
        context.checking(new Expectations() {{
            one(limeHttpClient).execute(with(certificateRequest));
            will(returnValue(createCertificateHttpResponse("slave.cert.20")));
        }});
        
        createSimppManager();
        waitForDiskUpdateRun();
        // ensure valid certificate was saved
        assertFilesEqual("simpp.cert.4", certFile);
        // ensure older default simpp was accepted
        assertVersionNumbers(656, 4, 4);
        
        // testing "accept simpp in this session"
        File newSimppFile = TestUtils.getResourceInPackage("simpp.xml.Ov10_Kv_20_Nv30_NoCert", getClass());        
        TestConnection conn = new TestConnection(newSimppFile, 10, 20, 30, true, true, messageFactory);
        conn.start();
        
        waitForUpdateRun();
        assertVersionNumbers(10, 20, 30);
        assertFilesEqual("slave.cert.20", certFile);
        context.assertIsSatisfied();
    }
    
    /////////////////////download a simpp from a peer, or not/////////////////////////////
    
    public void testOldClientsAdvertiseSimppGreater() throws Exception {
        loadCertAndSimpp("slave.cert.15", "simpp.xml.Ov5_Kv_15_Nv25_NoCert");
        createSimppManager();
        assertVersionNumbers(5, 15, 25);
        
        File newSimppFile = TestUtils.getResourceInPackage("simpp.xml.Ov6_Kv15_Nv26_NoCert", getClass());
        TestConnection conn = new TestConnection(newSimppFile, 6, true, true, messageFactory);        
        conn.start();
        
        waitForUpdateRun();
        assertVersionNumbers(6, 15, 26);
        
    }   
    public void testOldClientsAdvertiseSimppNotGreater() throws Exception {
        loadCertAndSimpp("slave.cert.20", "simpp.xml.Ov10_Kv_20_Nv30_NoCert");
        createSimppManager();
        assertVersionNumbers(10, 20, 30);
        
        File newSimppFile = TestUtils.getResourceInPackage("simpp.xml.Ov6_Kv15_Nv26_NoCert", getClass());
        TestConnection conn = new TestConnection(newSimppFile, 6, false, false, messageFactory);        
        conn.start();
        
        Thread.sleep(6000);
        
        // TODO: should not download
        // still have old version numbers
        assertVersionNumbers(10, 20, 30);
    }   
    
    public void testKvEqualNvGreater() throws Exception{
        loadCertAndSimpp("slave.cert.15", "simpp.xml.Ov5_Kv_15_Nv25_NoCert");
        createSimppManager();
        assertVersionNumbers(5, 15, 25);
        
        File newSimppFile = TestUtils.getResourceInPackage("simpp.xml.Ov6_Kv15_Nv26_NoCert", getClass());
        TestConnection conn = new TestConnection(newSimppFile, 6, 15, 26, true, true, messageFactory);        
        conn.start();
        
        waitForUpdateRun();
        assertVersionNumbers(6, 15, 26);
        
    }

    public void testKvEqualNvLess() throws Exception{
        loadCertAndSimpp("slave.cert.15", "simpp.xml.Ov6_Kv15_Nv26_NoCert");
        createSimppManager();
        assertVersionNumbers(6, 15, 26);
        
        File newSimppFile = TestUtils.getResourceInPackage("simpp.xml.Ov5_Kv_15_Nv25_NoCert", getClass());
        TestConnection conn = new TestConnection(newSimppFile, 5, 15, 25, false, false, messageFactory);        
        conn.start();
        
        Thread.sleep(6000);
        
        // TODO: should not download
        // still have old version numbers
        assertVersionNumbers(6, 15, 26);
        
    }
    public void testKvLess() throws Exception{
        loadCertAndSimpp("slave.cert.20", "simpp.xml.Ov10_Kv_20_Nv30_NoCert");
        createSimppManager();
        assertVersionNumbers(10, 20, 30);
        
        File newSimppFile = TestUtils.getResourceInPackage("simpp.xml.Ov5_Kv_15_Nv25_NoCert", getClass());
        TestConnection conn = new TestConnection(newSimppFile, 5, 15, 25, false, false, messageFactory);        
        conn.start();
        
        Thread.sleep(6000);
        
        // TODO: should not download
        // still have old version numbers
        assertVersionNumbers(10, 20, 30);
        
    }
    /////////////////////Verify a simpp from a peer/////////////////////////////
    
    // accept. 
    public void testValidSimpp() throws Exception {
        loadCertAndSimpp("slave.cert.15", "simpp.xml.Ov5_Kv_15_Nv25_NoCert");
        createSimppManager();
        assertEquals(5, simppManager.getVersion());
        
        File newSimppFile = TestUtils.getResourceInPackage("simpp.xml.Ov6_Kv15_Nv26_NoCert", getClass());
        TestConnection conn = new TestConnection(newSimppFile, 6, 15, 26, true, true, messageFactory);        
        conn.start();
        
        waitForUpdateRun();
        assertVersionNumbers(6, 15, 26);
    }
    
    // reject.   
    public void testBadOldSigSimpp() throws Exception {
        loadCertAndSimpp("slave.cert.15", "simpp.xml.Ov5_Kv_15_Nv25_NoCert");
        createSimppManager();
        assertEquals(5, simppManager.getVersion());
        
        File newSimppFile = TestUtils.getResourceInPackage("simpp.xml.Ov10_Kv_15_Nv30_badOldSig", getClass());
        TestConnection conn = new TestConnection(newSimppFile, 10, 15, 30, true, true, messageFactory);        
        conn.start();
        
        waitForUpdateRun();
        assertVersionNumbers(5, 15, 25);
    } 
    
    // reject.   
    public void testKvLocalGreater() throws Exception {
        loadCertAndSimpp("slave.cert.20", "simpp.xml.Ov10_Kv_20_Nv30_NoCert");
        createSimppManager();
        assertEquals(10, simppManager.getVersion());
        
        File newSimppFile = TestUtils.getResourceInPackage("simpp.xml.Ov11_Kv15_Nv31_NoCert", getClass());
        //lie about keyVersion = 25 in capabilityVM
        TestConnection conn = new TestConnection(newSimppFile, 11, 25, 31, true, true, messageFactory);        
        conn.start();
        
        waitForUpdateRun();
        assertVersionNumbers(10, 20, 30);
    }
    
    // reject.   
    public void testKvEqualNvLocalGreater() throws Exception {
        loadCertAndSimpp("slave.cert.15", "simpp.xml.Ov5_Kv_15_Nv25_NoCert");
        createSimppManager();
        assertEquals(5, simppManager.getVersion());
        
        File newSimppFile = TestUtils.getResourceInPackage("simpp.xml.Ov11_Kv15_Nv24_NoCert", getClass());
        //lie about newVersion = 30 in capabilityVM
        TestConnection conn = new TestConnection(newSimppFile, 11, 15, 30, true, true, messageFactory);        
        conn.start();
        
        waitForUpdateRun();
        assertVersionNumbers(5, 15, 25);
    }
    
    // reject.   
    public void testOvGreaterKvEqualNvequal() throws Exception {
        loadCertAndSimpp("slave.cert.15", "simpp.xml.Ov5_Kv_15_Nv25_NoCert");
        createSimppManager();
        assertEquals(5, simppManager.getVersion());
        
        File newSimppFile = TestUtils.getResourceInPackage("simpp.xml.Ov11_Kv15_Nv25_NoCert", getClass());
        //lie about newVersion = 30 in capabilityVM
        TestConnection conn = new TestConnection(newSimppFile, 11, 15, 30, true, true, messageFactory);        
        conn.start();
        
        waitForUpdateRun();
        assertVersionNumbers(5, 15, 25);
    }    
    
    // reject. If already tested, move here for completeness  
    public void testKvEqualNvLocalLessBadNewSig() throws Exception {
        loadCertAndSimpp("slave.cert.15", "simpp.xml.Ov5_Kv_15_Nv25_NoCert");
        createSimppManager();
        assertEquals(5, simppManager.getVersion());
        
        File newSimppFile = TestUtils.getResourceInPackage("simpp.xml.Ov6_Kv15_Nv26_NoCert_badNewSig", getClass());
        TestConnection conn = new TestConnection(newSimppFile, 6, 15, 26, true, true, messageFactory);        
        conn.start();
        
        waitForUpdateRun();        
        assertVersionNumbers(5, 15, 25);
    }
    
    // reject.   
    public void testKvNetwkGreaterNotIGIDCertInSimmpBadCertSig() throws Exception {
        loadCertAndSimpp("slave.cert.15", "simpp.xml.Ov5_Kv_15_Nv25_NoCert");
        createSimppManager();
        assertEquals(5, simppManager.getVersion());
        
        File newSimppFile = TestUtils.getResourceInPackage("simpp.xml.Ov6_Kv20_Nv26_Cert_badCertSig", getClass());
        TestConnection conn = new TestConnection(newSimppFile, 6, 20, 26, true, true, messageFactory);        
        conn.start();
        
        waitForUpdateRun();
        assertVersionNumbers(5, 15, 25);
    }
    
    // reject. 
    public void testKvNetwkGreaterNotIGIDCertInSimmpKvSimppGreaterThanCert() throws Exception {
        loadCertAndSimpp("slave.cert.15", "simpp.xml.Ov5_Kv_15_Nv25_NoCert");
        createSimppManager();
        assertEquals(5, simppManager.getVersion());
        
        File newSimppFile = TestUtils.getResourceInPackage("simpp.xml.Ov6_Kv20_Nv26_Cert_badSimppKv21", getClass());
        TestConnection conn = new TestConnection(newSimppFile, 6, 20, 26, true, true, messageFactory);
        conn.start();
        
        waitForUpdateRun();
        assertVersionNumbers(5, 15, 25);
    }
    
    // reject. 
    public void testKvNetwkGreaterNotIGIDCertInSimmpKvEqualBadNewSig() throws Exception {
        loadCertAndSimpp("slave.cert.15", "simpp.xml.Ov5_Kv_15_Nv25_NoCert");
        createSimppManager();
        assertEquals(5, simppManager.getVersion());
        
        File newSimppFile = TestUtils.getResourceInPackage("simpp.xml.Ov6_Kv20_Nv26_Cert_badNewSig", getClass());
        TestConnection conn = new TestConnection(newSimppFile, 6, 20, 26, true, true, messageFactory);
        conn.start();
        
        waitForUpdateRun();
        assertVersionNumbers(5, 15, 25);
    }
    
    // accept. New Cert stored
    public void testKvNetwkGreaterNotIGIDCertInSimmpKvEqualGoodNewSig() throws Exception {
        loadCertAndSimpp("slave.cert.15", "simpp.xml.Ov5_Kv_15_Nv25_NoCert");
        createSimppManager();
        assertEquals(5, simppManager.getVersion());
        
        File newSimppFile = TestUtils.getResourceInPackage("simpp.xml.Ov10_Kv_20_Nv30_Cert", getClass());
        TestConnection conn = new TestConnection(newSimppFile, 10, 20, 30, true, true, messageFactory);        
        conn.start();
        
        waitForUpdateRun();
        assertVersionNumbers(10, 20, 30);
    }
    
    
    // reject.   
    public void testKvNetwkGreaterNotIGIDCertNotInSimppDownloadFailed() throws Exception {
        loadCertAndSimpp("slave.cert.15", "simpp.xml.Ov5_Kv_15_Nv25_NoCert");
        context.checking(new Expectations() {{
            one(limeHttpClient).execute(with(certificateRequest));
            will(throwException(new IOException()));
        }});
        createSimppManager();
        assertEquals(5, simppManager.getVersion());
        File newSimppFile = TestUtils.getResourceInPackage("simpp.xml.Ov10_Kv_20_Nv30_NoCert", getClass());
        
        TestConnection conn = new TestConnection(newSimppFile, 10, 20, 30, true, true, messageFactory);
        conn.start();
        
        waitForUpdateRun();
        // version numbers should still be old
        assertVersionNumbers(5, 15, 25);
        
        context.assertIsSatisfied();
    }
    // reject.   
    public void testKvNetwkGreaterNotIGIDCertNotInSimppDownloadOKBadCertSig() throws Exception {
        loadCertAndSimpp("slave.cert.15", "simpp.xml.Ov5_Kv_15_Nv25_NoCert");
        createSimppManager();
        waitForDiskUpdateRun();
        assertVersionNumbers(5, 15, 25);

        context.checking(new Expectations() {{
            one(limeHttpClient).execute(with(certificateRequest));
            will(returnValue(createCertificateHttpResponse("slave.cert.20_badSig")));
        }});
        
        File newSimppFile = TestUtils.getResourceInPackage("simpp.xml.Ov10_Kv_20_Nv30_NoCert", getClass());        
        TestConnection conn = new TestConnection(newSimppFile, 10, 20, 30, true, true, messageFactory);
        conn.start();
        
        waitForUpdateRun();
        // version numbers should still be old
        assertVersionNumbers(5, 15, 25);
        
        context.assertIsSatisfied();
        
    }
    // accept. New Cert stored.   
    public void testKvNetwkGreaterNotIGIDCertNotInSimppDownloadOKKvEqualGoodNewSig() throws Exception {
        loadCertAndSimpp("slave.cert.15", "simpp.xml.Ov5_Kv_15_Nv25_NoCert");
        createSimppManager();
        assertEquals(5, simppManager.getVersion());

        context.checking(new Expectations() {{
            one(limeHttpClient).execute(with(certificateRequest));
            will(returnValue(createCertificateHttpResponse("slave.cert.20")));
        }}); 
        
        File newSimppFile = TestUtils.getResourceInPackage("simpp.xml.Ov10_Kv_20_Nv30_NoCert", getClass());        
        TestConnection conn = new TestConnection(newSimppFile, 10, 20, 30, true, true, messageFactory);
        conn.start();
        
        waitForUpdateRun();
        // version numbers updated
        assertVersionNumbers(10, 20, 30);
        
        // ensure valid certificate was saved
        assertEquals(FileUtils.readFileFully(TestUtils.getResourceInPackage("slave.cert.20", getClass())), FileUtils.readFileFully(certFile));
        
        context.assertIsSatisfied();
        
    }
    // reject.   
    public void testKvNetwkGreaterNotIGIDCertNotInSimppDownloadOKKvSimppNotEqualCert() throws Exception {
        loadCertAndSimpp("slave.cert.15", "simpp.xml.Ov5_Kv_15_Nv25_NoCert");
        createSimppManager();
        assertEquals(5, simppManager.getVersion());
        
        context.checking(new Expectations() {{
            one(limeHttpClient).execute(with(certificateRequest));
            will(returnValue(createCertificateHttpResponse("slave.cert.20")));
        }}); 

        File newSimppFile = TestUtils.getResourceInPackage("simpp.xml.Ov10_Kv19_Nv30_noCert", getClass());
        TestConnection conn = new TestConnection(newSimppFile, 10, 19, 30, true, true, messageFactory);
        conn.start();
        
        waitForUpdateRun();
        // version numbers should still be old
        assertVersionNumbers(5,15,25);
        
        context.assertIsSatisfied();
    }
    
    // reject.  
    public void testKvNetwkGreaterIGIDCertDownloadOKBadCertSig() throws Exception {
        loadCertAndSimpp("slave.cert.15", "simpp.xml.Ov5_Kv_15_Nv25_NoCert");
        createSimppManager();
        assertEquals(5, simppManager.getVersion());

        context.checking(new Expectations() {{
            one(limeHttpClient).execute(with(certificateRequest));
            will(returnValue(createCertificateHttpResponse("slave.cert.2147483647_badSig")));
        }}); 
        
        File newSimppFile = TestUtils.getResourceInPackage("simpp.xml.Ov6_Kv2147483647_Nv2147483647_NoCert", getClass());        
        TestConnection conn = new TestConnection(newSimppFile, 6, 2147483647, 2147483647, true, true, messageFactory);
        conn.start();
        
        waitForUpdateRun();
        // version numbers should still be old
        assertVersionNumbers(5, 15, 25);        
        context.assertIsSatisfied();
    }

    // reject.   
    public void testKvNetwkGreaterIGIDCertDownloadOKGoodCertSigKvNotIGID() throws Exception {
        loadCertAndSimpp("slave.cert.15", "simpp.xml.Ov5_Kv_15_Nv25_NoCert");
        createSimppManager();
        assertEquals(5, simppManager.getVersion());

        context.checking(new Expectations() {{
            one(limeHttpClient).execute(with(certificateRequest));
            will(returnValue(createCertificateHttpResponse("slave.cert.20")));
        }}); 
        
        File newSimppFile = TestUtils.getResourceInPackage("simpp.xml.Ov6_Kv2147483647_Nv2147483647_NoCert", getClass());        
        TestConnection conn = new TestConnection(newSimppFile, 6, 2147483647, 2147483647, true, true, messageFactory);
        conn.start();
        
        waitForUpdateRun();
        // version numbers should still be old
        assertVersionNumbers(5, 15, 25);        
        context.assertIsSatisfied();
    }
    
    ////////////////////////////////http download simpp ///////////////////////////
    
    // cert looks good, trigger http download of simpp. 
    // simpp is good too, store simpp and cert
    // accept
    public void testKvIGIDCertDownloadOKSimppDownloadOK() throws Exception {
        loadCertAndSimpp("slave.cert.15", "simpp.xml.Ov5_Kv_15_Nv25_NoCert");
        createSimppManager();
        assertEquals(5, simppManager.getVersion());
        
        context.checking(new Expectations() {{
            one(httpExecutor).execute(with(simppRequest), 
                    with(any(HttpParams.class)), with(any(HttpClientListener.class)));
            will(uploadSimppFile("simpp.xml.Ov7_Kv2147483647_Nv2147483647_Cert"));
        }});
        
        TestConnection connection = new TestConnection(TestUtils.getResourceInPackage("simpp.xml.Ov6_Kv2147483647_Nv2147483647_Cert", getClass()),
                6, 2147483647, 2147483647, true, true, messageFactory);
        connection.start();
        
        waitForUpdateRun();
        
        waitForHttpUpdateRun();
        // version numbers should be IGID
        assertVersionNumbers(7, 2147483647, 2147483647);
        // ensure valid simpp has been saved
        assertEquals(FileUtils.readFileFully(TestUtils.getResourceInPackage("simpp.xml.Ov7_Kv2147483647_Nv2147483647_Cert", getClass())), FileUtils.readFileFully(_simppFile));

        context.assertIsSatisfied();
    }
            
    // cert looks good, trigger http download of simpp, but failed. 
    // reject simpp, cert is not stored, it will be store only after http simpp gets accepted
    public void testKvIGIDCertDownloadOKSimppDownloadFail() throws Exception {
        loadCertAndSimpp("slave.cert.15", "simpp.xml.Ov5_Kv_15_Nv25_NoCert");
        createSimppManager();
        assertEquals(5, simppManager.getVersion());

        context.checking(new Expectations() {{
            one(httpExecutor).execute(with(simppRequest), 
                    with(any(HttpParams.class)), with(any(HttpClientListener.class)));
            will(MockUtils.failUpload());
        }});
        
        TestConnection connection = new TestConnection(TestUtils.getResourceInPackage("simpp.xml.Ov6_Kv2147483647_Nv2147483647_Cert", getClass()),
                6, 2147483647, 2147483647, true, true, messageFactory);
        connection.start();
        
        waitForUpdateRun();
        // version numbers should still be old
        assertVersionNumbers(5, 15, 25);
        assertFilesEqual("slave.cert.15", certFile);
        assertEquals(15, certificateProvider.get().getKeyVersion());

        context.assertIsSatisfied();
    }
    
    // reject simpp, cert is not stored
    public void testKvIGIDCertDownloadOKSimppDownloadOkBadOldSig() throws Exception {
        loadCertAndSimpp("slave.cert.15", "simpp.xml.Ov5_Kv_15_Nv25_NoCert");
        createSimppManager();
        assertEquals(5, simppManager.getVersion());

        context.checking(new Expectations() {{
            one(httpExecutor).execute(with(simppRequest), 
                    with(any(HttpParams.class)), with(any(HttpClientListener.class)));
            will(uploadSimppFile("simpp.xml.Ov6_Kv2147483647_Nv2147483647_Cert_badOldSig"));
        }});
        
        TestConnection connection = new TestConnection(TestUtils.getResourceInPackage("simpp.xml.Ov6_Kv2147483647_Nv2147483647_Cert", getClass()),
                6, 2147483647, 2147483647, true, true, messageFactory);
        connection.start();
        
        waitForUpdateRun();

        // version numbers should still be old
        assertVersionNumbers(5, 15, 25);
        assertEquals(FileUtils.readFileFully(TestUtils.getResourceInPackage("slave.cert.15", getClass())), FileUtils.readFileFully(certFile));
        assertEquals(15, certificateProvider.get().getKeyVersion());

        context.assertIsSatisfied();
    }
    
    // reject simpp, cert is not stored
    public void testKvIGIDCertDownloadOKSimppDownloadOkKvnotIGID() throws Exception {
        loadCertAndSimpp("slave.cert.15", "simpp.xml.Ov5_Kv_15_Nv25_NoCert");
        createSimppManager();
        assertEquals(5, simppManager.getVersion());

        context.checking(new Expectations() {{
            one(httpExecutor).execute(with(simppRequest), 
                    with(any(HttpParams.class)), with(any(HttpClientListener.class)));
            will(uploadSimppFile("simpp.xml.Ov6_Kv20_Nv2147483647_Cert"));
        }});
        
        TestConnection connection = new TestConnection(TestUtils.getResourceInPackage("simpp.xml.Ov6_Kv2147483647_Nv2147483647_Cert", getClass()),
                6, 2147483647, 2147483647, true, true, messageFactory);
        connection.start();
        
        waitForUpdateRun();

        // version numbers should old ones
        assertVersionNumbers(5, 15, 25);
        assertFilesEqual("simpp.xml.Ov5_Kv_15_Nv25_NoCert", _simppFile);

        context.assertIsSatisfied();
    }

    // reject simpp, cert is not stored
    public void testKvIGIDCertDownloadOKSimppDownloadOkNvnotIGID() throws Exception {
        loadCertAndSimpp("slave.cert.15", "simpp.xml.Ov5_Kv_15_Nv25_NoCert");
        createSimppManager();
        assertEquals(5, simppManager.getVersion());

        context.checking(new Expectations() {{
            one(httpExecutor).execute(with(simppRequest), 
                    with(any(HttpParams.class)), with(any(HttpClientListener.class)));
            will(uploadSimppFile("simpp.xml.Ov6_Kv2147483647_Nv30_Cert"));
        }});
        
        TestConnection connection = new TestConnection(TestUtils.getResourceInPackage("simpp.xml.Ov6_Kv2147483647_Nv2147483647_Cert", getClass()),
                6, 2147483647, 2147483647, true, true, messageFactory);
        connection.start();
        
        waitForUpdateRun();

        // version numbers should old ones
        assertVersionNumbers(5, 15, 25);
        assertFilesEqual("simpp.xml.Ov5_Kv_15_Nv25_NoCert", _simppFile);

        context.assertIsSatisfied();
    }

    // reject simpp, cert is not stored
    public void testKvIGIDCertDownloadOKSimppDownloadOkBadNewSig() throws Exception {
        loadCertAndSimpp("slave.cert.15", "simpp.xml.Ov5_Kv_15_Nv25_NoCert");
        createSimppManager();
        assertEquals(5, simppManager.getVersion());

        context.checking(new Expectations() {{
            one(httpExecutor).execute(with(simppRequest), 
                    with(any(HttpParams.class)), with(any(HttpClientListener.class)));
            will(uploadSimppFile("simpp.xml.Ov6_Kv2147483647_Nv2147483647_Cert_badNewSig"));
        }});
        
        TestConnection connection = new TestConnection(TestUtils.getResourceInPackage("simpp.xml.Ov6_Kv2147483647_Nv2147483647_Cert", getClass()),
                6, 2147483647, 2147483647, true, true, messageFactory);
        connection.start();
        
        waitForUpdateRun();

        // version numbers should old ones
        assertVersionNumbers(5, 15, 25);
        assertFilesEqual("simpp.xml.Ov5_Kv_15_Nv25_NoCert", _simppFile);

        context.assertIsSatisfied();
    }
    
    // reject.   
    public void testKvNetwkGreaterIGIDCertDownloadFailed() throws Exception {
        loadCertAndSimpp("slave.cert.15", "simpp.xml.Ov5_Kv_15_Nv25_NoCert");
        createSimppManager();
        assertEquals(5, simppManager.getVersion());

        context.checking(new Expectations() {{
            one(limeHttpClient).execute(with(certificateRequest));
            will(throwException(new IOException()));
        }});
        
        File newSimppFile = TestUtils.getResourceInPackage("simpp.xml.Ov6_Kv2147483647_Nv2147483647_NoCert", getClass());        
        TestConnection conn = new TestConnection(newSimppFile, 6, 2147483647, 2147483647, true, true, messageFactory);
        conn.start();
        
        waitForUpdateRun();
        // version numbers should still be old
        assertVersionNumbers(5, 15, 25);        
        context.assertIsSatisfied();
    }
    
    private void assertFilesEqual(String expectedFile, File actualFile) {
        byte[] expectedBytes = FileUtils.readFileFully(TestUtils.getResourceInPackage(expectedFile, getClass()));
        byte[] actualBytes = FileUtils.readFileFully(actualFile); 
        assertEquals(StringUtils.toUTF8String(expectedBytes), StringUtils.toUTF8String(actualBytes));
    }
    
    ////////////////////////////////private methods///////////////////////////
    private void assertVersionNumbers(int version, int keyVersion, int newVersion){
        assertEquals(version, simppManager.getVersion());
        assertEquals(keyVersion, simppManager.getKeyVersion());
        assertEquals(newVersion, simppManager.getNewVersion());        
    }
    
    private void loadCertAndSimpp(String certFilename, String simppFilename) throws Exception {
        File certFile = TestUtils.getResourceInPackage(certFilename, getClass());
        changeCertFile(certFile);
        File simppFile = TestUtils.getResourceInPackage(simppFilename, getClass());
        changeSimppFile(simppFile);
    }
    
    void waitForConnection(TestConnection connection) throws InterruptedException {
        assertTrue(connection.waitForConnection(5, TimeUnit.SECONDS));
    }
    
    void waitForUpdateRun() throws InterruptedException {
        assertTrue(backgroundExecutor.waitForSimppUpdate(5, TimeUnit.SECONDS));   
    }
    
    void waitForDiskUpdateRun() throws InterruptedException {
        assertTrue(backgroundExecutor.waitForUpdateFromDisk(5, TimeUnit.SECONDS));   
    }
    
    void waitForHttpUpdateRun() throws InterruptedException {
        assertTrue(backgroundExecutor.waitForUpdateFromHttp(5, TimeUnit.SECONDS));   
    }
    
    private void changeSimppFile(File inputFile) throws Exception {        
        FileUtils.copy(inputFile, _simppFile);
        
        PrivilegedAccessor.setValue(SimppManagerImpl.class, "MIN_VERSION", 
                                    new Integer(0));//so we can use 1,2,3
        //reload the SimppManager and Capabilities VM
        if (capabilitiesVMFactory != null) {
            capabilitiesVMFactory.updateCapabilities();
            capabilitiesVMFactory.getCapabilitiesVM();
        }
    }
    
    private void changeCertFile(File inputFile) throws Exception {
        FileUtils.copy(inputFile, certFile);
    }
    
    private byte[] readFile(String fileName) {
        byte[] contents = FileUtils.readFileFully(TestUtils.getResourceInPackage(fileName, getClass()));
        assertNotNull(contents);
        return contents;
    }
    
    private HttpResponse createCertificateHttpResponse(String certFilename) {
        return createHttpResponse(readFile(certFilename));
    }
    
    private HttpResponse createHttpResponse(byte[] payload) {
        final HttpResponse httpResponse = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
        ByteArrayEntity byteArrayEntity = new ByteArrayEntity(payload);
        byteArrayEntity.setContentEncoding("UTF-8");
        httpResponse.setEntity(byteArrayEntity);
        return httpResponse;
    }
    
    private CustomAction uploadSimppFile(final String simppFilename) {
        return new CustomAction("upload simpp file") {
            @Override
            public Object invoke(Invocation invocation) throws Throwable {
                HttpClientListener httpClientListener = (HttpClientListener) invocation.getParameter(2);
                httpClientListener.requestComplete((HttpUriRequest) invocation.getParameter(0), createHttpResponse(IOUtils.deflate(readFile(simppFilename))));
                return null;
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


        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch diskLatch = new CountDownLatch(1);
        final CountDownLatch httpLatch = new CountDownLatch(1);
        
        @Override
        public void execute(Runnable command) {
            super.execute(command);
            if (command.getClass().getName().endsWith("SimppManagerImpl$2")) {
                super.execute(new Runnable() {
                    @Override
                    public void run() {
                        latch.countDown();
                    }
                });
            } else if (command.getClass().getName().endsWith("SimppManagerImpl$1")) {
                super.execute(new Runnable() {
                    @Override
                    public void run() {
                        diskLatch.countDown();
                    }
                });
            }  else if (command.getClass().getName().endsWith("SimppManagerImpl$RequestHandler$1")) {
                super.execute(new Runnable() {
                    @Override
                    public void run() {
                        httpLatch.countDown();
                    }
                });
            }
        }

        @Override
        public ScheduledListeningFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            // execute http immediately
            if (command.getClass().getName().contains("SimppManagerImpl")) {
                command.run();
                return null;
            } else {
                return super.schedule(command, delay, unit);
            }
        }

        public boolean waitForSimppUpdate(long timeout, TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }
        
        public boolean waitForUpdateFromDisk(long timeout, TimeUnit unit) throws InterruptedException {
            return diskLatch.await(timeout, unit);
        }
        
        public boolean waitForUpdateFromHttp(int timeout, TimeUnit unit) throws InterruptedException {
            return httpLatch.await(timeout, unit);
        }

    }
}
