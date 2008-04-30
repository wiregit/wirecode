package com.limegroup.gnutella.uploader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.http.httpclient.HttpClientUtils;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.io.LocalSocketAddressProvider;
import org.limewire.net.ConnectionDispatcher;
import org.limewire.util.CommonUtils;
import org.limewire.util.PrivilegedAccessor;
import org.limewire.util.TestUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.CreationTimeCache;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileEventListener;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.FileManagerEvent;
import com.limegroup.gnutella.HTTPAcceptor;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UploadManager;
import com.limegroup.gnutella.dime.DIMEParser;
import com.limegroup.gnutella.dime.DIMERecord;
import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.downloader.VerifyingFileFactory;
import com.limegroup.gnutella.security.Tiger;
import com.limegroup.gnutella.settings.ChatSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.NetworkSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.stubs.LocalSocketAddressProviderStub;
import com.limegroup.gnutella.tigertree.HashTree;
import com.limegroup.gnutella.tigertree.HashTreeCache;
import com.limegroup.gnutella.tigertree.HashTreeCacheImpl;
import com.limegroup.gnutella.tigertree.HashTreeUtils;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * Test that a client uploads a file correctly. Depends on a file containing the
 * lowercase characters a-z.
 */
//ITEST
public class UploadTest extends LimeTestCase {

    private static final int PORT = 6668;

    /** The file name, plain and encoded. */
    private static final String testDirName = "com/limegroup/gnutella/uploader/data";

    private static final String incName = "partial alphabet.txt";

    private static final String fileName = "alphabet test file#2.txt";

    private static final String otherFileName = "upperAlphabet.txt";

    /** The hash of the file contents. */
    private static final String baseHash = "GLIQY64M7FSXBSQEZY37FIM5QQSA2OUJ";

    private static final String hash = "urn:sha1:" + baseHash;

    private static final String badHash = "urn:sha1:GLIQY64M7FSXBSQEZY37FIM5QQSA2SAM";

    private static final String incompleteHash = "urn:sha1:INCOMCPLETEXBSQEZY37FIM5QQSA2OUJ";
    
    private String incompleteHashUrl = "/uri-res/N2R?"
            + incompleteHash;

    /** The verifying file for the shared incomplete file */
    private VerifyingFile vf;

    private LimeHttpClient client;

    protected String protocol;

    private Injector injector;

    private UploadManager uploadManager;

    private FileManager fileManager;

    private String fileNameUrl;

    private String otherFileNameUrl;
    
    private String host;

    public UploadTest(String name) {
        super(name);
        
        this.protocol = "http";
    }

    public static Test suite() {
        return buildTestSuite(UploadTest.class);
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }

