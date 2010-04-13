package org.limewire.core.impl.integration;

import java.io.File;
import java.util.Arrays;

import org.apache.http.HttpStatus;
import org.limewire.rest.RestPrefix;
import org.limewire.util.FileUtils;

import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileManagerTestUtils;

/**
 * streaming integration tests for Rest APIs NOTE utilizing library files
 * provided by superclass
 */
public class RestStreamingTest extends AbstractRestIntegrationTestcase {

    private static final String STREAM_PREFIX = RestPrefix.STREAM.pattern();

    public RestStreamingTest(String name) throws Exception {
        super(name);
    }

    @Override public void setUp() throws Exception {
        super.setUp();
        loadLibraryFiles();
    }

    // -------------------------- tests --------------------------

    public void testBasic() throws Exception {

        // basic status check
        FileDesc fd = library.getFileDescForIndex(5);
        assertStatusCode(getUrn(fd),HttpStatus.SC_OK);

        // compare local file and stream data
        byte[] fileData = FileUtils.readFileFully(fd.getFile());
        byte[] streamData = getStreamingFile(fd);
        String assertStr = "streaming data should match original file: "+fd.getFileName();
        assertTrue(assertStr,Arrays.equals(fileData,streamData));

        // 1MB file
        File tmpFile = FileManagerTestUtils.createNewTestFile(1*1024*1024,_scratchDir);
        FileManagerTestUtils.assertAdds(library,tmpFile);
        fd = library.getFileDesc(tmpFile);        
        byte[] data = getStreamingFile(fd);
        assertEquals("unexpected stream filesize",fd.getFileSize(),data.length);
    }

    /**
     * test submitting meaningless garbage and fuzzy inputs
     */
    public void testNegatives() throws Exception {

        String NotInLibUrn = "/BA8DB600AC11FE2EE3033F5AFF57F500";

        // parameters with either no urn or invalid formats
        // assertNotFound(); // LWC-5532
        assertNotFound("/");
        assertNotFound("/"+bigString(200));
        assertNotFound(NotInLibUrn.substring(0,5));
        assertNotFound(NotInLibUrn+"BA");

        // not in library at all
        assertNotFound(NotInLibUrn);

        // in library but deleted on filesystem
        // FileDesc fd1 = forceMissingLibFile(); // LWC-5537
        // assertNotFound(getUrn(fd1));
    }

    // ---------------------- private methods ----------------------

    /**
     * String built by EntityUtils.toString(HttpEntity) for now. Throws an
     * IllegalArgumentException if the streamed data size is >= 2^31b
     */
    protected byte[] getStreamingFile(FileDesc fd) throws Exception {
        byte[] response = getHttpResponseBytes(STREAM_PREFIX+"/"+getUrn(fd),NO_PARAMS);
        return response;
    }

    protected void assertStatusCode(String urn, int expectedCode) throws Exception {
        int statusCode = getHTTPStatus(STREAM_PREFIX+"/"+urn,NO_PARAMS);
        assertEquals("code uexpected",expectedCode,statusCode);
    }

    protected void assertNotFound(String urn) throws Exception {
        assertStatusCode(urn,HttpStatus.SC_NOT_FOUND);
    }

}
