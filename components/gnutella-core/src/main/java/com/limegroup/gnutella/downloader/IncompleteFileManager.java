package com.limegroup.gnutella.downloader;

import java.io.*;
import com.limegroup.gnutella.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.util.FileComparator;

/** 
 * A repository of temporary filenames.  Gives out file names for temporary
 * files, ensuring that two duplicate files always get the same name.  This
 * enables smart resumes across hosts.  Also keeps track of the blocks 
 * downloaded, for smart downloading purposes.  <b>Thread safe.</b><p>
 *
 * The original version of this class ensured that two IncompleteFileManager
 * never gave out the same temporary files.  That restriction has now been
 * relaxed, so this class is really somewhat overkill.  However, in the future
 * it may become smarter by looking at hashes, etc. 
 */
public class IncompleteFileManager implements Serializable {
    /** Ensures backwards compatibility. */
    static final long serialVersionUID = -7658285233614679878L;

    /** The delimiter to use between the size and a real name of a temporary
     * file.  To make it easier to break the temporary name into its constituent
     * parts, this should not contain a number. */
    static final String SEPARATOR="-";
    /** The prefix added to preview copies of incomplete files. */
    public static final String PREVIEW_PREFIX="Preview-";
    
    /**
     * A mapping from temporary files (File) to the blocks of the file stored on
     * disk (List of Interval).  Needed for resumptive smart downloads.
     * INVARIANT: all blocks disjoint, no two intervals can be coalesced into
     * one interval.  Note that blocks are no sorted; there are typically few
     * blocks so performance isn't an issue.
     */
    private Map /* File -> List<Interval> */ blocks=
        new TreeMap(new FileComparator());  


    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Deletes incomplete files more than INCOMPLETE_PURGE_TIME days old from
     * disk.  Then removes any entries from this for which there is no file on
     * disk.  
     * @return true iff any entries were purged
     */
    public synchronized boolean purge() {
        boolean ret=false;
        for (Iterator iter=blocks.keySet().iterator(); iter.hasNext(); ) {
            File file=(File)iter.next();
            if (!file.exists() || isOld(file)) {
                ret=true;
                file.delete();  //always safe to call; return value ignored
                iter.remove();
            }                
        }
        return ret;
    }

    /** Returns true iff file is "too old". */
    private static final boolean isOld(File file) {
        //Inlining this method allows some optimizations--not that they matter.
        long lastModified=file.lastModified();
        int days=SettingsManager.instance().getIncompletePurgeTime();
        //Back up a couple days. 
        //24 hour/day * 60 min/hour * 60 sec/min * 1000 msec/sec
        long purgeTime=System.currentTimeMillis()-days*24*60*60*1000;
        return lastModified<purgeTime;            
    }

    /** Creates a new TemporaryFile object to for a normal of the given
     *  file/location pair.  The location of the file is determined by the
     *  INCOMPLETE_DIRECTORY property.  The disk is not modified.
     *
     *  This method gives duplicate files the same temporary file.  That is, for
     *  all rfd_i and rfd_j
     *
     *       rfd_i~=rfd_j <==> getFile(rfd_i).equals(getFile(rfd_j))  
     * 
     *  Currently rfd_i~=rfd_j if rfd_i.getName().equals(rfd_j) &&
     *  rfd_i.getSize()==rfd_j.getSize().  In the future, this definition may be
     *  strengthened to depend on hash values.
     */
    public File getFile(RemoteFileDesc rfd) {
        return getFile(rfd.getFileName(), rfd.getSize());
    } 

