package com.limegroup.gnutella.uploader;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.Test;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.message.BasicHttpRequest;
import org.limewire.collection.Interval;
import org.limewire.collection.IntervalSet;
import org.limewire.util.CommonUtils;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.CreationTimeCache;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.HTTPUploadManager;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.dime.DIMEParser;
import com.limegroup.gnutella.dime.DIMERecord;
import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.http.HttpClientManager;
import com.limegroup.gnutella.settings.ChatSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * Test that a client uploads a file correctly. Depends on a file containing the
 * lowercase characters a-z.
 */
public class UploadTest extends LimeTestCase {

    private static final int PORT = 6668;

    /** The file name, plain and encoded. */
    private static String testDirName = "com/limegroup/gnutella/uploader/data";

    private static String incName = "partial alphabet.txt";

    private static String fileName = "alphabet test file#2.txt";

    private static String fileNameUrl = "/get/0/alphabet%20test+file%232.txt";

    private static String otherFileName = "upperAlphabet.txt";

    private static String otherFileNameUrl = "/get/1/upperAlphabet.txt";

    /** The hash of the file contents. */
    private static final String baseHash = "GLIQY64M7FSXBSQEZY37FIM5QQSA2OUJ";

    private static final String hash = "urn:sha1:" + baseHash;

    private static final String badHash = "urn:sha1:GLIQY64M7FSXBSQEZY37FIM5QQSA2SAM";

    private static final String incompleteHash = "urn:sha1:INCOMCPLETEXBSQEZY37FIM5QQSA2OUJ";

    private static final String incompleteHashUrl = "/uri-res/N2R?"
            + incompleteHash;

    /** The verifying file for the shared incomplete file */
    private VerifyingFile vf;

    /** The filedesc of the shared file. */
    private FileDesc FD;

    /** The root32 of the shared file. */
    private String ROOT32;

    private HttpClient client;

    private HostConfiguration hostConfig;

    private static RouterService ROUTER_SERVICE;

    private static final Object loaded = new Object();

