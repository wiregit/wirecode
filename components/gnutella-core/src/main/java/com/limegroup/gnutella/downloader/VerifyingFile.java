package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.tigertree.HashTree;
import com.limegroup.gnutella.util.FileUtils;
import com.limegroup.gnutella.util.IntervalSet;
import com.limegroup.gnutella.util.ProcessingQueue;

/**
 * A control point for all access to the file being downloaded to, also does 
 * on-the-fly verification.
 * 
 * Every region of the file can be in one of five states, and can move from one
 * state to another only in the following order:
 * 
 *   1. available for download 
 *   2. currently being downloaded 
 *   3. waiting to be written.
 *   4. written (and immediately into, if possible..)
 *   5. verified, or if it doesn't verify back to
 *   1. available for download   
 *   
 * In order to maintain these constraints, the only possible operations are:
 *   Lease a block - find an area which is available for download and claim it
 *   Write a block - report that the specified block has been read from the network.
 *   Release a block - report that the specified block will not be downloaded.
 */
public class VerifyingFile {
    
    private static final Log LOG = LogFactory.getLog(VerifyingFile.class);
    
    /**
     * The thread that does the actual verification & writing
     */
    private static final ProcessingQueue QUEUE = new ProcessingQueue("BlockingVF", 
            true, // managed 
            Thread.NORM_PRIORITY+1); // a little higher priority than normal
    
    /**
     * Do not queue up more than this many chunks otherwise the queue grows unbounded
     */
    private static final int MAX_CACHED_BUFFERS = 512; // = half meg
    
    /**
     * If the number of corrupted data gets over this, assume the file will not be recovered
     */
    static final float MAX_CORRUPTION = 0.9f;
    
    /** The default chunk size - if we don't have a tree we request chunks this big.
     * 
     *  This is a power of two in order to minimize the number of small partial chunk
     *  downloads that will be required after we learn the chunk size from the TigerTree,
     *  since the chunk size will always be a power of two.
     */
    /* package */ static final int DEFAULT_CHUNK_SIZE = 131072; //128 KB = 128 * 1024 B = 131072 bytes
    
    /** a bunch of cached byte[]s for partial chunks */
    private static final Stack CACHE = new Stack();
    static {
        CACHE.ensureCapacity(MAX_CACHED_BUFFERS);
        RouterService.schedule(new CacheCleaner(),10 * 60 * 1000, 10 * 60 * 1000);
    }
    
    /** 
     * how many buffers were created
     * LOCKING: hold CACHE 
     */
    private static int numCreated;
    
    /** a bunch of cached byte[]s for verifyable chunks */
    private static final Map CHUNK_CACHE = new HashMap(20);
    
    /**
     * The file we're writing to / reading from.
     */
    private volatile RandomAccessFile fos;
    
    /**
     * Whether this file is open for writing
     */
    private volatile boolean isOpen;

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
     * Ranges that are discarded (but verification was attempted)
     */
    private IntervalSet savedCorruptBlocks;
    
    /**
     * Ranges which are pending writing & verification.
     */
    private IntervalSet pendingBlocks;
    
    /**
     * Decides which blocks to start downloading next.
     */
    private SelectionStrategy blockChooser = null;
    
    /**
     * The hashtree we use to verify chunks, if any
     */
    private HashTree hashTree;
    
    /**
     * The expected TigerTree root (null if we'll accept any).
     */
    private String expectedHashRoot;
    
    /**
     * Whether someone is currently requesting the hash tree
     */
    private boolean hashTreeRequested;
    
    /**
     * Whether we are actually verifying chunks
     */
    private boolean discardBad = true;
    
