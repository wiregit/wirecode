package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.*;
import com.sun.java.util.collections.*;
import junit.framework.*;
import java.io.*;

public class IncompleteFileManagerTest extends TestCase {
    private IncompleteFileManager ifm;
    private RemoteFileDesc rfd1, rfd2;

    public IncompleteFileManagerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return new TestSuite(IncompleteFileManagerTest.class);
    }
    
    public void setUp() {
        ifm=new IncompleteFileManager();
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
           fail("Invalid URN");
           return null;
       }
    }

    /////////////////////////////////////////////////////////////

	public void testLegacy() {
        //Tests blocks access.  Requires access to private data.
        IncompleteFileManager.unitTest();
    }

    /*
    public void testTempName() {
        //Requires change of access to tempName
        assertEquals("T-748-abc def",
                     IncompleteFileManager.tempName("abc def", 748, 0));
        assertEquals("T-748-abc def (2)",
                     IncompleteFileManager.tempName("abc def", 748, 2));
        assertEquals("T-748-abc.txt",
                     IncompleteFileManager.tempName("abc.txt", 748, 1));
        assertEquals("T-748-abc (2).txt",
                     IncompleteFileManager.tempName("abc.txt", 748, 2));
        assertEquals("T-748-.txt",
                     IncompleteFileManager.tempName(".txt", 748, 1));
        assertEquals("T-748- (2).txt",
                     IncompleteFileManager.tempName(".txt", 748, 2));
    }
    */
    

    /** Different name or size ==> different temp file */
    public void testGetFile_DifferentSize() {
        rfd1=newRFD("some file name", 1839, null);
        rfd2=newRFD("some file name", 1223, null);
        File tmp1=ifm.getFile(rfd1);
        File tmp2=ifm.getFile(rfd2);
        assertTrue(! tmp2.equals(tmp1));
    }

    /** 
     * You should be able to resume to a file with same hash but different name.
     */
    public void testGetFile_DifferentNameSameHash() {
        rfd1=newRFD("some file name", 1839, 
                    "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        rfd2=newRFD("another file name", 1839, 
                    "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
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
        File tmp1=ifm.getFile(rfd1);
        File tmp2=ifm.getFile(rfd2);
        assertTrue(! tmp2.equals(tmp1));
    }

    /** Risky resumes are allowed: no hashes */
    public void testGetFile_NoHash() {
        rfd1=newRFD("some file name", 1839, null);
        rfd2=newRFD("some file name", 1839, 
                    "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        File tmp1=ifm.getFile(rfd1);
        File tmp2=ifm.getFile(rfd2);
        assertEquals(tmp1, tmp2);
    }

    /** Serializes, then deserializes. */
    public void testSerialize() {
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
            
            //Make sure it's the same--this includes the hash.
            //TODO: check hash purging as well.
            VerifyingFile vf2=(VerifyingFile)ifm2.getEntry(
                                                 new File("T-1839-file name.txt"));
            assertTrue(vf2!=null);
            Iterator /* of Interval */ iter=vf2.getBlocks();
            assertTrue(iter.hasNext());
            assertEquals(new Interval(10, 100), iter.next());
            assertTrue(!iter.hasNext());
            assertEquals(new File("T-1839-file name.txt"),
                ifm2.getFile(newRFD("different name.txt", 1839, 
                                    "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB")));
        } catch (IOException e) {
            e.printStackTrace();
            fail("IO problem");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            fail("Class not found");
        } finally {
            if (tmp!=null)
                tmp.delete();
        }
    }


    /** Test that serialized versions of IncompleteFileManager v. 1.9 can be
     *  deserialized.  (No hashes, didn't use VerifyingFile internally.) */
    public void testDeserialize_19() {
        doDeserializeTest("IncompleteFileManager.1_9.dat");
    }

    /** Test that serialized versions of IncompleteFileManager v. 1.14 can be
     *  deserialized.  (No hashes.) */
    public void testOldDeserialize_114() {
        doDeserializeTest("IncompleteFileManager.1_14.dat");
    }

    private IncompleteFileManager doDeserializeTest(String filename) {
        IncompleteFileManager read=null;
        try {
            ObjectInputStream in=new ObjectInputStream(
                new FileInputStream(
                    "com/limegroup/gnutella/downloader/"+filename));
            read=(IncompleteFileManager)in.readObject();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
            fail("IO problem: "+e);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            fail("Class not found: "+e);
        }

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
}
