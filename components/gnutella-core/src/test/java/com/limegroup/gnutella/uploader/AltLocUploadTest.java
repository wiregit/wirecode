package com.limegroup.gnutella.uploader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.AssertionFailedError;
import junit.framework.Test;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HttpContext;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.limewire.net.HttpClientManager;
import org.limewire.util.TestUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.HTTPAcceptor;
import com.limegroup.gnutella.HTTPUploadManager;
import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UploadManager;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.altlocs.DirectAltLoc;
import com.limegroup.gnutella.altlocs.PushAltLoc;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.connection.ConnectionCapabilities;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.http.ConstantHTTPHeaderValue;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.vendor.HeadPing;
import com.limegroup.gnutella.messages.vendor.HeadPong;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.util.LimeTestCase;

public class AltLocUploadTest extends LimeTestCase {

    private static final int PORT = 6668;

    /** The file name, plain and encoded. */
    private static final String testDirName = "com/limegroup/gnutella/uploader/data";

    private static final String fileName = "alphabet test file#2.txt";

    private static final String fileNameUrl = "/get/0/alphabet%20test+file%232.txt";

    /** The hash of the file contents. */
    private static final String baseHash = "GLIQY64M7FSXBSQEZY37FIM5QQSA2OUJ";
    
    private static final String hash = "urn:sha1:" + baseHash;

    private static final String hashUrl = "/uri-res/N2R?" + hash;
    
    /**
     * Features for push loc testing.
     */
    private final static Header FALTFeatures = new Header(
            HTTPHeaderName.FEATURES.httpStringValue(),
            ConstantHTTPHeaderValue.PUSH_LOCS_FEATURE.httpStringValue());

    private final static Header FWALTFeatures = new Header(
            HTTPHeaderName.FEATURES.httpStringValue(),
            ConstantHTTPHeaderValue.FWT_PUSH_LOCS_FEATURE.httpStringValue());

    /** The filedesc of the shared file. */
    private FileDesc fd;

    private HttpClient client;

    private TestUploadManager uploadManager;

    private AltLocManager altLocManager;

    private URN hashURN;

    private AlternateLocationFactory alternateLocationFactory;

    private Injector injector;

    private FileManager fileManager;
    
    private LifecycleManager lifecycleManager;

    public AltLocUploadTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(AltLocUploadTest.class);
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }

    @Override
    protected void setUp() throws Exception {
        hashURN = URN.createSHA1Urn(hash);
        
        doSettings();

        File testDir = TestUtils.getResourceFile(testDirName);
        assertTrue("test directory could not be found", testDir.isDirectory());
        File testFile = new File(testDir, fileName);
        assertTrue("test file should exist", testFile.exists());
        File sharedFile = new File(_sharedDir, fileName);
        // we must use a separate copy method
        // because the filename has a # in it which can't be a resource.
        LimeTestUtils.copyFile(testFile, sharedFile);
        assertTrue("should exist", sharedFile.exists());
        assertGreaterThan("should have data", 0, sharedFile.length());

        // initialize services
        injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(UploadManager.class).to(TestUploadManager.class);
                bind(HTTPUploadSessionManager.class).to(TestUploadManager.class);
            }
        });


        lifecycleManager = injector.getInstance(LifecycleManager.class);
        lifecycleManager.start();
        uploadManager = (TestUploadManager) injector.getInstance(UploadManager.class);
        
