package com.limegroup.gnutella.updates;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import junit.framework.Test;

import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Unit tests for UpdateMessageVerifier
 */
public class UpdateManagerTest extends LimeTestCase {
    
    static final int OLD = 0;
    
    static final int MIDDLE = 1;

    static final int NEW = 2;
    
    static final int DEF_SIGNATURE = 3;

    static final int DEF_MESSAGE = 4;

    static final int BAD_XML = 5;
    
    static final int RANDOM_BYTES = 6;
    
    private static File OLD_VERSION_FILE;

    private static File MIDDLE_VERSION_FILE;
    
    private static File NEW_VERSION_FILE;

    private static File DEF_SIG_FILE;

    private static File DEF_MESSAGE_FILE;

    private static File BAD_XML_FILE;

    private static File RANDOM_BYTES_FILE;

    private static File updateXMLFile;
    
    private static int updateVersion; 

    static final int PORT = 6347;
    
    private static boolean testCallback = false;
    
    private static final Object lock = new Object();
    
    private static final String defaultVersion = "0.0.0";
    
	public UpdateManagerTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(UpdateManagerTest.class);//, "testUpdateMessageDelayed");
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
    
    public static void globalSetUp() throws Exception {
        setSettings();
        RouterService rs = new RouterService(new MyActivityCallbackStub());
        rs.start();
        RouterService.clearHostCatcher();
        RouterService.connect();
    }
    
    private static void setSettings() throws Exception {
        //Get access to all the test files we need.
        String updateDir = "com/limegroup/gnutella/updates/";
        OLD_VERSION_FILE =
                CommonUtils.getResourceFile(updateDir + "old_verFile.xml"); 
        MIDDLE_VERSION_FILE = 
                CommonUtils.getResourceFile(updateDir + "middle_verFile.xml");
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
        assertTrue(MIDDLE_VERSION_FILE.exists());
        assertTrue(NEW_VERSION_FILE.exists());
        assertTrue(DEF_SIG_FILE.exists());
        assertTrue(DEF_MESSAGE_FILE.exists());

        File pub = CommonUtils.getResourceFile(updateDir+"public.key");
        File pub2 = new File(_settingsDir, "public.key");
        FileUtils.copy(pub, pub2);
        assertTrue("test could not be set up", pub2.exists());
        updateXMLFile = new File(_settingsDir,"update.xml");
        
        //set the version file to be the old one. 
        changeUpdateFile(OLD_VERSION_FILE);

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
		ConnectionSettings.NUM_CONNECTIONS.setValue(5);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);	
		ConnectionSettings.USE_GWEBCACHE.setValue(false);
		ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
        UltrapeerSettings.NEED_MIN_CONNECT_TIME.setValue(false);
        