    public UploadTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(UploadTest.class);
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }

    public static void globalSetUp() {
        ROUTER_SERVICE = new RouterService(new FManCallback());
    }

    @Override
    protected void setUp() throws Exception {
        // allows to run single tests from Eclipse
        if (ROUTER_SERVICE == null) {
            globalSetUp();
        }

        SharingSettings.ADD_ALTERNATE_FOR_SELF.setValue(false);
        FilterSettings.BLACK_LISTED_IP_ADDRESSES
                .setValue(new String[] { "*.*.*.*" });
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(new String[] {
                "127.*.*.*", InetAddress.getLocalHost().getHostAddress() });
        RouterService.getIpFilter().refreshHosts();
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

        File testDir = CommonUtils.getResourceFile(testDirName);
        // we must use a separate copy method
        // because the filename has a # in it which can't be a resource.
        LimeTestUtils.copyFile(new File(testDir, fileName), new File(
                _sharedDir, fileName));
        LimeTestUtils.copyFile(new File(testDir, otherFileName), new File(
                _sharedDir, otherFileName));
        assertTrue("Copying resources failed", new File(_sharedDir, fileName)
                .exists());
        assertGreaterThan("should have data", 0, new File(_sharedDir, fileName)
                .length());

        if (!RouterService.isLoaded()) {
            startAndWaitForLoad();
            // Thread.sleep(2000);
        }

        FileManager fm = RouterService.getFileManager();
        File incFile = new File(_incompleteDir, incName);
        fm.removeFileIfShared(incFile);
        CommonUtils.copyResourceFile(testDirName + "/" + incName, incFile);
        URN urn = URN.createSHA1Urn(incompleteHash);
        Set<URN> urns = new HashSet<URN>();
        urns.add(urn);
        vf = new VerifyingFile(252450);
        fm.addIncompleteFile(incFile, urns, incName, 1981, vf);
        assertEquals(1, fm.getNumIncompleteFiles());
        assertEquals(2, fm.getNumFiles());

        FD = fm.getFileDescForFile(new File(_sharedDir, fileName));
        while (FD.getHashTree() == null)
            Thread.sleep(300);
        ROOT32 = FD.getHashTree().getRootHash();

        assertEquals("Unexpected uploads in progress", 0, RouterService
                .getUploadManager().uploadsInProgress());
        assertEquals("Unexpected queued uploads", 0, RouterService
                .getUploadManager().getNumQueuedUploads());

        client = HttpClientManager.getNewClient();
        hostConfig = new HostConfiguration();
        hostConfig.setHost("localhost", PORT);
        client.setHostConfiguration(hostConfig);
    }

    @Override
    public void tearDown() {
        ((HTTPUploadManager) RouterService.getUploadManager()).cleanup();

        assertEquals(0, RouterService.getUploadSlotManager().getNumQueued());
        assertEquals(0, RouterService.getUploadSlotManager().getNumActive());
    }

    public void testHTTP10Download() throws Exception {
        GetMethod method = new GetMethod(fileNameUrl);
        method.setHttp11(false);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertEquals("abcdefghijklmnopqrstuvwxyz", method
                    .getResponseBodyAsString());
        } finally {
            method.releaseConnection();
        }
    }

    public void testHTTP10DownloadRange() throws Exception {
        GetMethod method = new GetMethod(fileNameUrl);
        method.addRequestHeader("Range", "bytes=2-");
        method.setHttp11(false);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response);
            assertEquals("cdefghijklmnopqrstuvwxyz", method
                    .getResponseBodyAsString());
        } finally {
            method.releaseConnection();
        }
    }

    public void testHTTP10DownloadMissingRange() throws Exception {
        GetMethod method = new GetMethod(fileNameUrl);
        method.addRequestHeader("Range", "bytes 2-");
        method.setHttp11(false);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response);
            assertEquals("cdefghijklmnopqrstuvwxyz", method
                    .getResponseBodyAsString());
        } finally {
            method.releaseConnection();
        }
    }

    public void testHTTP10DownloadMiddleRange() throws Exception {
        GetMethod method = new GetMethod(fileNameUrl);
        method.addRequestHeader("Range", "bytes 2-5");
        method.setHttp11(false);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response);
            assertNotNull(method.getResponseHeader("Content-range"));
            assertEquals("bytes 2-5/26", method.getResponseHeader(
                    "Content-range").getValue());
            assertEquals("cdef", method.getResponseBodyAsString());
        } finally {
            method.releaseConnection();
        }
    }

    public void testHTTP10DownloadRangeNoSpace() throws Exception {
        GetMethod method = new GetMethod(fileNameUrl);
        method.addRequestHeader(new org.apache.commons.httpclient.Header(
                "Range", "") {
            public String toExternalForm() {
                return "Range:bytes 2-";
            };

            public String toString() {
                return "Range:bytes 2-";
            };
        });
        method.setHttp11(false);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_BAD_REQUEST, response);
        } finally {
            method.releaseConnection();
        }
    }

    public void testHTTP10DownloadRangeLastByte() throws Exception {
        GetMethod method = new GetMethod(fileNameUrl);
        method.addRequestHeader("Range", "bytes=-5");
        method.setHttp11(false);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response);
            assertEquals("vwxyz", method.getResponseBodyAsString());
        } finally {
            method.releaseConnection();
        }
    }

    public void testHTTP10DownloadRangeTooBigNegative() throws Exception {
        GetMethod method = new GetMethod(fileNameUrl);
        method.addRequestHeader("Range", "bytes=-30");
        method.setHttp11(false);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertEquals("abcdefghijklmnopqrstuvwxyz", method
                    .getResponseBodyAsString());
        } finally {
            method.releaseConnection();
        }
    }

    public void testHTTP10DownloadRangeExtraSpace() throws Exception {
        GetMethod method = new GetMethod(fileNameUrl);
        method.addRequestHeader("Range", "   bytes=  2  -  5 ");
        method.setHttp11(false);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response);
            assertEquals("cdef", method.getResponseBodyAsString());
        } finally {
            method.releaseConnection();
        }
    }

    public void testHTTP11DownloadNoRangeHeader() throws Exception {
        GetMethod method = new GetMethod(fileNameUrl);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertEquals("abcdefghijklmnopqrstuvwxyz", method
                    .getResponseBodyAsString());
        } finally {
            method.releaseConnection();
        }
    }

    public void testHTTP11Download() throws Exception {
        GetMethod method = new GetMethod(fileNameUrl);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertEquals("abcdefghijklmnopqrstuvwxyz", method
                    .getResponseBodyAsString());
        } finally {
            method.releaseConnection();
        }
    }

    public void testHTTP11DownloadRange() throws Exception {
        GetMethod method = new GetMethod(fileNameUrl);
        method.addRequestHeader("Range", "bytes=2-");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response);
            assertEquals("cdefghijklmnopqrstuvwxyz", method
                    .getResponseBodyAsString());
        } finally {
            method.releaseConnection();
        }
    }

    public void testHTTP11DownloadMissingRange() throws Exception {
        GetMethod method = new GetMethod(fileNameUrl);
        method.addRequestHeader("Range", "bytes 2-");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response);
            assertEquals("cdefghijklmnopqrstuvwxyz", method
                    .getResponseBodyAsString());
        } finally {
            method.releaseConnection();
        }
    }

    public void testHTTP11DownloadMiddleRange() throws Exception {
        GetMethod method = new GetMethod(fileNameUrl);
        method.addRequestHeader("Range", "bytes 2-5");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response);
            assertNotNull(method.getResponseHeader("Content-range"));
            assertEquals("bytes 2-5/26", method.getResponseHeader(
                    "Content-range").getValue());
            assertEquals("cdef", method.getResponseBodyAsString());
        } finally {
            method.releaseConnection();
        }
    }

    public void testHTTP11DownloadRangeNoSpace() throws Exception {
        GetMethod method = new GetMethod(fileNameUrl);
        method.addRequestHeader(new org.apache.commons.httpclient.Header(
                "Range", "") {
            public String toExternalForm() {
                return "Range:bytes 2-";
            };

            public String toString() {
                return "Range:bytes 2-";
            };
        });
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_BAD_REQUEST, response);
        } finally {
            method.releaseConnection();
        }
    }

    public void testHTTP11DownloadRangeLastByte() throws Exception {
        GetMethod method = new GetMethod(fileNameUrl);
        method.addRequestHeader("Range", "bytes=-5");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response);
            assertEquals("vwxyz", method.getResponseBodyAsString());
        } finally {
            method.releaseConnection();
        }
    }

    public void testHTTP11DownloadRangeTooBigNegative() throws Exception {
        GetMethod method = new GetMethod(fileNameUrl);
        method.addRequestHeader("Range", "bytes=-30");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertEquals("abcdefghijklmnopqrstuvwxyz", method
                    .getResponseBodyAsString());
        } finally {
            method.releaseConnection();
        }
    }

    public void testHTTP11IncompleteRange() throws Exception {
        Interval iv = new Interval(2, 6);
        IntervalSet vb = (IntervalSet) PrivilegedAccessor.getValue(vf,
                "verifiedBlocks");
        vb.add(iv);

        GetMethod method = new GetMethod(incompleteHashUrl);
        method.addRequestHeader("Range", "bytes 2-5");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response);
            assertEquals("cdef", method.getResponseBodyAsString());
        } finally {
            method.releaseConnection();
        }

        method = new GetMethod(incompleteHashUrl);
        method.addRequestHeader("Range", "bytes 1-3");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response);
            assertEquals("cd", method.getResponseBodyAsString());
        } finally {
            method.releaseConnection();
        }

        method = new GetMethod(incompleteHashUrl);
        method.addRequestHeader("Range", "bytes 3-10");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response);
            assertEquals("defg", method.getResponseBodyAsString());
        } finally {
            method.releaseConnection();
        }

        method = new GetMethod(incompleteHashUrl);
        method.addRequestHeader("Range", "bytes 0-20");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response);
            assertEquals("cdefg", method.getResponseBodyAsString());
        } finally {
            method.releaseConnection();
        }
    }

    public void testHTTP11PipeliningDownload() throws Exception {
        HttpUploadClient client = new HttpUploadClient();
        try {
            client.connect("localhost", PORT);
            HttpRequest request = new BasicHttpRequest("GET", fileNameUrl);
            client.writeRequest(request);
            client.writeRequest(request);
            client.writeRequest(request);
            HttpResponse response = client.readResponse();
            assertEquals(HttpStatus.SC_OK, response.getStatusLine()
                    .getStatusCode());
            assertEquals("abcdefghijklmnopqrstuvwxyz", client
                    .readBody(response));
            response = client.readResponse();
            assertEquals(HttpStatus.SC_OK, response.getStatusLine()
                    .getStatusCode());
            assertEquals("abcdefghijklmnopqrstuvwxyz", client
                    .readBody(response));
            response = client.readResponse();
            assertEquals(HttpStatus.SC_OK, response.getStatusLine()
                    .getStatusCode());
            assertEquals("abcdefghijklmnopqrstuvwxyz", client
                    .readBody(response));
        } finally {
            client.close();
        }
    }

    /**
     * Makes sure that a HEAD request followed by a GET request does the right
     * thing.
     */
    public void testHTTP11HeadFollowedByGetDownload() throws Exception {
        http11DownloadPersistentHeadFollowedByGet(fileNameUrl);
    }

    /**
     * Tests persistent connections with URI requests. (Raphael Manfredi claimed
     * this was broken.)
     */
    public void testHTTP11DownloadPersistentURI() throws Exception {
        http11DownloadPersistentHeadFollowedByGet("/uri-res/N2R?" + hash);
    }

    private void http11DownloadPersistentHeadFollowedByGet(String url)
            throws IOException {
        HttpMethodBase method = new HeadMethod(url);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
        } finally {
            method.releaseConnection();
        }

        method = new GetMethod(url);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertEquals("abcdefghijklmnopqrstuvwxyz", method
                    .getResponseBodyAsString());
        } finally {
            method.releaseConnection();
        }

        method = new GetMethod(url);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertEquals("abcdefghijklmnopqrstuvwxyz", method
                    .getResponseBodyAsString());
        } finally {
            method.releaseConnection();
        }

        method = new HeadMethod(url);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
        } finally {
            method.releaseConnection();
        }

        method = new GetMethod(url);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertEquals("abcdefghijklmnopqrstuvwxyz", method
                    .getResponseBodyAsString());
        } finally {
            method.releaseConnection();
        }
    }

    /**
     * Tests the case of requests for different file over the same HTTP session.
     */
    public void testMultipleUploadSession() throws Exception {
        GetMethod method = new GetMethod(fileNameUrl);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertEquals("abcdefghijklmnopqrstuvwxyz", method
                    .getResponseBodyAsString());
        } finally {
            method.releaseConnection();
        }

        assertConnectionIsOpen(true);

        method = new GetMethod(otherFileNameUrl);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZ\n", method
                    .getResponseBodyAsString());
        } finally {
            method.releaseConnection();
        }
    }

    public void testIncompleteFileUpload() throws Exception {
        GetMethod method = new GetMethod("/uri-res/N2R?" + incompleteHash);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE,
                    response);
        } finally {
            method.releaseConnection();
        }

        assertConnectionIsOpen(true);
        
        method = new GetMethod("/uri-res/N2R?" + incompleteHash);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE,
                    response);
        } finally {
            method.releaseConnection();
        }
    }

    public void testIncompleteFileWithRanges() throws Exception {
        // add a range to the incomplete file.
        Interval iv = new Interval(50, 102500);
        IntervalSet vb = (IntervalSet) PrivilegedAccessor.getValue(vf,
                "verifiedBlocks");
        vb.add(iv);

        GetMethod method = new GetMethod("/uri-res/N2R?" + incompleteHash);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE,
                    response);
            assertNotNull(method.getResponseHeader("X-Available-Ranges"));
            assertEquals("bytes 50-102499", method.getResponseHeader(
                    "X-Available-Ranges").getValue());
        } finally {
            method.releaseConnection();
        }

        assertConnectionIsOpen(true);

        // add another range and make sure we display it.
        iv = new Interval(150050, 252450);
        vb.add(iv);
        method = new GetMethod("/uri-res/N2R?" + incompleteHash);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE,
                    response);
            assertNotNull(method.getResponseHeader("X-Available-Ranges"));
            assertEquals("bytes 50-102499, 150050-252449", method
                    .getResponseHeader("X-Available-Ranges").getValue());
        } finally {
            method.releaseConnection();
        }

        assertConnectionIsOpen(true);

        // add an interval too small to report and make sure we don't report
        iv = new Interval(102505, 150000);
        vb.add(iv);
        method = new GetMethod("/uri-res/N2R?" + incompleteHash);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE,
                    response);
            assertNotNull(method.getResponseHeader("X-Available-Ranges"));
            assertEquals("bytes 50-102499, 150050-252449", method
                    .getResponseHeader("X-Available-Ranges").getValue());
        } finally {
            method.releaseConnection();
        }

        assertConnectionIsOpen(true);

        // add the glue between the other intervals and make sure we condense
        // the ranges into a single larger range.
        iv = new Interval(102500, 102505);
        vb.add(iv);
        iv = new Interval(150000, 150050);
        vb.add(iv);
        method = new GetMethod("/uri-res/N2R?" + incompleteHash);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE,
                    response);
            assertNotNull(method.getResponseHeader("X-Available-Ranges"));
            assertEquals("bytes 50-252449", method.getResponseHeader(
                    "X-Available-Ranges").getValue());
        } finally {
            method.releaseConnection();
        }
    }

    public void testIncompleteFileWithRangeRequest() throws Exception {
        GetMethod method = new GetMethod("/uri-res/N2R?" + incompleteHash);
        method.addRequestHeader("Range", "bytes 20-40");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE,
                    response);
            assertEquals("Requested Range Unavailable", method.getStatusText());
        } finally {
            method.releaseConnection();
        }

        assertConnectionIsOpen(true);
    }

    public void testHTTP11WrongURI() throws Exception {
        GetMethod method = new GetMethod("/uri-res/N2R?" + badHash);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_NOT_FOUND, response);
            assertEquals("Not Found", method.getStatusText());
        } finally {
            method.releaseConnection();
        }

        assertConnectionIsOpen(true);
    }

    public void testHTTP10WrongURI() throws Exception {
        GetMethod method = new GetMethod("/uri-res/N2R?" + badHash);
        method.setHttp11(false);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_NOT_FOUND, response);
            assertEquals("Not Found", method.getStatusText());
        } finally {
            method.releaseConnection();
        }

        assertConnectionIsOpen(false);
    }

    public void testHTTP11MalformedURI() throws Exception {
        GetMethod method = new GetMethod("/uri-res/N2R?" + "no%20more%20school");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_BAD_REQUEST, response);
            assertEquals("Malformed Request", method.getStatusText());
        } finally {
            method.releaseConnection();
        }

        assertConnectionIsOpen(false);
    }

    public void testHTTP10MalformedURI() throws Exception {
        GetMethod method = new GetMethod("/uri-res/N2R?" + "%20more%20school");
        method.setHttp11(false);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_BAD_REQUEST, response);
            assertEquals("Malformed Request", method.getStatusText());
        } finally {
            method.releaseConnection();
        }

        assertConnectionIsOpen(false);
    }

    public void testHTTP11MalformedGet() throws Exception {
        GetMethod method = new GetMethod("/get/some/dr/pepper");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_BAD_REQUEST, response);
            assertEquals("Malformed Request", method.getStatusText());
        } finally {
            method.releaseConnection();
        }

        assertConnectionIsOpen(false);
    }

    public void testHTTP11MalformedHeader() throws Exception {
        GetMethod method = new GetMethod("/uri-res/N2R?" + hash);
        method.addRequestHeader("Range", "2-5");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_BAD_REQUEST, response);
            assertEquals("Malformed Request", method.getStatusText());
        } finally {
            method.releaseConnection();
        }

        assertConnectionIsOpen(false);
    }

    /**
     * Test that creation time header is returned.
     */
    public void testCreationTimeHeaderReturned() throws Exception {
        // assert that creation time exists
        URN urn = URN.createSHA1Urn(hash);
        Long creationTime = CreationTimeCache.instance().getCreationTime(urn);
        assertNotNull(creationTime);
        assertTrue(creationTime.longValue() > 0);

        GetMethod method = new GetMethod("/uri-res/N2R?" + hash);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertNotNull(method.getResponseHeader("X-Create-Time"));
            assertEquals(creationTime + "", method.getResponseHeader(
                    "X-Create-Time").getValue());
        } finally {
            method.releaseConnection();
        }
    }

    public void testCreationTimeHeaderReturnedForIncompleteFile()
            throws Exception {
        Interval iv = new Interval(2, 5);
        IntervalSet vb = (IntervalSet) PrivilegedAccessor.getValue(vf,
                "verifiedBlocks");
        vb.add(iv);

        URN urn = URN.createSHA1Urn(incompleteHash);
        Long creationTime = new Long("10776");
        CreationTimeCache.instance().addTime(urn, creationTime.longValue());

        GetMethod method = new GetMethod("/uri-res/N2R?" + incompleteHash);
        method.addRequestHeader("Range", "bytes 2-5");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response);
            assertEquals("cdef", method.getResponseBodyAsString());
            assertNotNull(method.getResponseHeader("X-Create-Time"));
            assertEquals(creationTime + "", method.getResponseHeader(
                    "X-Create-Time").getValue());
        } finally {
            method.releaseConnection();
        }
    }

    public void testChatFeatureHeader() throws Exception {
        ChatSettings.CHAT_ENABLED.setValue(true);
        GetMethod method = new GetMethod(fileNameUrl);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertNotNull(method.getResponseHeader("X-Features"));
            String header = method.getResponseHeader("X-Features").getValue();
            assertTrue(header.contains("fwalt/0.1"));
            assertTrue(header.contains("browse/1.0"));
            assertTrue(header.contains("chat/0.1"));
        } finally {
            method.releaseConnection();
        }

        assertConnectionIsOpen(true);

        // feature headers are only sent with the first response
        ChatSettings.CHAT_ENABLED.setValue(false);
        method = new GetMethod(fileNameUrl);
        method.addRequestHeader("Connection", "close");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertNull(method.getResponseHeader("X-Features"));
        } finally {
            method.releaseConnection();
        }

        assertConnectionIsOpen(false);

        // try a new connection
        method = new GetMethod(fileNameUrl);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertNotNull(method.getResponseHeader("X-Features"));
            String header = method.getResponseHeader("X-Features").getValue();
            assertTrue(header.contains("fwalt/0.1"));
            assertTrue(header.contains("browse/1.0"));
            assertFalse(header.contains("chat/0.1"));
        } finally {
            method.releaseConnection();
        }
    }

    public void testThexHeader() throws Exception {
        GetMethod method = new GetMethod(fileNameUrl);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertNotNull(method.getResponseHeader("X-Thex-URI"));
            String header = method.getResponseHeader("X-Thex-URI").getValue();
            assertEquals("/uri-res/N2X?" + hash + ";" + ROOT32, header);
        } finally {
            method.releaseConnection();
        }
    }

    public void testDownloadFromBitprintUrl() throws Exception {
        GetMethod method = new GetMethod("/uri-res/N2R?urn:bitprint:"
                + baseHash + "." + ROOT32);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertEquals("abcdefghijklmnopqrstuvwxyz", method
                    .getResponseBodyAsString());
        } finally {
            method.releaseConnection();
        }

        // the request is checked for a valid bitprint length
        method = new GetMethod("/uri-res/N2R?urn:bitprint:" + baseHash + "."
                + "asdoihffd");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_BAD_REQUEST, response);
        } finally {
            method.releaseConnection();
        }

        // but not for the valid base32 root -- in the future we may
        // and this test will break
        method = new GetMethod("/uri-res/N2R?urn:bitprint:" + baseHash + "."
                + "SAMUWJUUSPLMMDUQZOWX32R6AEOT7NCCBX6AGBI");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertEquals("abcdefghijklmnopqrstuvwxyz", method
                    .getResponseBodyAsString());
        } finally {
            method.releaseConnection();
        }

        // make sure "bitprint:" is required for bitprint uploading.
        method = new GetMethod("/uri-res/N2R?urn:sha1:" + baseHash + "."
                + ROOT32);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_BAD_REQUEST, response);
        } finally {
            method.releaseConnection();
        }
    }

    public void testBadGetTreeRequest() throws Exception {
        GetMethod method = new GetMethod("/uri-res/N2X?" + badHash);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_NOT_FOUND, response);
        } finally {
            method.releaseConnection();
        }

        method = new GetMethod("/uri-res/N2X?" + "no hash");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_BAD_REQUEST, response);
        } finally {
            method.releaseConnection();
        }
    }

    public void testGetTree() throws Exception {
        GetMethod method = new GetMethod("/uri-res/N2X?" + hash);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            DIMEParser parser = new DIMEParser(method.getResponseBodyAsStream());
            parser.nextRecord(); // xml
            DIMERecord tree = parser.nextRecord();
            assertFalse(parser.hasNext());
            List allNodes = FD.getHashTree().getAllNodes();
            byte[] data = tree.getData();
            int offset = 0;
            for (Iterator genIter = allNodes.iterator(); genIter.hasNext();) {
                for (Iterator i = ((List) genIter.next()).iterator(); i
                        .hasNext();) {
                    byte[] current = (byte[]) i.next();
                    for (int j = 0; j < current.length; j++) {
                        assertEquals("offset: " + offset + ", idx: " + j,
                                current[j], data[offset++]);
                    }
                }
            }
            assertEquals(data.length, offset);
            // more extensive validity checks are in HashTreeTest
            // this is just checking to make sure we sent the right tree.
        } finally {
            method.releaseConnection();
        }
    }

    private static class FManCallback extends ActivityCallbackStub {
        public void fileManagerLoaded() {
            synchronized (loaded) {
                loaded.notify();
            }
        }
    }

    private static void startAndWaitForLoad() {
        synchronized (loaded) {
            try {
                ROUTER_SERVICE.start();
                loaded.wait();
            } catch (InterruptedException e) {
                // good.
            }
        }
    }

    private void assertConnectionIsOpen(boolean open) {
        HttpConnection connection = client.getHttpConnectionManager()
                .getConnection(hostConfig);
        assertEquals(open, connection.isOpen());
        client.getHttpConnectionManager().releaseConnection(connection);
    }

}
