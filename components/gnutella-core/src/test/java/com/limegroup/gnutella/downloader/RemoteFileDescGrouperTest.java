package com.limegroup.gnutella.downloader;

import junit.framework.*;
import com.limegroup.gnutella.*;
import com.sun.java.util.collections.*;
import java.io.IOException;

public class RemoteFileDescGrouperTest extends com.limegroup.gnutella.util.BaseTestCase {

    public RemoteFileDescGrouperTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(RemoteFileDescGrouperTest.class);
    }

    RemoteFileDesc rf1, rf2, rf3, rf4, rf5, rf6;
    IncompleteFileManager ifm;
    
    public void setUp() throws Exception {
        RemoteFileDescGrouper.DEBUG=true;

        ifm=new IncompleteFileManager();
        rf1=new RemoteFileDesc(
            "1.2.3.4", 6346, 0, "some file.txt", 1000, 
            new byte[16], SpeedConstants.T1_SPEED_INT, false, 3, 
            false, null, null);
        rf2=new RemoteFileDesc(
            "1.2.3.5", 6346, 0, "some file.txt", 1000, 
            new byte[16], SpeedConstants.T1_SPEED_INT, false, 3, 
            false, null, null);
        rf3=new RemoteFileDesc(
            "1.2.3.6", 6346, 0, "some file.txt", 1010, 
            new byte[16], SpeedConstants.T1_SPEED_INT, false, 3, 
            false, null, null);
        rf4=new RemoteFileDesc(
            "1.2.3.6", 6346, 0, "some file.txt", 1010, 
            new byte[16], SpeedConstants.T3_SPEED_INT, false, 0, 
            false, null, null);

        Set /* of URN */ urnSet1=null;
        Set /* of URN */ urnSet2=null;
        try {
            final URN urn1=URN.createSHA1Urn(
                "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
            urnSet1=new HashSet(1);
            urnSet1.add(urn1);
            final URN urn2=URN.createSHA1Urn(
                "urn:sha1:ALST4IPQGSSZTS5FJUPAKPZWUGYQYPFB");
            urnSet2=new HashSet(1);
            urnSet2.add(urn2);
        } catch (IOException e) { 
            fail("Couldn't make valid URN", e);
        }

        //Same name and size as rf1/rf2, different hashes.
        rf5=new RemoteFileDesc(
            "1.2.3.7", 6346, 0, "some file.txt", 1000, 
            new byte[16], SpeedConstants.T1_SPEED_INT, false, 3, 
            false, null, urnSet1);
        rf6=new RemoteFileDesc(
            "1.2.3.8", 6346, 0, "some file.txt", 1000, 
            new byte[16], SpeedConstants.T1_SPEED_INT, false, 3, 
            false, null, urnSet2);
    }


    public void testLegacy() {
        //Test bucketing.  Note that the 1-star result is ignored.
        RemoteFileDescGrouper grouper=null;
        RemoteFileDesc[] allFiles=null;
        Iterator iter=null;
        List list=null; //a bucket        

        //Simple case
        allFiles=new RemoteFileDesc[] {rf3, rf2, rf1, rf4};
        grouper=new RemoteFileDescGrouper(allFiles, ifm);
        iter=grouper.buckets();
        list=(List)iter.next();
        assertEquals(2, list.size());
        assertTrue(list.contains(rf1));
        assertTrue(list.contains(rf2));
        list=(List)iter.next();
        assertEquals(2, list.size());
        assertTrue(list.contains(rf3));
        assertTrue(list.contains(rf4));
        assertTrue(! iter.hasNext());


        //Incremental addition to existing bucket via add(..), reflected in
        //iterator.
        allFiles=new RemoteFileDesc[] {rf3};
        grouper=new RemoteFileDescGrouper(allFiles, ifm);
        iter=grouper.buckets();
        assertTrue(grouper.add(rf4));
        list=(List)iter.next();
        assertEquals(2, list.size());
        assertTrue(list.contains(rf3));
        assertTrue(list.contains(rf4));
        
        //Incremental addition of new bucket via add(..), reflected in iterator.
        //Note that rf1 is added AFTER rf3.
        allFiles=new RemoteFileDesc[] {rf3};
        grouper=new RemoteFileDescGrouper(allFiles, ifm);
        iter=grouper.buckets();
        list=(List)iter.next();
        assertEquals(1, list.size());
        assertTrue(list.contains(rf3));
        assertTrue(!grouper.add(rf1));
        list=(List)iter.next();
        assertEquals(1, list.size());
        assertTrue(list.contains(rf1));

        //Large part written on disk
        VerifyingFile vf=new VerifyingFile(false);
        vf.addInterval(new Interval(0, 1008));
        ifm.addEntry(ifm.getFile(rf3), vf);
        allFiles=new RemoteFileDesc[] {rf3, rf2, rf1, rf4};
        grouper=new RemoteFileDescGrouper(allFiles, ifm);
        iter=grouper.buckets();
        list=(List)iter.next();
        assertEquals(2, list.size());
        assertTrue(list.contains(rf3));
        assertTrue(list.contains(rf4));
        list=(List)iter.next();
        assertEquals(2, list.size());
        assertTrue(list.contains(rf1));
        assertTrue(list.contains(rf2));
        assertTrue(! iter.hasNext());

        //Adding an entry to a "phantom" bucket still in use
        allFiles=new RemoteFileDesc[] {rf3};
        grouper=new RemoteFileDescGrouper(allFiles, ifm);
        iter=grouper.buckets();
        list=(List)iter.next();
        assertEquals(1, list.size());
        assertTrue(list.contains(rf3));
        list.remove(rf3);
        assertEquals(0, list.size());
        assertTrue(grouper.add(rf4));
        assertEquals(1, list.size());
        assertTrue(list.contains(rf4));
    }

    public void testAddWithHash() {
        RemoteFileDesc[] allFiles = new RemoteFileDesc[] {rf1};
        RemoteFileDescGrouper grouper = new RemoteFileDescGrouper(allFiles,ifm);
        Iterator iter = grouper.buckets();
        List list = (List)iter.next();
        assertEquals(1,list.size());
        grouper.add(rf5);
        iter = grouper.buckets();
        list = (List)iter.next();
        assertTrue(!iter.hasNext());//only one bucket
        assertEquals(2,list.size());
        assertTrue(list.contains(rf1));
        assertTrue(list.contains(rf5));
        grouper.add(rf6); //this creates a new bucket
        iter = grouper.buckets();
        list = (List)iter.next(); //first bucket w/ rf1 and rf5 
        //(wish we could check sha1s[0] == rf5.getSHA1Urn().)
        assertEquals(2,list.size());
        assertTrue(list.contains(rf1));
        assertTrue(list.contains(rf5));
        list = (List)iter.next(); //second bucket w/ rf6
        //(wish we could check sha1s[1] == rf6.getSHA1Urn().)
        assertTrue(!iter.hasNext());
        assertEquals(1,list.size());
        assertTrue(list.contains(rf6));
    }


    public void testConstructorWithHash() {
        //Stricter than necessary.
        RemoteFileDesc[] allFiles=new RemoteFileDesc[] {rf5, rf6, rf1};
        RemoteFileDescGrouper grouper=new RemoteFileDescGrouper(allFiles, ifm);
        Iterator iter=grouper.buckets();
        List list=(List)iter.next();
        assertEquals(2, list.size());
        assertTrue(list.contains(rf5));
        assertTrue(list.contains(rf1));
        list=(List)iter.next();
        assertEquals(1, list.size());
        assertTrue(list.contains(rf6));
        assertTrue(! iter.hasNext());        
    }

    public void testPhantomHash() {        
        RemoteFileDesc[] allFiles=new RemoteFileDesc[] {rf5};
        RemoteFileDescGrouper grouper=new RemoteFileDescGrouper(allFiles, ifm);
        Iterator iter=grouper.buckets();
        List list=(List)iter.next();
        assertTrue(list.size()==1);
        assertTrue(list.contains(rf5));
        list.remove(rf5);
        assertTrue(list.size()==0);

        //Add rf6.  The memory of rf5's hash is retained, so rf6 is put in
        //another bucket.
        assertTrue(!grouper.add(rf6));
        assertTrue(list.size()==0);

        iter=grouper.buckets();
        list=(List)iter.next();
        assertEquals(0, list.size());
        list=(List)iter.next();
        assertEquals(1, list.size());
        assertTrue(list.contains(rf6));
        assertTrue(!iter.hasNext());
    }
}
