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
 * add(..) method can modify a bucket that is being used externally.)<p>
 *
 * Currently this algorithm ensures that all elements of a bucket have the 
 * same name and size, and no elements of a bucket have differing hashes.
 * It does NOT guarantee that two elements with the same hash are in the same
 * bucket (for example, they may have differing names and sizes).  Nor does it
 * guarantee that all elements of a bucket actually have a hash.  While risky,
 * this policy maximizes swarming benefits from older clients.  In the future,
 * we may strengthen this policy.
 */
class RemoteFileDescGrouper implements Serializable {
    /** The actual buckets, each a list of same RemoteFileDesc's. */
    private List /* of List of RemoteFileDesc2 */ buckets=new ArrayList();
   
    /** The corresponding incomplete files for the buckets.  This is needed
     *  to implement addFile when all the elements of a bucket are removed.
     *  INVARIANT: for all i, j, 
     *     incompleteFileManager.getFile(buckets[i][j])=incompletes[i] 
     *  COROLLARY: buckets.size()==incompletes.size(); 
     *  COROLLARY: all elements of buckets[i] have same name and size
     */
    private List /* of File */ incompletes=new ArrayList();

    /** The corresponding SHA1 hashes for the buckets, if known.  This is needed
     *  to implement addFile when all the elements of a bucket are removed.
     *  We use an array because List's sometimes have trouble with null elements.
     *  INVARIANT: for all i, sha1s[i]==null || sha1s[i].isSHA1()
     *  INVARIANT: for all i, j, S=buckets[i][j].getSHA1Urn(),
     *     S!=null ==> sha1s[i].equals(S)
     *  COROLLARY: buckets.size()==incompletes.size(); 
     *  COROLLARY: no elements of buckets[i] have differing hashes.  This does
     *     NOT imply that two elements with the same hash are in the same 
     *     bucket. 
     *
     * Also note that if no element ever added to a bucket i had a SHA1, 
     * sha1s[i]==null.  However, sha1s[i] may be non-null even if a bucket
     * has no hashes because of previous entries to the bucket. */
    private URN[] /* of URN */ sha1s=new URN[0];

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

        //1. Bucket the requested files by repeatedly calling add(..).  This
        //runs in O(N^2) time.  It's possible to optimize.
        for (int i=0; i<rfds.length; i++) 
            add(rfds[i]);
        repOk();

        //2.  Now we need to rearrange the buckets according to download time.
        //We do this using another array.  First estimate remaining download
        //time.  This assumes that we'll be able to download a file from all
        //(only) three and four-star locations in parallel at exactly the
        //advertised speed.  Fat chance that will happen, but it's probably a
        //good enough heuristic.  Still, we may want to preference buckets with
        //more quality loctions even if the total bandwidth is lower.
        FileTuple[] pairs=new FileTuple[buckets.size()];
        for (int i=0; i<buckets.size(); i++) {
            File incompleteFile=(File)incompletes.get(i);
            List /* of RemoteFileDesc */ files=(List)buckets.get(i);
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
            pairs[i]=new FileTuple(files, incompleteFile, sha1s[i], time);
        }
        
        //3. Sort by download time and overwrite elements of buckets,
        //incompletes, and sha1s.
        Arrays.sort(pairs);
        for (int i=0; i<pairs.length; i++) {
            FileTuple pair=pairs[i];
            buckets.set(i, pair.bucket);
            incompletes.set(i, pair.incompleteFile);
            sha1s[i]=pair.sha1;
        }
        repOk();
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

    /** Bucket/incomplete/sha1/time pair to help implement constructor. */
    private static class FileTuple
            implements com.sun.java.util.collections.Comparable {
        List /* of RemoteFileDesc */ bucket;
        File incompleteFile;
        URN sha1;
        float time;

        public FileTuple(List /* of RemoteFileDesc */ bucket, 
                         File incompleteFile, 
                         URN sha1, 
                         float time) {
            this.bucket=bucket;;
            this.incompleteFile=incompleteFile;
            this.sha1=sha1;
            this.time=time;
        }

        public int compareTo(Object o) {
            float diff=this.time-((FileTuple)o).time;
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
        repOk();
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
            if (otherIncompleteFile.equals(incompleteFile)
                    && hashEquals(rfd2.getSHA1Urn(), sha1s[i])) {
                //"Same" file, append to existing bucket.  TODO: insert into
                //appropriate place of bucket based on speed?
                //Note: The URNs could have matched if sha1s[i]==null but
                //rfd2.getSHA1Urn()!=null. In this case, we set 
                //sha1s[i] == rfd2.getSHA1Urn().
                if(sha1s[i]==null && rfd2.getSHA1Urn()!=null)
                    sha1s[i]=rfd2.getSHA1Urn();
                List bucket=(List)buckets.get(i);
                bucket.add(rfd2);
                repOk();
                return true;
            }
        }

        //No match?  Add in new bucket at END of bucket list.  Don't forget to
        List bucket=new ArrayList();
        bucket.add(rfd2);
        buckets.add(bucket);
        incompletes.add(incompleteFile);
        //note: we need to be careful while adding to sha1s, since some elements
        //may be set to null, so we cannot simply append to the end of the list.
        int p = incompletes.size();
        URN[] newArray = new URN[p];
        System.arraycopy(sha1s,0,newArray,0,sha1s.length);
        newArray[p-1] = rfd.getSHA1Urn();   //may be null
        sha1s = newArray;
        repOk();
        return false;
    }

    synchronized URN getURNForBucket(int n) {
        if (n <0 || n>=buckets.size())
            throw new IllegalArgumentException();
        return sha1s[n];
    }



    /**
     * True if the URNS are equals.
     * @param urn1 a SHA1 URN, or null
     * @param urn2 a SHA1 URN, or null
     * @return false if urn1 and urn2 have differing hashes, true otherwise.
     *  Hence if urn1 or urn2 is null, this always returns true.
     */
    private static boolean hashEquals(URN urn1, URN urn2) {
        if (urn1==null || urn2==null)
            return true;
        return urn1.equals(urn2);
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

    //Unit tests: tests/com/limegroup/gnutella/
    //               downloader/RemoteFileDescGrouperTest.java

    static boolean DEBUG=false;
    /** Checks internal consistency.  Slow. */
    protected void repOk() {
        if (!DEBUG)
            return;

        Assert.that(buckets.size()==incompletes.size());
        Assert.that(buckets.size()==sha1s.length);

        //Check incompletes.  For each bucket i...
        for (int i=0; i<buckets.size(); i++) {
            List bucket=(List)buckets.get(i);
            File tmp1=(File)incompletes.get(i);
            for (int j=0; j<bucket.size(); j++) {
                File tmp2=incompleteFileManager.getFile(
                                    (RemoteFileDesc)bucket.get(j));
                Assert.that(tmp1.equals(tmp2));
            }
        }

        //Check URN's.  For each bucket i...
        for (int i=0; i<buckets.size(); i++) {
            List bucket=(List)buckets.get(i);
            URN urn1=sha1s[i];
            boolean gotURN=false;
            for (int j=0; j<bucket.size(); j++) {
                URN urn2=((RemoteFileDesc)bucket.get(j)).getSHA1Urn();
                if (urn2!=null) {      
                    Assert.that(urn1!=null);
                    Assert.that(urn1.equals(urn2));
                    gotURN=true;
                }
            }
        }
    }
}
