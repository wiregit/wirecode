package com.limegroup.gnutella.simpp;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.concurrent.SimpleTimer;
import org.limewire.core.settings.ApplicationSettings;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.FilterSettings;
import org.limewire.core.settings.MessageSettings;
import org.limewire.core.settings.NetworkSettings;
import org.limewire.core.settings.UltrapeerSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.PrivilegedAccessor;
import org.limewire.util.TestUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMStubHelper;

public class SimppManagerTest extends LimeTestCase {
    
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

    private SimppManager simppManager;

    private LifecycleManager lifecycleManager;
    
    private MessageFactory messageFactory;

    private File CERT_FILE_4;

    private File certFile;

    private File ABOVE_MAX_FILE;

    private File BELOW_MIN_FILE;

    private NotifyingSimpleTimer backgroundExecutor = new NotifyingSimpleTimer(); 
    
    public SimppManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(SimppManagerTest.class);
    }

    @Override
    public void setUp() throws Exception {
        setSettings();
    }
    
    public void createSimppManager() throws Exception {
        Injector injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(SimppDataProvider.class).toInstance(new SimppDataProvider() {
                    public byte[] getDefaultData() {
                        return null;
                    }
                    public byte[] getOldUpdateResponse() {
                        return new SimppDataProviderImpl().getOldUpdateResponse();
                    }
                });
                bind(SimppDataVerifier.class).toInstance(new SimppDataVerifierImpl("GCBADNZQQIASYBQHFKDERTRYAQATBAQBD4BIDAIA7V7VHAI5OUJCSUW7JKOC53HE473BDN2SHTXUIAGDDY7YBNSREZUUKXKAEJI7WWJ5RVMPVP6F6W5DB5WLTNKWZV4BHOAB2NDP6JTGBN3LTFIKLJE7T7UAI6YQELBE7O5J277LPRQ37A5VPZ6GVCTBKDYE7OB7NU6FD3BQENKUCNNBNEJS6Z27HLRLMHLSV37SEIBRTHORJAA4OAQVACLWAUEPCURQXTFSSK4YFIXLQQF7AWA46UBIDAIA67Q2BBOWTM655S54VNODNOCXXF4ZJL537I5OVAXZK5GAWPIHQJTVCWKXR25NIWKP4ZYQOEEBQC2ESFTREPUEYKAWCO346CJSRTEKNYJ4CZ5IWVD4RUUOBI5ODYV3HJTVSFXKG7YL7IQTKYXR7NRHUAJEHPGKJ4N6VBIZBCNIQPP6CWXFT4DJFC3GL2AHWVJFMQAUYO76Z5ESUA4BQQAAFAMAAHNFDNZU6UKXDJP5N7NGWAD2YQMOU23C5IRAJHNHHSDJQITAY3BRZGMUONFNOJFR74VMICCOS4UNEPZMDA46ACY5BCGRSRLPGU3XIIXZATSCOL5KFHWGOJZCZUAVFHHQHENYOIJVGFSFULPIXRK2AS45PHNNFCYCDLHZ4SQNFLZN43UIVR4DOO6EYGYP2QYCPLVU2LJXW745S"));
                bind(ScheduledExecutorService.class).annotatedWith(Names.named("backgroundExecutor")).toInstance(backgroundExecutor);
            }
        });
		capabilitiesVMFactory = injector.getInstance(CapabilitiesVMFactory.class);
		connectionServices = injector.getInstance(ConnectionServices.class);
		simppManager = injector.getInstance(SimppManager.class);
		lifecycleManager = injector.getInstance(LifecycleManager.class);
        messageFactory = injector.getInstance(MessageFactory.class);
		
        lifecycleManager.start();
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
    
    public void testNewSignatureWorks() throws Exception {
        createSimppManager();
        
        File file = TestUtils.getResourceInPackage("simpp.xml.ov657_kv4_nv_44_nocert", getClass());
        assertTrue(file.exists());
        
        TestConnection conn = new TestConnection(file, 657, true, true, messageFactory);
        conn.start();
        
        waitForUpdateRun();
        
        assertEquals(657, simppManager.getVersion());
    }

    ////////////////////////////////private methods///////////////////////////
    
    void waitForConnection(TestConnection connection) throws InterruptedException {
        assertTrue(connection.waitForConnection(5, TimeUnit.SECONDS));
    }
    
    void waitForUpdateRun() throws InterruptedException {
        assertTrue(backgroundExecutor.waitForSimppUpdate(5, TimeUnit.SECONDS));   
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
    
    /**
     * Executor that lowers a countdown latch when the SimppManager has
     * received a new message.
     */
    private class NotifyingSimpleTimer extends SimpleTimer {
        
        public NotifyingSimpleTimer() {
            super(true);
        }

        final CountDownLatch latch = new CountDownLatch(1);
        
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
            }
        }
        
        public boolean waitForSimppUpdate(long timeout, TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }
    }
}
