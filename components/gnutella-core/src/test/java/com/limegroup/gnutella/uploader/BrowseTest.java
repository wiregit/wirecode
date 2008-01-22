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

import junit.framework.Test;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.http.HttpStatus;
import org.limewire.net.HttpClientManager;
import org.limewire.util.TestUtils;

import com.google.inject.Injector;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * Test that a client uploads a list of files correctly.
 */
public class BrowseTest extends LimeTestCase {

    private final int PORT = 6668;

    private File sharedDirectory;

    private HttpClient client;

    private HostConfiguration hostConfig;

    protected String protocol;

    private FileManager fileManager;

    private MessageFactory messageFactory;
    
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
        ConnectionSettings.PORT.setValue(PORT);

        Injector injector = LimeTestUtils.createInjectorAndStart();
        
        fileManager = injector.getInstance(FileManager.class);
        messageFactory = injector.getInstance(MessageFactory.class);
        
        fileManager.loadSettingsAndWait(100000);

        client = HttpClientManager.getNewClient();
        hostConfig = new HostConfiguration();
        hostConfig.setHost("localhost", PORT, protocol);
        client.setHostConfiguration(hostConfig);
    }

    public void testBrowse() throws Exception {
        GetMethod method = new GetMethod("/");
        method.addRequestHeader("Accept", "application/x-gnutella-packets");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);

            InputStream in = new BufferedInputStream(method.getResponseBodyAsStream());
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
            method.releaseConnection();
        }
    }

    public void testBrowseHead() throws Exception {
        HeadMethod method = new HeadMethod("/");
        method.addRequestHeader("Accept", "application/x-gnutella-packets");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);
            assertNull(method.getResponseBodyAsString());
        } finally {
            method.releaseConnection();
        }
    }

    public void testBrowseNoAcceptHeader() throws Exception {
        HttpMethodBase method = new GetMethod("/");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_NOT_ACCEPTABLE, response);
        } finally {
            method.releaseConnection();
        }
        
        method = new HeadMethod("/");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_NOT_ACCEPTABLE, response);
            assertNull(method.getResponseBodyAsString());
        } finally {
            method.releaseConnection();
        }
    }

    public void testBrowseNoAcceptHeaderHttp10() throws Exception {
        HttpMethodBase method = new GetMethod("/");
        method.setHttp11(false);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_NOT_ACCEPTABLE, response);
        } finally {
            method.releaseConnection();
        }
        
        method = new HeadMethod("/");
        method.setHttp11(false);
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_NOT_ACCEPTABLE, response);
            assertNull(method.getResponseBodyAsString());
        } finally {
            method.releaseConnection();
        }
    }

}
