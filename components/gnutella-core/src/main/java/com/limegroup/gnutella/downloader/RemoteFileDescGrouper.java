package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.*;
import com.sun.java.util.collections.*;
import java.io.*;

/**
 * A sorted list of download "buckets", each of which is a sorted list of "same"
 * RemoteFileDesc.  Used by ManagedDownloader to group RemoteFileDesc to
 * implement swarmed downloads.  Oddly, this relies heavily on
 * IncompleteFileManager to decide if two files "same".  This class is
 * synchronized, but that is generally not sufficient for many contexts.  (The
 * add(..) method can modify a bucket that is being used externally.)  
 */
class RemoteFileDescGrouper implements Serializable {
    /** The actual buckets, each a list of same RemoteFileDesc's. */
    private List /* of List of RemoteFileDesc2 */ buckets;   
    /** The corresponding incomplete files for the buckets.  This is needed
     *  to implement addFile when all the elements of a bucket are removed.
     *  INVARIANT: for all i, j, 
     *     incompleteFileManager.getFile(buckets[i][j])=incompletes[i] 
     *  COROLLARY: buckets.size()==incompletes.size(); */
    private List /* of File */ incompletes;
    /** Used to calculate incomplete file lengths. */
    private IncompleteFileManager incompleteFileManager;
    /** The minimum quality considered likely to work.  Value of two corresponds
     *  to a THREE-star result. */
    private static final int DECENT_QUALITY=2;


    /** 
     * Creates a new RemoteFileDescGrouper containing copies of files (as
     * RemoteFileDesc2), in bucketed and sorted order.
     *
     * @param rfds the file/location pairs to group
     * @param incompleteFileManager passed by ManagedDownloader, used
     *  to detect "same" files by looking at temporary file names 
     */
    RemoteFileDescGrouper(RemoteFileDesc[] rfds,
                          IncompleteFileManager incompleteFileManager) {
        this.incompleteFileManager=incompleteFileManager;
        //This code is taken from the old bucket(..) method of
        //ManagedDownloader.  It runs in O(n lg n) time, where n==rfds.length.
        //Note that it is not equivalent to just call add(rfds[i]) on each
        //element, as add(..) does not reorder buckets.
        
        //1. Bucket the requested files.  
        Map /* File -> List<RemoteFileDesc> */ bucketMap=new HashMap();         
        for (int i=0; i<rfds.length; i++) {
            RemoteFileDesc rfd=new RemoteFileDesc2(rfds[i], false);
            File incompleteFile=incompleteFileManager.getFile(rfd);
            List siblings=(List)bucketMap.get(incompleteFile);
            if (siblings==null) {
                siblings=new ArrayList();
                bucketMap.put(incompleteFile, siblings);
            }
            siblings.add(rfd);
        }

        //2. Now for each file, estimate remaining download time.  This assumes
        //that we'll be able to download a file from all (only) three and
        //four-star locations in parallel at exactly the advertised speed.  Fat
        //chance that will happen, but it's probably a good enough heuristic.
        //Still, we may want to preference buckets with more quality loctions
        //even if the total bandwidth is lower.
        FilePair[] pairs=new FilePair[bucketMap.keySet().size()];
        int i=0;
        for (Iterator iter=bucketMap.keySet().iterator(); iter.hasNext(); i++) {
            File incompleteFile=(File)iter.next();
            List /* of RemoteFileDesc */ files=
                (List)bucketMap.get(incompleteFile);
            int size=((RemoteFileDesc)files.get(0)).getSize()
                        - incompleteFileManager.getBlockSize(incompleteFile);
            int bandwidth=1; //prevent divide by zero
            for (Iterator iter2=files.iterator(); iter2.hasNext(); ) {
                RemoteFileDesc2 rfd2=(RemoteFileDesc2)iter2.next();
                if (rfd2.getQuality()>=DECENT_QUALITY) {
                    //TODO2: don't normalize measured speeds.
					bandwidth+=normalize(rfd2.getSpeed());
				}
            }
            float time=(float)size/(float)bandwidth;
            pairs[i]=new FilePair(incompleteFile, time);
        }
        
        //3. Sort by download time and copy corresponding lists of files to new
        //array.
        Arrays.sort(pairs);
        buckets=new ArrayList(pairs.length);
        for (i=0; i<pairs.length; i++)
            buckets.add((List)bucketMap.get(pairs[i].file));

        //4. Build incompletes list by mapping getFile on first element of all
        //buckets.
        incompletes=new ArrayList(buckets.size());        
        for (Iterator iter=buckets.iterator(); iter.hasNext(); ) {
            List bucket=(List)iter.next();
            Assert.that(bucket.size()>0, "Empty bucket");
            RemoteFileDesc rfd=(RemoteFileDesc)bucket.get(0); //all same
            incompletes.add(incompleteFileManager.getFile(rfd));
        }
    }