    /** Same thing as getFile(rfd), where rfd.getFile().equals(name) 
     *  and rfd.getSize()==size. */ 
    public File getFile(String name, int size) {
		File incDir = null;
		try {
			incDir = SettingsManager.instance().getIncompleteDirectory();
		} catch(java.io.FileNotFoundException fnfe) {
			// this is fine, as this will just create a file in the current
			// working directory.
		}
        return new File(incDir,"T"+SEPARATOR+size+SEPARATOR+name);
    }


    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Returns an unmodifiable iterator of the complete blocks of the given
     * file, each as a half-open Interval.  If nothing is known of the file,
     * returns an empty iterator.  <b>Be sure to synchronize on this when using
     * the Iterator in a multithreaded environment.</b>
     *
     * @param incompleteFile a fully qualified temporary file, i.e., the result
     *  of getFile
     * @return an iterator of Interval.  For each interval I, bytes I.low to
     *  I.high-1 of incompleteFile have been written.  
     */
    public synchronized Iterator getBlocks(File incompleteFile) {
        //TODO3: return unmodifiable
        List intervals=(List)blocks.get(incompleteFile);
        if (intervals==null)
            return (new LinkedList()).iterator();
        else
            return intervals.iterator();
    }
    
    /** 
     * Returns the total amount of the given file on disk, i.e., the sum of all
     * the size of all intervals returned by getBlocks(incompleteFile).
     * 
     * @param incompleteFile a fully qualified temporary file, i.e., the result
     *  of getFile
     * @return the amount of the file downloaded
     */
    public synchronized int getBlockSize(File incompleteFile) {
        //If this is slow, it can be optimized by augmenting the data structure.
        int sum=0;
        for (Iterator iter=getBlocks(incompleteFile); iter.hasNext(); ) {
            Interval block=(Interval)iter.next();
            sum+=block.high-block.low;
        }
        return sum;
    }

    /**
     * Notes that bytes low to high-1 of incompleteFile has been has been
     * written to disk.  
     *
     * @param incompleteFile a fully qualified temporary file, i.e., the result
     *  of getFile
     * @param low the index of the first byte written
     * @param high the index of the last byte written plus one
     * @exception IllegalArgumentException if low>high
     */
    public synchronized void addBlock(File incompleteFile, int low, int high) 
            throws IllegalArgumentException {
        repOk();
        if (low>high)
            throw new IllegalArgumentException();
        if (low==high)
            return;  //empty interval!

        //Get list of blocks assocated with incompleteFile, creating new one if
        //necessary.
        List intervals=(List)blocks.get(incompleteFile);
        if (intervals==null) {
            intervals=new LinkedList();
            blocks.put(incompleteFile, intervals);
        }
 
        //Search for intervals bordering above and below.  This has some
        //similarities to IntSet.add, but I can't find any way of factoring
        //that.
        Interval lower=null;
        Interval higher=null;
        for (Iterator iter=intervals.iterator(); iter.hasNext(); ) {
            Interval interval=(Interval)iter.next();
            if (low<=interval.low && interval.high<=high) {//  <low-------high>
                iter.remove();                             //      interval
                continue;
            }

            if (low<=interval.high && interval.low<low)    //     <low, high>
                lower=interval;                            //  interval........

            if (interval.low<=high && interval.high>high)  //     <low, high>
                higher=interval;                           //  .........interval
        }

        //Add block.  Note that remove(..) is linear time.  That's not an issue
        //because there are typically few blocks.
        if (lower==null && higher==null) {
            //a) Doesn't overlap
            intervals.add(new Interval(low, high));
        } else if (lower!=null && higher!=null) {
            //b) Join two blocks
            intervals.remove(higher);
            intervals.remove(lower);
            intervals.add(new Interval(lower.low, higher.high));
        } else if (higher!=null) {
            //c) Join with higher
            intervals.remove(higher);
            intervals.add(new Interval(low, higher.high));
        } else if (lower!=null) {
            //d) Join with lower
            intervals.remove(lower);
            intervals.add(new Interval(lower.low, high));
        }   
        repOk();
    }