//        startServices();
        fileManager = injector.getInstance(FileManager.class);
        fileManager.loadSettingsAndWait(2000);
        fd = fileManager.getFileDescForFile(sharedFile);
        assertNotNull(fd);
        
        altLocManager = injector.getInstance(AltLocManager.class);
        //altLocManager.purge();

        alternateLocationFactory = injector.getInstance(AlternateLocationFactory.class);

        HostConfiguration config = new HostConfiguration();
        config.setHost("localhost", PORT);

        client = HttpClientManager.getNewClient();
        client.setHostConfiguration(config);
        client.setHttpConnectionManager(new MultiThreadedHttpConnectionManager());
    }

    @Override
    protected void tearDown() throws Exception {
//        stopServices();
        lifecycleManager.shutdown();
        Thread.sleep(1000);
//        LimeTestUtils.waitForNIO();
    }

    private void doSettings() throws UnknownHostException {
        SharingSettings.ADD_ALTERNATE_FOR_SELF.setValue(false);
        FilterSettings.BLACK_LISTED_IP_ADDRESSES
                .setValue(new String[] { "*.*.*.*" });
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(new String[] {
                "127.*.*.*", InetAddress.getLocalHost().getHostAddress() });
        ConnectionSettings.PORT.setValue(PORT);

        SharingSettings.EXTENSIONS_TO_SHARE.setValue("txt");
        UploadSettings.HARD_MAX_UPLOADS.setValue(10);
        UploadSettings.UPLOADS_PER_PERSON.setValue(10);
        UploadSettings.MAX_PUSHES_PER_HOST.setValue(9999);

        FilterSettings.FILTER_DUPLICATES.setValue(false);

        ConnectionSettings.NUM_CONNECTIONS.setValue(8);
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(true);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
        UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
    }

    public void testFALTNotRequested() throws Exception {
        URN sha1 = URN.createSHA1Urn(hash);
        GUID clientGUID = new GUID(GUID.makeGuid());
        GUID clientGUID2 = new GUID(GUID.makeGuid());

        AlternateLocation direct = alternateLocationFactory.create("1.2.3.4:5", sha1);
        AlternateLocation push = alternateLocationFactory.create(clientGUID
                .toHexString()
                + ";1.2.3.4:5", sha1);
        ((PushAltLoc) push).updateProxies(true);
        PushAltLoc pushFwt = (PushAltLoc) alternateLocationFactory.create(clientGUID2
                .toHexString()
                + ";5555:129.168.9.5;fwt/1.0;1.2.3.4:6", sha1);
        pushFwt.updateProxies(true);

        altLocManager.add(direct, null);
        altLocManager.add(push, null);
        altLocManager.add(pushFwt, null);

        assertEquals(0, ((PushAltLoc) push).supportsFWTVersion());
        assertEquals(1, pushFwt.supportsFWTVersion());
        assertEquals(3, altLocManager.getNumLocs(
                fd.getSHA1Urn()));

        GetMethod method = new GetMethod(fileNameUrl);
        method.addRequestHeader("X-Alt", "");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertNull(method.getResponseHeader("X-Falt"));
        } finally {
            method.releaseConnection();
        }

        assertEquals(1, uploadManager.activeUploads.size());
        HTTPUploader uploader = uploadManager.activeUploads.get(0);
        assertFalse(uploader.getAltLocTracker().wantsFAlts());
        assertEquals(0, uploader.getAltLocTracker().getFwtVersion());
    }

    public void testFALTWhenRequested() throws Exception {
        URN sha1 = URN.createSHA1Urn(hash);
        GUID clientGUID = new GUID(GUID.makeGuid());
        GUID clientGUID2 = new GUID(GUID.makeGuid());

        AlternateLocation direct = alternateLocationFactory.create("1.2.3.4:5", sha1);
        final AlternateLocation push = alternateLocationFactory.create(clientGUID
                .toHexString()
                + ";1.2.3.4:5", sha1);
        ((PushAltLoc) push).updateProxies(true);
        final PushAltLoc pushFwt = (PushAltLoc) alternateLocationFactory.create(
                clientGUID2.toHexString() + ";5555:129.168.9.5;fwt/1.0;1.2.3.4:6", sha1);
        pushFwt.updateProxies(true);

        altLocManager.add(direct, null);
        altLocManager.add(push, null);
        altLocManager.add(pushFwt, null);

        assertEquals(0, ((PushAltLoc) push).supportsFWTVersion());
        assertEquals(1, pushFwt.supportsFWTVersion());
        assertEquals(3, altLocManager.getNumLocs(
                fd.getSHA1Urn()));

        GetMethod method = new GetMethod(fileNameUrl);
        method.addRequestHeader(FALTFeatures);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertNotNull(method.getResponseHeader("X-FAlt"));
            String value = method.getResponseHeader("X-FAlt").getValue();
            assertEquals((push.httpStringValue() + ","
                    + pushFwt.httpStringValue()).length(), value.length());
            assertTrue(value.contains(push.httpStringValue()));
            assertTrue(value.contains(pushFwt.httpStringValue()));
        } finally {
            method.releaseConnection();
        }

        assertEquals(1, uploadManager.activeUploads.size());
        HTTPUploader uploader = uploadManager.activeUploads.get(0);
        assertTrue(uploader.getAltLocTracker().wantsFAlts());
        assertEquals(0, uploader.getAltLocTracker().getFwtVersion());
    }

    public void testFWALTWhenRequested() throws Exception {
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        URN sha1 = URN.createSHA1Urn(hash);
        GUID clientGUID = new GUID(GUID.makeGuid());
        GUID clientGUID2 = new GUID(GUID.makeGuid());

        AlternateLocation direct = alternateLocationFactory.create("1.2.3.4:5", sha1);
        final AlternateLocation push = alternateLocationFactory.create(clientGUID
                .toHexString()
                + ";1.2.3.4:5", sha1);
        ((PushAltLoc) push).updateProxies(true);
        final PushAltLoc pushFwt = (PushAltLoc) alternateLocationFactory.create(
                clientGUID2.toHexString() + ";5555:129.168.9.5;fwt/1.0;1.2.3.4:6", sha1);
        pushFwt.updateProxies(true);

        altLocManager.add(direct, null);
        altLocManager.add(push, null);
        altLocManager.add(pushFwt, null);

        assertEquals(0, ((PushAltLoc) push).supportsFWTVersion());
        assertEquals(1, pushFwt.supportsFWTVersion());
        assertEquals(3, altLocManager.getNumLocs(
                fd.getSHA1Urn()));

        GetMethod method = new GetMethod(fileNameUrl);
        method.addRequestHeader(FWALTFeatures);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertNotNull(method.getResponseHeader("X-FAlt"));
            String value = method.getResponseHeader("X-FAlt").getValue();
            assertEquals(pushFwt.httpStringValue(), value);
        } finally {
            method.releaseConnection();
        }

        assertEquals(1, uploadManager.activeUploads.size());
        HTTPUploader uploader = uploadManager.activeUploads.get(0);
        assertTrue(uploader.getAltLocTracker().wantsFAlts());
        assertEquals((int) HTTPConstants.FWT_TRANSFER_VERSION, uploader
                .getAltLocTracker().getFwtVersion());
    }

    public void testUploaderStoresAllAlts() throws Exception {
        URN sha1 = URN.createSHA1Urn(hash);
        GUID clientGUID = new GUID(GUID.makeGuid());

        AlternateLocation direct = alternateLocationFactory.create("1.2.3.4:5", sha1);
        AlternateLocation push = alternateLocationFactory.create(clientGUID
                .toHexString()
                + ";1.2.3.4:5", sha1);
        ((PushAltLoc) push).updateProxies(true);
        assertEquals(0, altLocManager.getNumLocs(
                fd.getSHA1Urn()));

        GetMethod method = new GetMethod(fileNameUrl);
        method.addRequestHeader("X-Alt", direct.httpStringValue());
        method.addRequestHeader("X-FAlt", push.httpStringValue());
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
        } finally {
            method.releaseConnection();
        }

        assertEquals(2, altLocManager.getNumLocs(
                fd.getSHA1Urn()));
        assertEquals(1, altLocManager.getPushNoFWT(
                fd.getSHA1Urn()).getAltLocsSize());
        assertEquals(1, altLocManager.getDirect(
                fd.getSHA1Urn()).getAltLocsSize());

        assertTrue(altLocManager.getPushNoFWT(fd.getSHA1Urn())
                .contains(push));
        assertTrue(altLocManager.getDirect(fd.getSHA1Urn())
                .contains(direct));
    }

   public void testAlternateLocationAddAndRemove() throws Exception {
        // add a simple marker alt so we know it only contains that
        AlternateLocation al = alternateLocationFactory.create("1.1.1.1:1", hashURN);
        altLocManager.add(al, null);
        GetMethod method = new GetMethod(hashUrl);
        method.addRequestHeader("Connection", "close");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertNotNull(method.getResponseHeader("X-Alt"));
            String value = method.getResponseHeader("X-Alt").getValue();
            assertEquals("1.1.1.1:1", value);
        } finally {
            method.releaseConnection();
        }
        
        // ensure that one removal doesn't stop it.
        altLocManager.remove(al, null);
        method = new GetMethod(hashUrl);
        method.addRequestHeader("Connection", "close");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertNotNull(method.getResponseHeader("X-Alt"));
            String value = method.getResponseHeader("X-Alt").getValue();
            assertEquals("1.1.1.1:1", value);
        } finally {
            method.releaseConnection();
        }

        // add a second one, so we can check to make sure
        // another removal removes the first one.
        AlternateLocation al2 = alternateLocationFactory.create("2.2.2.2:2", hashURN);
        altLocManager.add(al2, null);
        method = new GetMethod(hashUrl);
        method.addRequestHeader("Connection", "close");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertNotNull(method.getResponseHeader("X-Alt"));
            String value = method.getResponseHeader("X-Alt").getValue();
            assertEquals("2.2.2.2:2,1.1.1.1:1".length(), value.length());
            assertTrue(value.contains("1.1.1.1:1"));
            assertTrue(value.contains("2.2.2.2:2"));
        } finally {
            method.releaseConnection();
        }

        // remove the first guy again, should only have loc2 left.
        altLocManager.remove(al, null);
        method = new GetMethod(hashUrl);
        method.addRequestHeader("Connection", "close");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertNotNull(method.getResponseHeader("X-Alt"));
            String value = method.getResponseHeader("X-Alt").getValue();
            assertEquals("2.2.2.2:2", value);
        } finally {
            method.releaseConnection();
        }
    }

    public void testSentHeaderIsUsed() throws Exception {
        // add a simple marker alt so we know it only contains that
        AlternateLocation al = alternateLocationFactory.create("1.1.1.1:1", hashURN);
        altLocManager.add(al, null);
        GetMethod method = new GetMethod(hashUrl);
        method.addRequestHeader("Connection", "close");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertNotNull(method.getResponseHeader("X-Alt"));
            String value = method.getResponseHeader("X-Alt").getValue();
            assertEquals("1.1.1.1:1", value);
        } finally {
            method.releaseConnection();
        }

        // add a header that gives a new location.
        AlternateLocation sendAl = alternateLocationFactory.create("2.2.2.2:2", hashURN);
        method = new GetMethod(hashUrl);
        method.addRequestHeader("X-Alt", sendAl.httpStringValue());
        method.addRequestHeader("Connection", "close");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertNotNull(method.getResponseHeader("X-Alt"));
            String value = method.getResponseHeader("X-Alt").getValue();
            assertEquals("1.1.1.1:1", value);
        } finally {
            method.releaseConnection();
        }

        // make sure the FD has that loc now.
        AlternateLocationCollection<DirectAltLoc> alc = altLocManager
                .getDirect(fd.getSHA1Urn());
        assertEquals("unexpected number of locs", 2, alc.getAltLocsSize());
        List<DirectAltLoc> alts = new LinkedList<DirectAltLoc>();
        for (Iterator<DirectAltLoc> it = alc.iterator(); it.hasNext();) {
            alts.add(it.next());
        }
        assertTrue(alts.contains(al));
        assertTrue(alts.contains(sendAl));

        // make sure a request will give us both locs now.
        method = new GetMethod(hashUrl);
        method.addRequestHeader("Connection", "close");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertNotNull(method.getResponseHeader("X-Alt"));
            String value = method.getResponseHeader("X-Alt").getValue();
            assertEquals("2.2.2.2:2,1.1.1.1:1".length(), value.length());
            assertTrue(value.contains("1.1.1.1:1"));
            assertTrue(value.contains("2.2.2.2:2"));
        } finally {
            method.releaseConnection();
        }

        // demote the location (don't remove)
        method = new GetMethod(hashUrl);
        method.addRequestHeader("X-NAlt", sendAl.httpStringValue());
        method.addRequestHeader("Connection", "close");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertNotNull(method.getResponseHeader("X-Alt"));
            String value = method.getResponseHeader("X-Alt").getValue();
            assertEquals("1.1.1.1:1", value);
        } finally {
            method.releaseConnection();
        }

        // should still have it.
        assertEquals("unexpected number of locs", 2, alc.getAltLocsSize());
        alts = new LinkedList<DirectAltLoc>();
        for (Iterator<DirectAltLoc> it = alc.iterator(); it.hasNext();)
            alts.add(it.next());
        assertTrue(alts.contains(al));
        assertTrue(alts.contains(sendAl));

        // now remove
        method = new GetMethod(hashUrl);
        method.addRequestHeader("X-NAlt", sendAl.httpStringValue());
        method.addRequestHeader("Connection", "close");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertNotNull(method.getResponseHeader("X-Alt"));
            String value = method.getResponseHeader("X-Alt").getValue();
            assertEquals("1.1.1.1:1", value);
        } finally {
            method.releaseConnection();
        }
        assertEquals("unexpected number of locs", 1, alc.getAltLocsSize());
        assertEquals(al, alc.iterator().next());
    }

    public void testMiniNewHeaderIsUsed() throws Exception {
        // Add a simple marker alt so we know it only contains that
        AlternateLocation al = alternateLocationFactory.create("1.1.1.1:1", hashURN);
        altLocManager.add(al, null);
        GetMethod method = new GetMethod(hashUrl);
        method.addRequestHeader("Connection", "close");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertNotNull(method.getResponseHeader("X-Alt"));
            String value = method.getResponseHeader("X-Alt").getValue();
            assertEquals("1.1.1.1:1", value);
        } finally {
            method.releaseConnection();
        }

        // now try a header without a port, should be 6346.
        AlternateLocation sendAl = alternateLocationFactory.create("2.3.4.5:6346", hashURN);
        method = new GetMethod(hashUrl);
        method.addRequestHeader("X-Alt", "2.3.4.5");
        method.addRequestHeader("Connection", "close");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertNotNull(method.getResponseHeader("X-Alt"));
            String value = method.getResponseHeader("X-Alt").getValue();
            assertEquals("1.1.1.1:1", value);
        } finally {
            method.releaseConnection();
        }

        // nake sure the FD has that loc now.
        AlternateLocationCollection<DirectAltLoc> alc = altLocManager
        .getDirect(fd.getSHA1Urn());
        assertEquals("wrong # locs", 2, alc.getAltLocsSize());
        List<DirectAltLoc> alts = new LinkedList<DirectAltLoc>();
        for (Iterator<DirectAltLoc> it = alc.iterator(); it.hasNext();) {
            alts.add(it.next());
        }
        assertTrue(alts.contains(al));
        assertTrue(alts.contains(sendAl));
    }

    /**
     * Tests that headers with multiple values in them are
     * read correctly
     */
    public void testMultipleAlternates() throws Exception {
        // add a simple marker alt so we know it only contains that
        AlternateLocation al = alternateLocationFactory.create("1.1.1.1:1", hashURN);
        altLocManager.add(al, null);
        GetMethod method = new GetMethod(hashUrl);
        method.addRequestHeader("Connection", "close");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertNotNull(method.getResponseHeader("X-Alt"));
            String value = method.getResponseHeader("X-Alt").getValue();
            assertEquals("1.1.1.1:1", value);
        } finally {
            method.releaseConnection();
        }

        // add a header that gives a new location.
        AlternateLocation al1 = alternateLocationFactory.create("1.2.3.1:1", hashURN);
        AlternateLocation al2 = alternateLocationFactory.create("1.2.3.2:2", hashURN);
        AlternateLocation al3 = alternateLocationFactory.create("1.2.3.4:6346", hashURN);
        method = new GetMethod(hashUrl);
        method.addRequestHeader("X-Alt", al1.httpStringValue() + ", " + 
                                al2.httpStringValue() + ", " + al3.httpStringValue());
        method.addRequestHeader("Connection", "close");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertNotNull(method.getResponseHeader("X-Alt"));
            String value = method.getResponseHeader("X-Alt").getValue();
            assertEquals("1.1.1.1:1", value);
        } finally {
            method.releaseConnection();
        }
        
        // make sure the FD has that loc now.
        AlternateLocationCollection<DirectAltLoc> alc = altLocManager
                .getDirect(fd.getSHA1Urn());
        assertEquals("wrong # locs", 4, alc.getAltLocsSize());
        List<DirectAltLoc> alts = new LinkedList<DirectAltLoc>();
        for (Iterator<DirectAltLoc> it = alc.iterator(); it.hasNext();) {
            alts.add(it.next());
        }
        assertTrue(alts.contains(al));
        assertTrue(alts.contains(al1));
        assertTrue(alts.contains(al2));
        assertTrue(alts.contains(al3));

        // demote
        method = new GetMethod(hashUrl);
        method.addRequestHeader("X-NAlt", "1.2.3.1:1, " + al2.httpStringValue() + ", " + al3.httpStringValue());
        method.addRequestHeader("Connection", "close");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
        } finally {
            method.releaseConnection();
        }

        // should still have it
        assertEquals("wrong # locs", 4, alc.getAltLocsSize());

        // remove
        method = new GetMethod(hashUrl);
        method.addRequestHeader("X-NAlt", al1.httpStringValue() + ", 1.2.3.2:2, " + al3.httpStringValue());
        method.addRequestHeader("Connection", "close");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
        } finally {
            method.releaseConnection();
        }
        assertEquals("wrong # locs", 1, alc.getAltLocsSize());
        assertEquals(al, alc.iterator().next());
    }

    /**
     * Tests that when reading the NFAlt header we only remove proxies.
     */
    public void testRemovingNFAlt() throws Exception {
        GUID g = new GUID(GUID.makeGuid());

        URN urn = URN.createSHA1Urn(hash);

        PushAltLoc abc = (PushAltLoc) alternateLocationFactory.create(g.toHexString()
                + ";1.1.1.1:1;2.2.2.2:2;3.3.3.3:3", urn);
        String abcHttp = abc.httpStringValue();

        PushAltLoc bcd = (PushAltLoc) alternateLocationFactory.create(g.toHexString()
                + ";2.2.2.2:2;3.3.3.3:3;4.4.4.4:4", urn);
        bcd.updateProxies(true);

        String bcdHttp = bcd.httpStringValue();

        altLocManager.add(bcd, null);
        assertEquals(1, altLocManager.getNumLocs(urn));

        GetMethod method = new GetMethod(hashUrl);
        method.addRequestHeader("X-NFAlt", abcHttp);
        method.addRequestHeader("Connection", "close");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
        } finally {
            method.releaseConnection();
        }

        // two of the proxies of bcd should be gone
        assertEquals("wrong # locs", 1, altLocManager
                .getPushNoFWT(fd.getSHA1Urn()).getAltLocsSize());
        assertEquals("wrong # proxies", 1, bcd.getPushAddress().getProxies()
                .size());

        // now repeat, sending all three original proxies of bce as NFAlts
        //Thread.sleep(1000);
        method = new GetMethod(hashUrl);
        method.addRequestHeader("X-NFAlt", bcdHttp);
        method.addRequestHeader("Connection", "close");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
        } finally {
            method.releaseConnection();
        }

        // all proxies should be gone, and bcd should be removed from
        // the filedesc
        assertEquals("wrong # locs", 0, altLocManager
                .getPushNoFWT(fd.getSHA1Urn()).getAltLocsSize());
        assertEquals("wrong # proxies", 0, bcd.getPushAddress().getProxies()
                .size());
    }

    // unfortunately we can't test with private addresses
    // because all these connections require that local_is_private
    // is false, which turns off isPrivateAddress checking.
    public void testInvalidAltsAreIgnored() throws Exception {
        // add a simple marker alt so we know it only contains that
        AlternateLocation al = alternateLocationFactory.create("1.1.1.1:1", hashURN);
        altLocManager.add(al, null);
        GetMethod method = new GetMethod(hashUrl);
        method.addRequestHeader("Connection", "close");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertNotNull(method.getResponseHeader("X-Alt"));
            String value = method.getResponseHeader("X-Alt").getValue();
            assertEquals("1.1.1.1:1", value);
        } finally {
            method.releaseConnection();
        }

        // add an invalid alt
        String invalidAddr = "http://0.0.0.0:6346/uri-res/N2R?" + hash;
        method = new GetMethod(hashUrl);
        method.addRequestHeader("X-Alt", invalidAddr);
        method.addRequestHeader("Connection", "close");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertNotNull(method.getResponseHeader("X-Alt"));
            String value = method.getResponseHeader("X-Alt").getValue();
            assertEquals("1.1.1.1:1", value);
        } finally {
            method.releaseConnection();
        }
        // FD should still only have 1
        AlternateLocationCollection alc = altLocManager
                .getDirect(fd.getSHA1Urn());
        assertEquals("wrong # locs: " + alc, 1, alc.getAltLocsSize());
        assertEquals(al, alc.iterator().next());

        invalidAddr = "http://255.255.255.255:6346/uri-res/N2R?" + hash;
        method = new GetMethod(hashUrl);
        method.addRequestHeader("X-Alt", invalidAddr);
        method.addRequestHeader("Connection", "close");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertNotNull(method.getResponseHeader("X-Alt"));
            String value = method.getResponseHeader("X-Alt").getValue();
            assertEquals("1.1.1.1:1", value);
        } finally {
            method.releaseConnection();
        }
        // FD should still only have 1
        assertEquals("wrong # locs: " + alc, 1, alc.getAltLocsSize());
        assertEquals(al, alc.iterator().next());

        // add an invalid port
        String invalidPort = "http://1.2.3.4:0/uri-res/N2R?" + hash;
        method = new GetMethod(hashUrl);
        method.addRequestHeader("X-Alt", invalidPort);
        method.addRequestHeader("Connection", "close");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertNotNull(method.getResponseHeader("X-Alt"));
            String value = method.getResponseHeader("X-Alt").getValue();
            assertEquals("1.1.1.1:1", value);
        } finally {
            method.releaseConnection();
        }
        // FD should still only have 1
        assertEquals("wrong # locs: " + alc, 1, alc.getAltLocsSize());
        assertEquals(al, alc.iterator().next());

        invalidPort = "http://1.2.3.4:-2/uri-res/N2R?" + hash;
        method = new GetMethod(hashUrl);
        method.addRequestHeader("X-Alt", invalidPort);
        method.addRequestHeader("Connection", "close");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertNotNull(method.getResponseHeader("X-Alt"));
            String value = method.getResponseHeader("X-Alt").getValue();
            assertEquals("1.1.1.1:1", value);
        } finally {
            method.releaseConnection();
        }
        // FD should still only have 1
        assertEquals("wrong # locs: " + alc, 1, alc.getAltLocsSize());
        assertEquals(al, alc.iterator().next());
    }

    public void test10AltsAreSent() throws Exception {
        // add a simple marker alt so we know it only contains that
        AlternateLocation al = alternateLocationFactory.create("1.1.1.1:1", hashURN);
        altLocManager.add(al, null);
        GetMethod method = new GetMethod(hashUrl);
        method.setHttp11(false);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertNotNull(method.getResponseHeader("X-Alt"));
            String value = method.getResponseHeader("X-Alt").getValue();
            assertEquals("1.1.1.1:1", value);
        } finally {
            method.releaseConnection();
        }

        for (int i = 0; i < 20; i++) {
            altLocManager.add(
                    alternateLocationFactory.create("1.1.1." + i + ":6346", hashURN), null);
        }
        assertEquals(21, altLocManager.getDirect(
                fd.getSHA1Urn()).getAltLocsSize());

        String pre = "1.1.1.";
        String post = "";
        String comma = ", ";
        // note that this value can change depending on iterators,
        // so this is a very flaky test.
        String required = pre + 16 + post + comma + pre + 13 + post
                + comma + pre + 10 + post + comma + pre + 15 + post + comma
                + pre + 12 + post + comma + pre + 1 + post + comma + pre + 14
                + post + comma + pre + 11 + post + comma + pre + 0 + post
                + comma + pre + "1:1";
        method = new GetMethod(hashUrl);
        method.setHttp11(false);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertNotNull(method.getResponseHeader("X-Alt"));
            assertHeaderEquals(required, method.getResponseHeader("X-Alt").getValue());
        } finally {
            method.releaseConnection();
        }
    }

    private void assertHeaderEquals(String required, String value) {
        String[] requiredList = required.replace(" ", "").split(",");
        String[] valueList = value.replace(" ", "").split(",");
        assertGreaterThan(0, requiredList.length);
        assertGreaterThan(0, valueList.length);
        Arrays.sort(requiredList);
        Arrays.sort(valueList);
        assertEquals("Header value did not match", requiredList, valueList);
    }

    public void testAltsExpire() throws Exception {
        UploadSettings.LEGACY_EXPIRATION_DAMPER.setValue((float) Math.E - 0.2f);
        // test that an altloc will expire if given out too often
        AlternateLocation al = alternateLocationFactory.create("1.1.1.1:1", hashURN);
        assertTrue(al.canBeSent(AlternateLocation.MESH_LEGACY));
        altLocManager.add(al, null);

        // send it out several times
        int i = 0;
        try {
            for (i = 0; i < 10; i++) {
                GetMethod method = new GetMethod(hashUrl);
                method.addRequestHeader("Connection", "close");
                try {
                    int response = client.executeMethod(method);
                    assertEquals(HttpStatus.SC_OK, response);
                    assertNotNull(method.getResponseHeader("X-Alt"));
                    String value = method.getResponseHeader("X-Alt").getValue();
                    assertEquals("1.1.1.1:1", value);
                } finally {
                    method.releaseConnection();
                }
            }
            fail("altloc didn't expire");
        } catch (AssertionFailedError expected) {
        }
        assertLessThan(10, i);
        assertFalse(al.canBeSent(AlternateLocation.MESH_LEGACY));

        // now add the altloc again, it will be reset
        altLocManager.add(al, null);
        assertTrue(al.canBeSent(AlternateLocation.MESH_LEGACY));
    }

    /**
     * tests that when an altloc has expired from all the meshes it is removed.
     */
    public void testExpiredAltsRemoved() throws Exception {
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(new String[] { "*.*.*.*" });
        injector.getInstance(IPFilter.class).refreshHosts();
        // set the expiration values to the bare minimum
        UploadSettings.LEGACY_BIAS.setValue(0f);
        UploadSettings.PING_BIAS.setValue(0f);
        UploadSettings.RESPONSE_BIAS.setValue(0f);

        // create an altloc
        AlternateLocation al = alternateLocationFactory.create("1.1.1.1:1", hashURN);
        assertTrue(al.canBeSent(AlternateLocation.MESH_LEGACY));
        assertTrue(al.canBeSent(AlternateLocation.MESH_PING));
        assertTrue(al.canBeSent(AlternateLocation.MESH_RESPONSE));
        altLocManager.add(al, null);

        // drain the meshes in various orders
        drainLegacy();
        drainPing();
        drainResponse();
        assertFalse(altLocManager
                .hasAltlocs(al.getSHA1Urn()));

        // and re-add the altloc
        al = alternateLocationFactory.create("1.1.1.1:1", hashURN);
        altLocManager.add(al, null);

        // repeat
        drainResponse();
        drainLegacy();
        drainPing();
        assertFalse(altLocManager
                .hasAltlocs(al.getSHA1Urn()));

        al = alternateLocationFactory.create("1.1.1.1:1", hashURN);
        altLocManager.add(al, null);

        // repeat 2
        drainPing();
        drainResponse();
        drainLegacy();
        assertFalse(altLocManager
                .hasAltlocs(al.getSHA1Urn()));

        UploadSettings.LEGACY_BIAS.revertToDefault();
        UploadSettings.PING_BIAS.revertToDefault();
        UploadSettings.RESPONSE_BIAS.revertToDefault();
    }

    private void drainLegacy() throws Exception {
        int i = 0;
        try {
            for (; i < 20; i++) {
                GetMethod method = new GetMethod(hashUrl);
                method.addRequestHeader("Connection", "close");
                try {
                    int response = client.executeMethod(method);
                    assertEquals(HttpStatus.SC_OK, response);
                    assertNotNull(method.getResponseHeader("X-Alt"));
                    String value = method.getResponseHeader("X-Alt").getValue();
                    assertEquals("1.1.1.1:1", value);
                } finally {
                    method.releaseConnection();
                }
            }
            fail("altloc didn't expire");
        } catch (AssertionFailedError expected) {
        }
        assertGreaterThan(1, i);
        assertLessThan(20, i);
    }

    private void drainPing() throws Exception {
        MessageFactory messageFactory = injector.getInstance(MessageFactory.class);
        
        HeadPing ping = new HeadPing(new GUID(GUID.makeGuid()),
                fd.getSHA1Urn(), HeadPing.ALT_LOCS);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ping.write(baos);
        byte[] data = baos.toByteArray();
        DatagramPacket toSend = new DatagramPacket(data, data.length,
                new InetSocketAddress(InetAddress.getLocalHost(), PORT));

        int i = 0;
        for (; i < 20; i++) {
            DatagramSocket sock = null;
            try {
                sock = new DatagramSocket(10000 + i);
                sock.setSoTimeout(2000);
                sock.send(toSend);
                byte[] recv = new byte[5000];
                DatagramPacket rcv = new DatagramPacket(recv, recv.length);
                sock.receive(rcv);
                ByteArrayInputStream bais = new ByteArrayInputStream(recv, 0, rcv.getLength());
                HeadPong pong = (HeadPong) messageFactory.read(bais, Network.TCP);
                if (pong.getAltLocs() == null || pong.getAltLocs().isEmpty())
                    break;
            } catch (IOException iox) {
                fail("didn't get response "+i,iox);
            } finally {
                if (sock != null)
                    sock.close();
            }
        }

        assertGreaterThan(1, i);
        assertLessThan(20, i);
    }

    private void drainResponse() throws Exception {
        MessageFactory messageFactory = injector.getInstance(MessageFactory.class);
        MessageRouter messageRouter = injector.getInstance(MessageRouter.class);
        
        FilterSettings.FILTER_HASH_QUERIES.setValue(false); // easier with hash
        
        Mockery mockery = new Mockery();
        final RoutedConnection handler = mockery.mock(RoutedConnection.class);
        final ConnectionCapabilities capabilities = mockery.mock(ConnectionCapabilities.class);

        assertTrue(altLocManager.hasAltlocs(fd.getSHA1Urn()));
        int i = 0;
        for (; i < 20; i++) {
            
            final int iteration = i; // just for documentation
            
            final QueryRequest request = mockery.mock(QueryRequest.class);
            final Set<URN> urns = new HashSet<URN>();
            urns.add(fd.getSHA1Urn());
            final AtomicReference<QueryReply> replyRef = new AtomicReference<QueryReply>(null);
            final Action checkAlts = new Action() {

                public void describeTo(Description description) {
                    description.appendText("checks if query reply has altlocs at iteration "+iteration);
                }

                public Object invoke(Invocation invocation) throws Throwable {
                    QueryReply reply = (QueryReply) invocation.getParameter(0);
                    replyRef.set(reply);
                    return null;
                }
            };
            mockery.checking(new Expectations(){{
                one(handler).handleQueryReply(with(Matchers.notNullValue(QueryReply.class)), with(Matchers.nullValue(ReplyHandler.class)));
                will(checkAlts);
                
                // various stubbed out methods
                allowing(handler).isOpen();
                will(returnValue(true));
                allowing(handler).getConnectionCapabilities();
                will(returnValue(capabilities));
                allowing(capabilities).isOldLimeWire();
                will(returnValue(false));
                allowing(handler).isPersonalSpam(request);
                will(returnValue(false));
                allowing(handler).isSupernodeClientConnection();
                will(returnValue(false));
                allowing(handler).isGoodUltrapeer();
                will(returnValue(false));
                allowing(request).getNetwork();
                will(returnValue(Network.TCP));
                
                // some request-specific conditions
                one(request).hop();
                atLeast(1).of(request).getQueryUrns();
                will(returnValue(urns));
                atLeast(1).of(request).getHandlerClass();
                will(returnValue(QueryRequest.class));
                atLeast(1).of(request).getQuery();
                will(returnValue(""));
                atLeast(1).of(request).getGUID(); // its important that the guid be different
                will(returnValue((new GUID()).bytes())); // every iteration
                atLeast(1).of(request).desiresAll();
                will(returnValue(true));

                // stubbed out with default return values
                ignoring(request).getTotalLength();
                ignoring(request).isFirewalledSource();
                ignoring(request).canDoFirewalledTransfer();
                ignoring(request).desiresOutOfBandReplies();
                ignoring(request).isWhatIsNewRequest();
                ignoring(request).getTTL();
                ignoring(request).getHops();
                ignoring(request).getRichQuery();
                ignoring(request).isQueryForLW();
                ignoring(request).getFeatureSelector();
                ignoring(request).isFeatureQuery();
                ignoring(request).desiresXMLResponses();
                ignoring(request).isMulticast();
                ignoring(request).getLength();
                ignoring(request).desiresPartialResults();
            }});
            
            messageRouter.handleMessage(request, handler);
            mockery.assertIsSatisfied();
            QueryReply replied = replyRef.get();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            replied.write(baos);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            QueryReply reply = (QueryReply) messageFactory.read(bais, Network.TCP);
            Response resp = reply.getResultsArray()[0];
            if (resp.getLocations().isEmpty())
                break;
        }

        assertGreaterThan(1, i);
        assertLessThan(20, i);
        FilterSettings.FILTER_HASH_QUERIES.revertToDefault();
    }

    public void testChunksGiveDifferentLocs() throws Exception {
        // add a simple marker alt so we know it only contains that
        AlternateLocation al = alternateLocationFactory.create("1.1.1.1:1", hashURN);
        altLocManager.add(al, null);
        GetMethod method = new GetMethod(hashUrl);
        method.addRequestHeader("Connection", "close");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertNotNull(method.getResponseHeader("X-Alt"));
            String value = method.getResponseHeader("X-Alt").getValue();
            assertEquals("1.1.1.1:1", value);
        } finally {
            method.releaseConnection();
        }

        for (int i = 0; i < 20; i++) {
            altLocManager.add(
                    alternateLocationFactory.create("1.1.1." + i + ":6346", hashURN), null);
        }
        assertEquals(21, altLocManager.getNumLocs(
                fd.getSHA1Urn()));

        String pre = "1.1.1.";
        String post = "";
        String comma = ", ";
        // note that this value can change depending on iterators,
        // so this is a very flaky test.
        String required = null;

        required = pre + 16 + post + comma + pre + 13 + post
                + comma + pre + 10 + post + comma + pre + 15 + post + comma
                + pre + 12 + post + comma + pre + 1 + post + comma + pre + 14
                + post + comma + pre + 11 + post + comma + pre + 0 + post
                + comma + pre + "1:1";
        method = new GetMethod(hashUrl);
        method.addRequestHeader("Range", "bytes=0-1");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response);
            assertNotNull(method.getResponseHeader("X-Alt"));
            assertHeaderEquals(required, method.getResponseHeader("X-Alt").getValue());
        } finally {
            method.releaseConnection();
        }

        required = pre + 5 + post + comma + pre + 2 + post + comma
                + pre + 18 + post + comma + pre + 7 + post + comma + pre + 4
                + post + comma + pre + 17 + post + comma + pre + 6 + post
                + comma + pre + 3 + post + comma + pre + 19 + post + comma
                + pre + 8 + post;
        method = new GetMethod(hashUrl);
        method.addRequestHeader("Range", "bytes=2-3");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response);
            assertNotNull(method.getResponseHeader("X-Alt"));
            assertHeaderEquals(required, method.getResponseHeader("X-Alt").getValue());
        } finally {
            method.releaseConnection();
        }

        required = pre + 9 + post;
        method = new GetMethod(hashUrl);
        method.addRequestHeader("Range", "bytes=4-5");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response);
            assertNotNull(method.getResponseHeader("X-Alt"));
            assertHeaderEquals(required, method.getResponseHeader("X-Alt").getValue());
        } finally {
            method.releaseConnection();
        }

        // Now if some more are added to file desc, make sure they're reported.
        altLocManager.add(
                alternateLocationFactory.create("1.1.1.99:6346", hashURN), null);
        required = pre + 99 + post;
        method = new GetMethod(hashUrl);
        method.addRequestHeader("Range", "bytes=6-7");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response);
            assertNotNull(method.getResponseHeader("X-Alt"));
            assertHeaderEquals(required, method.getResponseHeader("X-Alt").getValue());
        } finally {
            method.releaseConnection();
        }
    }

    public void testPrioritizingAlternates() throws Exception {
        // add a simple marker alt so we know it only contains that
        AlternateLocation al = alternateLocationFactory.create("1.1.1.1:1", hashURN);
        altLocManager.add(al, null);
        GetMethod method = new GetMethod(hashUrl);
        method.addRequestHeader("Connection", "close");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertNotNull(method.getResponseHeader("X-Alt"));
            String value = method.getResponseHeader("X-Alt").getValue();
            assertEquals("1.1.1.1:1", value);
        } finally {
            method.releaseConnection();
        }
        // get rid of it.
        altLocManager.remove(al, null);
        altLocManager.remove(al, null);

        for (int i = 0; i < 50; i++) {
            al = alternateLocationFactory.create("1.1.1." + i + ":6346", hashURN);

            altLocManager.add(al, null);

            // 0-9, make as demoted.
            if (i < 10) {
                altLocManager.remove(al, null); // should
                // demote.
            }
            // 10-19, increment once.
            else if (i < 20) {
                altLocManager.add(al, null); // should
                // increment.
            }
            // 20-29, increment & demote.
            else if (i < 30) {
                altLocManager.add(al, null); // increment
                altLocManager.remove(al, null); // demote
            }
            // 30-39, increment twice.
            else if (i < 40) {
                altLocManager.add(al, null); // increment
                altLocManager.add(al, null); // increment
            }
            // 40-49, leave normal.
        }
        AlternateLocationCollection alc = altLocManager
                .getDirect(fd.getSHA1Urn());
        assertEquals(50, alc.getAltLocsSize());

        // Order of return should be:
        // 40-49 returned first
        // 10-19 returned next
        // 30-39 returned next
        // 0-9 returned next
        // 20-29 returned next

        String pre = "1.1.1.";
        String post = "";
        String comma = ", ";
        // note that this value can change depending on iterators,
        // so this is a very flaky test.
        String required = null;

        required = pre + 40 + post + comma + pre + 41 + post
                + comma + pre + 42 + post + comma + pre + 43 + post + comma
                + pre + 44 + post + comma + pre + 45 + post + comma + pre + 46
                + post + comma + pre + 47 + post + comma + pre + 48 + post
                + comma + pre + 49 + post;
        method = new GetMethod(hashUrl);
        method.addRequestHeader("Range", "bytes=0-1");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response);
            assertNotNull(method.getResponseHeader("X-Alt"));
            assertHeaderEquals(required, method.getResponseHeader("X-Alt").getValue());
        } finally {
            method.releaseConnection();
        }

        required = pre + 16 + post + comma + pre + 13 + post
                + comma + pre + 10 + post + comma + pre + 18 + post + comma
                + pre + 15 + post + comma + pre + 12 + post + comma + pre + 17
                + post + comma + pre + 14 + post + comma + pre + 11 + post
                + comma + pre + 19 + post;
        method = new GetMethod(hashUrl);
        method.addRequestHeader("Range", "bytes=2-3");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response);
            assertNotNull(method.getResponseHeader("X-Alt"));
            assertHeaderEquals(required, method.getResponseHeader("X-Alt").getValue());
        } finally {
            method.releaseConnection();
        }

        required = pre + 35 + post + comma + pre + 32 + post
                + comma + pre + 37 + post + comma + pre + 34 + post + comma
                + pre + 31 + post + comma + pre + 39 + post + comma + pre + 36
                + post + comma + pre + 33 + post + comma + pre + 30 + post
                + comma + pre + 38 + post;
        method = new GetMethod(hashUrl);
        method.addRequestHeader("Range", "bytes=4-5");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response);
            assertNotNull(method.getResponseHeader("X-Alt"));
            assertHeaderEquals(required, method.getResponseHeader("X-Alt").getValue());
        } finally {
            method.releaseConnection();
        }

        required = pre + 5 + post + comma + pre + 2 + post + comma
                + pre + 7 + post + comma + pre + 4 + post + comma + pre + 1
                + post + comma + pre + 9 + post + comma + pre + 6 + post
                + comma + pre + 3 + post + comma + pre + 0 + post + comma + pre
                + 8 + post;
        method = new GetMethod(hashUrl);
        method.addRequestHeader("Range", "bytes=6-7");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response);
            assertNotNull(method.getResponseHeader("X-Alt"));
            assertHeaderEquals(required, method.getResponseHeader("X-Alt").getValue());
        } finally {
            method.releaseConnection();
        }

        required = pre + 24 + post + comma + pre + 21 + post
                + comma + pre + 29 + post + comma + pre + 26 + post + comma
                + pre + 23 + post + comma + pre + 20 + post + comma + pre + 28
                + post + comma + pre + 25 + post + comma + pre + 22 + post
                + comma + pre + 27 + post;
        method = new GetMethod(hashUrl);
        method.addRequestHeader("Range", "bytes=8-9");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response);
            assertNotNull(method.getResponseHeader("X-Alt"));
            assertHeaderEquals(required, method.getResponseHeader("X-Alt").getValue());
        } finally {
            method.releaseConnection();
        }
    }

    public void testAltsDontExpire() throws Exception {
        UploadSettings.LEGACY_EXPIRATION_DAMPER.setValue((float) Math.E / 4);
        // test that an altloc will not expire if given out less often
        AlternateLocation al = alternateLocationFactory.create("1.1.1.1:1", hashURN);
        assertTrue(al.canBeSent(AlternateLocation.MESH_LEGACY));
        altLocManager.add(al, null);

        for (int i = 0; i < 10; i++) {
            GetMethod method = new GetMethod(hashUrl);
            method.addRequestHeader("Connection", "close");
            try {
                int response = client.executeMethod(method);
                assertEquals(HttpStatus.SC_OK, response);
                assertNotNull(method.getResponseHeader("X-Alt"));
                String value = method.getResponseHeader("X-Alt").getValue();
                assertEquals("1.1.1.1:1", value);
            } finally {
                method.releaseConnection();
            }
            Thread.sleep(8 * 1000);
        }
        assertTrue(al.canBeSent(AlternateLocation.MESH_LEGACY));
    }

    /**
     * testFALTNotRequested(), testFALTWhenRequested() and
     * testFWALTWhenRequested() fail if the server processes the entire request
     * before we start reading from the InputStreams. That means: our
     * HTTPUploader is added to UploadManagers private _activeUploadsList, the
     * request is processed and the HTTPUploader is removed from the List. We
     * start reading from the InputStream and the assertions in the mentioned
     * tests fail because our HTTPUploader is no longer in that List. So, we
     * have to cache the HTTPUploader somehow what this extension does.
     */
    @Singleton
    private static class TestUploadManager extends HTTPUploadManager {

        private List<HTTPUploader> activeUploads = new ArrayList<HTTPUploader>();
        
        @Inject
        public TestUploadManager(UploadSlotManager slotManager,
                HttpRequestHandlerFactory httpRequestHandlerFactory,
                Provider<ContentManager> contentManager,
                Provider<HTTPAcceptor> httpAcceptor,
                Provider<FileManager> fileManager,
                Provider<ActivityCallback> activityCallback) {
            super(slotManager, httpRequestHandlerFactory, contentManager, httpAcceptor,
                    fileManager, activityCallback);
        }

        @Override
        public synchronized void addAcceptedUploader(HTTPUploader uploader, HttpContext context) {
            activeUploads.add(uploader);
            super.addAcceptedUploader(uploader, context);
        }

        public void clearUploads() {
            cleanup();

            activeUploads.clear();
        }
    }
}
