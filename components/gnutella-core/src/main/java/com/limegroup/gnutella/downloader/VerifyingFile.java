package com.limegroup.gnutella.downloader;

import com.sun.java.util.collections.*;
import java.io.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.tigertree.HashTree;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

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
    
    private static final Log LOG = LogFactory.getLog(VerifyingFile.class);
    
    /**
     * The file we're writing to / reading from.
     */
    private RandomAccessFile fos;

    /**
     * Whether or not we're doing overlap checking.
     */
    private final boolean checkOverlap;
    
    /**
     * Whether or not we've detected corruption in the file.
     */
    private volatile boolean isCorrupted;

    /**
     * The ManagedDownloader this is working for.
     */
    private ManagedDownloader managedDownloader; 
    
    /**
     * The eventual completed size of the file we're writing.
     */
    private final int completedSize;
    
    /**
     * The VerifyingFile uses an IntervalSet to keep track of the blocks written
     * to disk and find out which blocks to check before writing to disk
     */
    private final IntervalSet writtenBlocks;
    
    /**
     * Ranges that are currently being written by the ManagedDownloader. 
     * 
     * Replaces the IntervalSet of needed ranges previously stored in the 
     * ManagedDownloader but which could get out of sync with the writtenBlocks
     * IntervalSet and is therefore replaced by a more failsafe implementation.
     */
    private IntervalSet leasedBlocks;
    
    /**
     * Constructs a new VerifyingFile for the specified size.
     * If checkOverlap is true, will scan for overlap corruption.
     */
    public VerifyingFile(boolean checkOverlap, int completedSize) {
        this.completedSize = completedSize;
        this.checkOverlap = checkOverlap;
        writtenBlocks = new IntervalSet();
        leasedBlocks = new IntervalSet();
    }
    
    /**
     * Opens this VerifyingFile for writing.
     * MUST be called before anything else.
     */
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
        releaseBlock(interval);
    }

    /**
     * Writes bytes to the underlying file.
     */
    public synchronized void writeBlock(long currPos, int numBytes, byte[] buf)
                                                    throws IOException {
        if(numBytes==0) //nothing to write? return
            return;
        if(fos == null)
            throw new IOException();
        boolean checkBeforeWrite = false;
        List overlapBlocks = null;
        Interval intvl = new Interval((int)currPos,(int)currPos+numBytes-1);
        if(checkOverlap) {
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
        writtenBlocks.add(intvl);
        releaseBlock(intvl);
    }

    /**
     * Returns the first full block of data that needs to be written.
     */
    public synchronized Interval leaseWhite() throws NoSuchElementException {
        IntervalSet freeBlocks = writtenBlocks.invert(completedSize);
        freeBlocks.delete(leasedBlocks);
        Interval ret = freeBlocks.removeFirst();
        leaseBlock(ret);
        return ret;
    }
    
    /**
     * Returns the first block of data that needs to be written.
     * The returned block will NEVER be larger than chunkSize.
     */
    public synchronized Interval leaseWhite(int chunkSize) 
      throws NoSuchElementException {
        Interval temp = leaseWhite();
        return fixIntervalForChunk(temp, chunkSize);
    }
    
    /**
     * Returns the first block of data that needs to be written
     * and is within the specified set of ranges.
     */
    public synchronized Interval leaseWhite(IntervalSet ranges)
      throws NoSuchElementException {
        ranges.delete(writtenBlocks);
        ranges.delete(leasedBlocks);
        Interval temp = ranges.removeFirst();
        
        // damn you, overlap checking!
        while (temp != null 
               && temp.high - temp.low <= ManagedDownloader.OVERLAP_BYTES)
            temp = ranges.removeFirst();
        
        Interval ret =
            new Interval(temp.low+ManagedDownloader.OVERLAP_BYTES, temp.high);

        leaseBlock(ret);
        return ret;
    }
    
    /**
     * Returns the first block of data that needs to be written
     * and is within the specified set of ranges.
     * The returned block will NEVER be larger than chunkSize.
     */
    public synchronized Interval leaseWhite(IntervalSet ranges, int chunkSize)
      throws NoSuchElementException {
        Interval temp = leaseWhite(ranges);
        return fixIntervalForChunk(temp, chunkSize);
    }

    /**
     * Removes the specified internal from the set of leased intervals.
     */
    public synchronized void releaseBlock(Interval in) {
        if(LOG.isDebugEnabled())
            LOG.debug("Releasing interval: " + in);
        leasedBlocks.delete(in);
    }
    
    /**
     * Returns all written blocks with an Iterator.
     */
    public synchronized Iterator getBlocks() {
        return writtenBlocks.getAllIntervals();
    }

    /**
     * Returns all written blocks as a List.
     */ 
    public synchronized List getBlocksAsList() {
        return writtenBlocks.getAllIntervalsAsList();
    }

    /**
     * Returns all blocks that are free assuming the specified
     * maximum size, as an Iterator.
     */
    public synchronized Iterator getFreeBlocks(int maxSize) {
        return writtenBlocks.getNeededIntervals(maxSize);
    }

    /**
     * Returns the total number of bytes written to disk.
     */
    public synchronized int getBlockSize() {
        return writtenBlocks.getSize();
    }
  
    /**
     * Determines if all blocks have been written to disk.
     */
    public synchronized boolean isComplete() {
        return (writtenBlocks.getSize() == completedSize);
    }
    
    /**
     * Determines if there are any blocks that are not assigned
     * or written.
     */
    public synchronized boolean hasFreeBlocksToAssign() {
        return ( writtenBlocks.getSize() + leasedBlocks.getSize() < completedSize); 
    }
    
    /**
     * Deletes any blocks that were corrupt.
     *
     * Returns the number of blocks deleted.
     */
    synchronized int deleteCorruptedBlocks (HashTree tree, File file)
      throws IOException {
        InputStream is = null;
        int deleted = 0;
        try {
            is = new BufferedInputStream(new FileInputStream(file));
            List corruptRanges = tree.getCorruptRanges(is);
            for (Iterator iter = corruptRanges.iterator(); iter.hasNext(); ) {
                deleted++;
                writtenBlocks.delete((Interval)iter.next());
            }
            isCorrupted = false;
        } finally {
            if(is != null) {
                try {
                    is.close();
                } catch(IOException ignored) {}
            }
        }
        return deleted;
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

    /**
     * Leases the specified interval.
     */
    private synchronized void leaseBlock(Interval in) {
        if(LOG.isDebugEnabled())
            LOG.debug("Obtaining interval: " + in);
        leasedBlocks.add(in);
    }
}
