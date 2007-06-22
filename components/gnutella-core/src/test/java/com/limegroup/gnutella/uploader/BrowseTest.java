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
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpStatus;
import org.limewire.util.CommonUtils;

import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.http.HttpClientManager;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * Test that a client uploads a list of files correctly.
 */
public class BrowseTest extends LimeTestCase {

    private static final int PORT = 6668;

    private static RouterService rs;

    private static File sharedDirectory;

    private HttpClient client;

    private HostConfiguration hostConfig;

    public BrowseTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(BrowseTest.class);
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }

    public static void globalSetUp() throws Exception {
        String directoryName = "com/limegroup/gnutella";
        sharedDirectory = CommonUtils.getResourceFile(directoryName);
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

        doSettings();

        rs = new RouterService(new ActivityCallbackStub());
    }

    private static void doSettings() {
        SharingSettings.EXTENSIONS_TO_SHARE.setValue("class");
        SharingSettings.DIRECTORIES_TO_SHARE.setValue(Collections
                .singleton(sharedDirectory));

        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.PORT.setValue(PORT);
    }

    @Override
    protected void setUp() throws Exception {
        // allow running single tests from Eclipse
        if (rs == null) {
            globalSetUp();
        }

        doSettings();

        if (!RouterService.isLoaded()) {
            rs.start();
        }
        RouterService.getFileManager().loadSettingsAndWait(100000);

        client = HttpClientManager.getNewClient();
        hostConfig = new HostConfiguration();
        hostConfig.setHost("localhost", PORT);
        client.setHostConfiguration(hostConfig);
    }

    @Override
    public void tearDown() {
    }

    public void testBrowse() throws Exception {
        GetMethod method = new GetMethod("/");
        method.addRequestHeader("Accept", "application/x-gnutella-packets");
        try {
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_OK, response);

            InputStream in = new BufferedInputStream(method.getResponseBodyAsStream());
            List<String> files = new ArrayList<String>();
            while (in.available() > 0) {
                Message m;
                try {
                    m = MessageFactory.read(in);
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

            assertEquals(RouterService.getNumSharedFiles(), files.size());
            
            for (Iterator<Response> it = RouterService.getFileManager()
                    .getIndexingIterator(false); it.hasNext();) {
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

}
