package com.limegroup.gnutella;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.search.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.guess.*;
import com.limegroup.gnutella.routing.*;
import com.limegroup.gnutella.security.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.downloader.*;
import com.bitzi.util.*;

import junit.framework.*;
import java.util.Properties;
import java.util.StringTokenizer;
import com.sun.java.util.collections.*;
import java.io.*;
import java.net.*;

/**
 * Checks whether (multi)leaves avoid forwarding messages to ultrapeers, do
 * redirects properly, etc.  The test includes a leaf attached to 3 
 * Ultrapeers.
 */
public class CreationTimeCacheTest 
    extends com.limegroup.gnutella.util.BaseTestCase {
    private static final int PORT=6669;
    private static final int TIMEOUT=3000;
    private static final byte[] ultrapeerIP=
        new byte[] {(byte)18, (byte)239, (byte)0, (byte)144};
    private static final byte[] oldIP=
        new byte[] {(byte)111, (byte)22, (byte)33, (byte)44};

    private static Connection[] testUPs = new Connection[4];
    private static RouterService rs;

    private static MyActivityCallback callback;

    private static URN hash1;
    private static URN hash2;
    private static URN hash3;
    private static URN hash4;

    /**
     * Ultrapeer 1 UDP connection.
     */
    private static DatagramSocket[] UDP_ACCESS;

    /**
     * File where urns (currently SHA1 urns) get persisted to
     */
    private static final String CREATION_CACHE_FILE = "createtimes.cache";
    private static final String FILE_PATH = "com/limegroup/gnutella/util";
    private static FileDesc[] descs;


	/**
	 * Constructs a new CreationTimeCacheTest with the specified name.
	 */
	public CreationTimeCacheTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(CreationTimeCacheTest.class);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

    private static void doSettings() {
        ConnectionSettings.PORT.setValue(PORT);
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
		UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(false);
		UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(true);
		UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(false);
		ConnectionSettings.NUM_CONNECTIONS.setValue(0);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
		SharingSettings.EXTENSIONS_TO_SHARE.setValue("txt;mp3");
        // get the resource file for com/limegroup/gnutella
        File berkeley = 
            CommonUtils.getResourceFile("com/limegroup/gnutella/berkeley.txt");
        File susheel = 
            CommonUtils.getResourceFile("com/limegroup/gnutella/susheel.txt");
        File mp3 = 
            CommonUtils.getResourceFile("com/limegroup/gnutella/mp3/mpg1layIII_0h_58k-VBRq30_frame1211_44100hz_joint_XingTAG_sample.mp3");
        // now move them to the share dir
        CommonUtils.copy(berkeley, new File(_sharedDir, "berkeley.txt"));
        CommonUtils.copy(susheel, new File(_sharedDir, "susheel.txt"));
        CommonUtils.copy(mp3, new File(_sharedDir, "metadata.mp3"));
        // make sure results get through
        SearchSettings.MINIMUM_SEARCH_QUALITY.setValue(-2);
    }        
    
    public static void globalSetUp() throws Exception {
        doSettings();

        callback=new MyActivityCallback();
        
        rs=new RouterService(callback, new StandardMessageRouter(),
                             new MyFileManager());
        assertEquals("unexpected port",
            PORT, ConnectionSettings.PORT.getValue());
        rs.start();
        rs.clearHostCatcher();
        rs.connect();
        Thread.sleep(1000);
        assertEquals("unexpected port",
            PORT, ConnectionSettings.PORT.getValue());

        hash1 = URN.createSHA1Urn("urn:sha1:GLIQY64M7FSXBSQEZY37FIM5QQSASUSH");
        hash2 = URN.createSHA1Urn("urn:sha1:GLIQY64M7FSXBSQEZY37FIM5QQSANITA");
        hash3 = URN.createSHA1Urn("urn:sha1:GLIQY64M7FSXBSQEZY37FIM5QQABOALT");
        hash4 = URN.createSHA1Urn("urn:sha1:GLIQY64M7FSXBSQEZY37FIM5BERKELEY");
    }        
    
    public void setUp() throws Exception  {
        doSettings();
    }
     
     private static Connection connect(RouterService rs, int port, 
                                       boolean ultrapeer) 
         throws Exception {
         ServerSocket ss=new ServerSocket(port);
         rs.connectToHostAsynchronously("127.0.0.1", port);
         Socket socket = ss.accept();
         ss.close();
         
         socket.setSoTimeout(3000);
         InputStream in=socket.getInputStream();
         String word=readWord(in);
         if (! word.equals("GNUTELLA"))
             throw new IOException("Bad word: "+word);
         
         HandshakeResponder responder;
         if (ultrapeer) {
             responder = new UltrapeerResponder();
         } else {
             responder = new OldResponder();
         }
         Connection con = new Connection(socket, responder);
         con.initialize();
         replyToPing(con, ultrapeer);
         return con;
     }
     
     /**
      * Acceptor.readWord
      *
      * @modifies sock
      * @effects Returns the first word (i.e., no whitespace) of less
      *  than 8 characters read from sock, or throws IOException if none
      *  found.
      */
     private static String readWord(InputStream sock) throws IOException {
         final int N=9;  //number of characters to look at
         char[] buf=new char[N];
         for (int i=0 ; i<N ; i++) {
             int got=sock.read();
             if (got==-1)  //EOF
                 throw new IOException();
             if ((char)got==' ') { //got word.  Exclude space.
                 return new String(buf,0,i);
             }
             buf[i]=(char)got;
         }
         throw new IOException();
     }

     private static void replyToPing(Connection c, boolean ultrapeer) 
             throws Exception {
        // respond to a ping iff one is given.
        Message m = null;
        byte[] guid;
        try {
            while (!(m instanceof PingRequest)) {
                m = c.receive(500);
            }
            guid = ((PingRequest)m).getGUID();            
        } catch(InterruptedIOException iioe) {
            //nothing's coming, send a fake pong anyway.
            guid = new GUID().bytes();
        }
        
        Socket socket = (Socket)PrivilegedAccessor.getValue(c, "_socket");
        PingReply reply = 
        PingReply.createExternal(guid, (byte)7,
                                 socket.getLocalPort(), 
                                 ultrapeer ? ultrapeerIP : oldIP,
                                 ultrapeer);
        reply.hop();
        c.send(reply);
        c.flush();
     }

    ///////////////////////// Actual Tests ////////////////////////////

    /** Tests that the URN_MAP is derived correctly from the TIME_MAP
     */
    public void testMapCreation() throws Exception {
        // mock up our own createtimes.txt
        Map toSerialize = new HashMap();
        toSerialize.put(hash1, new Long(0));
        toSerialize.put(hash2, new Long(1));
        toSerialize.put(hash3, new Long(1));
        toSerialize.put(hash4, new Long(2));

        ObjectOutputStream oos = 
        new ObjectOutputStream(new FileOutputStream(new File(_settingsDir,
                                                             CREATION_CACHE_FILE)));
        oos.writeObject(toSerialize);
        oos.close();
        
        // now have the CreationTimeCache read it in
        CreationTimeCache ctCache = new CreationTimeCache();
        Map TIME_MAP = (Map)PrivilegedAccessor.getValue(ctCache, "TIME_MAP");
        assertEquals(toSerialize, TIME_MAP);
    }


    /** Tests the getFiles() method.
     */
    public void testGetFiles() throws Exception {
        // mock up our own createtimes.txt
        Map toSerialize = new HashMap();
        toSerialize.put(hash1, new Long(1));
        toSerialize.put(hash2, new Long(2));
        toSerialize.put(hash3, new Long(0));
        toSerialize.put(hash4, new Long(1));

        ObjectOutputStream oos = 
        new ObjectOutputStream(new FileOutputStream(new File(_settingsDir,
                                                             CREATION_CACHE_FILE)));
        oos.writeObject(toSerialize);
        oos.close();
        
        // now have the CreationTimeCache read it in
        CreationTimeCache ctCache = new CreationTimeCache();
        // is everything mapped correctly from URN to Long?
        assertEquals(ctCache.getCreationTime(hash1), new Long(1));
        assertEquals(ctCache.getCreationTime(hash2), new Long(2));
        assertEquals(ctCache.getCreationTime(hash3), new Long(0));
        assertEquals(ctCache.getCreationTime(hash4), new Long(1));

        {
            Iterator iter = ctCache.getFiles();
            assertEquals(hash2, iter.next());
            URN urn = (URN) iter.next();
            assertTrue(urn.equals(hash1) || urn.equals(hash4));
            urn = (URN) iter.next();
            assertTrue(urn.equals(hash1) || urn.equals(hash4));
            assertEquals(hash3, iter.next());
            assertFalse(iter.hasNext());
        }

        {
            Iterator iter = ctCache.getFiles(4);
            assertEquals(hash2, iter.next());
            URN urn = (URN) iter.next();
            assertTrue(urn.equals(hash1) || urn.equals(hash4));
            urn = (URN) iter.next();
            assertTrue(urn.equals(hash1) || urn.equals(hash4));
            assertEquals(hash3, iter.next());
            assertFalse(iter.hasNext());
        }

        {
            Iterator iter = ctCache.getFiles(3);
            assertEquals(hash2, iter.next());
            URN urn = (URN) iter.next();
            assertTrue(urn.equals(hash1) || urn.equals(hash4));
            urn = (URN) iter.next();
            assertTrue(urn.equals(hash1) || urn.equals(hash4));
            assertFalse(iter.hasNext());
        }

        {
            Iterator iter = ctCache.getFiles(2);
            assertEquals(hash2, iter.next());
            URN urn = (URN) iter.next();
            assertTrue(urn.equals(hash1) || urn.equals(hash4));
            assertFalse(iter.hasNext());
        }

        {
            Iterator iter = ctCache.getFiles(1);
            assertEquals(hash2, iter.next());
            assertFalse(iter.hasNext());
        }

        {
            try {
                Iterator iter = ctCache.getFiles(0);
            }
            catch (IllegalArgumentException expected) {}
        }

    }
    

    /**
     * Test read & write of map
     */
    public void testPersistence() throws Exception {
        deleteCacheFile();
        assertTrue("cache should not be present", !cacheExists() );
        
        CreationTimeCache cache = CreationTimeCache.instance();
        FileDesc[] descs = createFileDescs();
        assertNotNull("should have some file descs", descs);
        assertGreaterThan("should have some file descs", 0, descs.length);
        cache.persistCache();
        assertTrue("cache should now exist", cacheExists());
        for( int i = 0; i < descs.length; i++) {
            Long cTime = cache.getCreationTime(descs[i].getSHA1Urn());
            assertNotNull("file should be present in cache", cTime);
        }
    }

	private static FileDesc[] createFileDescs() throws Exception {
        File path = CommonUtils.getResourceFile(FILE_PATH);
        File[] files = path.listFiles(new FileFilter() { 
            public boolean accept(File file) {
                return !file.isDirectory();
            }
        });
		FileDesc[] fileDescs = new FileDesc[files.length];
		for(int i=0; i<files.length; i++) {
			Set urns = FileDesc.calculateAndCacheURN(files[i]);            
			fileDescs[i] = new FileDesc(files[i], urns, i);
            CreationTimeCache.instance().addTime(fileDescs[i].getSHA1Urn(),
                                                 files[i].lastModified());
		}				
		return fileDescs;
	}

	private static void deleteCacheFile() {
		File cacheFile = new File(_settingsDir, CREATION_CACHE_FILE);
		cacheFile.delete();
	}

	/**
	 * Convenience method for making sure that the serialized file exists.
	 */
	private static boolean cacheExists() {
		File cacheFile = new File(_settingsDir, CREATION_CACHE_FILE);
		return cacheFile.exists();
	}



    //////////////////////////////////////////////////////////////////

    private void drainAll() throws Exception {
        drainAll(testUPs);
    }
    
    private static byte[] myIP() {
        return new byte[] { (byte)127, (byte)0, 0, 1 };
    }

    private static final boolean DEBUG = false;
    
    static void debug(String message) {
        if(DEBUG) 
            System.out.println(message);
    }

    private static class UltrapeerResponder implements HandshakeResponder {
        public HandshakeResponse respond(HandshakeResponse response, 
                boolean outgoing) throws IOException {
            Properties props = new UltrapeerHeaders("127.0.0.1"); 
            props.put(HeaderNames.X_DEGREE, "42");           
            return HandshakeResponse.createResponse(props);
        }
    }


    private static class OldResponder implements HandshakeResponder {
        public HandshakeResponse respond(HandshakeResponse response, 
                boolean outgoing) throws IOException {
            Properties props=new Properties();
            return HandshakeResponse.createResponse(props);
        }
    }

    public static class MyActivityCallback extends ActivityCallbackStub {
    }

    public static class MyFileManager extends MetaFileManager {
        private FileDesc fd = null;

        public FileDesc getFileDescForUrn(URN urn){
            if (fd == null) {
                File cacheFile = new File(_settingsDir, CREATION_CACHE_FILE);
                Set urnSet = new HashSet();
                urnSet.add(hash1);
                fd = new FileDesc(cacheFile, urnSet, 0);
            }
            if (urn.equals(hash1) ||
                urn.equals(hash2) ||
                urn.equals(hash3) ||
                urn.equals(hash4)) return fd;
            else return super.getFileDescForUrn(urn);
        }
    }

}