    /**
     * Returns the blocks in this that are not written.  This assumes that no
     * downloaders are currently writing to the file.  <b>Be sure to synchronize
     * on this when using the Iterator in a multithreaded environment.</b>
     * 
     * @param incompleteFile a fully qualified temporary file, i.e., the result
     *  of getFile
     * @param fileSize the size of the file.  (Needed in case 
     *  incompleteFile doesn't exist.)
     * @return an Iterator of Interval.  For each interval I, I.low to
     *  I.high-1 needs downloading
     */
    public synchronized Iterator getFreeBlocks(File incompleteFile,
                                               int fileSize) {
        List intervals=(List)blocks.get(incompleteFile);
        if (intervals==null || intervals.size()==0) {
            //Nothing recorded?
            Interval block=new Interval(0, fileSize);
            List buf=new LinkedList(); 
            buf.add(block);
            return buf.iterator();
        }
            
        //Sort list by low point in ascending order.  This has a side effect but
        //it doesn't matter.
        Collections.sort(intervals, new IntervalComparator());

        //Now step through list one element at a time, putting gaps into buf.
        //We take advantage of the fact that intervals are disjoint.  Treat
        //beginning specially.  
        //LOOP INVARIANT: interval!=null ==> low==interval.high
        List buf=new LinkedList();
        int low=0;
        Interval interval=null;
        for (Iterator iter=intervals.iterator(); iter.hasNext(); ) {
            interval=(Interval)iter.next();
            if (low<interval.low)      //needed for first interval
                buf.add(new Interval(low, interval.low));
            low=interval.high;
        }
        //Special case space between last block and end of file.
        Assert.that(interval!=null, "Null interval in getFreeBlocks");
        if (interval.high<fileSize)
            buf.add(new Interval(interval.high, fileSize));
        
        return buf.iterator();
    }

    private class IntervalComparator implements Comparator {
        public int compare(Object a, Object b) {
            Interval ia=(Interval)a;
            Interval ib=(Interval)b;
            return ia.low-ib.low;
        }
    }

    /** Tests all invariants.  Slow if actually enabled. */
    private void repOk() {
        /*
        //For each block list
        for (Iterator values=blocks.values().iterator(); values.hasNext(); ) {
            List blockList=(List)values.next();
            //Make sure blocks are disjoint (quadratic time)
            for (Iterator iter1=blockList.iterator(); iter1.hasNext(); ) {
                Interval a=(Interval)iter1.next();
                for (Iterator iter2=blockList.iterator(); iter2.hasNext(); ) {
                    Interval b=(Interval)iter2.next();
                    if (a!=b) {
                        Assert.that(! a.overlaps(b), 
                                    "Intervals "+a+" and "+b+" overlap!");
                        Assert.that(! a.adjacent(b), 
                                    "Intervals "+a+" and "+b+" are adjacent!");
                    }
                }
            }
        }
        */
    }

    public synchronized String toString() {
        StringBuffer buf=new StringBuffer();
        buf.append("{");
        boolean first=true;
        for (Iterator iter=blocks.keySet().iterator(); iter.hasNext(); ) {
            if (! first)
                buf.append(", ");

            File key=(File)iter.next();
            List intervals=(List)blocks.get(key);
            buf.append(key);
            buf.append(":");
            buf.append(intervals.toString());            

            first=false;
        }
        buf.append("}");
        return buf.toString();
    }