        //Set the version to a lower version
        PrivilegedAccessor.setValue(LimeWireUtils.class, "testVersion", "3.2.2");
    }

    public void setUp() throws Exception {
        /*
        // Sleep for 3.0 seconds, regardless of exceptions
        long stopTime = 3*1000 + System.currentTimeMillis();
        long sleepLength = 3*1000;
        while (sleepLength > 0) {
            try {
                Thread.sleep(sleepLength);
            } catch (Exception ignoreMe){
                //Minimize busy waiting in the face of exceptions. 
                Thread.yield();
            }
            sleepLength = stopTime - System.currentTimeMillis();
        }
        */
        // Sleep for 3.0 seconds, unless interrupted
        Thread.sleep(3*1000);
        setSettings();
    }


    /**
     * Tests that UpdateManager thinks the version it knows about is the one on
     * disk so long as it is verified correctly
     */
    public void testSimpleVersionCheck() throws Exception {
        UpdateManager man = UpdateManager.instance();
        assertEquals("Problem with old update file", "2.9.3", man.getVersion());
    }

    
    public void testNewerVersionAccepted() throws Exception {
        updateVersion = NEW;
        changeUpdateFile();
        UpdateManager man = UpdateManager.instance();
        assertEquals("Problem with new update file", "3.6.3", man.getVersion());
    }
    
    public void testBadSignatureFails() throws Exception {
        //Attempt to change the version to 3.6.3 using a bad signature
        changeUpdateFile(DEF_SIG_FILE);
        UpdateManager man = UpdateManager.instance();
        assertEquals("Accepted defective signature", defaultVersion, man.getVersion());
    }


    public void testBadMessageFails() throws Exception {
        //Attempt to change the version to 3.6.3 using a defective message
        changeUpdateFile(DEF_MESSAGE_FILE);
        UpdateManager man = UpdateManager.instance();
        assertEquals("Problem with new update file", defaultVersion, man.getVersion());
    }

    public void testBadXMLFileFails() throws Exception {
        //Attempt to change verision to 2.9.3 using a bad XML file
        changeUpdateFile(BAD_XML_FILE);
        UpdateManager man = UpdateManager.instance();
        assertEquals("Problem with new update file", defaultVersion, man.getVersion());
    }

    public void testGarbageFileFails() throws Exception {
        updateVersion = RANDOM_BYTES;
        changeUpdateFile();
        UpdateManager man = UpdateManager.instance();
        assertEquals("Problem with new update file", defaultVersion, man.getVersion());
    }

    public void testOldVersionNotAcceptedFromNetwork() throws Exception {
        updateVersion = MIDDLE;
        changeUpdateFile();
        TestConnection conn = null;
        try {
            conn = new TestConnection(6666,"3.6.3",OLD); 
        } catch(IOException iox) {
            fail("could not set up test");
        }
        conn.start();
        try {
            Thread.sleep(2000);
        } catch(InterruptedException ix) {
            fail("unable to set up test");
        }
        UpdateManager man = UpdateManager.instance();
        assertEquals("Update manager accepted lower version",
                                                    "3.2.2",man.getVersion());
        conn.killThread();
    }


    public void testIOXLeavesVersionIntact() throws Exception {
        updateVersion = OLD;
        changeUpdateFile();
        TestConnection conn = null;
        try {
            conn = new TestConnection(6667,"3.6.3",NEW); 
        } catch(IOException iox) {
            fail("could not set up test");
        }
        conn.setSendUpdateData(false);
        conn.start();
        try {
            Thread.sleep(2000);
        } catch(InterruptedException ix) {
            fail("unable to set up test");
        }
        UpdateManager man = UpdateManager.instance();
        assertEquals("Update manager accepted lower version",
                                                    "2.9.3",man.getVersion());
        conn.killThread();
    }


    public void testBadSignatureFailsOnNetwork() throws Exception {
        updateVersion = OLD;
        changeUpdateFile();
        TestConnection conn = null;
        try {
            conn = new TestConnection(6668,"3.6.3", DEF_SIGNATURE);
        } catch(IOException iox) {
            fail("could not set test up");
        }
        conn.start();
       try {
            Thread.sleep(2000);
        } catch(InterruptedException ix) {
            fail("unable to set up test");
        }
        UpdateManager man = UpdateManager.instance();
        assertEquals("Update manager accepted lower version",
                                                    "2.9.3", man.getVersion());
        conn.killThread();
    }

    public void testEqualVersionNotRequested() throws Exception {
        updateVersion = OLD;
        changeUpdateFile();
        TestConnection conn = null;
        try {
            conn = new TestConnection(6669, "2.9.3", NEW);
        } catch(IOException iox) {
            fail("could not set test up");
        }
        conn.setTestUpdateNotRequested(true);
        conn.start();
       try {
            Thread.sleep(3000);
        } catch(InterruptedException ix) {
            fail("unable to set up test");
        }
        UpdateManager man = UpdateManager.instance();
        assertEquals("Update manager accepted lower version",
                             "2.9.3", man.getVersion());
        //conn.killThread();
    }

    public void testLowerMajorVersionNotRequested() throws Exception {
        updateVersion = OLD;
        changeUpdateFile();
        TestConnection conn = null;
        try {
            conn = new TestConnection(6670, "2.3.3", NEW);
        } catch(IOException iox) {
            fail("could not set test up");
        }
        conn.setTestUpdateNotRequested(true);
        conn.start();
        try {
            Thread.sleep(2000);
        } catch(InterruptedException ix) {
            fail("unable to set up test");
        }
        UpdateManager man = UpdateManager.instance();
        assertEquals("Update manager accepted lower version",
                             "2.9.3", man.getVersion());
        //conn.killThread();
    }

    public void testDifferentMinorVersionNotRequested() throws Exception {
        updateVersion = OLD;
        changeUpdateFile();
        TestConnection conn = null;
        try {
            conn = new TestConnection(6671, "2.9.5", NEW);
        } catch(IOException iox) {
            fail("could not set test up");
        }
        conn.setTestUpdateNotRequested(true);
        conn.start();
       try {
            Thread.sleep(2000);
        } catch(InterruptedException ix) {
            fail("unable to set up test");
        }
        UpdateManager man = UpdateManager.instance();
        assertEquals("Update manager accepted lower version",
                             "2.9.3", man.getVersion());
        //conn.killThread();
    }

   public void testNewerVersionAcceptedOnNetwork() throws Exception {
        updateVersion = OLD;
        changeUpdateFile();
        TestConnection conn = null;
        try { //header says same as me, but my version file is older, 
            conn = new TestConnection(6672, "3.6.3", NEW);
        } catch(IOException iox) {
            fail("could not set test up");
        }
        conn.setTestUpdateNotRequested(false);
        conn.start();
        try {
            Thread.sleep(2000);
        } catch(InterruptedException ix) {
            fail("unable to set up test");
        }
        UpdateManager man = UpdateManager.instance();
       assertEquals("Update should have got new version",
                                                    "3.6.3", man.getVersion());
    }


    /**
     * Tests that we will request the update file even our version is the same
     * as the version advertised, but our version file is older than the version
     * file advertised by the other guy
     */
    public void testNewerVersionFileWithSameVersionRequested() throws Exception {
        updateVersion = OLD;
        changeUpdateFile();
        TestConnection conn = null;
        try { //header says same as me, but my version file is older, 
            conn = new TestConnection(6673, "3.2.2", NEW);
        } catch(IOException iox) {
            fail("could not set test up");
        }
        conn.start();
        try {
            Thread.sleep(300);
        } catch(Exception e) {}
        UpdateManager man = UpdateManager.instance();
        assertEquals("Update should have got new version",
                                                    "3.6.3", man.getVersion());
    }



    public void testBadMessageFailsOnNetwork() throws Exception {
    	removeConnections();
        updateVersion = OLD;
        changeUpdateFile();
        TestConnection conn = null;
        try { //header says same as me, but my version file is older, 
            conn = new TestConnection(6674, "3.6.3", DEF_MESSAGE);
        } catch(IOException iox) {
            fail("could not set test up");
        }
        conn.start();
        try {
            Thread.sleep(300);
        } catch(Exception e) {}
        UpdateManager man = UpdateManager.instance();
        assertEquals("Update should have got new version",
                                                    "2.9.3", man.getVersion());
    }

    public void testBadXMLFailsOnNetwork() throws Exception {
    	removeConnections();
        updateVersion = OLD;
        changeUpdateFile();
        TestConnection conn = null;
        try { //header says same as me, but my version file is older, 
            conn = new TestConnection(6675, "3.6.3", BAD_XML);
        } catch(IOException iox) {
            fail("could not set test up");
        }
        conn.start();
        try {
            Thread.sleep(300);
        } catch(Exception e) {}
        UpdateManager man = UpdateManager.instance();
        assertEquals("Update should have got new version",
                                                    "2.9.3", man.getVersion());
    }

    public void testGarbageDataFailsOnNetwork() throws Exception {
    	removeConnections();
        updateVersion = OLD;
        changeUpdateFile();
        TestConnection conn = null;
        try { //header says same as me, but my version file is older, 
            conn = new TestConnection(6676, "3.6.3", RANDOM_BYTES);
        } catch(IOException iox) {
            fail("could not set test up");
        }
        conn.start();
        try {
            Thread.sleep(300);
        } catch(Exception e) {}
        UpdateManager man = UpdateManager.instance();
        assertEquals("Update should have got new version",
                                                    "2.9.3", man.getVersion());
    }

   public void testUpdateNotRequesteFromSpecial() throws Exception {
   		removeConnections();
       updateVersion = OLD;
       changeUpdateFile();
       TestConnection conn = null;
       try {
           conn = new TestConnection(6681, "@version@", NEW);
       } catch (IOException e) {
           fail("could not setup test");
       }
       conn.setTestUpdateNotRequested(true);
       conn.start();
       try {
           Thread.sleep(300);
       } catch (InterruptedException ix) { }
       UpdateManager man = UpdateManager.instance();
       assertEquals("should not have requested new file",
                                              "2.9.3", man.getVersion());
   }
    
    /**
    * puts an update file in the user pref dir based on updateVersion and set
    * the UpdateManager.INSTANCE to null so the new file is parsed everytime we
    * call UpdateManager.instance()
    *
    * @param updateFile the File that is to be coppied over the update XML file
    */
    private static void changeUpdateFile(File updateFile) throws Exception {
        FileUtils.copy(updateFile, updateXMLFile);
        //Set UpdateManager.INSTANCE to null
        PrivilegedAccessor.setValue(UpdateManager.class, "INSTANCE", null);
    }
    
    /**
     * puts an update file in the user pref dir based on updateVersion and set
     * the UpdateManager.INSTANCE to null so the new file is parsed everytime we
     * call UpdateManager.instance() 
     */
    private static void changeUpdateFile() throws Exception {        
        if(updateVersion == OLD) 
            FileUtils.copy(OLD_VERSION_FILE, updateXMLFile);
        else if(updateVersion == MIDDLE)
            FileUtils.copy(MIDDLE_VERSION_FILE, updateXMLFile);
        else if(updateVersion == NEW) 
            FileUtils.copy(NEW_VERSION_FILE, updateXMLFile);
        else if(updateVersion == DEF_MESSAGE)
            FileUtils.copy(DEF_MESSAGE_FILE, updateXMLFile);
        else if(updateVersion == DEF_SIGNATURE)
            FileUtils.copy(DEF_SIG_FILE, updateXMLFile);
        else if(updateVersion == BAD_XML)
            FileUtils.copy(BAD_XML_FILE, updateXMLFile);
        else if(updateVersion == RANDOM_BYTES)
            FileUtils.copy(RANDOM_BYTES_FILE, updateXMLFile);
        else
            fail("updateVersion set to wrong value");
        
        //Set UpdateManager.INSTANCE to null
        PrivilegedAccessor.setValue(UpdateManager.class, "INSTANCE", null);
    }
    
    private static class MyActivityCallbackStub extends ActivityCallbackStub {
        public void indicateNewVersion() {
            if(!testCallback)
                return;
            synchronized(lock) {
                lock.notifyAll();
            }
        }
    }
    
    /**
     * removes all existing connections to the node because previous tests
     * may influence outcome
     * @throws Exception
     */
    private void removeConnections()  {
    	ConnectionManager cman = RouterService.getConnectionManager();
    	for (Iterator iter = cman.getConnections().iterator();iter.hasNext();) {
    		Connection c = (Connection)iter.next();
    		c.close();
    	}
    }

}
