package com.limegroup.gnutella.updates;

import java.io.File;
import java.io.RandomAccessFile;

import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;

import junit.framework.Test;

import com.limegroup.gnutella.util.LimeTestCase;

/**
 * Unit tests for UpdateMessageVerifier
 */
public class UpdateMessageVerifierTest extends LimeTestCase {

	private static final String testXMLPath = "com"+File.separator+
												"limegroup"+File.separator+
												"gnutella"+File.separator+
												"updates"+File.separator;
        
	public UpdateMessageVerifierTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(UpdateMessageVerifierTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void testRandomBytes() throws Exception {
		specificTest("random_bytesFile.xml",false);
	}
	
	public void testNewVerFile() throws Exception {
		specificTest("new_verFile.xml",true);
	}
	
	public void testMiddleVerFile() throws Exception {
		specificTest("middle_verFile.xml",true);
	}
	
	
	 //I'm assuming only the xml is bad, cause the signature checks out.
	 // --zab
	public void testBadXml() throws Exception {
		specificTest("bad_xmlFile.xml",true);
	}
	
	
	
	
	
	public void testDefMessageFile() throws Exception {
		specificTest("def_messageFile.xml",false);
	}
	
	public void testDefVerFile() throws Exception {
        specificTest("def_verFile.xml",false);
	}
	

	public void testOldVerFile() throws Exception {
		specificTest("old_verFile.xml",true);
	}
    
    protected void setUp() throws Exception {
    	File pub = CommonUtils.getResourceFile(testXMLPath+"public.key");
        File pub2 = new File(_settingsDir, "public.key");
        FileUtils.copy(pub, pub2);
        assertTrue("test could not be set up", pub2.exists());
    }
    private void specificTest(String filename, boolean good) throws Exception{
    	
        
    	File _f = CommonUtils.getResourceFile(testXMLPath+filename);
    	
    	assertTrue(_f.exists());
    	assertGreaterThan(0,_f.length());
    	
    	RandomAccessFile f=new RandomAccessFile(_f, "r");
        
        byte[] content = new byte[(int)f.length()];
        f.readFully(content);
        f.close();
        
    	
    	//System.out.println(new String(content));
    	UpdateMessageVerifier tester = new UpdateMessageVerifier(content,true);
    	
    	if (good) 
         assertTrue(tester.verifySource());
        else
         assertFalse(tester.verifySource());
    }
}
