package com.limegroup.gnutella.updates;

import junit.framework.Test;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.settings.*;
import java.io.*;
import java.net.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.stubs.*;

import java.util.Iterator;

/**
 * Unit tests for UpdateMessageVerifier
 */
public class UpdateManagerTest extends BaseTestCase {
    
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

    private static final long DELAY = 60l*1000;//1 min
    

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
        rs.clearHostCatcher();
        rs.connect();

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
		ConnectionSettings.NUM_CONNECTIONS.setValue(5);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);	
		ConnectionSettings.USE_GWEBCACHE.setValue(false);
		ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
        //Set the version to a lower version
        PrivilegedAccessor.setValue(CommonUtils.class, "testVersion", "3.2.2");
    }

    public void setUp() throws Exception {
        Thread.sleep(3000);
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

    public void testOldVersionNotAcceptedFromNetwork() {
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


    public void testIOXLeavesVersionIntact() {
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


    public void testBadSignatureFailsOnNetwork() {
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

    public void testEqualVersionNotRequested() {
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

    public void testLowerMajorVersionNotRequested() {
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

    public void testDifferentMinorVersionNotRequested() {
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

   public void testNewerVersionAcceptedOnNetwork() {
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
    public void testNewerVersionFileWithSameVersionRequested() {
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



    public void testBadMessageFailsOnNetwork() {
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

    public void testBadXMLFailsOnNetwork() {
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

    public void testGarbageDataFailsOnNetwork() {
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

   public void testUpdateNotRequesteFromSpecial() {
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
    
    
    public void testUpdateMessageDelayed() {
    	removeConnections();
        //Note:If the update file does not contain the timestamp attibute, the
        //UpdateManager assumes it is the current time. We can use this to test
        //that there is a delay in our displaying the message
        testCallback = true;
        updateVersion = OLD;
        changeUpdateFile();        
        TestConnection conn = null;
        try {
            conn = new TestConnection(6682, "3.6.3", NEW);//no timestamp
        } catch (IOException e) {
            fail("could not setup test");
        }
        conn.start();
        long startTime = System.currentTimeMillis();
        synchronized(lock) {
            try {
                lock.wait(DELAY);
            } catch (InterruptedException ix) {}
            if(System.currentTimeMillis() == startTime)
                fail("GUI notified too soon");
            else if(System.currentTimeMillis() > startTime+DELAY)
                fail("GUI notified too late");
        }
        testCallback = false;
    }

    /**
     * puts an update file in the user pref dir based on updateVersion and set
     * the UpdateManager.INSTANCE to null so the new file is parsed everytime we
     * call UpdateManager.instance() 
     */
    private static void changeUpdateFile() {        
        if(updateVersion == OLD) 
            CommonUtils.copy(OLD_VERSION_FILE, updateXMLFile);
        else if(updateVersion == MIDDLE)
            CommonUtils.copy(MIDDLE_VERSION_FILE, updateXMLFile);
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
            PrivilegedAccessor.setValue(UpdateManager.class, "DELAY", 
                                        new Long(DELAY));
        } catch(IllegalAccessException eax) {
            fail("unable to nullify UpdateManager.INSTANCE");
        } catch(NoSuchFieldException nsfx) {
            fail("unable to nullify UpdateManager.INSTANCE");
        }
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
