package com.limegroup.gnutella.downloader;

import com.sun.java.util.collections.*;
import java.io.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.tigertree.HashTree;

/**
 * All the HTTPDownloaders associated with a ManagedDownloader will commit
 * the parts of the file they are downloading through a single object of this 
 * class. 
 * <p> 
 * Keeps track of which bytes have already been written to disk,
 * and based on this information makes a decision about whether or not to do
 * checking. 
 * <p>
 * Users of this class must call open(...) before calling writeBlock.
 * <p>
 * @author Sumeet Thadani, Chris Rohrs
 */
public class VerifyingFile {
    
    private RandomAccessFile fos;

    private boolean checkOverlap;
    
    private volatile boolean isCorrupted;

    private ManagedDownloader managedDownloader; 
    
    private int completedSize;
    
    /**
     * The VerifyingFile uses an IntervalSet to keep track of the blocks written
     * to disk and find out which blocks to check before writing to disk
     */
    private IntervalSet writtenBlocks;
    
    /**
     * Ranges that are currently being written by the ManagedDownloader. 
     * 
     * Replaces the IntervalSet of needed ranges previously stored in the 
     * ManagedDownloader but which could get out of sync with the writtenBlocks
     * IntervalSet and is therefore replaced by a more failsafe implementation.
     */
    private IntervalSet leasedBlocks;
    
    public VerifyingFile(boolean checkOverlap, int completedSize) {
        this.completedSize = completedSize;
        this.checkOverlap = checkOverlap;
        writtenBlocks = new IntervalSet();
        leasedBlocks = new IntervalSet();
    }
    
    public void open(File file, ManagedDownloader md) throws IOException {
        this.managedDownloader = md;
        // Ensure that the directory this file is in exists & is writeable.
        File parentFile = FileUtils.getParentFile(file);
        if( parentFile != null ) {
            parentFile.mkdirs();
            FileUtils.setWriteable(parentFile);
        }
        FileUtils.setWriteable(file);
        this.fos =  new RandomAccessFile(file,"rw");
        // cleanup leased blocks
        leasedBlocks = new IntervalSet();
    }

    /**
     * used to add blocks direcly. WARNING: the intervals added using this 
     * method are not checked for overlaps, incorrect use of this method, may 
     * break integrity constrains.
     */
    public synchronized void addInterval(Interval interval) {
        //delegates to underlying IntervalSet
        writtenBlocks.add(interval);
    }

    public synchronized void writeBlock(long currPos, int numBytes, byte[] buf)
                                                    throws IOException {
        if(numBytes==0) //nothing to write? return
            return;
        if(fos == null)
            throw new IOException();
        boolean checkBeforeWrite = false;
        List overlapBlocks = null;
        Interval intvl= null;
        if(checkOverlap) {
            intvl =new Interval((int)currPos,(int)currPos+numBytes-1);
            overlapBlocks = writtenBlocks.getOverlapIntervals(intvl);
            if(overlapBlocks.size()>0)
                checkBeforeWrite = true;
        }
        //OK now we know whether or not to check before writing to disk.
        //1. If there are overlaps, check those parts of the buffer we are about
        // to write.
        if(checkBeforeWrite) { //we found overlaps
            for(Iterator iter = overlapBlocks.iterator(); iter.hasNext();) {
                Interval overlapInterval = (Interval)iter.next();
                int amountToCheck=(overlapInterval.high-overlapInterval.low)+1;
                byte[] fileBuf = new byte[amountToCheck];
                fos.seek(overlapInterval.low);//seek to begining of overlap part
                fos.readFully(fileBuf,0,amountToCheck);
                int j = findInitialPoint(overlapInterval,currPos);
                for(int i=0;i<amountToCheck;i++,j++) {
                    if (buf[j]!=fileBuf[i]) { //corrupt bytes
                        isCorrupted = true; // flag as corrupted.
                        if(managedDownloader!=null)//md may be null for testing
                            managedDownloader.promptAboutCorruptDownload();
                    }
                }
            }
        }
        //Got this far? Either no need to check, or it checks out OK. 
        //2. get the fp back to the position we want to write to.
        fos.seek(currPos);
        //3. Write to disk.
        fos.write(buf, 0, numBytes);
        //4. add this interval
        if(intvl==null)
            writtenBlocks.
            add(new Interval((int)currPos, (int)currPos+numBytes-1));
        else 
            writtenBlocks.add(intvl);
    }