    /**
     * The IOException, if any, we got while writing.
     */
    private IOException storedException;
    
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
        savedCorruptBlocks = new IntervalSet();
    }
    
    /**
     * Opens this VerifyingFile for writing.
     * MUST be called before anything else.
     *
     * If there is no completion size, this fails.
     */
    public void open(File file) throws IOException {
        if(completedSize == -1)
            throw new IllegalStateException("cannot open for unknown size.");
        
        // Ensure that the directory this file is in exists & is writeable.
        File parentFile = FileUtils.getParentFile(file);
        if( parentFile != null ) {
            parentFile.mkdirs();
            FileUtils.setWriteable(parentFile);
        }
        FileUtils.setWriteable(file);
        this.fos =  new RandomAccessFile(file,"rw");
        SelectionStrategy myStrategy = SelectionStrategyFactory.getStrategyFor(
                FileUtils.getFileExtension(file), completedSize);
        
        synchronized(this) {
            storedException = null;
            
            // Figure out which SelectionStrategy to use
            blockChooser = myStrategy;
            isOpen = true;
        }
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
     * @throws InterruptedException if the downloader gets killed during the process
     */
    public void writeBlock(long pos,byte[] data) throws InterruptedException {
        writeBlock(pos,data.length,data);
    }
    
    /**
     * Writes bytes to the underlying file.
     * @throws InterruptedException if the downloader gets killed during the process
     */
    public void writeBlock(long currPos, int length, byte[] buf) 
    throws InterruptedException {
        
        if (LOG.isTraceEnabled())
            LOG.trace(" trying to write block at offset "+currPos+" with size "+length);
        
        if(buf.length==0) //nothing to write? return
            return;
        if(fos == null)
            throw new IllegalStateException("no fos!");
        
        if (!isOpen())
            return;
		
		Interval intvl = new Interval(currPos,currPos+length-1);
		
        
        byte [] temp = getBuffer();
        Assert.that(temp.length >= length);
        
        synchronized(this) {
    		/// some stuff to help debugging ///
    		if (!leasedBlocks.contains(intvl)) {
    			Assert.that(false, "trying to write an interval "+intvl+
                        " that wasn't leased.\n"+dumpState());
            }
    		
    		if (verifiedBlocks.contains(intvl) || partialBlocks.contains(intvl) ||
                savedCorruptBlocks.contains(intvl) || pendingBlocks.contains(intvl)) {
                Assert.that(false,"trying to write an interval "+intvl+
                        " that was already written"+dumpState());
    		}
                
            leasedBlocks.delete(intvl);
            pendingBlocks.add(intvl);
        }
        
        System.arraycopy(buf,0,temp,0,length);
        QUEUE.add(new ChunkHandler(temp,intvl));
        
    }
    
    private static byte [] getBuffer() throws InterruptedException {
        byte [] temp = null;
        synchronized(CACHE) {
            while (true) {
                if (!CACHE.isEmpty())
                    return (byte []) CACHE.pop();
                else if (numCreated < MAX_CACHED_BUFFERS) {
                    temp = new byte[HTTPDownloader.BUF_LENGTH];
                    numCreated++;
                    return temp;
                } else 
                    CACHE.wait();   
            }
        }
    }

    public String dumpState() {
        return "verified:"+verifiedBlocks+"\npartial:"+partialBlocks+
            "\ndiscarded:"+savedCorruptBlocks+
        	"\npending:"+pendingBlocks+"\nleased:"+leasedBlocks;
    }
    
    /**
     * Returns a block of data that needs to be written.
     * 
     * This method will not break up contiguous chunks into smaller chunks.
     */
    public Interval leaseWhite() throws NoSuchElementException {
        return leaseWhiteHelper(null, completedSize);
    }
    
    /**
     * Returns a block of data that needs to be written.
     * The returned block will NEVER be larger than chunkSize.
     */
    public Interval leaseWhite(int chunkSize) 
      throws NoSuchElementException {
        return leaseWhiteHelper(null, chunkSize);
    }
    
    /**
     * Returns a block of data that needs to be written
     * and is within the specified set of ranges.
     * The parameter IntervalSet is modified
     */
    public Interval leaseWhite(IntervalSet ranges)
      throws NoSuchElementException {
        return leaseWhiteHelper(ranges, DEFAULT_CHUNK_SIZE);
    }
    
    /**
     * Returns a block of data that needs to be written
     * and is within the specified set of ranges.
     * The returned block will NEVER be larger than chunkSize.
     */
    public Interval leaseWhite(IntervalSet ranges, int chunkSize)
      throws NoSuchElementException {
        return leaseWhiteHelper(ranges, chunkSize);
    }

    /**
     * Removes the specified internal from the set of leased intervals.
     */
    public synchronized void releaseBlock(Interval in) {
        if (!leasedBlocks.contains(in)) {
            Assert.that(false, "trying to release an interval "+in+
                    " that wasn't leased "+dumpState());
        }
        if(LOG.isInfoEnabled())
            LOG.info("Releasing interval: " + in+" state "+dumpState());
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
    
    /**
     * @return byte-packed representation of the verified blocks.
     */
    public synchronized byte [] toBytes() {
    	return verifiedBlocks.toBytes();
    }
    
    public String toString() {
        return dumpState();
    }

    /**
     * @return List of Intervals that should be serialized.  Excludes pending intervals.
     */
    public synchronized List getSerializableBlocks() {
        IntervalSet ret = new IntervalSet();
        for (Iterator iter = verifiedBlocks.getAllIntervals(); iter.hasNext();) 
            ret.add((Interval) iter.next());
        for (Iterator iter = partialBlocks.getAllIntervals(); iter.hasNext();) 
            ret.add((Interval) iter.next());
        for (Iterator iter = savedCorruptBlocks.getAllIntervals(); iter.hasNext();) 
            ret.add((Interval) iter.next());
        
        return ret.getAllIntervalsAsList();
        
    }
    /**
     * @return all downloaded blocks as list
     */
    public synchronized List getBlocksAsList() {
        List l = new ArrayList();
        l.addAll(verifiedBlocks.getAllIntervalsAsList());
        l.addAll(partialBlocks.getAllIntervalsAsList());
        l.addAll(savedCorruptBlocks.getAllIntervalsAsList());
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
        	savedCorruptBlocks.getSize() +
        	pendingBlocks.getSize();
    }
    
    public synchronized int getPendingSize() {
        return pendingBlocks.getSize();
    }
    
    public static int getNumPendingItems() {
        return QUEUE.size();
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
        if (hashTree != null)
            return verifiedBlocks.getSize() + savedCorruptBlocks.getSize() == completedSize;
        else {
            return verifiedBlocks.getSize() + savedCorruptBlocks.getSize() + 
            partialBlocks.getSize()== completedSize;
        }
    }
    
    /**
     * If the last remaining chunks of the file are currently pending writing & verification,
     * wait until it finishes.
     */
    public synchronized void waitForPendingIfNeeded() throws InterruptedException, DiskException {
        if(storedException != null)
            throw new DiskException(storedException);
        
        while (!isComplete() && getBlockSize() == completedSize) {
            if(storedException != null)
                throw new DiskException(storedException);
            if (LOG.isInfoEnabled())
                LOG.info("waiting for a pending chunk to verify or write..");
            wait();
        }
    }
    
    /**
     * @return whether we think we will not be able to complete this file
     */
    public synchronized boolean isHopeless() {
        return lostSize >= MAX_CORRUPTION * completedSize;
    }
    
    public boolean isOpen() {
        return isOpen;
    }
    /**
     * Determines if there are any blocks that are not assigned
     * or written.
     */
    public synchronized int hasFreeBlocksToAssign() {
        return  completedSize - (verifiedBlocks.getSize() + 
                leasedBlocks.getSize() +
                partialBlocks.getSize() +
                savedCorruptBlocks.getSize() +
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
        isOpen = false;
        if(fos==null)
            return;
        try { 
            fos.close();
        } catch (IOException ioe) {}
    }
    
    /////////////////////////private helpers//////////////////////////////
    /**
     * Determines which interval should be assigned next, leases that interval,
     * and returns that interval.
     * 
     * @param availableRanges if ranges is non-null, the return value will be a chosen 
     *      from within availableRanges
     * @param chunkSize if greater than zero, the return value will end one byte before 
     *      a chunkSize boundary and will be at most chunkSize bytes large.
     * @return the leased interval
     */
    private synchronized Interval leaseWhiteHelper(IntervalSet availableBytes, long chunkSize) throws NoSuchElementException {
        if (LOG.isDebugEnabled())
            LOG.debug("leasing white, state:\n"+dumpState());
      
        // If ranges is null, make ranges represent the entire file
        if (availableBytes == null)
            availableBytes = IntervalSet.createSingletonSet(0, completedSize-1);
        
        // Figure out which blocks we still need to assign
        IntervalSet neededBytes = IntervalSet.createSingletonSet(0, completedSize-1);
        neededBytes.delete(verifiedBlocks);
        neededBytes.delete(leasedBlocks);
        neededBytes.delete(partialBlocks);
        neededBytes.delete(savedCorruptBlocks);
        neededBytes.delete(pendingBlocks);
        
        if (LOG.isDebugEnabled())
            LOG.debug("needed bytes: "+neededBytes);
        
        // Calculate the intersection of neededBytes and availableBytes
        availableBytes.delete(neededBytes.invert(completedSize));
        
        Interval ret = blockChooser.pickAssignment(availableBytes, neededBytes,
                chunkSize);
        
        leaseBlock(ret);
        
        if (LOG.isDebugEnabled())
            LOG.debug("leasing white interval "+ret+"\nof available intervals "+
                    neededBytes);
        
        return ret;
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
     * Sets the expected hash tree root.  If non-null, we'll only accept
     * hash trees whose root hash matches this.
     */
    public synchronized void setExpectedHashTreeRoot(String root) {
        expectedHashRoot = root;
    }
    
    public synchronized HashTree getHashTree() {
        return hashTree;
    }
    
    /**
     * sets the HashTree the current download will use.  That affects whether
     * we do overlap checking.
     */
    public synchronized void setHashTree(HashTree tree) {
        // doesn't match our expected tree, bail.
        if(expectedHashRoot != null && tree != null &&
                !tree.getRootHash().equalsIgnoreCase(expectedHashRoot))
            return;
        
        // if the tree is of incorrect size, ignore it
        if (tree != null && tree.getFileSize() != completedSize)
            return;
        
        // if we did not have a tree previously and there are no pending blocks,
        // trigger verification
        HashTree previoius = hashTree;
        hashTree = tree;
        if (previoius == null && 
            tree != null &&
            pendingBlocks.getSize() == 0 && 
            partialBlocks.getSize() > 0) 
            QUEUE.add(new EmptyVerifier());
    }
    
    /**
     * flags that someone is currently requesting the tree
     */
    public synchronized void setHashTreeRequested(boolean yes) {
        hashTreeRequested = yes;
    }
    
    public synchronized boolean isHashTreeRequested() {
        return hashTreeRequested;
    }
    
    public synchronized void setDiscardUnverified(boolean yes) {
        discardBad = yes;
    }
    
    public synchronized int getChunkSize() {
        return hashTree == null ? DEFAULT_CHUNK_SIZE : hashTree.getNodeSize();
    }
    

    
	/**
	 * Schedules those chunks that can be verified against the hash tree
	 * for verification.
	 */
	private void verifyChunks() {
	    HashTree tree = getHashTree(); // capture the tree.
	    if(tree != null) {
            // if we have a tree, see if there is a completed chunk in the partial list
            for (Iterator iter = findVerifyableBlocks().iterator(); iter.hasNext();)  {
                Interval i = (Interval)iter.next();
                boolean good = verifyChunk(i, tree);
                
                synchronized(this) {
                    partialBlocks.delete(i);
                    if(good)
                        verifiedBlocks.add(i);
                    else {
                        if(!discardBad)
                            savedCorruptBlocks.add(i);
                        lostSize += (i.high - i.low + 1);
                    }
                }
            }
        }
    }
        
    /**
     * @return whether this chunk is corrupt according to the given hash tree
     */
    private boolean verifyChunk(Interval i, HashTree tree) {
        if (LOG.isDebugEnabled())
            LOG.debug("verifying interval "+i);
        
        
        byte []b = getChunkBuf(i.high - i.low+1);
        // read the interval from the file
        try {
			synchronized(fos) {
				fos.seek(i.low);
				fos.readFully(b);
			}
        } catch (IOException bad) {
            // we failed reading back from the file - assume block is corrupt
            // and it will have to be re-downloaded
            return false;
        }
        
        boolean corrupt = tree.isCorrupt(i,b);
        
        if (LOG.isDebugEnabled() && corrupt)
            LOG.debug("block corrupt!");
        
        return !corrupt;
    }
    
    /**
     * @return a byte array of the specified size, using cached one
     * if possible.
     */
	private static byte [] getChunkBuf(int size) {

		// cache only chunks size powers of two
		// others are very unlikely to be reused
		int exp;
		for (exp = 1 ; exp < size ; exp*=2);
		if (exp > size) 
			return new byte[size];
		
		Integer i = new Integer(size);
		byte [] ret = (byte []) CHUNK_CACHE.get(i);
		if (ret == null) {
			ret = new byte[size];
			CHUNK_CACHE.put(i,ret);
		} 
		return ret;
	}
	
    /**
     * iterates through the pending blocks and checks if the recent write has created
     * some (verifiable) full chunks.  Its not possible to verify more than two chunks
     * per method call unless the downloader is being deserialized from disk
     */
    private synchronized List findVerifyableBlocks() {
        if (LOG.isTraceEnabled())
            LOG.trace("trying to find verifyable blocks out of "+partialBlocks);
        
        List verifyable = new ArrayList(2);
        List partial = partialBlocks.getAllIntervalsAsList();
        int chunkSize = getChunkSize();
        
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
        if (!partial.isEmpty()) {
            int lastChunkOffset = completedSize - (completedSize % chunkSize);
            if (lastChunkOffset == completedSize)
                lastChunkOffset-=chunkSize;
            Interval last = (Interval) partial.get(partial.size() - 1);
            if (last.high == completedSize-1 && last.low <= lastChunkOffset ) {
                if(LOG.isDebugEnabled())
                    LOG.debug("adding the last chunk for verification");
                
                verifyable.add(new Interval(lastChunkOffset, last.high));
            }
        }
        
        return verifyable;
    }
    
    /**
     * Runnable that writes chunks to disk & verifies partial blocks.
     */
    private class ChunkHandler implements Runnable {
        /** The buffer we are about to write to the file */
        private final byte[] buf;
        
        /** The interval that we are about to write */
        private final Interval intvl;
        
        public ChunkHandler(byte[] buf, Interval intvl) {
            this.buf = buf;
            this.intvl = intvl;
        }
        
        public void run() {
            boolean freedPending = false;
    		try {
    		    if(LOG.isTraceEnabled())
    		        LOG.trace("Writing intvl: " + intvl);
                
    			synchronized(fos) {
    				fos.seek(intvl.low);
    				fos.write(buf, 0, intvl.high - intvl.low + 1);
    			}
    			
    			synchronized(VerifyingFile.this) {
    			    pendingBlocks.delete(intvl);
    			    partialBlocks.add(intvl);
                    freedPending = true;
    			}
    			
    			verifyChunks();
            } catch(IOException diskIO) {
                synchronized(VerifyingFile.this) {
                    pendingBlocks.delete(intvl);
                    storedException = diskIO;
                }
            } finally {
                // return the buffer to the cache
                synchronized(CACHE) {
                    CACHE.push(buf);
                    CACHE.notifyAll();
                }
                
                synchronized(VerifyingFile.this) {
                    if (!freedPending)
                        pendingBlocks.delete(intvl);
                    VerifyingFile.this.notify(); 
                }
            }
        }
	}
    
    private class EmptyVerifier implements Runnable {
        public void run() {
            verifyChunks();
            synchronized(VerifyingFile.this) {
                VerifyingFile.this.notify();
            }
        }
    }
    
    private static class CacheCleaner implements Runnable {
        public void run() {
            LOG.info("clearing cache");
            synchronized(CACHE) {
                int size = CACHE.size();
                CACHE.clear();
                numCreated -= size;
                CACHE.notifyAll();
            }
            QUEUE.add(new ChunkCacheCleaner());
        }
    }
    
    private static class ChunkCacheCleaner implements Runnable {
    	public void run() {
    		CHUNK_CACHE.clear();
    	}
    }
}
