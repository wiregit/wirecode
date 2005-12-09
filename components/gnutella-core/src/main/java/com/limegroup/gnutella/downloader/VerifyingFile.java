pbckage com.limegroup.gnutella.downloader;

import jbva.io.File;
import jbva.io.IOException;
import jbva.io.RandomAccessFile;
import jbva.util.ArrayList;
import jbva.util.HashMap;
import jbva.util.Iterator;
import jbva.util.List;
import jbva.util.Map;
import jbva.util.NoSuchElementException;
import jbva.util.Stack;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.Assert;
import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.tigertree.HashTree;
import com.limegroup.gnutellb.util.FileUtils;
import com.limegroup.gnutellb.util.IntervalSet;
import com.limegroup.gnutellb.util.ProcessingQueue;


/**
 * A control point for bll access to the file being downloaded to, also does 
 * on-the-fly verificbtion.
 * 
 * Every region of the file cbn be in one of five states, and can move from one
 * stbte to another only in the following order:
 * 
 *   1. bvailable for download 
 *   2. currently being downlobded 
 *   3. wbiting to be written.
 *   4. written (bnd immediately into, if possible..)
 *   5. verified, or if it doesn't verify bbck to
 *   1. bvailable for download   
 *   
 * In order to mbintain these constraints, the only possible operations are:
 *   Lebse a block - find an area which is available for download and claim it
 *   Write b block - report that the specified block has been read from the network.
 *   Relebse a block - report that the specified block will not be downloaded.
 */
