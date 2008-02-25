package com.limegroup.gnutella.lws.server;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import junit.textui.TestRunner;

import com.limegroup.gnutella.downloader.LWSIntegrationServices;

/**
 * Tests the <code>Download</code> command.
 */
public class LWSDownloadTest extends AbstractCommunicationSupportWithNoLocalServer {

    private final LWSDownloadTestConstants constants = new LWSDownloadTestConstants();
    protected final String filePath = "../testData/" + getClass().getName();
    protected File dataDir = new File(filePath);

    public LWSDownloadTest(String s) {
        super(s);
    }

    public static Test suite() {
        return buildTestSuite(LWSDownloadTest.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    private SimpleWebServer server;

    public void testDownload() {

        long length = constants.LENGTH;

        Map<String, String> args = new HashMap<String, String>();
        args.put("url", constants.URL);
        args.put("file", constants.FILE);
        args.put("id", constants.ID);
        args.put("length", String.valueOf(length));

        // Send the client a command to start the download
        sendCommandToClient("Download", args);

        // Do a busy wait until we've run out of time or the file we wanted
        // was downloaded and is the size we wanted
        File savedFile = new File(new File(dataDir, "store"), constants.FILE);
        for (long toStop = System.currentTimeMillis() + constants.DOWNLOAD_WAIT_TIME; System.currentTimeMillis() < toStop;) {
            if (server.getBytesWritten() == length && savedFile.length() == length) {
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
        }
        assertEquals(savedFile.getAbsolutePath(), length, savedFile.length());
        assertEquals(String.valueOf(savedFile.length()), length, server.getBytesWritten());
    }

    /**
     * Set up the little web server.
     */
    protected void afterSetup() {
        super.afterSetup();

        // Make sure we are downloading from the right spot
        LWSIntegrationServices services = getInstance(LWSIntegrationServices.class);
        services.setDownloadPrefix(constants.HOST + ":" + constants.PORT);

        // Start up the server
        server = new SimpleWebServer(constants);
        server.start();
        
        deleteAllFiles();

        // Reset and authenticate
        doAuthenticate();
    }

    private void deleteAllFiles() {
        deleteAllFiles(dataDir);
    }
    
    private void deleteAllFiles(File d) {
        if (!d.exists()) {
            return;
        }
        File[] files = d.listFiles();
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (files[i].isDirectory()) {
                deleteAllFiles(f);
            } else {
                f.delete();
            }
        }
    }

    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++)
            files[i].delete();
        dir.delete();
    }
    /**
     * Tear down the little web server.
     */
    protected void afterTearDown() {
        super.afterTearDown();
        server.stop();
    }
}