    /*
    public static void main(String args[]) {
        File file=new File("C:/tmp/test.txt");
        IncompleteFileManager ifm=new IncompleteFileManager();
        Iterator iter=null;

        //0 blocks
        Assert.that(! ifm.getBlocks(file).hasNext());
        Assert.that(ifm.getBlockSize(file)==0);
        //1 block
        ifm.addBlock(file, 0, 10);
        Assert.that(ifm.getBlockSize(file)==10);
        iter=ifm.getBlocks(file);
        Assert.that(iter.next().equals(new Interval(0, 10)));
        Assert.that(! iter.hasNext());
        //2 blocks.  Note that this is stricter than required by the
        //specification, which does not say anything about order.
        ifm.addBlock(file, 20, 30);
        Assert.that(ifm.getBlockSize(file)==20);
        iter=ifm.getBlocks(file);
        Assert.that(iter.next().equals(new Interval(0, 10)));
        Assert.that(iter.next().equals(new Interval(20, 30)));
        Assert.that(! iter.hasNext());
        //Merge from above.  Still two blocks.
        ifm.addBlock(file, 10, 12);
        Assert.that(ifm.getBlockSize(file)==22);
        iter=ifm.getBlocks(file);
        Assert.that(iter.next().equals(new Interval(20, 30)));
        Assert.that(iter.next().equals(new Interval(0, 12)));
        Assert.that(! iter.hasNext());
        System.out.println(ifm.toString());
        //Merge from below.  Still two blocks.
        ifm.addBlock(file, 18, 20);
        Assert.that(ifm.getBlockSize(file)==24);
        iter=ifm.getBlocks(file);
        Assert.that(iter.next().equals(new Interval(0, 12)));
        Assert.that(iter.next().equals(new Interval(18, 30)));
        Assert.that(! iter.hasNext());
        //Join outer blocks.  One block.
        ifm.addBlock(file, 12, 18);
        Assert.that(ifm.getBlockSize(file)==30);
        iter=ifm.getBlocks(file);
        Assert.that(iter.next().equals(new Interval(0, 30)));
        Assert.that(! iter.hasNext());

        ifm=new IncompleteFileManager();
        ifm.addBlock(file, 0, 2);
        ifm.addBlock(file, 5, 7);
        ifm.addBlock(file, 10, 12);
        ifm.addBlock(file, 15, 17);        
        //Merge, consuming middle blocks
        ifm.addBlock(file, 1, 17);
        Assert.that(ifm.getBlockSize(file)==17);
        iter=ifm.getBlocks(file);
        Assert.that(iter.next().equals(new Interval(0, 17)));
        Assert.that(! iter.hasNext());

        ifm=new IncompleteFileManager();
        ifm.addBlock(file, 0, 2);
        ifm.addBlock(file, 5, 7);
        ifm.addBlock(file, 10, 12);
        ifm.addBlock(file, 15, 17);        
        //Merge, consuming middle blocks
        ifm.addBlock(file, 0, 16);
        Assert.that(ifm.getBlockSize(file)==17);
        iter=ifm.getBlocks(file);
        Assert.that(iter.next().equals(new Interval(0, 17)));
        Assert.that(! iter.hasNext());

        File file2=new File("C:/tmp/another file.txt");
        ifm=new IncompleteFileManager();
        ifm.addBlock(file, 0, 2);
        ifm.addBlock(file2, 10, 13);
        Assert.that(ifm.getBlockSize(file)==2);
        Assert.that(ifm.getBlockSize(file2)==3);
        System.out.println(ifm.toString());

        ///////////////////// Test inverse block operations /////////////////
        
        //No blocks
        ifm=new IncompleteFileManager();
        iter=ifm.getFreeBlocks(file, 10);
        Assert.that(iter.next().equals(new Interval(0, 10)));
        Assert.that(! iter.hasNext());
        
        //Not overlapping ends
        ifm.addBlock(file, 2, 5);
        ifm.addBlock(file, 8, 10);
        iter=ifm.getFreeBlocks(file, 12);
        Assert.that(iter.next().equals(new Interval(0, 2)));
        Assert.that(iter.next().equals(new Interval(5, 8)));
        Assert.that(iter.next().equals(new Interval(10, 12)));
        Assert.that(! iter.hasNext());        

        //Overlapping ends
        ifm=new IncompleteFileManager();
        ifm.addBlock(file, 0, 2);
        ifm.addBlock(file, 8, 10);
        iter=ifm.getFreeBlocks(file, 10);
        Assert.that(iter.next().equals(new Interval(2, 8)));
        Assert.that(! iter.hasNext());   
    }
    */
}
