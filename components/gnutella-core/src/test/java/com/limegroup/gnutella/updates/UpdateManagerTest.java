package com.limegroup.gnutella.updates;

import junit.framework.Test;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.settings.*;
import java.io.*;
import java.net.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.stubs.*;


/**
 * Unit tests for UpdateMessageVerifier
 */
public class UpdateManagerTest extends BaseTestCase {
    
    private static final int OLD = 0;
    
    private static final int NEW = 1;
    
    private static final int DEF_SIGNATURE = 2;

    private static final int DEF_MESSAGE = 3;

    private static final int BAD_XML = 4;
    
    private static final int RANDOM_BYTES = 5;
    
    private static File OLD_VERSION_FILE;
    
    private static File NEW_VERSION_FILE;

    private static File DEF_SIG_FILE;

    private static File DEF_MESSAGE_FILE;

    private static File BAD_XML_FILE;

    private static File RANDOM_BYTES_FILE;

    private static File updateXMLFile;
    
    private static int updateVersion; 

    static final int PORT = 6347;

	public UpdateManagerTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(UpdateManagerTest.class);//,"testHandshaking");
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
    
    public static void globalSetUp() throws Exception {
        setSettings();
        RouterService rs = new RouterService(new ActivityCallbackStub());
        rs.start();
        rs.clearHostCatcher();
        rs.connect();

    }
    
    private static void setSettings() throws Exception {
        //Get access to all the test files we need.
        String updateDir = "com/limegroup/gnutella/updates/";
        OLD_VERSION_FILE =
                CommonUtils.getResourceFile(updateDir + "old_verFile.xml"); 
        NEW_VERSION_FILE = 
                CommonUtils.getResourceFile(updateDir + "new_verFile.xml");
        DEF_SIG_FILE = 
                CommonUtils.getResourceFile(updateDir + "def_verFile.xml");
        DEF_MESSAGE_FILE = 
                CommonUtils.getResourceFile(updateDir + "def_messageFile.xml");
        BAD_XML_FILE = 
                CommonUtils.getResourceFile(updateDir + "bad_xmlFile.xml");
        RANDOM_BYTES_FILE =
                CommonUtils.getResourceFile(updateDir + "random_bytesFile.xml");

        assertTrue(OLD_VERSION_FILE.exists());
        assertTrue(NEW_VERSION_FILE.exists());
        assertTrue(DEF_SIG_FILE.exists());
        assertTrue(DEF_MESSAGE_FILE.exists());

        File pub = CommonUtils.getResourceFile(updateDir+"public.key");
        File pub2 = new File(_settingsDir, "public.key");
        CommonUtils.copy(pub, pub2);
        assertTrue("test could not be set up", pub2.exists());
        updateXMLFile = new File(_settingsDir,"update.xml");
        //set the version file to be the old one. 
        updateVersion = OLD;
        changeUpdateFile();

        updateXMLFile = new File(_settingsDir,"update.xml");
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(
                                                   new String[] {"*.*.*.*"} );
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(
                                                   new String[] {"127.*.*.*"});
        ConnectionSettings.PORT.setValue(PORT);
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
		UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(false);
		UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
		UltrapeerSettings.MAX_LEAVES.setValue(5);
		ConnectionSettings.NUM_CONNECTIONS.setValue(2);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);	
		ConnectionSettings.USE_GWEBCACHE.setValue(false);
		ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
        //Set the version to a lower version
        PrivilegedAccessor.setValue(CommonUtils.class, "testVersion", "3.2.2");
    }

    public void setUp() throws Exception {
        setSettings();
    }


    /**
     * Tests that UpdateManager thinks the version it knows about is the one on
     * disk so long as it is verified correctly
     */
    public void testSimpleVersionCheck() {
        UpdateManager man = UpdateManager.instance();
        assertEquals("Problem with old update file", "2.9.3", man.getVersion());
    }

    
    public void testNewerVersionAccepted() {
        updateVersion = NEW;
        changeUpdateFile();
        UpdateManager man = UpdateManager.instance();
        assertEquals("Problem with new update file", "3.6.3", man.getVersion());
    }
    
    public void testBadSignatureFails() {
        //First test bad signaute with the file.
        updateVersion = DEF_SIGNATURE;
        changeUpdateFile();
        UpdateManager man = UpdateManager.instance();
        assertEquals("Accepted defective signature", "3.2.2", man.getVersion());
    }


    public void testBadMessageFails() {
        updateVersion = DEF_MESSAGE;
        changeUpdateFile();
        UpdateManager man = UpdateManager.instance();
        assertEquals("Problem with new update file", "3.2.2", man.getVersion());
    }

    public void testBadXMLFileFails() {
        updateVersion = BAD_XML;
        changeUpdateFile();
        UpdateManager man = UpdateManager.instance();
        assertEquals("Problem with new update file", "3.2.2", man.getVersion());
    }

    public void testGarbageFileFails() {
        updateVersion = RANDOM_BYTES;
        changeUpdateFile();
        UpdateManager man = UpdateManager.instance();
        assertEquals("Problem with new update file", "3.2.2", man.getVersion());
    }
    
//      public void testHandshaking() {
//          TestConnection conn = new TestConnection(6666,"3.2.2");        
//      }


//      public void testNoMessageOnAtVersion() {
//          InputStream is = null;
//          OutputStream os = null;
//          Socket s = new Socket("localhost",PORT);
//          os = s.getOutputStream();
//          is = s.getInputStream();
//      }


//      public void testBadSignatureFailsOnNetwork() {
//          //Now do it with the network
//      }

//     public void testBadMessageFailsOnNetwork() {

//      }


//      public void testNewerVersionAcceptedonNetwork() {

//      }

//      public void testOlderVersionRejected() {

//      }

//      public void testEqualVersionRejected() {

//      }

//      public void testIOXCausesNonAcceptance() {

//      }

//      public void testFileReadVerifiedWithJava118() {

//      }

//      public void testJava118NetworkVerification() {

//      }

    ///////////////////////////////helper methods/////////////////////////
    
    private Connection makeConnection() {
        //TODO1: implement
        return null;
    }

    private void killConnection() {

    }

    private void sendHeaders() {

    }
    
    /**
     * puts an update file in the user pref dir based on updateVersion and set
     * the UpdateManager.INSTANCE to null so the new file is parsed everytime we
     * call UpdateManager.instance() 
     */
    private static void changeUpdateFile() {        
        if(updateVersion == OLD) 
            CommonUtils.copy(OLD_VERSION_FILE, updateXMLFile);
        else if(updateVersion == NEW) 
            CommonUtils.copy(NEW_VERSION_FILE, updateXMLFile);
        else if(updateVersion == DEF_MESSAGE)
            CommonUtils.copy(DEF_MESSAGE_FILE, updateXMLFile);
        else if(updateVersion == DEF_SIGNATURE)
            CommonUtils.copy(DEF_SIG_FILE, updateXMLFile);
        else if(updateVersion == BAD_XML)
            CommonUtils.copy(BAD_XML_FILE, updateXMLFile);
        else if(updateVersion == RANDOM_BYTES)
            CommonUtils.copy(RANDOM_BYTES_FILE, updateXMLFile);
        else
            fail("updateVersion set to wrong value");
        
        //Set UpdateManager.INSTANCE to null
        try {
            PrivilegedAccessor.setValue(UpdateManager.class, "INSTANCE", null);
        } catch(IllegalAccessException eax) {
            fail("unable to nullify UpdateManager.INSTANCE");
        } catch(NoSuchFieldException nsfx) {
            fail("unable to nullify UpdateManager.INSTANCE");
        }
    }

}