    public synchronized Interval leaseWhite() {
        IntervalSet freeBlocks = writtenBlocks.invert(completedSize);
        freeBlocks.delete(leasedBlocks);
        Interval ret = freeBlocks.removeFirst();
        leaseBlock(ret);
        return ret;
    }
    
    public synchronized Interval leaseWhite(int chunkSize) {
        Interval temp = leaseWhite();
        if (temp == null)
            return temp;
        return fixIntervalForChunk(temp, chunkSize);
    }
    
    public synchronized Interval leaseWhite(IntervalSet ranges) {
        ranges.delete(writtenBlocks);
        ranges.delete(leasedBlocks);
        Interval temp = ranges.removeFirst();
        
        // damn you, overlap checking!
        while (temp != null 
               && temp.high - temp.low <= ManagedDownloader.OVERLAP_BYTES)
            temp = ranges.removeFirst();
        
        if (temp == null)
            return null;
        
        Interval ret = new Interval(temp.low+ManagedDownloader.OVERLAP_BYTES, 
            temp.high);
        leaseBlock(ret);
        return ret;
    }
    
    public synchronized Interval leaseWhite(IntervalSet ranges, int chunkSize) {
        Interval temp = leaseWhite(ranges);
        if (temp == null)
            return temp;
        return fixIntervalForChunk(temp, chunkSize);
    }

    public synchronized void releaseBlock(Interval in) {
        if (in != null)
            leasedBlocks.delete(in);
    }
    
    public synchronized Iterator getBlocks() {
        return writtenBlocks.getAllIntervals();
    }

    public synchronized List getBlocksAsList() {
        return writtenBlocks.getAllIntervalsAsList();
    }

    public synchronized Iterator getFreeBlocks(int maxSize) {
        return writtenBlocks.getNeededIntervals(maxSize);
    }

    public synchronized int getBlockSize() {
        return writtenBlocks.getSize();
    }
  
    public synchronized boolean isComplete() {
        return (writtenBlocks.getSize() == completedSize);
    }
    
    public synchronized boolean hasFreeBlocksToAssign() {
        return ( writtenBlocks.getSize() + leasedBlocks.getSize() < completedSize); 
    }
    
    synchronized void deleteCorruptedBlocks (HashTree tree, File file) throws IOException {
        InputStream is = new FileInputStream(file);
        try {
            List corruptRanges = tree.getCorruptRanges(is);
            for (Iterator iter = corruptRanges.iterator(); iter.hasNext(); )
                writtenBlocks.delete((Interval)iter.next());
            isCorrupted = false;
        } finally {
            is.close();
        }
    }
        
    /**
     * Closes the file output stream.
     */
    public void close() {
        // This does not clear the ManagedDownloader because
        // it could still be in a waiting state, and we need
        // it to allow IncompleteFileDescs to funnel alt-locs
        // as sources to the downloader.
        if(fos==null)
            return;
        try { 
            fos.close();
        } catch (IOException ioe) {}
    }
    
    /**
     * Clears the ManagedDownloader variable, allowing it to be GC'ed.
     */
    public void clearManagedDownloader() {
        managedDownloader = null;
    }   
    
    /**
     * Returns whether or not we have determined if the written is corrupted.
     */
    public boolean isCorrupted() {
        return isCorrupted;
    }
    
    /**
     * Returns the ManagedDownloader this VerifyingFile is associated with.
     * If this VerifyingFile is closed, the return value will be null.
     */
    public ManagedDownloader getManagedDownloader() {
        return managedDownloader;
    }
    
    /////////////////////////private helpers//////////////////////////////
    
    /*
     * @return the number of bytes from low where the two intervals,
     * begin to overlap.
     */
    private int findInitialPoint(Interval interval, long low) {
        //Remember this is the overlap interval
        if(interval.low <= low)//equivalent to interval.low == low
            return 0;
        else //interval.low > low
            return interval.low - (int)low;
            
    }
    
    /**
     * Returns a new chunk that is less than chunkSize.
     * This reduces the high value of the interval.
     * removes what was cut from leasedBlocks.
     *
     * @return a new (smaller) interval up to chunkSize.
     */
    private synchronized Interval fixIntervalForChunk(Interval temp, int chunkSize) {
        Interval interval;
        if((temp.high-temp.low+1) > chunkSize) {
            int max = temp.low+chunkSize-1;
            interval = new Interval(temp.low, max);
            temp = new Interval(max+1,temp.high);
            releaseBlock(temp);
        } else { //temp's size <= chunkSize
            interval = temp;
}
        return interval;
    }

    private synchronized void leaseBlock(Interval in) {
        if (in != null)
            leasedBlocks.add(in);
    }
}
