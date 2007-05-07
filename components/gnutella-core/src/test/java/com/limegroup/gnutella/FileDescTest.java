package com.limegroup.gnutella;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.Test;

import org.limewire.util.CommonUtils;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.settings.DHTSettings;


/**
 * Test the public methods of the <tt>FileDesc</tt> class.
 */
@SuppressWarnings("unchecked")
public final class FileDescTest extends com.limegroup.gnutella.util.LimeTestCase {
    
    private static final long MAX_FILE_SIZE = 3L * 1024L * 1024;
    
	public FileDescTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(FileDescTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	/**
	 * Tests the FileDesc construcotor for invalid arguments
	 */
	public void testInvalidConstructorArguments() throws Exception {
        File file = CommonUtils.getResourceFile("build.xml");
        Set urns = calculateAndCacheURN(file);
        
		try {
			new FileDesc(null, urns, 0);
			fail("null file should not be permitted for FileDesc constructor");
		} catch(NullPointerException ignored) {}
        
        try {
            new FileDesc(file, null, 0);
            fail("null urns should not be permitted for FileDesc constructor");
        } catch(NullPointerException ignored) {}
        
        try {
            new FileDesc(file, urns, -1);
            fail("negative index should not be permitted for FileDesc constructor");
        } catch(IndexOutOfBoundsException ignored) {}
        
        try {
            new FileDesc(file, Collections.EMPTY_SET, 0);
            fail("no sha1 urn should not be permitted for FileDesc constructor");
        } catch(IllegalArgumentException ignored) {}
	}
    
    /**
     * Tests the FileDesc construcotor for valid arguments
     */
    public void testValidConstructorArguments() throws Exception {
        File parent = new File(getCoreDir(), "com/limegroup/gnutella");
        File[] allFiles = parent.listFiles(); 
        List fileList = new ArrayList(); 
        for(int i=0; i<allFiles.length; i++) { 
            if(allFiles[i].isFile() && allFiles[i].length() < MAX_FILE_SIZE) { 
                fileList.add(allFiles[i]); 
            } 
        }
        
        // Make sure we're running at least one check!
        assertFalse("no files to test! base: " + parent, fileList.isEmpty());
        
        Iterator it = fileList.iterator();
        for(int i = 0; it.hasNext(); i++) {
            File file = (File)it.next();
            Set urns = calculateAndCacheURN(file); 
            new FileDesc(file, urns, i);
        }
    }
    
    public void testIsRareFile() throws Exception {
        File file = CommonUtils.getResourceFile("build.xml");
        Set urns = calculateAndCacheURN(file);
        
        FileDesc fd = new FileDesc(file, urns, 0);
        
        // Initial State: Not Rare!
        assertLessThan(DHTSettings.RARE_FILE_ATTEMPTED_UPLOADS.getValue(), fd.getAttemptedUploads());
        assertLessThanOrEquals(DHTSettings.RARE_FILE_COMPLETED_UPLOADS.getValue(), fd.getCompletedUploads());
        
        long delta = System.currentTimeMillis() - fd.getLastAttemptedUploadTime();
        assertLessThan(DHTSettings.RARE_FILE_TIME.getValue(), delta);
        
        assertFalse(fd.isRareFile());
        
        // Modify the lastAttemptedUploadTime and it should be still not rare
        delta = System.currentTimeMillis() - DHTSettings.RARE_FILE_TIME.getValue();
        PrivilegedAccessor.setValue(fd, "lastAttemptedUploadTime", Long.valueOf(delta));
        
        assertFalse(fd.isRareFile());
        
        // Change the _attemptedUploads counter
        PrivilegedAccessor.setValue(fd, "_attemptedUploads", 
                Integer.valueOf(DHTSettings.RARE_FILE_ATTEMPTED_UPLOADS.getValue()));
        
        // And it should be rare
        assertTrue(fd.isRareFile());
        
        // Simulate an upload attempt and it shoudn't be rare anymore
        fd.incrementAttemptedUploads();
        assertFalse(fd.isRareFile());
    }
}
