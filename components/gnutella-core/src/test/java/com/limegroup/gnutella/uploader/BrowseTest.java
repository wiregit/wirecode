package com.limegroup.gnutella.uploader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.params.HttpProtocolParams;
import org.limewire.http.httpclient.HttpClientUtils;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.util.TestUtils;

import com.google.inject.Injector;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.NetworkSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.util.LimeTestCase;

import junit.framework.Test;

/**
 * Test that a client uploads a list of files correctly.
 */
public class BrowseTest extends LimeTestCase {

    private final int PORT = 6668;

    private File sharedDirectory;

    private HttpClient client;

    protected String protocol;

    private FileManager fileManager;

    private MessageFactory messageFactory;
    
    private String host;
    
    public BrowseTest(String name) {
        super(name);
        protocol = "http";
    }

    public static Test suite() {
        return buildTestSuite(BrowseTest.class);
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }

    @Override
    protected void setUp() throws Exception {

        String directoryName = "com/limegroup/gnutella";
        sharedDirectory = TestUtils.getResourceFile(directoryName);
        sharedDirectory = sharedDirectory.getCanonicalFile();
        assertTrue("Could not find directory: " + directoryName,
                sharedDirectory.isDirectory());

        File[] testFiles = sharedDirectory.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return !file.isDirectory() && file.getName().endsWith(".class");
            }
        });
        assertNotNull("No files to test against", testFiles);
        assertGreaterThan("Not enough files to test against", 50,
                testFiles.length);

    
        SharingSettings.EXTENSIONS_TO_SHARE.setValue("class");
        SharingSettings.DIRECTORIES_TO_SHARE.setValue(Collections
                .singleton(sharedDirectory));

        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        NetworkSettings.PORT.setValue(PORT);

        Injector injector = LimeTestUtils.createInjectorAndStart();
        
        fileManager = injector.getInstance(FileManager.class);
        messageFactory = injector.getInstance(MessageFactory.class);
        client = injector.getInstance(LimeHttpClient.class);
        
        fileManager.loadSettingsAndWait(100000);
        
        host = protocol + "://localhost:" + PORT;
    }

    public void testBrowse() throws Exception {
        HttpGet method = new HttpGet(host + "/");
        method.addHeader("Accept", "application/x-gnutella-packets");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

            InputStream in = new BufferedInputStream(response.getEntity().getContent());
            List<String> files = new ArrayList<String>();
            while (true) {
                Message m;
                try {
                    m = messageFactory.read(in, Network.TCP);
                } catch (IOException e) {
                    // no other way to tell if we have received all messages
                    if (!"Connection closed.".equals(e.getMessage())) {
                        throw e;
                    }
                    break;
                }
                assertTrue(m instanceof QueryReply);
                QueryReply q = (QueryReply) m;
                Response[] results = q.getResultsArray();
                for (Response result : results) {
                    files.add(result.getName());
                    assertTrue("Expected .class or LimeWire file, got: " + result.getName(),
                            result.getName().endsWith(".class") || result.getName().toLowerCase().startsWith("limewire"));
                }
            }

            assertEquals(fileManager.getNumFiles(), files.size());

            for (Iterator<Response> it = fileManager.getIndexingIterator(false); it.hasNext();) {
                Response result = it.next();
                boolean contained = files.remove(result.getName());
                assertTrue("File is missing in browse response: "
                        + result.getName(), contained);
            }
            assertTrue("Browse returned more results than shared: " + files,
                    files.isEmpty());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testBrowseHead() throws Exception {
        HttpHead method = new HttpHead(host + "/");
        method.addHeader("Accept", "application/x-gnutella-packets");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            assertNull(response.getEntity());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testBrowseNoAcceptHeader() throws Exception {
        HttpUriRequest method = new HttpGet(host + "/");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_NOT_ACCEPTABLE, response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        method = new HttpHead(host + "/");
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_NOT_ACCEPTABLE, response.getStatusLine().getStatusCode());
            assertNull(response.getEntity());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testBrowseNoAcceptHeaderHttp10() throws Exception {
        HttpUriRequest method = new HttpGet(host + "/");
        HttpProtocolParams.setVersion(client.getParams(), HttpVersion.HTTP_1_1);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_NOT_ACCEPTABLE, response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        method = new HttpHead(host + "/");
        HttpProtocolParams.setVersion(client.getParams(), HttpVersion.HTTP_1_1);
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_NOT_ACCEPTABLE, response.getStatusLine().getStatusCode());
            assertNull(response.getEntity());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

}
