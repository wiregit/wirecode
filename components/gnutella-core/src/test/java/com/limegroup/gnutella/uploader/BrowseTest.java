package com.limegroup.gnutella.uploader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpProtocolParams;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.NetworkSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.http.httpclient.HttpClientUtils;
import org.limewire.io.URN;
import org.limewire.io.UrnSet;

import com.google.inject.Inject;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.library.FileCollection;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileManagerTestUtils;
import com.limegroup.gnutella.library.FileView;
import com.limegroup.gnutella.library.GnutellaFiles;
import com.limegroup.gnutella.library.Library;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.Message.Network;

/**
 * Test that a client uploads a list of files correctly.
 */
public class BrowseTest extends LimeTestCase {

    private final int PORT = 6668;

    @Inject private HttpClient client;

    protected String protocol;

    @Inject private Library library;
    @Inject @GnutellaFiles private FileView gnutellaFileView;
    @Inject @GnutellaFiles private FileCollection gnutellaFileCollection;
    @Inject private MessageFactory messageFactory;
    
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
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        NetworkSettings.PORT.setValue(PORT);

        LimeTestUtils.createInjectorAndStart(LimeTestUtils.createModule(this));
        
        FileManagerTestUtils.waitForLoad(library,2000);
        File shareDir = LimeTestUtils.getDirectoryWithLotsOfFiles();
        File[] testFiles = shareDir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return !file.isDirectory();
            }
        });
        assertGreaterThan("Not enough files to test against", 50, testFiles.length);
        for(File file : testFiles) {
            FileDesc fileDesc = gnutellaFileCollection.add(file).get(1, TimeUnit.SECONDS);
            assertNotNull(fileDesc);
            URN sha1Urn = fileDesc.getSHA1Urn();
            // add sha1 as non-metadata hash
            fileDesc.addUrn(URN.createNMS1FromBytes(sha1Urn.getBytes()));
        }
        
        host = protocol + "://localhost:" + PORT;
    }

    public void testBrowseWithoutNMS1Urn() throws Exception {
        HttpGet request = new HttpGet(host + "/");
        browse(request, false);
    }
    
    public void testBrowseWithNMS1UrnInQuery() throws Exception {
        HttpGet request = new HttpGet(host + "/?nms1=1");
        browse(request, true);
    }
    
    public void testBrowseWithNMS1UrnInQueryAndOtherParam() throws Exception {
        HttpGet request = new HttpGet(host + "/?foo=bar&nms1=1");
        browse(request, true);
    }
    
    public void testBrowseWithNMS1UrnInHeader() throws Exception {
        HttpGet request = new HttpGet(host + "/");
        request.addHeader(new BasicHeader("X-NMS1", "1"));
        browse(request, true);
    }
    
    public void testBrowseWithNMS1UrnInHeaderAndExtraHeader() throws Exception {
        HttpGet request = new HttpGet(host + "/");
        request.addHeader(new BasicHeader("X-NMS1", "1"));
        request.addHeader(new BasicHeader("foo", "bar"));
        browse(request, true);
    }
    
    public void browse(HttpUriRequest request, boolean includeNMS1Urn) throws Exception {
        request.addHeader("Accept", "application/x-gnutella-packets");
        HttpResponse response = null;
        try {
            response = client.execute(request);
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
                assertInstanceof(QueryReply.class, m);
                QueryReply q = (QueryReply) m;
                Response[] results = q.getResultsArray();
                for (Response result : results) {
                    files.add(result.getName());
                    assertTrue("Expected .tmp or LimeWire file, got: " + result.getName(),
                            result.getName().endsWith(".tmp") || result.getName().toLowerCase().startsWith("limewire"));
                    URN nms1Urn = UrnSet.getNMS1(result.getUrns());
                    if (includeNMS1Urn) {
                        assertNotNull(nms1Urn);
                    } else {
                        assertNull(nms1Urn);
                    }
                }
            }

            assertEquals(gnutellaFileView.size(), files.size());
            gnutellaFileView.getReadLock().lock();
            try {
                for(FileDesc result : gnutellaFileView) {
                    boolean contained = files.remove(result.getFileName());
                    assertTrue("File is missing in browse response: " + result.getFileName(), contained);
                }
            } finally {
                gnutellaFileView.getReadLock().unlock();
            }
            assertTrue("Browse returned more results than shared: " + files, files.isEmpty());
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
