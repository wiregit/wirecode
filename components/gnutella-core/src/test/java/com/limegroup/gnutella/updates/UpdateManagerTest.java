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
    
    private static File OLD_VERSION_FILE;
    
    private static File NEW_VERSION_FILE;

    private static File DEF_SIG_FILE;

    private static File DEF_MESSAGE_FILE;

    private static File updateXMLFile;
    
    private static int updateVersion; 

    private static final int PORT = 6347;

	public UpdateManagerTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(UpdateManagerTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
    
    public static void globalSetUp() throws Exception {
        PrivilegedAccessor.setValue(CommonUtils.class, "SETTINGS_DIRECTORY",
                                    getTestDirectory());
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(
                                                   new String[] {"*.*.*.*"} );
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(
                                                   new String[] {"127.*.*.*"});
        ConnectionSettings.PORT.setValue(PORT);
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
		UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(false);
		UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
		UltrapeerSettings.MAX_LEAVES.setValue(1);
		ConnectionSettings.NUM_CONNECTIONS.setValue(1);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);	
		ConnectionSettings.USE_GWEBCACHE.setValue(false);
		ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);

        //Set the version to a lower version
        PrivilegedAccessor.setValue(CommonUtils.class, "testVersion", "3.2.2");
        
        //TODO1: make these version files with the messages using the private
        //key and add them to tests.
        File updateDir = new File("com/limegroup/gnutella/updates");
        OLD_VERSION_FILE = new File(updateDir, "old_verFile.xml"); 
        NEW_VERSION_FILE = new File(updateDir,"new_verFile.xml");
        DEF_SIG_FILE = new File(updateDir, "def_verFile.xml");        
        DEF_MESSAGE_FILE = new File(updateDir, "def_messageFile.xml");
        File pub = new File(updateDir,"public.key");
        File pub2 = new File(CommonUtils.getUserSettingsDir(),"public.key");
        CommonUtils.copy(pub, pub2);
        updateXMLFile = new File(CommonUtils.getUserSettingsDir(),"update.xml");
        //TODO:Store older file and replace at the end.
        updateVersion = OLD;
        changeUpdateFile();
        RouterService rs = new RouterService(new ActivityCallbackStub());
        rs.start();
        rs.clearHostCatcher();
        rs.connect();
    }
    

    public void setUp() {
        try {
            PrivilegedAccessor.setValue(CommonUtils.class, "SETTINGS_DIRECTORY",
                                        getTestDirectory());
        } catch(Exception e) {
            fail("problem setting up test");
        }
    }
        
    public void testSimpleVersionCheck() {
        UpdateManager man = UpdateManager.instance();
        assertEquals("Problem with old update file", "2.9.3",man.getVersion());
    }


    public void testNoMessageOnAtVersion() {
        InputStream is = null;
        OutputStream os = null;
        try {
            Socket s = new Socket("localhost",PORT);
            os = s.getOutputStream();
            is = s.getInputStream();
        } catch(Exception e) {
            fail("unable to make a connection with it");
        }
    }

//      public void testBadSignatureFails() {

//      }

//     public void testBadMessageFails() {

//      }


//      public void testNewerVersionAccepted() {

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
