package com.limegroup.gnutella.downloader;

import com.sun.java.util.collections.*;
import java.io.*;
/**
 * All the HTTPDownloaders associated with a ManagedDownloader will commit
 * the parts of the file they are downloading through a single object of this 
 * class. 
 * <p> 
 * A VerifyingFile keeps track of which bytes have already been written to disk,
 * and based on this information makes a decision about whether or not to do
 * checking. 
 * @author Sumeet Thadani, Chris Rohrs
 */
public class VerifyingFile {
    
    private RandomAccessFile fos;
    /**The list of intervals we have written to disk already*/
    private /* of intervals*/ List writtenBlocks;

    private boolean checkOverlap;

    private List /*of Intervals */ overlapBlocks;

    private ManagedDownloader managedDownloader; 
    
    public VerifyingFile(RandomAccessFile file, boolean checkOverlap,
                             ManagedDownloader md) {
        this.writtenBlocks = new ArrayList();
        this.fos =  file;
        this.checkOverlap = checkOverlap;
        this.managedDownloader = md;
    }

    public synchronized void addInterval(Interval interval) {
        //TODO1: Do we need to check for overlaps here?
        writtenBlocks.add(interval);
    }

    /**
     * @return true if a corruption was detected at write time.
     */
    public synchronized void writeBlock(long currPos, 
                                           int numBytes,
                                           byte[] buf) throws IOException {
        boolean checkBeforeWrite = false;
        if(checkOverlap)
            if(checkAndRememberOverlaps(currPos, numBytes))//causes side effect
                checkBeforeWrite = true;
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
//          FileDescriptor fd = fos.getFD();
//          fd.sync();
        //4. add this interval
        addInterval(new Interval((int)currPos, (int)currPos+numBytes-1));
        //5. cleanup and
        //TODO1: Should this be in fianlly?
        cleanup();
    }

    /**
     * @return true if there is an overlap between the intervals written to 
     * disk already, and the interval we are about to write now. This current
     * interval begins at low and is numbytes long.
     * <p>
     * If there are overlaps, this method makes a note of the overlap regions
     * in a global datastructure - overlapBlocks
     */
    private boolean checkAndRememberOverlaps(long low, int numBytes) {
        boolean retValue = false; //assume no overlaps.
        long high = low+numBytes -1;
        if (low >= high)
            return false;

        overlapBlocks = new ArrayList(); //initialize for this write

        for(Iterator iter = writtenBlocks.iterator(); iter.hasNext(); ) {
            Interval interval = (Interval)iter.next();
            //case a:
            if(low <= interval.low && interval.high <= high) {
                //Need to check the whole iterval, starting point=interval.low
                overlapBlocks.add(interval);
                retValue = true;
                continue;
            }
            //case b:
            if(low<=interval.high && interval.low < low) {
                overlapBlocks.add(new Interval((int)low,
                                           Math.min((int)high,interval.high)));
                retValue = true;
            }
            //case c:
            if(interval.low <= high && interval.high > high) {
                overlapBlocks.add(new Interval(Math.max(interval.low,(int)low),
                                               (int)high));
                retValue = true;
            }
            //Note: There is one condition under which case b and c are both
            //true. In this case the same interval will be added twice. The
            //effect of this is that we will check the same overlap interval 
            //2 times. We are still doing it this way, beacuse this conditon
            //will not happen in practice, and the code looks better this way, 
            //and finally, it cannot do any harm - the worst that can happen is
            //that we check the exact same interval twice.
        }
        return retValue;
    }
    
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
        
    private void cleanup() {
        overlapBlocks = null;
    }

    public void close() {
        try { 
            fos.close();
        } catch (IOException ioe) {}
    }
}
