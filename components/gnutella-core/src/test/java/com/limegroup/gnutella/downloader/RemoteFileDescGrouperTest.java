package com.limegroup.gnutella.downloader;

import junit.framework.*;
import com.limegroup.gnutella.*;
import com.sun.java.util.collections.*;

public class RemoteFileDescGrouperTest extends TestCase {

    public RemoteFileDescGrouperTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(RemoteFileDescGrouperTest.class);
    }

    public void testLegacy() {
        //Test bucketing.  Note that the 1-star result is ignored.
        RemoteFileDescGrouper grouper=null;
        RemoteFileDesc[] allFiles=null;
        IncompleteFileManager ifm=new IncompleteFileManager();
        Iterator iter=null;
        List list=null; //a bucket
        RemoteFileDesc rf1=new RemoteFileDesc(
            "1.2.3.4", 6346, 0, "some file.txt", 1000, 
            new byte[16], SpeedConstants.T1_SPEED_INT, false, 3, 
            false, null, null);
        RemoteFileDesc rf2=new RemoteFileDesc(
            "1.2.3.5", 6346, 0, "some file.txt", 1000, 
            new byte[16], SpeedConstants.T1_SPEED_INT, false, 3, 
            false, null, null);
        RemoteFileDesc rf3=new RemoteFileDesc(
            "1.2.3.6", 6346, 0, "some file.txt", 1010, 
            new byte[16], SpeedConstants.T1_SPEED_INT, false, 3, 
            false, null, null);
        RemoteFileDesc rf4=new RemoteFileDesc(
            "1.2.3.6", 6346, 0, "some file.txt", 1010, 
            new byte[16], SpeedConstants.T3_SPEED_INT, false, 0, 
            false, null, null);
        

        //Simple case
        allFiles=new RemoteFileDesc[] {rf3, rf2, rf1, rf4};
        grouper=new RemoteFileDescGrouper(allFiles, ifm);
        iter=grouper.buckets();
        list=(List)iter.next();
        assertTrue(list.size()==2);
        assertTrue(list.contains(rf1));
        assertTrue(list.contains(rf2));
        list=(List)iter.next();
        assertTrue(list.size()==2);
        assertTrue(list.contains(rf3));
        assertTrue(list.contains(rf4));
        assertTrue(! iter.hasNext());


        //Incremental addition to existing bucket via add(..), reflected in
        //iterator.
        allFiles=new RemoteFileDesc[] {rf3};
        grouper=new RemoteFileDescGrouper(allFiles, ifm);
        iter=grouper.buckets();
        assertTrue(grouper.add(rf4)==true);
        list=(List)iter.next();
        assertTrue(list.size()==2);
        assertTrue(list.contains(rf3));
        assertTrue(list.contains(rf4));
        
        //Incremental addition of new bucket via add(..), reflected in iterator.
        //Note that rf1 is added AFTER rf3.
        allFiles=new RemoteFileDesc[] {rf3};
        grouper=new RemoteFileDescGrouper(allFiles, ifm);
        iter=grouper.buckets();
        list=(List)iter.next();
        assertTrue(list.size()==1);
        assertTrue(list.contains(rf3));
        assertTrue(grouper.add(rf1)==false);
        list=(List)iter.next();
        assertTrue(list.size()==1);
        assertTrue(list.contains(rf1));

        //Large part written on disk
        VerifyingFile vf=new VerifyingFile(false);
        vf.addInterval(new Interval(0, 1008));
        ifm.addEntry(ifm.getFile(rf3), vf);
        allFiles=new RemoteFileDesc[] {rf3, rf2, rf1, rf4};
        grouper=new RemoteFileDescGrouper(allFiles, ifm);
        iter=grouper.buckets();
        list=(List)iter.next();
        assertTrue(list.size()==2);
        assertTrue(list.contains(rf3));
        assertTrue(list.contains(rf4));
        list=(List)iter.next();
        assertTrue(list.size()==2);
        assertTrue(list.contains(rf1));
        assertTrue(list.contains(rf2));
        assertTrue(! iter.hasNext());

        //Adding an entry to a "phantom" bucket still in use
        allFiles=new RemoteFileDesc[] {rf3};
        grouper=new RemoteFileDescGrouper(allFiles, ifm);
        iter=grouper.buckets();
        list=(List)iter.next();
        assertTrue(list.size()==1);
        assertTrue(list.contains(rf3));
        list.remove(rf3);
        assertTrue(list.size()==0);
        assertTrue(grouper.add(rf4));
        assertTrue(list.size()==1);
        assertTrue(list.contains(rf4));
    }
}
