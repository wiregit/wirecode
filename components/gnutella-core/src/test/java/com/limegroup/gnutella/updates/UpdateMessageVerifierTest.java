package com.limegroup.gnutella.updates;

import junit.framework.*;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.util.BaseTestCase;
import com.sun.java.util.collections.*;

import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.Expand;
import java.io.*;

/**
 * Unit tests for UpdateMessageVerifier
 */
public class UpdateMessageVerifierTest extends BaseTestCase {
        
	public UpdateMessageVerifierTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(UpdateMessageVerifierTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void testLegacy() throws Exception {
	    Expand.expandFile( getUpdateVer(),
	                       CommonUtils.getUserSettingsDir() );
        RandomAccessFile f=new RandomAccessFile(
            new File(CommonUtils.getUserSettingsDir(), "update.xml"), "r");
        byte[] content = new byte[(int)f.length()];
        f.readFully(content);
        f.close();
        UpdateMessageVerifier tester = new UpdateMessageVerifier(content);
        assertTrue(tester.verifySource());
    }
    
    private static File getUpdateVer() throws Exception {
                  // tests/TestData    /tests          / ..
        File f = getTestDirectory().getParentFile().getParentFile();
        return new File(f, "gui/update.ver");
    }
}