    @Override
    protected void setUp() throws Exception {
        doSettings();

        // copy resources
        File testDir = TestUtils.getResourceFile(testDirName);
        // we must use a separate copy method
        // because the filename has a # in it which can't be a resource.
        File target = new File(_sharedDir, fileName);
        LimeTestUtils.copyFile(new File(testDir, fileName), target);
        target = new File(_sharedDir, otherFileName);
        LimeTestUtils.copyFile(new File(testDir, otherFileName), target);
        assertTrue("Copying resources failed", target.exists());
        assertGreaterThan("Expected file to contain data", 0, target.length());

        // initialize services
        injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(LocalSocketAddressProvider.class).to(LocalSocketAddressProviderStub.class);
            }
        });
        
        startServices();

        // get urls from file manager
        FileDesc fd = fileManager.getFileDescForFile(new File(_sharedDir, fileName));
        assertNotNull("File not loaded", fd);
        host = protocol + "://localhost:" + PORT;
        fileNameUrl = host + "/get/" + fd.getIndex() + "/" + URLEncoder.encode(fileName, "UTF-8");
        
        fd = fileManager.getFileDescForFile(new File(_sharedDir, otherFileName));
        assertNotNull("File not loaded", fd);
        otherFileNameUrl = host + "/get/" + fd.getIndex() + "/" + URLEncoder.encode(otherFileName, "UTF-8");
        
        // add incomplete file to file manager
        File incFile = new File(_incompleteDir, incName);
        fileManager.removeFileIfSharedOrStore(incFile);
        CommonUtils.copyResourceFile(testDirName + "/" + incName, incFile, false);
        URN urn = URN.createSHA1Urn(incompleteHash);
        Set<URN> urns = new HashSet<URN>();
        urns.add(urn);
        vf = injector.getInstance(VerifyingFileFactory.class).createVerifyingFile(252450);
        fileManager.addIncompleteFile(incFile, urns, incName, 1981, vf);
        incompleteHashUrl = host + incompleteHashUrl;

        assertEquals(1, fileManager.getNumIncompleteFiles());
        assertEquals(2, fileManager.getNumFiles());
        assertEquals("Unexpected uploads in progress", 0, uploadManager.uploadsInProgress());
        assertEquals("Unexpected queued uploads", 0, uploadManager.getNumQueuedUploads());

        client = injector.getInstance(LimeHttpClient.class);
        
        //client = new DefaultHttpClient();
        //Scheme https = client.getConnectionManager().getSchemeRegistry().getScheme("https");
        //Scheme tls = new Scheme("tls", https.getSocketFactory(), https.getDefaultPort());
        //client.getConnectionManager().getSchemeRegistry().register(tls);
        HttpConnectionParams.setConnectionTimeout(client.getParams(), 500);
        HttpConnectionParams.setSoTimeout(client.getParams(), 2000);        
    }

    @Override
    public void tearDown() throws Exception {
        stopServices();
        LimeTestUtils.waitForNIO();
    }

    private void doSettings() throws UnknownHostException {
        SharingSettings.ADD_ALTERNATE_FOR_SELF.setValue(false);
        FilterSettings.BLACK_LISTED_IP_ADDRESSES
                .setValue(new String[] { "*.*.*.*" });
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(new String[] {
                "127.*.*.*", InetAddress.getLocalHost().getHostAddress() });
        NetworkSettings.PORT.setValue(PORT);

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

    private void startServices() throws Exception {
        HTTPAcceptor httpAcceptor = injector.getInstance(HTTPAcceptor.class);
        httpAcceptor.start();
        
        uploadManager = injector.getInstance(UploadManager.class);
        uploadManager.start();
        
        // make sure the FileDesc objects in file manager are up-to-date
        fileManager = injector.getInstance(FileManager.class);
        fileManager.startAndWait(2000);
        
        ConnectionDispatcher connectionDispatcher = injector.getInstance(Key.get(ConnectionDispatcher.class, Names.named("global")));
        connectionDispatcher.addConnectionAcceptor(httpAcceptor, false, httpAcceptor.getHttpMethods());
        
        Acceptor acceptor = injector.getInstance(Acceptor.class);        
        acceptor.setListeningPort(PORT);
        acceptor.start();
        
        LimeTestUtils.waitForNIO();
    }

    private void stopServices() throws Exception {
        Acceptor acceptor = injector.getInstance(Acceptor.class);
        acceptor.setListeningPort(0);
        acceptor.shutdown();
    }    
    
    public void testHTTP10Download() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        HttpProtocolParams.setVersion(client.getParams(), HttpVersion.HTTP_1_0);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("abcdefghijklmnopqrstuvwxyz", result);
        } finally {                                
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP10DownloadRange() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        method.addHeader("Range", "bytes=2-");
        HttpProtocolParams.setVersion(client.getParams(), HttpVersion.HTTP_1_0);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("cdefghijklmnopqrstuvwxyz", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP10DownloadMissingRange() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        method.addHeader("Range", "bytes 2-");
        HttpProtocolParams.setVersion(client.getParams(), HttpVersion.HTTP_1_0);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("cdefghijklmnopqrstuvwxyz", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP10DownloadMiddleRange() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        method.addHeader("Range", "bytes 2-5");
        HttpProtocolParams.setVersion(client.getParams(), HttpVersion.HTTP_1_0);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response.getStatusLine().getStatusCode());
            assertNotNull(response.getFirstHeader("Content-range"));
            assertEquals("bytes 2-5/26", response.getFirstHeader(
                    "Content-range").getValue());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("cdef", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP10DownloadRangeNoSpace() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        method.addHeader("Range", "bytes 2-5");
        HttpProtocolParams.setVersion(client.getParams(), HttpVersion.HTTP_1_0);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("cdef", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP10DownloadRangeLastByte() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        method.addHeader("Range", "bytes=-5");
        HttpProtocolParams.setVersion(client.getParams(), HttpVersion.HTTP_1_0);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("vwxyz", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP10DownloadRangeTooBigNegative() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        method.addHeader("Range", "bytes=-30");
        HttpProtocolParams.setVersion(client.getParams(), HttpVersion.HTTP_1_0);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("abcdefghijklmnopqrstuvwxyz", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP10DownloadRangeExtraSpace() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        method.addHeader("Range", "   bytes=  2  -  5 ");
        HttpProtocolParams.setVersion(client.getParams(), HttpVersion.HTTP_1_0);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("cdef", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }
    
    public void testHTTP10DownloadHead() throws Exception {
        HttpHead method = new HttpHead(fileNameUrl);
        HttpProtocolParams.setVersion(client.getParams(), HttpVersion.HTTP_1_0);
        method.addHeader("Range", "bytes 2-5");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response.getStatusLine().getStatusCode());
            assertNotNull(response.getFirstHeader("Content-range"));
            assertEquals("bytes 2-5/26", response.getFirstHeader(
                    "Content-range").getValue());
            assertNull(response.getEntity());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP11DownloadNoRangeHeader() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("abcdefghijklmnopqrstuvwxyz", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP11Download() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("abcdefghijklmnopqrstuvwxyz", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP11DownloadRange() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        method.addHeader("Range", "bytes=2-");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("cdefghijklmnopqrstuvwxyz", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP11DownloadInvalidRange() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        method.addHeader("Range", "bytes=-0");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE, response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP11DownloadMissingRange() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        method.addHeader("Range", "bytes 2-");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("cdefghijklmnopqrstuvwxyz", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP11DownloadMiddleRange() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        method.addHeader("Range", "bytes 2-5");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response.getStatusLine().getStatusCode());
            assertNotNull(response.getFirstHeader("Content-range"));
            assertEquals("bytes 2-5/26", response.getFirstHeader(
                    "Content-range").getValue());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("cdef", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP11DownloadRangeNoSpace() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        method.addHeader(new BasicHeader(
                "Range", "bytes 2-5"));/* {
            public String toExternalForm() {
                return "Range:bytes 2-5\r\n";
            }

            ;
        });*/
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("cdef", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP11DownloadRangeLastByte() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        method.addHeader("Range", "bytes=-5");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("vwxyz", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP11DownloadRangeTooBigNegative() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        method.addHeader("Range", "bytes=-30");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("abcdefghijklmnopqrstuvwxyz", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP11IncompleteRange() throws Exception {
        Range iv = Range.createRange(2, 6);
        IntervalSet vb = (IntervalSet) PrivilegedAccessor.getValue(vf,
                "verifiedBlocks");
        vb.add(iv);

        HttpGet method = new HttpGet(incompleteHashUrl);
        method.addHeader("Range", "bytes 2-5");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("cdef", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
        method = new HttpGet(incompleteHashUrl);
        method.addHeader("Range", "bytes 1-3");
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("cd", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        method = new HttpGet(incompleteHashUrl);
        method.addHeader("Range", "bytes 3-10");
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("defg", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
        method = new HttpGet(incompleteHashUrl);
        method.addHeader("Range", "bytes 0-20");
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("cdefg", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP11PipeliningDownload() throws Exception {
        HttpUploadClient client = new HttpUploadClient();
        try {
            client.connect("localhost", PORT);
            HttpRequest request = new BasicHttpRequest("GET", fileNameUrl.substring(host.length()));
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
        http11DownloadPersistentHeadFollowedByGet(host + "/uri-res/N2R?" + hash);
    }

    private void http11DownloadPersistentHeadFollowedByGet(String url)
            throws IOException, URISyntaxException, HttpException, InterruptedException {
        HttpUriRequest method = new HttpHead(url);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        } finally {
            client.releaseConnection(response);
        }
        method = new HttpGet(url);
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("abcdefghijklmnopqrstuvwxyz", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
        method = new HttpGet(url);
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("abcdefghijklmnopqrstuvwxyz", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        method = new HttpHead(url);
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
        method = new HttpGet(url);
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("abcdefghijklmnopqrstuvwxyz", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testLongHeader() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("1234567890");
        }

        HttpGet method = new HttpGet(fileNameUrl);
        method.addHeader("Header", sb.toString());
        HttpResponse response = null;
        try {
            response = client.execute(method);
            fail("Expected remote end to close connection, got: " + response);
        } catch (IOException expected) {
            // expected result
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        // request a very long filename
        method = new HttpGet(host + "/" + sb.toString());
        try {
            response = client.execute(method);
            fail("Expected remote end to close connection, got: " + response);
        } catch (IOException expected) {
            // expected result
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testLongFoldedHeader() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("1234567890");
        for (int i = 0; i < 1000; i++) {
            sb.append("\n 1234567890");
        }

        HttpGet method = new HttpGet(fileNameUrl);
        method.addHeader("Header", sb.toString());
        HttpResponse response = null;
        try {
            response = client.execute(method);
            fail("Expected remote end to close connection, got: " + response);
        } catch (IOException expected) {
            // expected result
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testManyHeaders() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        for (int i = 0; i < 100; i++) {
            method.addHeader("Header", "abc");
        }
        HttpResponse response = null;
        try {
            response = client.execute(method);
            fail("Expected remote end to close connection, got: " + response);
        } catch (IOException expected) {
            // expected result
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void test10KBRequest() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("1234567890");
        }

        HttpGet method = new HttpGet(fileNameUrl);
        // add headers with a size of 1000 byte each
        for (int i = 0; i < 10; i++) {
            method.addHeader("Header", sb.toString());
        }

        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }
    
    public void test200KBRequest() throws Exception {
        // 4088 byte, leave some space for "Header: "
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 511; i++) {
            sb.append("1234578");
        }

        HttpGet method = new HttpGet(fileNameUrl);
        // add headers with a size of 4 kb each
        // HttpClient will add a few standard headers, so we can't add the full 50 headers
        for (int i = 0; i < 40; i++) {
            method.addHeader("Header", sb.toString());
        }

        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

    }
    
    public void testHeaderLengthThatMatchesCharacterBufferLength()
            throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        StringBuilder sb = new StringBuilder(512);
        for (int i = 0; i < 512 - 3; i++) {
            sb.append("a");
        }
        method.addHeader("A", sb.toString());
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testInvalidCharactersInRequest() throws Exception {
        // build request with non US-ASCII characters
        String url = new String(new char[] { 0x47, 0x72, 0xFC, 0x65, 0x7A,
                0x69, 0x5F, 0x7A, 0xE4, 0x6D, 0xE4 });

        // use home grown test client since HttpClient will not send theses
        // characters in the first place
        HttpUploadClient client = new HttpUploadClient();
        try {
            client.connect("localhost", PORT);
            HttpRequest request = new BasicHttpRequest("GET", "/get/0/" + url);
            client.writeRequest(request);
            HttpResponse response = client.readResponse();
            assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
            // this test used to expect a closed stream but LimeWire now accepts 
            // non US-ASCII characters: CORE-225 
//            InputStream in = client.getSocket().getInputStream();
//            assertEquals("Expected remote end to close socket", -1, in.read());
        } finally {
            client.close();
        }
    }

    public void testInvalidCharactersInHeader() throws Exception {
        final String value = new String(new char[] { 0x31 + 0x32 + 0x38 + 0x2C
                + 0xE9 + 0x5E + 0xB0 + 0xE7 + 0x88 + 0x76 + 0xF1 + 0xCA + 0x51
                + 0x8F + 0x6D + 0xBF + 0xC8 + 0xAA + 0x82 + 0x6C + 0x33 + 0x35
                + 0x25 + 0x25 + 0x86 + 0xCC + 0xBF + 0x7E + 0xC5 + 0xEE + 0x58
                + 0x96 + 0xB6 + 0x2D + 0x7E + 0x9E + 0x21 + 0xD5 });
        
        // use home grown test client since HttpClient will not send theses
        // characters in the first place
        HttpUploadClient client = new HttpUploadClient();
        try {
            client.connect("localhost", PORT);
            HttpRequest request = new BasicHttpRequest("GET", "/uri-res/N2R?" + hash);
            request.addHeader("FP-1a: ", value);
            client.writeRequest(request);
            HttpResponse response = client.readResponse();
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        } finally {
            client.close();
        }
    }
    
    public void testFreeloader() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        method.setHeader("User-Agent", "Mozilla");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            String body = result;
            if (!body.startsWith("<html>")) {
                fail("Expected free loader page, got: " + body);
            }
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    /**
     * Tests the case of requests for different file over the same HTTP session.
     */
    public void testMultipleUploadSession() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("abcdefghijklmnopqrstuvwxyz", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
        assertConnectionIsOpen(true, method);

        method = new HttpGet(otherFileNameUrl);
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZ\n", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testIncompleteFileUpload() throws Exception {
        HttpGet method = new HttpGet(host + "/uri-res/N2R?" + incompleteHash);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE,
                    response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        assertConnectionIsOpen(true, method);

        method = new HttpGet(host + "/uri-res/N2R?" + incompleteHash);
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE,
                    response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testIncompleteFileWithRanges() throws Exception {
        // add a range to the incomplete file.
        Range iv = Range.createRange(50, 102500);
        IntervalSet vb = (IntervalSet) PrivilegedAccessor.getValue(vf,
                "verifiedBlocks");
        vb.add(iv);

        HttpGet method = new HttpGet(host + "/uri-res/N2R?" + incompleteHash);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE,
                    response.getStatusLine().getStatusCode());
            assertNotNull(response.getFirstHeader("X-Available-Ranges"));
            assertEquals("bytes 50-102499", response.getFirstHeader(
                    "X-Available-Ranges").getValue());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        assertConnectionIsOpen(true, method);

        // add another range and make sure we display it.
        iv = Range.createRange(150050, 252450);
        vb.add(iv);
        method = new HttpGet(host + "/uri-res/N2R?" + incompleteHash);
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE,
                    response.getStatusLine().getStatusCode());
            assertNotNull(response.getFirstHeader("X-Available-Ranges"));
            assertEquals("bytes 50-102499, 150050-252449", response
                    .getFirstHeader("X-Available-Ranges").getValue());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        assertConnectionIsOpen(true, method);

        // add an interval too small to report and make sure we don't report
        iv = Range.createRange(102505, 150000);
        vb.add(iv);
        method = new HttpGet(host + "/uri-res/N2R?" + incompleteHash);
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE,
                    response.getStatusLine().getStatusCode());
            assertNotNull(response.getFirstHeader("X-Available-Ranges"));
            assertEquals("bytes 50-102499, 150050-252449", response
                    .getFirstHeader("X-Available-Ranges").getValue());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        assertConnectionIsOpen(true, method);

        // add the glue between the other intervals and make sure we condense
        // the ranges into a single larger range.
        iv = Range.createRange(102500, 102505);
        vb.add(iv);
        iv = Range.createRange(150000, 150050);
        vb.add(iv);
        method = new HttpGet(host + "/uri-res/N2R?" + incompleteHash);
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE,
                    response.getStatusLine().getStatusCode());
            assertNotNull(response.getFirstHeader("X-Available-Ranges"));
            assertEquals("bytes 50-252449", response.getFirstHeader(
                    "X-Available-Ranges").getValue());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testIncompleteFileWithRangeRequest() throws Exception {
        HttpGet method = new HttpGet(host + "/uri-res/N2R?" + incompleteHash);
        method.addHeader("Range", "bytes 20-40");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE,
                    response.getStatusLine().getStatusCode());
            assertEquals("Requested Range Unavailable", response.getStatusLine().getReasonPhrase());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        assertConnectionIsOpen(true, method);
    }

    public void testHTTP11WrongURI() throws Exception {
        HttpGet method = new HttpGet(host + "/uri-res/N2R?" + badHash);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
            assertEquals("Not Found", response.getStatusLine().getReasonPhrase());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        assertConnectionIsOpen(true, method);
    }

    public void testHTTP10WrongURI() throws Exception {
        HttpGet method = new HttpGet(host + "/uri-res/N2R?" + badHash);
        HttpProtocolParams.setVersion(client.getParams(), HttpVersion.HTTP_1_0);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
            assertEquals("Not Found", response.getStatusLine().getReasonPhrase());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        assertConnectionIsOpen(true, method);
    }

    public void testHTTP11MalformedURI() throws Exception {
        HttpGet method = new HttpGet(host + "/uri-res/N2R?" + "no%20more%20school");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
            assertEquals("Malformed Request", response.getStatusLine().getReasonPhrase());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        assertConnectionIsOpen(false, method);
    }

    public void testHTTP10MalformedURI() throws Exception {
        HttpGet method = new HttpGet(host + "/uri-res/N2R?" + "%20more%20school");
        HttpProtocolParams.setVersion(client.getParams(), HttpVersion.HTTP_1_0);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
            assertEquals("Malformed Request", response.getStatusLine().getReasonPhrase());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        assertConnectionIsOpen(false, method);
    }

    public void testHTTP11MalformedGet() throws Exception {
        HttpGet method = new HttpGet(host + "/get/some/dr/pepper");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
            assertEquals("Malformed Request", response.getStatusLine().getReasonPhrase());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        assertConnectionIsOpen(false, method);
    }

    public void testHTTP11MalformedHeader() throws Exception {
        HttpGet method = new HttpGet(host + "/uri-res/N2R?" + hash);
        method.addHeader("Range", "2-5");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
            assertEquals("Malformed Request", response.getStatusLine().getReasonPhrase());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        assertConnectionIsOpen(false, method);
    }

    public void testHTTP11Post() throws Exception {
        HttpPost method = new HttpPost(host + "/uri-res/N2R?" + hash);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            fail("Expected HttpConnection due to connection close, got: " + response);
        } catch (IOException expected) {
            // the connection was either closed or timed out due to a failed TLS
            // handshake
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
        assertConnectionIsOpen(false, method);
    }

    public void testHTTP11ExpectContinue() throws Exception {
        HttpPost method = new HttpPost(host + "/uri-res/N2R?" + hash) {
            @Override
            public String getMethod() {
                return "GET";
             }
        };
        // TODO method.setUseExpectHeader(true);
        method.setEntity(new NStringEntity("Foo"));
        HttpResponse response = null;
        try {
            response = client.execute(method);
            // since this is actually a GET request and not a POST 
            // the Expect header should be ignored
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        } catch (HttpException expected) {
            // expected result
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    /**
     * Test that creation time header is returned.
     */
    public void testCreationTimeHeaderReturned() throws Exception {
        // assert that creation time exists
        URN urn = URN.createSHA1Urn(hash);
        Long creationTime = injector.getInstance(CreationTimeCache.class).getCreationTime(urn);
        assertNotNull(creationTime);
        assertTrue(creationTime.longValue() > 0);

        HttpGet method = new HttpGet(host + "/uri-res/N2R?" + hash);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            assertNotNull(response.getFirstHeader("X-Create-Time"));
            assertEquals(creationTime + "", response.getFirstHeader(
                    "X-Create-Time").getValue());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testCreationTimeHeaderReturnedForIncompleteFile()
            throws Exception {
        Range iv = Range.createRange(2, 5);
        IntervalSet vb = (IntervalSet) PrivilegedAccessor.getValue(vf,
                "verifiedBlocks");
        vb.add(iv);

        URN urn = URN.createSHA1Urn(incompleteHash);
        Long creationTime = new Long("10776");
        injector.getInstance(CreationTimeCache.class).addTime(urn, creationTime.longValue());

        HttpGet method = new HttpGet(host + "/uri-res/N2R?" + incompleteHash);
        method.addHeader("Range", "bytes 2-5");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("cdef", result);
            assertNotNull(response.getFirstHeader("X-Create-Time"));
            assertEquals(creationTime + "", response.getFirstHeader(
                    "X-Create-Time").getValue());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testChatFeatureHeader() throws Exception {
        ChatSettings.CHAT_ENABLED.setValue(true);
        HttpGet method = new HttpGet(fileNameUrl);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            assertNotNull(response.getFirstHeader("X-Features"));
            String header = response.getFirstHeader("X-Features").getValue();
            assertTrue(header.contains("fwalt/0.1"));
            assertTrue(header.contains("browse/1.0"));
            assertTrue(header.contains("chat/0.1"));
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        assertConnectionIsOpen(true, method);

        // feature headers are only sent with the first response
        ChatSettings.CHAT_ENABLED.setValue(false);
        method = new HttpGet(fileNameUrl);
        method.addHeader("Connection", "close");
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            assertNull(method.getFirstHeader("X-Features"));
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        assertConnectionIsOpen(false, method);

        // try a new connection
        method = new HttpGet(fileNameUrl);
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            assertNotNull(response.getFirstHeader("X-Features"));
            String header = response.getFirstHeader("X-Features").getValue();
            assertTrue(header.contains("fwalt/0.1"));
            assertTrue(header.contains("browse/1.0"));
            assertFalse(header.contains("chat/0.1"));
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testThexHeader() throws Exception {
        HashTreeCache tigerTreeCache = injector.getInstance(HashTreeCache.class);
        HashTree tree = getThexTree(tigerTreeCache);

        HttpGet method = new HttpGet(fileNameUrl);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            assertNotNull(response.getFirstHeader("X-Thex-URI"));
            String header = response.getFirstHeader("X-Thex-URI").getValue();
            assertEquals("/uri-res/N2X?" + hash + ";" + tree.getRootHash(), header);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testDownloadFromBitprintUrl() throws Exception {
        HashTreeCache tigerTreeCache = injector.getInstance(HashTreeCache.class);
        HashTree tree = getThexTree(tigerTreeCache);

        HttpGet method = new HttpGet(host + "/uri-res/N2R?urn:bitprint:"
                + baseHash + "." + tree.getRootHash());
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("abcdefghijklmnopqrstuvwxyz", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        // the request is checked for a valid bitprint length
        method = new HttpGet(host + "/uri-res/N2R?urn:bitprint:" + baseHash + "."
                + "asdoihffd");
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        // but not for the valid base32 root -- in the future we may
        // and this test will break
        method = new HttpGet(host + "/uri-res/N2R?urn:bitprint:" + baseHash + "."
                + "SAMUWJUUSPLMMDUQZOWX32R6AEOT7NCCBX6AGBI");
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("abcdefghijklmnopqrstuvwxyz", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        // make sure "bitprint:" is required for bitprint uploading.
        method = new HttpGet(host + "/uri-res/N2R?urn:sha1:" + baseHash + "."
                + tree.getRootHash());
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }
    
    public void testBadGetTreeRequest() throws Exception {
        HttpGet method = new HttpGet(host + "/uri-res/N2X?" + badHash);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        method = new HttpGet(host + "/uri-res/N2X?" + "no_hash");
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testGetTree() throws Exception {
        HashTreeCache tigerTreeCache = injector.getInstance(HashTreeCache.class);
        HashTree tree = getThexTree(tigerTreeCache);

        HttpGet method = new HttpGet(host + "/uri-res/N2X?" + hash);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            DIMEParser parser = new DIMEParser(response.getEntity().getContent());
            parser.nextRecord(); // xml
            DIMERecord record = parser.nextRecord();
            assertFalse(parser.hasNext());
            List<List<byte[]>> allNodes = HashTreeUtils.createAllParentNodes(tree.getNodes(), new Tiger());
            byte[] data = record.getData();
            int offset = 0;
            for (Iterator<List<byte[]>> genIter = allNodes.iterator(); genIter
                    .hasNext();) {
                for (Iterator<byte[]> it = genIter.next().iterator(); it
                        .hasNext();) {
                    byte[] current = it.next();
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
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testGetNonExistingTree() throws Exception {
        HashTreeCache tigerTreeCache = injector.getInstance(HashTreeCache.class);
        
        URN urn = URN.createSHA1Urn(hash);
        tigerTreeCache.purgeTree(urn);
        HttpGet method = new HttpGet(host + "/uri-res/N2X?" + hash);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testDownloadChangedFile() throws Exception {
        // modify shared file and make sure it gets new timestamp
        Thread.sleep(1000);
        File file = new File(_sharedDir, fileName);
        FileDesc fd = fileManager.getFileDescForFile(file);        
        FileOutputStream out = new FileOutputStream(file);
        try {
            out.write("abc".getBytes());
        } finally {
            out.close();
        }
        assertNotEquals(file.lastModified(), fd.lastModified());

        // catch notification when file is reshared
        final CountDownLatch latch = new CountDownLatch(1);
        FileEventListener listener = new FileEventListener() {
            public void handleFileEvent(FileManagerEvent event) {
                if (event.isAddEvent()) {
                    latch.countDown();
                }
            }            
        };
        try {
            fileManager.addFileEventListener(listener);

            HttpGet method = new HttpGet(host + "/get/" + fd.getIndex() + "/" + URLEncoder.encode(fd.getFileName(), "US-ASCII"));
            HttpResponse response = null;
            try {
                response = client.execute(method);
                assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
            } finally {
                HttpClientUtils.releaseConnection(response);
            }

            latch.await(500, TimeUnit.MILLISECONDS);

            fd = fileManager.getFileDescForFile(file);
            assertNotNull(fd);
            method = new HttpGet(host + "/get/" + fd.getIndex() + "/" + URLEncoder.encode(fd.getFileName(), "US-ASCII"));
            try {
                response = client.execute(method);
                assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
                String result;
                if (response.getEntity() != null) {
                    result = EntityUtils.toString(response.getEntity());
                } else {
                    result = null;
                }
                assertEquals("abc", result);
            } finally {
                HttpClientUtils.releaseConnection(response);
            }
        } finally {
            fileManager.removeFileEventListener(listener);
        }
        
    }

    private void assertConnectionIsOpen(boolean open, HttpUriRequest request) throws InterruptedException, ConnectionPoolTimeoutException {
        URI uri = request.getURI();
        HttpRoute route = new HttpRoute(new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme()));
        ManagedClientConnection connection = client.getConnectionManager().requestConnection(route, null).getConnection(0, null);
        assertEquals(open, connection.isOpen());
        client.getConnectionManager().releaseConnection(connection);
    }

    private HashTree getThexTree(HashTreeCache tigerTreeCache) throws Exception {
        FileDesc fd = fileManager.getFileDescForFile(new File(_sharedDir, fileName));
        return ((HashTreeCacheImpl)tigerTreeCache).getHashTreeAndWait(fd, 1000);
    }

}
