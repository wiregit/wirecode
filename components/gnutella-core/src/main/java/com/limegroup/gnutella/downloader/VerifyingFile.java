package com.limegroup.gnutella.downloader;

import com.sun.java.util.collections.*;
import java.io.*;
import com.limegroup.gnutella.util.*;

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

    private ManagedDownloader managedDownloader; 
    /**
     * The VerifyingFile uses an IntervalSet to keep track of the blocks written
     * to disk and find out which blocks to check before writing to disk
     */
    private IntervalSet writtenBlocks;
    
    public VerifyingFile(boolean checkOverlap) {
        this.checkOverlap = checkOverlap;
        writtenBlocks = new IntervalSet();
    }
    
    public void open(File file, ManagedDownloader md) throws IOException {
        this.managedDownloader = md;
        this.fos =  new RandomAccessFile(file,"rw");
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
                int j = findInitialPoint(overlapInterval,currPos, numBytes);
                for(int i=0;i<amountToCheck;i++,j++) {
                    if (buf[j]!=fileBuf[i]) //corrupt bytes
                        if(managedDownloader!=null)//md may be null for testing
                            managedDownloader.promptAboutCorruptDownload();
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
  
    public void close() {
        try { 
            fos.close();
        } catch (IOException ioe) {}
    }
    
    /////////////////////////private helpers//////////////////////////////
    
    /*
     * @return the number of bytes from low where the two intervals,
     * begin to overlap.
     */
    private int findInitialPoint(Interval interval, long low, long numBytes) {
        //Remember this is the overlap interval
        if(interval.low <= low)//equivalent to interval.low == low
            return 0;
        else //interval.low > low
            return interval.low - (int)low;
            
    }
}
