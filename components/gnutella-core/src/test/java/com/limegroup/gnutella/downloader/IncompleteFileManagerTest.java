package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.sun.java.util.collections.*;
import junit.framework.*;
import java.io.*;

public class IncompleteFileManagerTest extends com.limegroup.gnutella.util.BaseTestCase {
    private IncompleteFileManager ifm;
    private RemoteFileDesc rfd1, rfd2;
    private FileManager fm;
    
    static {
        SettingsManager.instance();
    }
    

    public IncompleteFileManagerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(IncompleteFileManagerTest.class);
    }
    
    public void setUp() {
        ifm=new IncompleteFileManager();
        fm = RouterService.getFileManager();
    }

    /** @param urn a SHA1 urn, or null */
    public static RemoteFileDesc newRFD(String name, int size, String urn) {
       try {
           Set urns=new HashSet(1);
           if (urn!=null) 
               urns.add(URN.createSHA1Urn(urn));
           return new RemoteFileDesc(
               "18.239.0.144", 6346, 13l,
               name, size,
               new byte[16], 56, false, 4, true, null,
               urns);
       } catch (IOException e) {
           fail("Invalid URN", e);
           return null;
       }
    }

    /////////////////////////////////////////////////////////////

	public void testLegacy() throws Throwable {
        File file=new File(getSaveDirectory(), "T-748-test.txt");
        IncompleteFileManager ifm=new IncompleteFileManager();
        Iterator iter=null;
        VerifyingFile vf = new VerifyingFile(true);
        //0 blocks
        assertNull(ifm.getEntry(file));
        assertEquals(0, ifm.getBlockSize(file));
        //1 block
        vf.addInterval(new Interval(0,10));
        ifm.addEntry(file,vf);
        assertEquals(11, ifm.getBlockSize(file));//full inclusive now
        iter=ifm.getEntry(file).getBlocks();
        assertEquals(new Interval(0, 10), iter.next());
        assertTrue(! iter.hasNext());
        
        SettingsManager.instance().setIncompletePurgeTime(26);
        File young=new FakeTimedFile(25);
        File old=new FakeTimedFile(27);
        assertTrue(!isOld(young));
        assertTrue(isOld(old));
    }

    public void testTempName() throws Throwable {
        assertEquals("T-748-abc def",
                     tempName("abc def", 748, 0));
        assertEquals("T-748-abc def (2)",
                     tempName("abc def", 748, 2));
        assertEquals("T-748-abc.txt",
                     tempName("abc.txt", 748, 1));
        assertEquals("T-748-abc (2).txt",
                     tempName("abc.txt", 748, 2));
        assertEquals("T-748-.txt",
                     tempName(".txt", 748, 1));
        assertEquals("T-748- (2).txt",
                     tempName(".txt", 748, 2));
    }
    

    /** Different name or size ==> different temp file */
    public void testGetFile_DifferentSize() {
        rfd1=newRFD("some file name", 1839, null);
        rfd2=newRFD("some file name", 1223, null);
        assertTrue(! IncompleteFileManager.same(rfd1, rfd2));
        File tmp1=ifm.getFile(rfd1);
        File tmp2=ifm.getFile(rfd2);
        assertNotEquals(tmp2, tmp1);
    }

    /** 
     * You should be able to resume to a file with same hash but different name.
     */
    public void testGetFile_DifferentNameSameHash() {
        rfd1=newRFD("some file name", 1839, 
                    "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        rfd2=newRFD("another file name", 1839, 
                    "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        assertTrue(IncompleteFileManager.same(rfd1, rfd2));
        File tmp1=ifm.getFile(rfd1);
        File tmp2=ifm.getFile(rfd2);
        assertEquals(tmp1, tmp2);
    }

    /** 
     * You should NOT be able to resume to file w/ same name but different hash.
     */
    public void testGetFile_SameNameDifferentHash() {
        rfd1=newRFD("some file name", 1839, 
                    "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        rfd2=newRFD("some file name", 1839, 
                    "urn:sha1:LSTHIPQGSGSZTS5FJPAKPZWUGYQYPFBU");
        assertTrue(! IncompleteFileManager.same(rfd1, rfd2));
        File tmp1=ifm.getFile(rfd1);
        File tmp2=ifm.getFile(rfd2);
        assertNotEquals(tmp2, tmp1);
    }

    /** Risky resumes are allowed: no hashes */
    public void testGetFile_NoHash() {
        rfd1=newRFD("some file name", 1839, null);
        rfd2=newRFD("some file name", 1839, 
                    "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        assertTrue(IncompleteFileManager.same(rfd1, rfd2));
        File tmp1=ifm.getFile(rfd1);
        File tmp2=ifm.getFile(rfd2);
        assertEquals(tmp1, tmp2);
    }

    /** Checks that removeEntry clears blocks AND hashes. */
    public void testRemoveEntry() {       
        //Populate IFM with a hash.
        rfd1=newRFD("some file name", 1839, 
                    "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB"); 
        File tmp1=ifm.getFile(rfd1);
        VerifyingFile vf=new VerifyingFile(true);
        ifm.addEntry(tmp1, vf);

        //After deleting entry there should be no more blocks...
        ifm.removeEntry(tmp1);      
        assertNull(ifm.getEntry(tmp1));

        //...and same temp file should be used for different hash. [sic]
        rfd2=newRFD("some file name", 1839, 
                    "urn:sha1:LSTHIPQGSGSZTS5FJPAKPZWUGYQYPFBU");
        File tmp2=ifm.getFile(rfd2);
        assertEquals(tmp1, tmp2);
        assertEquals(tmp2, ifm.getFile(newRFD("some file name", 1839, null)));
    }
    
    /**
     * Checks that addEntry & removeEntry notify the FileManager of the
     * added / removed incomplete file.
     */
    public void testFileManagerIsNotified() throws Exception {
        assertEquals(0, fm.getNumIncompleteFiles()); // begin with 0 shared.
        
        //Populate IFM with a hash.
        rfd1=newRFD("some file name", 1839, 
                    "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        File tmp1=ifm.getFile(rfd1);
        VerifyingFile vf=new VerifyingFile(true);
        ifm.addEntry(tmp1, vf);
        
        assertEquals(1, fm.getNumIncompleteFiles()); // 1 added.
        
        // make sure it's associated with a URN.
        URN urn = URN.createSHA1Urn(    
            "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        FileDesc fd = fm.getFileDescForUrn(urn);
        assertNotNull(urn);
        assertInstanceof(IncompleteFileDesc.class, fd);
        
        ifm.removeEntry(tmp1);
        
        assertEquals(0, fm.getNumIncompleteFiles()); // back to 0 shared.
    }   

    public void testCompletedHash_NotFound() {
        File tmp2=new File("T-1839-some file name");
        assertNull(ifm.getCompletedHash(tmp2));
    }

    public void testCompletedHash_Found() {
        rfd1=newRFD("some file name", 1839, 
                    "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        File tmp1=ifm.getFile(rfd1);
        try {
            URN urn=URN.createSHA1Urn( 
                "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
            assertEquals(urn, ifm.getCompletedHash(tmp1));
        } catch (IOException e) {
            fail("Couldn't make URN", e);
        }
    }

    public void testCompletedName() {
        File tmp1=new File("T-1839-some file.txt");
        assertEquals("some file.txt", ifm.getCompletedName(tmp1));
    }

    public void testCompletedName_IllegalArgument() {
        try {
            ifm.getCompletedName(new File("no dash.txt"));
            fail("Accepted bad file");
        } catch (IllegalArgumentException pass) { }

        try {
            ifm.getCompletedName(new File("T-one dash.txt"));
            fail("Accepted bad file");
        } catch (IllegalArgumentException pass) { }

        try {
            ifm.getCompletedName(new File("T-123-"));
            fail("Accepted bad file");
        } catch (IllegalArgumentException pass) { }
    }

    public void testCompletedSize() {
        File tmp1=new File("T-1839-some file.txt");
        assertEquals(1839, ifm.getCompletedSize(tmp1));
    }

    public void testCompletedSize_IllegalArgument() {
        try {
            ifm.getCompletedSize(new File("no dash.txt"));
            fail("Accepted bad file");
        } catch (IllegalArgumentException pass) { }

        try {
            ifm.getCompletedSize(new File("T-one dash.txt"));
            fail("Accepted bad file");
        } catch (IllegalArgumentException pass) { }

        try {
            ifm.getCompletedSize(new File("T--no number.txt"));
            fail("Accepted bad file");
        } catch (IllegalArgumentException pass) { }

        try {
            ifm.getCompletedSize(new File("T-x-bad number.txt"));
            fail("Accepted bad file");
        } catch (IllegalArgumentException pass) { }
    }

    /** Tests that hash information is purged when calling purge(true). */
    public void testPurgeHash_Yes() {
        //These files have the same hash, but no blocks have been written.
        RemoteFileDesc rfd1=newRFD("file name.txt", 1839, 
                                   "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        RemoteFileDesc rfd1b=newRFD("other file.txt", 1839, 
                                   "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        File file1=ifm.getFile(rfd1);
        File file1b=ifm.getFile(rfd1b);
        assertEquals(file1, file1b);

        //These files have the same hash, but blocks have been written to disk.
        RemoteFileDesc rfd2=newRFD("another name.txt", 1839, 
                                   "urn:sha1:LSTHIPQGSSZGTS5FJUPAKPZWUGYQYPFB");
        RemoteFileDesc rfd2b=newRFD("yet another file.txt", 1839, 
                                   "urn:sha1:LSTHIPQGSSZGTS5FJUPAKPZWUGYQYPFB");
        File file2=ifm.getFile(rfd2);
        File file2b=ifm.getFile(rfd2b);
        assertEquals(file2, file2b);
        try {
            file2.createNewFile();
        } catch (IOException e) {
            fail("Couldn't create "+file2, e);
        }

        //After purging, only hashes associated with files that exists remain.
        ifm.purge(true);
        File file1c=ifm.getFile(rfd1b);
        assertNotEquals(file1b, file1c);
        File file2c=ifm.getFile(rfd2b);
        assertEquals(file2b ,file2c);
        file2.delete();
    }

    /** Tests that hash information is not purged when calling purge(false). */
    public void testPurgeHash_No() {
        RemoteFileDesc rfd1=newRFD("file name.txt", 1839, 
                                   "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        RemoteFileDesc rfd2=newRFD("other file.txt", 1839, 
                                   "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        File file1=ifm.getFile(rfd1);
        File file2=ifm.getFile(rfd2);
        assertEquals(file1, file2);
        ifm.purge(false);             //Does nothing
        File file2b=ifm.getFile(rfd2);
        assertEquals(file2, file2b);
    }

    /** Serializes, then deserializes. */
    public void testSerialize() throws Exception {
        File tmp=null;
        try {
            //Create an IFM with one entry, with hash.
            IncompleteFileManager ifm1=new IncompleteFileManager();
            RemoteFileDesc rfd=newRFD("file name.txt", 1839, 
                "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
            File incomplete=ifm1.getFile(rfd);
            VerifyingFile vf=new VerifyingFile(false);
            vf.addInterval(new Interval(10, 100));  //inclusive
            ifm1.addEntry(incomplete, vf);

            //Write to disk.
            tmp=File.createTempFile("IncompleteFileManagerTest", ".dat");
            ObjectOutputStream out=new ObjectOutputStream(
                                       new FileOutputStream(tmp));
            out.writeObject(ifm1);
            out.close();
            ifm1=null;
            
            //Read IFM from disk.
            ObjectInputStream in=new ObjectInputStream(
                                       new FileInputStream(tmp));
            IncompleteFileManager ifm2=(IncompleteFileManager)in.readObject();
            in.close();
            
            //Make sure it's the same.
            File incomp = null;
            File inDir = null;
            try {
                inDir = SettingsManager.instance().getIncompleteDirectory();
            } catch(java.io.FileNotFoundException fnfe) {
                fail("unable to set up test-cannot find incomplete directory", fnfe);
            }
            incomp =  new File(inDir, "T-1839-file name.txt");
            VerifyingFile vf2=(VerifyingFile)ifm2.getEntry(incomp);
            assertTrue(vf2!=null);
            Iterator /* of Interval */ iter=vf2.getBlocks();
            assertTrue(iter.hasNext());
            assertEquals(new Interval(10, 100), iter.next());
            assertTrue(!iter.hasNext());
            assertEquals(new File(inDir, "T-1839-file name.txt"),
                ifm2.getFile(newRFD("different name.txt", 1839, 
                                    "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB")));
        } finally {
            if (tmp!=null)
                tmp.delete();
        }
    }


    /** Test that serialized versions of IncompleteFileManager v. 1.9 can be
     *  deserialized.  (No hashes, didn't use VerifyingFile internally.) */
    public void testDeserialize_19() throws Exception{
        doDeserializeTest("IncompleteFileManager.1_9.dat");
    }

    /** Test that serialized versions of IncompleteFileManager v. 1.14 can be
     *  deserialized.  (No hashes.) */
    public void testOldDeserialize_114() throws Exception {
        doDeserializeTest("IncompleteFileManager.1_14.dat");
    }

    private IncompleteFileManager doDeserializeTest(String filename) throws Exception {
        IncompleteFileManager read=null;
        ObjectInputStream in=new ObjectInputStream(
            new FileInputStream( CommonUtils.getResourceFile(
                "com/limegroup/gnutella/downloader/"+filename
            ) )
        );
        read=(IncompleteFileManager)in.readObject();
        in.close();

        VerifyingFile vf=(VerifyingFile)read.getEntry(
                                               new File("another file.txt"));
        assertTrue(vf!=null);
        Iterator /* of Interval */ iter=vf.getBlocks();
        assertTrue(iter.hasNext());
        assertEquals(new Interval(3, 999), iter.next());
        assertTrue(!iter.hasNext());

        vf=(VerifyingFile)read.getEntry(new File("hello world.txt"));
        assertTrue(vf!=null);
        iter=vf.getBlocks();
        assertTrue(iter.hasNext());
        assertEquals(new Interval(1, 99), iter.next());
        assertTrue(!iter.hasNext());

        return ifm;
    }
    
	private static String tempName(String s, int one, int two) throws Throwable {
	    try {
            return (String)PrivilegedAccessor.invokeMethod(
                IncompleteFileManager.class, "tempName", 
                new Object[] {s, new Integer(one), new Integer(two)},
                new Class[] {String.class, Integer.TYPE, Integer.TYPE});
        } catch(Exception e) {
            if ( e.getCause() != null ) 
                throw e.getCause();
            else throw e;
        }
    }
    
	private static boolean isOld(File f) throws Throwable {
	    try {
            return ((Boolean)PrivilegedAccessor.invokeMethod(
                IncompleteFileManager.class, "isOld", 
                new Object[] {f},
                new Class[] {File.class} )).booleanValue();
        } catch(Exception e) {
            if ( e.getCause() != null ) 
                throw e.getCause();
            else throw e;
        }
    }    
    
    static class FakeTimedFile extends File {
        private long days;
        FakeTimedFile(int days) {
            super("whatever.txt");
            this.days=days;
        }

        public long lastModified() {
            //30 days ago
            return System.currentTimeMillis()-days*24l*60l*60l*1000l;
        }
    }       
}