    /** Normalizes the given speed, e.g., reports "50 KB/s" for T3 speeds
     *  @param speed the reported speed in kilobits/s
     *  @return the normalized speed, in kilobytes/s */
    private static int normalize(int speed) {
        if (speed<SpeedConstants.MODEM_SPEED_INT) {
            return 3;
        } else if (speed<SpeedConstants.CABLE_SPEED_INT) {
            return 30;
        } else if (speed<SpeedConstants.T1_SPEED_INT) {
            return 40;
        } else {
            return 50;
        }
    }

    /** File/time pair to help implement constructor. */
    private static class FilePair
            implements com.sun.java.util.collections.Comparable {
        float time;     //TODO: does this work with 1.1.8?
        File file;

        public FilePair(File file, float time) {
            this.time=time;
            this.file=file;
        }

        public int compareTo(Object o) {
            float diff=this.time-((FilePair)o).time;
            if (diff<0)
                return -1;
            else if (diff>0)
                return 1;
            else
                return 0;
        }
    }

    
    /** 
     * Adds the given rfd to this, modifying an existing bucket or adding a new
     * bucket as needed.
     *     @return true if a copy of rfd was added to an existing bucket, false
     *      added to a new bucket.
     *     @see buckets 
     */
    synchronized boolean add(RemoteFileDesc rfd) {
        //Convert rfd to a RemoteFileDesc2 so we can store auxilliary
        //information in ManagedDownloader.
        RemoteFileDesc rfd2=new RemoteFileDesc2(rfd, false);
        File incompleteFile=incompleteFileManager.getFile(rfd2);

        //Compare the incomplete file for rfd with the incomplete file for each
        //bucket.  Note that all elements of a bucket have the same incomplete
        //file.
        int n=buckets.size();
        Assert.that(incompletes.size()==n, 
                    "Length of buckets and incompletes different: "
                    +n+"!="+incompletes.size());
        for (int i=0; i<n; i++) {
            //This bucket may be empty, so we look at the incompleteFile.
            File otherIncompleteFile=(File)incompletes.get(i);
            if (otherIncompleteFile.equals(incompleteFile)) {
                //"Same" file, append to existing bucket.  TODO: insert into
                //appropriate place of bucket based on speed.
                List bucket=(List)buckets.get(i);
                bucket.add(rfd2);
                return true;
            }
        }

        //No match?  Add in new bucket at END of bucket list.  Don't forget to
        List bucket=new ArrayList();
        bucket.add(rfd2);
        buckets.add(bucket);
        incompletes.add(incompleteFile);
        return false;
    }

    /** 
     * Returns an iterator of the buckets in this, sorted by estimated download
     * speed.  Each bucket is a List of RemoteFileDesc, sorted by priority.  The
     * representation of this is exposed; changes to each bucket will modify
     * this and vice versa.  Furthermore, if a new bucket is subsequently added
     * to this by add(..), the new value WILL be yielded by the returned
     * iterator.  
     */
    Iterator /* of List of RemoteFileDesc */ buckets() {
        return new BucketIterator();
    }

    private class BucketIterator implements Iterator {
        /** The NEXT bucket to yield.  Implementation note: this iterator can
         *  reflect mutations to this because elements are only added to
         *  buckets, never removed. */
        int i=0;

        public boolean hasNext() {
            synchronized (RemoteFileDescGrouper.this) {
                return i<buckets.size();
            }
        }

        public Object next() {
            synchronized (RemoteFileDescGrouper.this) {
                if (! hasNext())
                    throw new NoSuchElementException();
                return buckets.get(i++);
            }
        }

