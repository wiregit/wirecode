package com.limegroup.gnutella;

import java.io.File;
import java.io.IOException;

import junit.framework.Test;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpRequest;
import org.limewire.util.CommonUtils;

import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HTTPUtils;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.xml.MetaFileManager;

/**
 * This class tests HTTP requests involving URNs, as specified in HUGE v094,
 * utilizing the X-Gnutella-Content-URN header and the
 * X-Gnutella-Alternate-Location header.
 */
public final class UrnHttpRequestTest extends LimeTestCase {

    private static RouterService ROUTER_SERVICE;

    private static final String STATUS_503 = "HTTP/1.1 503 Service Unavailable";

    private static final String STATUS_404 = "HTTP/1.1 404 Not Found";

    private static MetaFileManager fm;

    private HTTPAcceptor acceptor;

    private HTTPUploadManager uploadManager;

    public UrnHttpRequestTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(UrnHttpRequestTest.class);
    }

    public static void globalSetUp() throws Exception {
        // create shared files
        File TEMP_DIR = new File("temp");
        TEMP_DIR.mkdirs();
        TEMP_DIR.deleteOnExit();

        String dirString = "com/limegroup/gnutella";
        File testDir = CommonUtils.getResourceFile(dirString);
        assertTrue("could not find the images directory", testDir.isDirectory());
        File[] files = testDir.listFiles();

        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (!files[i].isFile())
                    continue;
                CommonUtils.copyResourceFile(dirString + "/"
                        + files[i].getName(), new File(TEMP_DIR, files[i]
                        .getName()
                        + ".tmp"));
            }
        }

        // TODO remove me: need to initialize call back
        ROUTER_SERVICE = new RouterService(new ActivityCallbackStub());
        
        setSharedDirectories(new File[] { TEMP_DIR });
        SharingSettings.EXTENSIONS_TO_SHARE.setValue("tmp");

        fm = new MetaFileManager();
        fm.startAndWait(4000);

        assertGreaterThan("FileManager should have loaded files", 4, fm
                .getNumFiles());
    }

    public static void globalTearDown() throws Exception {
        fm.stop();
        
        ROUTER_SERVICE = null;
    }

    /**
     * Runs this test individually.
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    protected void setUp() throws Exception {
        if (ROUTER_SERVICE == null) {
            globalSetUp();
        }
        
        acceptor = new HTTPAcceptor();
        
        uploadManager = new HTTPUploadManager(RouterService.getUploadSlotManager());
        uploadManager.setFileManager(fm);
        uploadManager.start(acceptor);
    }

    @Override
    protected void tearDown() throws Exception {
        uploadManager.stop(acceptor);
    }

    /**
     * Tests requests that follow the traditional "get" syntax to make sure that
     * the X-Gnutella-Content-URN header is always returned.
     */
    public void testLimitReachedRequests() throws Exception {
        int maxUploads = UploadSettings.HARD_MAX_UPLOADS.getValue();
        UploadSettings.HARD_MAX_UPLOADS.setValue(0);
        
        try {
            for (int i = 0; i < fm.getNumFiles(); i++) {
                FileDesc fd = fm.get(i);
                String uri = "/get/" + fd.getIndex() + "/" + fd.getFileName();

                BasicHttpRequest request = new BasicHttpRequest("GET", uri,
                        HttpVersion.HTTP_1_1);
                request.addHeader(HTTPHeaderName.GNUTELLA_CONTENT_URN.create(fd
                        .getSHA1Urn()));

                sendRequestThatShouldFail(request, STATUS_503);
                // sendRequestThatShouldFail(HTTPRequestMethod.HEAD, request, fd,
                // STATUS_503);
            }
        } finally {
            UploadSettings.HARD_MAX_UPLOADS.setValue(maxUploads);
        }
    }

    /**
     * Test requests by URN.
     */
    public void testHttpUrnRequest() throws Exception {
        for (int i = 0; i < fm.getNumFiles(); i++) {
            FileDesc fd = fm.get(i);
            String uri = "/uri-res/N2R?" + fd.getSHA1Urn().httpStringValue();

            BasicHttpRequest request = new BasicHttpRequest("GET", uri,
                    HttpVersion.HTTP_1_1);
            sendRequestThatShouldSucceed(request, fd);

            request = new BasicHttpRequest("HEAD", uri, HttpVersion.HTTP_1_1);
            sendRequestThatShouldSucceed(request, fd);
        }
    }

    /**
     * Test requests by URN that came from LimeWire 2.8.6.
     * /get/0//uri-res/N2R?urn:sha1:AZUCWY54D63______PHN7VSVTKZA3YYT HTTP/1.1
     */
    public void testMalformedHttpUrnRequest() throws Exception {
        for (int i = 0; i < fm.getNumFiles(); i++) {
            FileDesc fd = fm.get(i);
            String uri = "/get/0//uri-res/N2R?"
                    + fd.getSHA1Urn().httpStringValue();

            BasicHttpRequest request = new BasicHttpRequest("GET", uri,
                    HttpVersion.HTTP_1_1);
            sendRequestThatShouldFail(request, STATUS_404);

            request = new BasicHttpRequest("HEAD", uri, HttpVersion.HTTP_1_1);
            sendRequestThatShouldFail(request, STATUS_404);
        }
    }

    /**
     * Tests requests that follow the traditional "get" syntax to make sure that
     * the X-Gnutella-Content-URN header is always returned.
     */
    public void testTraditionalGetForReturnedUrn() throws Exception {
        for (int i = 0; i < fm.getNumFiles(); i++) {
            FileDesc fd = fm.get(i);
            String uri = "/get/" + fd.getIndex() + "/" + fd.getFileName();

            BasicHttpRequest request = new BasicHttpRequest("GET", uri,
                    HttpVersion.HTTP_1_1);
            request.addHeader(HTTPHeaderName.GNUTELLA_CONTENT_URN.create(fd
                    .getSHA1Urn()));
            sendRequestThatShouldSucceed(request, fd);

            request = new BasicHttpRequest("HEAD", uri, HttpVersion.HTTP_1_1);
            request.addHeader(HTTPHeaderName.GNUTELLA_CONTENT_URN.create(fd
                    .getSHA1Urn()));
            sendRequestThatShouldSucceed(request, fd);
        }
    }

    /**
     * Tests requests that follow the traditional "get" syntax but that also
     * include the X-Gnutella-Content-URN header. In these requests, both the
     * URN and the file name and index are correct, so a valid result is
     * expected.
     */
    public void testTraditionalGetWithContentUrn() throws Exception {
        for (int i = 0; i < fm.getNumFiles(); i++) {
            FileDesc fd = fm.get(i);
            String uri = "/get/" + fd.getIndex() + "/" + fd.getFileName();

            BasicHttpRequest request = new BasicHttpRequest("GET", uri,
                    HttpVersion.HTTP_1_1);
            sendRequestThatShouldSucceed(request, fd);

            request = new BasicHttpRequest("HEAD", uri, HttpVersion.HTTP_1_1);
            sendRequestThatShouldSucceed(request, fd);
        }
    }

    /**
     * Tests get requests that follow the traditional Gnutella get format and
     * that include an invalid content URN header -- these should fail with
     * error code 404.
     */
    public void testTraditionalGetWithInvalidContentUrn() throws Exception {
        for (int i = 0; i < fm.getNumFiles(); i++) {
            FileDesc fd = fm.get(i);
            String uri = "/get/" + fd.getIndex() + "/" + fd.getFileName();

            BasicHttpRequest request = new BasicHttpRequest("GET", uri,
                    HttpVersion.HTTP_1_1);
            request.addHeader(HTTPHeaderName.GNUTELLA_CONTENT_URN
                    .create("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB"));
            sendRequestThatShouldFail(request, STATUS_404);

            request = new BasicHttpRequest("HEAD", uri, HttpVersion.HTTP_1_1);
            request.addHeader(HTTPHeaderName.GNUTELLA_CONTENT_URN
                    .create("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB"));
            sendRequestThatShouldFail(request, STATUS_404);
        }
    }

    /**
     * Tests to make sure that invalid traditional Gnutella get requests with
     * matching X-Gnutella-Content-URN header values also fail with 404.
     */
    public void testInvalidTraditionalGetWithValidContentUrn() throws Exception {
        for (int i = 0; i < fm.getNumFiles(); i++) {
            FileDesc fd = fm.get(i);
            String uri = "/get/" + fd.getIndex() + "/" + fd.getFileName()
                    + "invalid";

            BasicHttpRequest request = new BasicHttpRequest("GET", uri,
                    HttpVersion.HTTP_1_1);
            sendRequestThatShouldFail(request, STATUS_404);

            request = new BasicHttpRequest("HEAD", uri, HttpVersion.HTTP_1_1);
            sendRequestThatShouldFail(request, STATUS_404);
        }
    }

    /**
     * Sends an HTTP request that should succeed and send back all of the
     * expected headers.
     */
    private void sendRequestThatShouldSucceed(HttpRequest request, FileDesc fd)
            throws Exception {
        HttpResponse response = acceptor.process(request);
        assertEquals(200, response.getStatusLine().getStatusCode());

        // clean up any created uploaders
        uploadManager.cleanup();

        boolean contentUrnHeaderPresent = false;
        Header[] headers = response.getAllHeaders();
        assertTrue("HTTP response headers should be present: " + fd,
                headers.length > 0);
        for (Header header : headers) {
            String curString = header.toString();
            if (HTTPHeaderName.ALT_LOCATION.matchesStartOfString(curString)) {
                continue;
            } else if (HTTPHeaderName.GNUTELLA_CONTENT_URN
                    .matchesStartOfString(curString)) {
                URN curUrn = null;
                try {
                    String tmpString = HTTPUtils.extractHeaderValue(curString);
                    curUrn = URN.createSHA1Urn(tmpString);
                } catch (IOException e) {
                    assertTrue("unexpected exception: " + e, false);
                }
                assertEquals(HTTPHeaderName.GNUTELLA_CONTENT_URN.toString()
                        + "s should be equal for " + fd, fd.getSHA1Urn(),
                        curUrn);
                contentUrnHeaderPresent = true;
            } else if (HTTPHeaderName.CONTENT_RANGE
                    .matchesStartOfString(curString)) {
                continue;
            } else if (HTTPHeaderName.CONTENT_TYPE
                    .matchesStartOfString(curString)) {
                continue;
            } else if (HTTPHeaderName.CONTENT_LENGTH
                    .matchesStartOfString(curString)) {
                String value = HTTPUtils.extractHeaderValue(curString);
                assertEquals("sizes should match for " + fd, (int) fd
                        .getFileSize(), Integer.parseInt(value));
            } else if (HTTPHeaderName.SERVER.matchesStartOfString(curString)) {
                continue;
            }
        }
        assertTrue("content URN header should always be reported:\r\n" + fd
                + "\r\n" + "reply: " + response, contentUrnHeaderPresent);
    }

    /**
     * Sends an HTTP request that should fail if everything is working
     * correctly.
     */
    private void sendRequestThatShouldFail(HttpRequest request, String error)
            throws Exception {
        HttpResponse response = acceptor.process(request);
        assertEquals("unexpected HTTP response", error,
                getStatusLine(response));
    }

    private String getStatusLine(HttpResponse response) {
        return response.getHttpVersion() + " "
                + response.getStatusLine().getStatusCode() + " "
                + response.getStatusLine().getReasonPhrase();
    }

}
