package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.tigertree.HashTree;
import com.limegroup.gnutella.util.FileUtils;
import com.limegroup.gnutella.util.IntervalSet;
import com.limegroup.gnutella.util.ProcessingQueue;


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
     * The thread that does the actual verification
     */
    private static final ProcessingQueue CHUNK_VERIFIER = new ProcessingQueue("chunk verifier");
    
    /**
     * If the number of corrupted data gets over this, assume the file will not be recovered
     */
    private static final float MAX_CORRUPTION = 0.1f;
    
    /**
     * The file we're writing to / reading from.
     * LOCKING: itself. this->fos is ok
     */
    private RandomAccessFile fos;

    /**
     * The ManagedDownloader this is working for.
     */
    private ManagedDownloader managedDownloader; 
    
    /**
     * The eventual completed size of the file we're writing.
     */
    private final int completedSize;
	
	/**
	 * How much data did we lose due to corruption
	 */
	private int lostSize;
    
    /**
     * The VerifyingFile uses an IntervalSet to keep track of the blocks written
     * to disk and find out which blocks to check before writing to disk
     */
    private final IntervalSet verifiedBlocks;
    
    /**
     * Ranges that are currently being written by the ManagedDownloader. 
     * 
     * Replaces the IntervalSet of needed ranges previously stored in the 
     * ManagedDownloader but which could get out of sync with the verifiedBlocks
     * IntervalSet and is therefore replaced by a more failsafe implementation.
     */
    private IntervalSet leasedBlocks;
    
    /**
     * Ranges that are currently written to disk, but do not form complete chunks
     * so cannot be verified by the HashTree.
     */
    private IntervalSet partialBlocks;
    
    /**
     * Ranges which are written and pending verification.
     */
    private IntervalSet pendingBlocks;
    
    /**
     * Constructs a new VerifyingFile, without a given completion size.
     *
     * Useful for tests.
     */
    public VerifyingFile() {
        this(-1);
    }
    
    /**
     * Constructs a new VerifyingFile for the specified size.
     * If checkOverlap is true, will scan for overlap corruption.
     */
    public VerifyingFile(int completedSize) {
        this.completedSize = completedSize;
        verifiedBlocks = new IntervalSet();
        leasedBlocks = new IntervalSet();
        pendingBlocks = new IntervalSet();
        partialBlocks = new IntervalSet();
    }
    
    /**
     * Opens this VerifyingFile for writing.
     * MUST be called before anything else.
     *
     * If there is no completion size, this fails.
     */
    public void open(File file, ManagedDownloader md) throws IOException {
        if(completedSize == -1)
            throw new IllegalStateException("cannot open for unknown size.");
        
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
     * used to add blocks direcly. Blocks added this way are marked
     * partial.
     */
    public synchronized void addInterval(Interval interval) {
        //delegates to underlying IntervalSet
        partialBlocks.add(interval);
    }

    /**
     * Writes bytes to the underlying file.
     */
    public synchronized void writeBlock(long currPos,  byte[] buf)
                                                    throws DiskException{
        
        if (LOG.isDebugEnabled())
            LOG.debug(" trying to write block at offset "+currPos+" with size "+buf.length);
        
        if(buf.length==0) //nothing to write? return
            return;
        if(fos == null)
            throw new DiskException("no file?");
		
		Interval intvl = new Interval((int)currPos,(int)currPos+buf.length-1);
		
		/// some stuff to help debugging ///
		if (!leasedBlocks.contains(intvl)) {
			LOG.error("trying to write an interval that wasn't leased "+dumpState(), 
					new Exception());
			System.exit(1);
		}
		
		if (verifiedBlocks.contains(intvl) || 
				partialBlocks.contains(intvl) ||
				pendingBlocks.contains(intvl)) {
			LOG.error("trying to write an interval that was already written"+dumpState(), 
					new Exception());
			System.exit(1);
		}
		
		////////////
		
        saveToDisk(currPos,buf);
		
        // 4. if write went ok, add this interval to the partial blocks
        if (LOG.isDebugEnabled())
            LOG.debug("adding chunk "+intvl+" to partialBlocks");
        leasedBlocks.delete(intvl);
        partialBlocks.add(intvl);

		//5. verify chunks
        checkVerifyableChunks();
    }

	/**
	 * Saves the given interval to disk. 
	 */
	private void saveToDisk(long currPos, byte [] buf) 
	throws DiskException{
		try {
            //2. get the fp back to the position we want to write to.
			synchronized(fos) {
				fos.seek(currPos);
				//3. Write to disk.
				fos.write(buf, 0, buf.length);
			}
        }catch(IOException diskIO) {
            throw new DiskException(diskIO);
        }
	}
    
	/**
	 * Schedules those chunks that can be verified against the hash tree
	 * for verification.
	 */
	private void checkVerifyableChunks() {
        // if we have a tree, see if there is a completed chunk in the partial list
		HashTree tree = managedDownloader.getHashTree(); 
		if (tree != null) {
			for (Iterator iter = findVerifyableBlocks().iterator();iter.hasNext();)  {
				Interval i = (Interval) iter.next();
				partialBlocks.delete(i);
				pendingBlocks.add(i);
				if (LOG.isDebugEnabled())
					LOG.debug("will schedule for verification "+i);
				CHUNK_VERIFIER.add(new ChunkVerifier(i,tree));
			}
		} else
			// if we couldn't find a tree during the entire download, 
			// we have to bite the bullet and rely on SHA1 alone
			if (partialBlocks.getSize() == completedSize) {
				Interval all = partialBlocks.removeFirst();
				verifiedBlocks.add(all);
			}
	}
	
    /**
     * iterates through the pending blocks and checks if the recent write has created
     * some (verifiable) full chunks.  Its not possible to verify more than two chunks
     * per method call unless the downloader is being deserialized from disk
     */
    private List findVerifyableBlocks() {
        if (LOG.isDebugEnabled())
            LOG.debug("trying to find verifyable blocks out of "+partialBlocks);
        
        List verifyable = new ArrayList(2);
        List partial = partialBlocks.getAllIntervalsAsList();
        int chunkSize = managedDownloader.getChunkSize();
        int lastChunkOffset = completedSize - (completedSize % chunkSize);
        
        for (int i = 0; i < partial.size() ; i++) {
            Interval current = (Interval)partial.get(i);
            
            // find the beginning of the first chunk offset
            int lowChunkOffset = current.low - current.low % chunkSize;
            if (current.low % chunkSize != 0)
                lowChunkOffset += chunkSize;
            while (current.high >= lowChunkOffset+chunkSize-1) {
                Interval complete = new Interval(lowChunkOffset, lowChunkOffset+chunkSize -1); 
                verifyable.add(complete);
                lowChunkOffset += chunkSize;
            }
        }
        
        // special case for the last chunk
        if (partial.size() > verifyable.size()) {
            Interval last = (Interval) partial.get(partial.size() - 1);
            if (last.high == completedSize-1 && last.low <= lastChunkOffset ) {
                if(LOG.isDebugEnabled())
                    LOG.debug("adding the last chunk for verification");
                
                verifyable.add(new Interval(lastChunkOffset, last.high));
            }
        }
        
        return verifyable;
    }
    
    public String dumpState() {
        return "verified:"+verifiedBlocks+"\npartial:"+partialBlocks+
        	"\npending:"+pendingBlocks+"\nleased:"+leasedBlocks;
    }
    
    /**
     * Returns the first full block of data that needs to be written.
     */
    public synchronized Interval leaseWhite() throws NoSuchElementException {
        if (LOG.isDebugEnabled())
            LOG.debug("leasing white, state: "+dumpState());
        IntervalSet freeBlocks = verifiedBlocks.invert(completedSize);
        freeBlocks.delete(leasedBlocks);
        freeBlocks.delete(partialBlocks);
        freeBlocks.delete(pendingBlocks);
        Interval ret = freeBlocks.removeFirst();
        if (LOG.isDebugEnabled())
            LOG.debug(" freeblocks: "+freeBlocks+" selected "+ret);
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
        return allignInterval(temp, chunkSize);
    }
    
    /**
     * Returns the first block of data that needs to be written
     * and is within the specified set of ranges.
     */
    public synchronized Interval leaseWhite(IntervalSet ranges)
      throws NoSuchElementException {
        ranges.delete(verifiedBlocks);
        ranges.delete(leasedBlocks);
        ranges.delete(partialBlocks);
        ranges.delete(pendingBlocks);
        Interval ret = ranges.removeFirst();
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
        return allignInterval(temp, chunkSize);
    }

    /**
     * Removes the specified internal from the set of leased intervals.
     */
    public synchronized void releaseBlock(Interval in) {
        if(LOG.isDebugEnabled())
            LOG.debug("Releasing interval: " + in+" state "+dumpState());
        leasedBlocks.delete(in);
    }
	
    /**
     * Returns all downloaded blocks with an Iterator.
     */
    public synchronized Iterator getBlocks() {
        return getBlocksAsList().iterator();
    }
    
    /**
     * Returns all verified blocks with an Iterator.
     */
    public synchronized Iterator getVerifiedBlocks() {
        return verifiedBlocks.getAllIntervals();
    }
    
    public synchronized byte [] toBytes() {
    	return verifiedBlocks.toBytes();
    }

    /**
     * @return all downloaded blocks as list
     */
    public synchronized List getBlocksAsList() {
        List l = new ArrayList();
        l.addAll(verifiedBlocks.getAllIntervalsAsList());
        l.addAll(partialBlocks.getAllIntervalsAsList());
        l.addAll(pendingBlocks.getAllIntervalsAsList());
        IntervalSet ret = new IntervalSet();
        for (Iterator iter = l.iterator();iter.hasNext();)
            ret.add((Interval)iter.next());
        return ret.getAllIntervalsAsList();
    }
    
    /**
     * Returns all verified blocks as a List.
     */ 
    public synchronized List getVerifiedBlocksAsList() {
        return verifiedBlocks.getAllIntervalsAsList();
    }

    /**
     * Returns the total number of bytes written to disk.
     */
    public synchronized int getBlockSize() {
        return verifiedBlocks.getSize() +
        	partialBlocks.getSize() +
        	pendingBlocks.getSize();
    }
    
    /**
     * Returns the total number of verified bytes written to disk.
     */
    public synchronized int getVerifiedBlockSize() {
        return verifiedBlocks.getSize();
    }
  
	/**
	 * @return how much data was lost due to corruption
	 */
	public synchronized int getAmountLost() {
		return lostSize;
	}
	
    /**
     * Determines if all blocks have been written to disk and verified
     */
    public synchronized boolean isComplete() {
        return (verifiedBlocks.getSize() == completedSize);
    }
    
    /**
     * @return whether we think we will not be able to complete this file
     */
    public synchronized boolean isHopeless() {
        return lostSize >= MAX_CORRUPTION * completedSize;
    }
    
    /**
     * if the file has become too corrupt prompt the user to cancel it
     */
    public void promptIfHopeless() {
        if (isHopeless())
            managedDownloader.promptAboutCorruptDownload();
    }
    
    /**
     * Determines if there are any blocks that are not assigned
     * or written.
     */
    public synchronized int hasFreeBlocksToAssign() {
        return  completedSize - (verifiedBlocks.getSize() + 
                leasedBlocks.getSize() +
                partialBlocks.getSize() +
                pendingBlocks.getSize()); 
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
     * Returns the ManagedDownloader this VerifyingFile is associated with.
     * If this VerifyingFile is closed, the return value will be null.
     */
    public ManagedDownloader getManagedDownloader() {
        return managedDownloader;
    }
    
    /////////////////////////private helpers//////////////////////////////
    
    /**
     * Fits an interval inside a chunk.  This ensures that the interval is never larger
     * than chunksize and finishes at exact chunk offset.
     */
    private synchronized Interval allignInterval(Interval temp, int chunkSize) {
        if (LOG.isDebugEnabled())
            LOG.debug("alligning "+temp +" with chunk size "+chunkSize+"\n"+dumpState());
        
        Interval interval;
        
        int intervalSize = temp.high - temp.low+1;
        
        // find where the next chunk starts
        int chunkStart = ( 1 + temp.low / chunkSize ) * chunkSize ;
        
        // if we're already covering an exact chunk, return 
        if (chunkStart == temp.high+1 && intervalSize == chunkSize) {
            LOG.debug("already at exact chunk");
            return temp;
        }
        
        // try to map the area until the chunk border
        if (chunkStart < temp.high) {
            interval = new Interval(temp.low, chunkStart-1);
            temp = new Interval(chunkStart,temp.high);
            releaseBlock(temp);
        } else
            interval = temp;

        if (LOG.isDebugEnabled())
            LOG.debug("aligned to interval: "+interval+" state is: "+dumpState());
        
        return interval;
    }

    /**
     * Leases the specified interval.
     */
    private synchronized void leaseBlock(Interval in) {
        //if(LOG.isDebugEnabled())
            //LOG.debug("Obtaining interval: " + in);
        leasedBlocks.add(in);
    }
    
    /**
     * a Runnable that verifies chunks without locking the VerifyingFile during
     * intensive cpu or i/o operations. After verification, the completed regions 
     * are added either as black or white.
     */
    private class ChunkVerifier implements Runnable {
        private final Interval _interval;
        private final HashTree _tree;
        public ChunkVerifier(Interval i, HashTree tree) {
            _interval = i;
            _tree = tree;
        }
        public void run() {
            // heavy i/o here
            boolean good = verifyChunk(_interval,_tree);
            
            synchronized(VerifyingFile.this) {
                pendingBlocks.delete(_interval);
                if (good) 
                    verifiedBlocks.add(_interval);
                else
					lostSize += (_interval.high - _interval.low + 1);
            }
        }
        
        /**
         * @return whether this chunk is corrupt according to the downloader's hash tree
         */
        private boolean verifyChunk(Interval i, HashTree tree) {
            if (LOG.isDebugEnabled())
                LOG.debug("verifying interval "+i);
            
            byte []b = new byte[i.high - i.low+1];
            // read the interval from the file
            long pos = -1;
            try {
				synchronized(fos) {
					fos.seek(i.low);
					fos.read(b);
				}
            }catch (IOException bad) {
                // we failed reading back from the file - assume block is corrupt
                // and it will have to be re-downloaded
                return false;
            }
            
            boolean corrupt = tree.isCorrupt(i,b);
            
            if (LOG.isDebugEnabled() && corrupt)
                LOG.debug("block corrupt!");
            
            return !corrupt;
        }
    }
}