        public void remove() {
            throw new com.sun.java.util.collections.UnsupportedOperationException();
        }
    }

    /*
    public static void main(String args[]) {
        //Test bucketing.  Note that the 1-star result is ignored.
        RemoteFileDescGrouper grouper=null;
        RemoteFileDesc[] allFiles=null;
        IncompleteFileManager ifm=new IncompleteFileManager();
        Iterator iter=null;
        List list=null; //a bucket
        RemoteFileDesc rf1=new RemoteFileDesc(
            "1.2.3.4", 6346, 0, "some file.txt", 1000, 
            new byte[16], SpeedConstants.T1_SPEED_INT, false, 3);
        RemoteFileDesc rf2=new RemoteFileDesc(
            "1.2.3.5", 6346, 0, "some file.txt", 1000, 
            new byte[16], SpeedConstants.T1_SPEED_INT, false, 3);
        RemoteFileDesc rf3=new RemoteFileDesc(
            "1.2.3.6", 6346, 0, "some file.txt", 1010, 
            new byte[16], SpeedConstants.T1_SPEED_INT, false, 3);
        RemoteFileDesc rf4=new RemoteFileDesc(
            "1.2.3.6", 6346, 0, "some file.txt", 1010, 
            new byte[16], SpeedConstants.T3_SPEED_INT, false, 0);
        

        //Simple case
        allFiles=new RemoteFileDesc[] {rf3, rf2, rf1, rf4};
        grouper=new RemoteFileDescGrouper(allFiles, ifm);
        iter=grouper.buckets();
        list=(List)iter.next();
        Assert.that(list.size()==2);
        Assert.that(list.contains(rf1));
        Assert.that(list.contains(rf2));
        list=(List)iter.next();
        Assert.that(list.size()==2);
        Assert.that(list.contains(rf3));
        Assert.that(list.contains(rf4));
        Assert.that(! iter.hasNext());


        //Incremental addition to existing bucket via add(..), reflected in
        //iterator.
        allFiles=new RemoteFileDesc[] {rf3};
        grouper=new RemoteFileDescGrouper(allFiles, ifm);
        iter=grouper.buckets();
        Assert.that(grouper.add(rf4)==true);
        list=(List)iter.next();
        Assert.that(list.size()==2);
        Assert.that(list.contains(rf3));
        Assert.that(list.contains(rf4));
        
        //Incremental addition of new bucket via add(..), reflected in iterator.
        //Note that rf1 is added AFTER rf3.
        allFiles=new RemoteFileDesc[] {rf3};
        grouper=new RemoteFileDescGrouper(allFiles, ifm);
        iter=grouper.buckets();
        list=(List)iter.next();
        Assert.that(list.size()==1);
        Assert.that(list.contains(rf3));
        Assert.that(grouper.add(rf1)==false);
        list=(List)iter.next();
        Assert.that(list.size()==1);
        Assert.that(list.contains(rf1));

        //Large part written on disk
        ifm.addBlock(ifm.getFile(rf3), 0, 1009);
        allFiles=new RemoteFileDesc[] {rf3, rf2, rf1, rf4};
        grouper=new RemoteFileDescGrouper(allFiles, ifm);
        iter=grouper.buckets();
        list=(List)iter.next();
        Assert.that(list.size()==2);
        Assert.that(list.contains(rf3));
        Assert.that(list.contains(rf4));
        list=(List)iter.next();
        Assert.that(list.size()==2);
        Assert.that(list.contains(rf1));
        Assert.that(list.contains(rf2));
        Assert.that(! iter.hasNext());

        //Adding an entry to a "phantom" bucket still in use
        allFiles=new RemoteFileDesc[] {rf3};
        grouper=new RemoteFileDescGrouper(allFiles, ifm);
        iter=grouper.buckets();
        list=(List)iter.next();
        Assert.that(list.size()==1);
        Assert.that(list.contains(rf3));
        list.remove(rf3);
        Assert.that(list.size()==0);
        Assert.that(grouper.add(rf4));
        Assert.that(list.size()==1);
        Assert.that(list.contains(rf4));
    }
    */
    
}
