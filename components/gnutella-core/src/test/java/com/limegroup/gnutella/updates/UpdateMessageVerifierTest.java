package com.limegroup.gnutella.updates;

import java.io.File;
import java.io.RandomAccessFile;

import junit.framework.Test;

import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.Expand;

/**
 * Unit tests for UpdateMessageVerifier
 */
public class UpdateMessageVerifierTest extends BaseTestCase {

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
	
	public void testBadXml() throws Exception {
		test("bad_xmlFile.xml",false);
	}
	
	public void testRandomBytes() throws Exception {
		test("random_bytesFile.xml",false);
	}
	
		public void testMiddleVerFile() throws Exception {
		test("middle_verFile.xml",true);
	}
	
	public void testDefMessageFile() throws Exception {
		test("def_messageFile.xml",true);
	}
	
	public void testDefVerFile() throws Exception {
		test("def_verFile.xml",true);
	}
	

	public void testOldVerFile() throws Exception {
		test("old_verFile.xml",true);
	}
	
	public void testNewVerFile() throws Exception {
		test("new_verFile.xml",true);
	}
		
	public void testLegacy() throws Exception {
	    Expand.expandFile( getUpdateVer(),
	                       CommonUtils.getUserSettingsDir() );
	                       
	    File _f = new File(CommonUtils.getUserSettingsDir(), "update.xml");
	   
	     
	    test(_f.getName(),true);
        
        
    }
    
    private static File getUpdateVer() throws Exception {
                  // tests/TestData    /tests          / ..
        File f = getTestDirectory().getParentFile().getParentFile();
        return new File(f, "gui/update.ver");
    }
    
    private void test(String filename, boolean good) throws Exception{
    	File _f = new File(testXMLPath+filename);
    	
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