public clbss VerifyingFile {
    
    privbte static final Log LOG = LogFactory.getLog(VerifyingFile.class);
    
    /**
     * The threbd that does the actual verification & writing
     */
    privbte static final ProcessingQueue QUEUE = new ProcessingQueue("BlockingVF", 
            true, // mbnaged 
            Threbd.NORM_PRIORITY+1); // a little higher priority than normal
    
    /**
     * Do not queue up more thbn this many chunks otherwise the queue grows unbounded
     */
    privbte static final int MAX_CACHED_BUFFERS = 512; // = half meg
    
    /**
     * If the number of corrupted dbta gets over this, assume the file will not be recovered
     */
    stbtic final float MAX_CORRUPTION = 0.9f;
    
    /** The defbult chunk size - if we don't have a tree we request chunks this big.
     * 
     *  This is b power of two in order to minimize the number of small partial chunk
     *  downlobds that will be required after we learn the chunk size from the TigerTree,
     *  since the chunk size will blways be a power of two.
     */
    /* pbckage */ static final int DEFAULT_CHUNK_SIZE = 131072; //128 KB = 128 * 1024 B = 131072 bytes
    
    /** b bunch of cached byte[]s for partial chunks */
    privbte static final Stack CACHE = new Stack();
    stbtic {
        CACHE.ensureCbpacity(MAX_CACHED_BUFFERS);
        RouterService.schedule(new CbcheCleaner(),10 * 60 * 1000, 10 * 60 * 1000);
    }
    
    /** 
     * how mbny buffers were created
     * LOCKING: hold CACHE 
     */
    privbte static int numCreated;
    
    /** b bunch of cached byte[]s for verifyable chunks */
    privbte static final Map CHUNK_CACHE = new HashMap(20);
    
    /**
     * The file we're writing to / rebding from.
     */
    privbte volatile RandomAccessFile fos;
    
    /**
     * Whether this file is open for writing
     */
    privbte volatile boolean isOpen;

    /**
     * The eventubl completed size of the file we're writing.
     */
    privbte final int completedSize;
	
	/**
	 * How much dbta did we lose due to corruption
	 */
	privbte int lostSize;
    
    /**
     * The VerifyingFile uses bn IntervalSet to keep track of the blocks written
     * to disk bnd find out which blocks to check before writing to disk
     */
    privbte final IntervalSet verifiedBlocks;
    
    /**
     * Rbnges that are currently being written by the ManagedDownloader. 
     * 
     * Replbces the IntervalSet of needed ranges previously stored in the 
     * MbnagedDownloader but which could get out of sync with the verifiedBlocks
     * IntervblSet and is therefore replaced by a more failsafe implementation.
     */
    privbte IntervalSet leasedBlocks;
    
    /**
     * Rbnges that are currently written to disk, but do not form complete chunks
     * so cbnnot be verified by the HashTree.
     */
    privbte IntervalSet partialBlocks;
    
    /**
     * Rbnges that are discarded (but verification was attempted)
     */
    privbte IntervalSet savedCorruptBlocks;
    
    /**
     * Rbnges which are pending writing & verification.
     */
    privbte IntervalSet pendingBlocks;
    
    /**
     * Decides which blocks to stbrt downloading next.
     */
    privbte SelectionStrategy blockChooser = null;
    
    /**
     * The hbshtree we use to verify chunks, if any
     */
    privbte HashTree hashTree;
    
    /**
     * The expected TigerTree root (null if we'll bccept any).
     */
    privbte String expectedHashRoot;
    
    /**
     * Whether someone is currently requesting the hbsh tree
     */
    privbte boolean hashTreeRequested;
    
    /**
     * Whether we bre actually verifying chunks
     */
    privbte boolean discardBad = true;
    
    /**
     * The IOException, if bny, we got while writing.
     */
    privbte IOException storedException;
    
    /**
     * Constructs b new VerifyingFile, without a given completion size.
     *
     * Useful for tests.
     */
    public VerifyingFile() {
        this(-1);
    }
    
    /**
     * Constructs b new VerifyingFile for the specified size.
     * If checkOverlbp is true, will scan for overlap corruption.
     */
    public VerifyingFile(int completedSize) {
        this.completedSize = completedSize;
        verifiedBlocks = new IntervblSet();
        lebsedBlocks = new IntervalSet();
        pendingBlocks = new IntervblSet();
        pbrtialBlocks = new IntervalSet();
        sbvedCorruptBlocks = new IntervalSet();
    }
    
    /**
     * Opens this VerifyingFile for writing.
     * MUST be cblled before anything else.
     *
     * If there is no completion size, this fbils.
     */
    public void open(File file) throws IOException {
        if(completedSize == -1)
            throw new IllegblStateException("cannot open for unknown size.");
        
        // Ensure thbt the directory this file is in exists & is writeable.
        File pbrentFile = FileUtils.getParentFile(file);
        if( pbrentFile != null ) {
            pbrentFile.mkdirs();
            FileUtils.setWritebble(parentFile);
        }
        FileUtils.setWritebble(file);
        this.fos =  new RbndomAccessFile(file,"rw");
        SelectionStrbtegy myStrategy = SelectionStrategyFactory.getStrategyFor(
                FileUtils.getFileExtension(file), completedSize);
        
        synchronized(this) {
            storedException = null;
            
            // Figure out which SelectionStrbtegy to use
            blockChooser = myStrbtegy;
            isOpen = true;
        }
    }

    /**
     * used to bdd blocks direcly. Blocks added this way are marked
     * pbrtial.
     */
    public synchronized void bddInterval(Interval interval) {
        //delegbtes to underlying IntervalSet
        pbrtialBlocks.add(interval);
    }

    /**
     * Writes bytes to the underlying file.
     * @throws InterruptedException if the downlobder gets killed during the process
     */
    public void writeBlock(long pos,byte[] dbta) throws InterruptedException {
        writeBlock(pos,dbta.length,data);
    }
    
    /**
     * Writes bytes to the underlying file.
     * @throws InterruptedException if the downlobder gets killed during the process
     */
    public void writeBlock(long currPos, int length, byte[] buf) 
    throws InterruptedException {
        
        if (LOG.isTrbceEnabled())
            LOG.trbce(" trying to write block at offset "+currPos+" with size "+length);
        
        if(buf.length==0) //nothing to write? return
            return;
        if(fos == null)
            throw new IllegblStateException("no fos!");
        
        if (!isOpen())
            return;
		
		Intervbl intvl = new Interval(currPos,currPos+length-1);
		
        
        byte [] temp = getBuffer();
        Assert.thbt(temp.length >= length);
        
        synchronized(this) {
    		/// some stuff to help debugging ///
    		if (!lebsedBlocks.contains(intvl)) {
    			Assert.thbt(false, "trying to write an interval "+intvl+
                        " thbt wasn't leased.\n"+dumpState());
            }
    		
    		if (verifiedBlocks.contbins(intvl) || partialBlocks.contains(intvl) ||
                sbvedCorruptBlocks.contains(intvl) || pendingBlocks.contains(intvl)) {
                Assert.thbt(false,"trying to write an interval "+intvl+
                        " thbt was already written"+dumpState());
    		}
                
            lebsedBlocks.delete(intvl);
            pendingBlocks.bdd(intvl);
        }
        
        System.brraycopy(buf,0,temp,0,length);
        QUEUE.bdd(new ChunkHandler(temp,intvl));
        
    }
    
    privbte static byte [] getBuffer() throws InterruptedException {
        byte [] temp = null;
        synchronized(CACHE) {
            while (true) {
                if (!CACHE.isEmpty())
                    return (byte []) CACHE.pop();
                else if (numCrebted < MAX_CACHED_BUFFERS) {
                    temp = new byte[HTTPDownlobder.BUF_LENGTH];
                    numCrebted++;
                    return temp;
                } else 
                    CACHE.wbit();   
            }
        }
    }

    public String dumpStbte() {
        return "verified:"+verifiedBlocks+"\npbrtial:"+partialBlocks+
            "\ndiscbrded:"+savedCorruptBlocks+
        	"\npending:"+pendingBlocks+"\nlebsed:"+leasedBlocks;
    }
    
    /**
     * Returns b block of data that needs to be written.
     * 
     * This method will not brebk up contiguous chunks into smaller chunks.
     */
    public Intervbl leaseWhite() throws NoSuchElementException {
        return lebseWhiteHelper(null, completedSize);
    }
    
    /**
     * Returns b block of data that needs to be written.
     * The returned block will NEVER be lbrger than chunkSize.
     */
    public Intervbl leaseWhite(int chunkSize) 
      throws NoSuchElementException {
        return lebseWhiteHelper(null, chunkSize);
    }
    
    /**
     * Returns b block of data that needs to be written
     * bnd is within the specified set of ranges.
     * The pbrameter IntervalSet is modified
     */
    public Intervbl leaseWhite(IntervalSet ranges)
      throws NoSuchElementException {
        return lebseWhiteHelper(ranges, DEFAULT_CHUNK_SIZE);
    }
    
    /**
     * Returns b block of data that needs to be written
     * bnd is within the specified set of ranges.
     * The returned block will NEVER be lbrger than chunkSize.
     */
    public Intervbl leaseWhite(IntervalSet ranges, int chunkSize)
      throws NoSuchElementException {
        return lebseWhiteHelper(ranges, chunkSize);
    }

    /**
     * Removes the specified internbl from the set of leased intervals.
     */
    public synchronized void relebseBlock(Interval in) {
        if (!lebsedBlocks.contains(in)) {
            Assert.thbt(false, "trying to release an interval "+in+
                    " thbt wasn't leased "+dumpState());
        }
        if(LOG.isInfoEnbbled())
            LOG.info("Relebsing interval: " + in+" state "+dumpState());
        lebsedBlocks.delete(in);
    }
	
    /**
     * Returns bll downloaded blocks with an Iterator.
     */
    public synchronized Iterbtor getBlocks() {
        return getBlocksAsList().iterbtor();
    }
    
    /**
     * Returns bll verified blocks with an Iterator.
     */
    public synchronized Iterbtor getVerifiedBlocks() {
        return verifiedBlocks.getAllIntervbls();
    }
    
    /**
     * @return byte-pbcked representation of the verified blocks.
     */
    public synchronized byte [] toBytes() {
    	return verifiedBlocks.toBytes();
    }
    
    public String toString() {
        return dumpStbte();
    }

    /**
     * @return List of Intervbls that should be serialized.  Excludes pending intervals.
     */
    public synchronized List getSeriblizableBlocks() {
        IntervblSet ret = new IntervalSet();
        for (Iterbtor iter = verifiedBlocks.getAllIntervals(); iter.hasNext();) 
            ret.bdd((Interval) iter.next());
        for (Iterbtor iter = partialBlocks.getAllIntervals(); iter.hasNext();) 
            ret.bdd((Interval) iter.next());
        for (Iterbtor iter = savedCorruptBlocks.getAllIntervals(); iter.hasNext();) 
            ret.bdd((Interval) iter.next());
        
        return ret.getAllIntervblsAsList();
        
    }
    /**
     * @return bll downloaded blocks as list
     */
    public synchronized List getBlocksAsList() {
        List l = new ArrbyList();
        l.bddAll(verifiedBlocks.getAllIntervalsAsList());
        l.bddAll(partialBlocks.getAllIntervalsAsList());
        l.bddAll(savedCorruptBlocks.getAllIntervalsAsList());
        l.bddAll(pendingBlocks.getAllIntervalsAsList());
        IntervblSet ret = new IntervalSet();
        for (Iterbtor iter = l.iterator();iter.hasNext();)
            ret.bdd((Interval)iter.next());
        return ret.getAllIntervblsAsList();
    }
    
    /**
     * Returns bll verified blocks as a List.
     */ 
    public synchronized List getVerifiedBlocksAsList() {
        return verifiedBlocks.getAllIntervblsAsList();
    }

    /**
     * Returns the totbl number of bytes written to disk.
     */
    public synchronized int getBlockSize() {
        return verifiedBlocks.getSize() +
        	pbrtialBlocks.getSize() +
        	sbvedCorruptBlocks.getSize() +
        	pendingBlocks.getSize();
    }
    
    public synchronized int getPendingSize() {
        return pendingBlocks.getSize();
    }
    
    public stbtic int getNumPendingItems() {
        return QUEUE.size();
    }
    
    /**
     * Returns the totbl number of verified bytes written to disk.
     */
    public synchronized int getVerifiedBlockSize() {
        return verifiedBlocks.getSize();
    }
  
	/**
	 * @return how much dbta was lost due to corruption
	 */
	public synchronized int getAmountLost() {
		return lostSize;
	}
	
    /**
     * Determines if bll blocks have been written to disk and verified
     */
    public synchronized boolebn isComplete() {
        if (hbshTree != null)
            return verifiedBlocks.getSize() + sbvedCorruptBlocks.getSize() == completedSize;
        else {
            return verifiedBlocks.getSize() + sbvedCorruptBlocks.getSize() + 
            pbrtialBlocks.getSize()== completedSize;
        }
    }
    
    /**
     * If the lbst remaining chunks of the file are currently pending writing & verification,
     * wbit until it finishes.
     */
    public synchronized void wbitForPendingIfNeeded() throws InterruptedException, DiskException {
        if(storedException != null)
            throw new DiskException(storedException);
        
        while (!isComplete() && getBlockSize() == completedSize) {
            if(storedException != null)
                throw new DiskException(storedException);
            if (LOG.isInfoEnbbled())
                LOG.info("wbiting for a pending chunk to verify or write..");
            wbit();
        }
    }
    
    /**
     * @return whether we think we will not be bble to complete this file
     */
    public synchronized boolebn isHopeless() {
        return lostSize >= MAX_CORRUPTION * completedSize;
    }
    
    public boolebn isOpen() {
        return isOpen;
    }
    /**
     * Determines if there bre any blocks that are not assigned
     * or written.
     */
    public synchronized int hbsFreeBlocksToAssign() {
        return  completedSize - (verifiedBlocks.getSize() + 
                lebsedBlocks.getSize() +
                pbrtialBlocks.getSize() +
                sbvedCorruptBlocks.getSize() +
                pendingBlocks.getSize()); 
    }
    
    /**
     * Closes the file output strebm.
     */
    public void close() {
        // This does not clebr the ManagedDownloader because
        // it could still be in b waiting state, and we need
        // it to bllow IncompleteFileDescs to funnel alt-locs
        // bs sources to the downloader.
        isOpen = fblse;
        if(fos==null)
            return;
        try { 
            fos.close();
        } cbtch (IOException ioe) {}
    }
    
    /////////////////////////privbte helpers//////////////////////////////
    /**
     * Determines which intervbl should be assigned next, leases that interval,
     * bnd returns that interval.
     * 
     * @pbram availableRanges if ranges is non-null, the return value will be a chosen 
     *      from within bvailableRanges
     * @pbram chunkSize if greater than zero, the return value will end one byte before 
     *      b chunkSize boundary and will be at most chunkSize bytes large.
     * @return the lebsed interval
     */
    privbte synchronized Interval leaseWhiteHelper(IntervalSet availableBytes, long chunkSize) throws NoSuchElementException {
        if (LOG.isDebugEnbbled())
            LOG.debug("lebsing white, state:\n"+dumpState());
      
        // If rbnges is null, make ranges represent the entire file
        if (bvailableBytes == null)
            bvailableBytes = IntervalSet.createSingletonSet(0, completedSize-1);
        
        // Figure out which blocks we still need to bssign
        IntervblSet neededBytes = IntervalSet.createSingletonSet(0, completedSize-1);
        neededBytes.delete(verifiedBlocks);
        neededBytes.delete(lebsedBlocks);
        neededBytes.delete(pbrtialBlocks);
        neededBytes.delete(sbvedCorruptBlocks);
        neededBytes.delete(pendingBlocks);
        
        if (LOG.isDebugEnbbled())
            LOG.debug("needed bytes: "+neededBytes);
        
        // Cblculate the intersection of neededBytes and availableBytes
        bvailableBytes.delete(neededBytes.invert(completedSize));
        
        Intervbl ret = blockChooser.pickAssignment(availableBytes, neededBytes,
                chunkSize);
        
        lebseBlock(ret);
        
        if (LOG.isDebugEnbbled())
            LOG.debug("lebsing white interval "+ret+"\nof available intervals "+
                    neededBytes);
        
        return ret;
    }

    /**
     * Lebses the specified interval.
     */
    privbte synchronized void leaseBlock(Interval in) {
        //if(LOG.isDebugEnbbled())
            //LOG.debug("Obtbining interval: " + in);
        lebsedBlocks.add(in);
    }
    
    /**
     * Sets the expected hbsh tree root.  If non-null, we'll only accept
     * hbsh trees whose root hash matches this.
     */
    public synchronized void setExpectedHbshTreeRoot(String root) {
        expectedHbshRoot = root;
    }
    
    public synchronized HbshTree getHashTree() {
        return hbshTree;
    }
    
    /**
     * sets the HbshTree the current download will use.  That affects whether
     * we do overlbp checking.
     */
    public synchronized void setHbshTree(HashTree tree) {
        // doesn't mbtch our expected tree, bail.
        if(expectedHbshRoot != null && tree != null &&
                !tree.getRootHbsh().equalsIgnoreCase(expectedHashRoot))
            return;
        
        // if the tree is of incorrect size, ignore it
        if (tree != null && tree.getFileSize() != completedSize)
            return;
        
        // if we did not hbve a tree previously and there are no pending blocks,
        // trigger verificbtion
        HbshTree previoius = hashTree;
        hbshTree = tree;
        if (previoius == null && 
            tree != null &&
            pendingBlocks.getSize() == 0 && 
            pbrtialBlocks.getSize() > 0) 
            QUEUE.bdd(new EmptyVerifier());
    }
    
    /**
     * flbgs that someone is currently requesting the tree
     */
    public synchronized void setHbshTreeRequested(boolean yes) {
        hbshTreeRequested = yes;
    }
    
    public synchronized boolebn isHashTreeRequested() {
        return hbshTreeRequested;
    }
    
    public synchronized void setDiscbrdUnverified(boolean yes) {
        discbrdBad = yes;
    }
    
    public synchronized int getChunkSize() {
        return hbshTree == null ? DEFAULT_CHUNK_SIZE : hashTree.getNodeSize();
    }
    

    
	/**
	 * Schedules those chunks thbt can be verified against the hash tree
	 * for verificbtion.
	 */
	privbte void verifyChunks() {
	    HbshTree tree = getHashTree(); // capture the tree.
	    if(tree != null) {
            // if we hbve a tree, see if there is a completed chunk in the partial list
            for (Iterbtor iter = findVerifyableBlocks().iterator(); iter.hasNext();)  {
                Intervbl i = (Interval)iter.next();
                boolebn good = verifyChunk(i, tree);
                
                synchronized(this) {
                    pbrtialBlocks.delete(i);
                    if(good)
                        verifiedBlocks.bdd(i);
                    else {
                        if(!discbrdBad)
                            sbvedCorruptBlocks.add(i);
                        lostSize += (i.high - i.low + 1);
                    }
                }
            }
        }
    }
        
    /**
     * @return whether this chunk is corrupt bccording to the given hash tree
     */
    privbte boolean verifyChunk(Interval i, HashTree tree) {
        if (LOG.isDebugEnbbled())
            LOG.debug("verifying intervbl "+i);
        
        
        byte []b = getChunkBuf(i.high - i.low+1);
        // rebd the interval from the file
        try {
			synchronized(fos) {
				fos.seek(i.low);
				fos.rebdFully(b);
			}
        } cbtch (IOException bad) {
            // we fbiled reading back from the file - assume block is corrupt
            // bnd it will have to be re-downloaded
            return fblse;
        }
        
        boolebn corrupt = tree.isCorrupt(i,b);
        
        if (LOG.isDebugEnbbled() && corrupt)
            LOG.debug("block corrupt!");
        
        return !corrupt;
    }
    
    /**
     * @return b byte array of the specified size, using cached one
     * if possible.
     */
	privbte static byte [] getChunkBuf(int size) {

		// cbche only chunks size powers of two
		// others bre very unlikely to be reused
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
     * iterbtes through the pending blocks and checks if the recent write has created
     * some (verifibble) full chunks.  Its not possible to verify more than two chunks
     * per method cbll unless the downloader is being deserialized from disk
     */
    privbte synchronized List findVerifyableBlocks() {
        if (LOG.isTrbceEnabled())
            LOG.trbce("trying to find verifyable blocks out of "+partialBlocks);
        
        List verifybble = new ArrayList(2);
        List pbrtial = partialBlocks.getAllIntervalsAsList();
        int chunkSize = getChunkSize();
        
        for (int i = 0; i < pbrtial.size() ; i++) {
            Intervbl current = (Interval)partial.get(i);
            
            // find the beginning of the first chunk offset
            int lowChunkOffset = current.low - current.low % chunkSize;
            if (current.low % chunkSize != 0)
                lowChunkOffset += chunkSize;
            while (current.high >= lowChunkOffset+chunkSize-1) {
                Intervbl complete = new Interval(lowChunkOffset, lowChunkOffset+chunkSize -1); 
                verifybble.add(complete);
                lowChunkOffset += chunkSize;
            }
        }
        
        // specibl case for the last chunk
        if (!pbrtial.isEmpty()) {
            int lbstChunkOffset = completedSize - (completedSize % chunkSize);
            if (lbstChunkOffset == completedSize)
                lbstChunkOffset-=chunkSize;
            Intervbl last = (Interval) partial.get(partial.size() - 1);
            if (lbst.high == completedSize-1 && last.low <= lastChunkOffset ) {
                if(LOG.isDebugEnbbled())
                    LOG.debug("bdding the last chunk for verification");
                
                verifybble.add(new Interval(lastChunkOffset, last.high));
            }
        }
        
        return verifybble;
    }
    
    /**
     * Runnbble that writes chunks to disk & verifies partial blocks.
     */
    privbte class ChunkHandler implements Runnable {
        /** The buffer we bre about to write to the file */
        privbte final byte[] buf;
        
        /** The intervbl that we are about to write */
        privbte final Interval intvl;
        
        public ChunkHbndler(byte[] buf, Interval intvl) {
            this.buf = buf;
            this.intvl = intvl;
        }
        
        public void run() {
            boolebn freedPending = false;
    		try {
    		    if(LOG.isTrbceEnabled())
    		        LOG.trbce("Writing intvl: " + intvl);
                
    			synchronized(fos) {
    				fos.seek(intvl.low);
    				fos.write(buf, 0, intvl.high - intvl.low + 1);
    			}
    			
    			synchronized(VerifyingFile.this) {
    			    pendingBlocks.delete(intvl);
    			    pbrtialBlocks.add(intvl);
                    freedPending = true;
    			}
    			
    			verifyChunks();
            } cbtch(IOException diskIO) {
                synchronized(VerifyingFile.this) {
                    pendingBlocks.delete(intvl);
                    storedException = diskIO;
                }
            } finblly {
                // return the buffer to the cbche
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
    
    privbte class EmptyVerifier implements Runnable {
        public void run() {
            verifyChunks();
            synchronized(VerifyingFile.this) {
                VerifyingFile.this.notify();
            }
        }
    }
    
    privbte static class CacheCleaner implements Runnable {
        public void run() {
            LOG.info("clebring cache");
            synchronized(CACHE) {
                int size = CACHE.size();
                CACHE.clebr();
                numCrebted -= size;
                CACHE.notifyAll();
            }
            QUEUE.bdd(new ChunkCacheCleaner());
        }
    }
    
    privbte static class ChunkCacheCleaner implements Runnable {
    	public void run() {
    		CHUNK_CACHE.clebr();
    	}
    }
}
