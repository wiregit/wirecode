package com.limegroup.gnutella.mail;

import java.io.File;

import com.limegroup.gnutella.mail.MailDownloadNotificator;
import com.limegroup.gnutella.settings.SettingTest;
import com.limegroup.gnutella.util.BaseTestCase;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class MailDownloadNotificatorTest extends BaseTestCase {

	String[][] testFilterString = new String[][]{{"name","contains","bla"}};
	
	File dlFile;
	MailDownloadNotificator mailer;
	
	public MailDownloadNotificatorTest(java.lang.String testName) {
        super(testName);
    }
    
	public void testNameEqualSingleFilter() throws Exception{
		String[][] filterString= new String[][]{
				{"name","equals","mlfiltertest.tmp"}
		};
		assertTrue("name equal should be true",mailer.filterFile(filterString,dlFile));
	}
	
	public void testNameContainsSingleFilter() throws Exception{
		String[][] filterString= new String[][]{
				{"name","contains","ml"}
		};
		assertTrue("name contains should be true",mailer.filterFile(filterString,dlFile));
	}
	
	public void testTypeEqualsSingleFilter() throws Exception{
		String[][] filterString= new String[][]{
				{"type","equals","tmp"}
		};
		assertTrue("type equals should be true",mailer.filterFile(filterString,dlFile));
	}
	
	public void testMultipleFilter() throws Exception{
		String[][] filterString= new String[][]{
				{"name","contains","ml","and","name","equals","mlfiltertest.tmp"}
		};
		assertTrue("Multiple should be true",mailer.filterFile(filterString,dlFile));
	}
	
	
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(MailDownloadNotificatorTest.class);
        return suite;
    }
    
    public void setUp() throws Exception {
    	dlFile = new File("mlfiltertest.tmp");
    	dlFile.createNewFile();
        mailer = new MailDownloadNotificator();
    }
        
    
    public void tearDown() {
    	dlFile.delete();
    	
    }
	
	

}
