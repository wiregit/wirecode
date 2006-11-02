package com.limegroup.bittorrent.disk;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.Serializable;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.bittorrent.BTInterval;
import com.limegroup.bittorrent.BTMetaInfo;
import com.limegroup.bittorrent.BTPiece;
import com.limegroup.bittorrent.PieceReadListener;
import com.limegroup.bittorrent.TorrentContext;
import com.limegroup.bittorrent.TorrentFile;
import com.limegroup.bittorrent.TorrentFileSystem;
import com.limegroup.bittorrent.settings.BittorrentSettings;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.Interval;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.util.AndView;
import com.limegroup.gnutella.util.BitField;
import com.limegroup.gnutella.util.BitFieldSet;
import com.limegroup.gnutella.util.IntervalSet;
import com.limegroup.gnutella.util.MultiIterable;
import com.limegroup.gnutella.util.BitSet;
import com.limegroup.gnutella.util.NECallable;
import com.limegroup.gnutella.util.NotView;
import com.limegroup.gnutella.util.RRProcessingQueue;
import com.limegroup.gnutella.util.SystemUtils;

/**
 * A default implementation of <tt>TorrentDiskManager</tt>
 */
class VerifyingFolder implements TorrentDiskManager {
	
	private static final Log LOG = LogFactory.getLog(VerifyingFolder.class);
	
	/**
	 * The thread that does the reading, writing and
	 * on-the-fly verification 
	 */
	private static final RRProcessingQueue QUEUE = new RRProcessingQueue(
			"TorrentDiskQueue");
	
	/**
	 * Queue for verifying torrents that already exist on the hard
	 * disk.
	 */
	private static final RRProcessingQueue VERIFY_QUEUE = 
		new RRProcessingQueue("TorrentVerifier");
	
	
	/**
	 * This is the max size of a block that we will ever request. Requesting
	 * larger ranges is not encouraged by the protocol.
	 */
	private static final int BLOCK_SIZE = 16384;
	
	/*
	 * The files of this torrent as an array
	 */
	private final List<TorrentFile> _files;

	/**
	 * Mapping of the index of each partial block and the written ranges within 
	 */
	private BlockRangeMap partialBlocks;
	
	/**
	 * Mapping of the index of each requested block and the requested ranges within 
	 */
	private BlockRangeMap requestedRanges;
	
	/**
	 * A view of the blocks the requested and partial blocks.
	 */
	private Iterable<Integer> requestedAndPartial;
	
	/**
	 * Mapping of the index of each block pending write and 
	 * pending ranges within 
	 */
	private BlockRangeMap pendingRanges;
	
	/**
	 * A BitSet for all blocks that are verified.
	 */
	private BitSet verifiedBlocks;
	
	private BitField missing, verified;
	
	/** a cached bitfield. LOCKING: this*/
	private byte [] bitField;
	/** whether the cached bitfield is dirty LOCKING: this */
	private boolean bitFieldDirty = true;

	/**
	 * the number of bytes that failed verification, either
	 * during download or initial checking.
	 */
	private long _corruptedBytes;

	private final TorrentContext context;
	
	/**
	 * an exception indicating Disk operation failed.
	 * a nice feature would be to have a separate
	 * exception for each file so that multi-file torrents would 
	 * not fail if an operation on one of the files throws.
	 */
	private volatile IOException storedException;
	
	/**
	 * The <tt>DiskManagerListener</tt> to notify.
	 */
	private volatile DiskManagerListener listener;
	
	/** Whether the files on disk are currently being verified */
	private volatile boolean isVerifying;
	
	/**
	 * Disk controller for performing the reads and writes.
	 */
	private final DiskController<TorrentFile> diskController;

	/**
	 * constructs instance of this
	 * 
	 * @param info a BTMetaInfo for which to create this Folder.
	 * @param complete if the download is completed
	 */
	VerifyingFolder(TorrentContext context, 
			boolean complete, 
            Serializable data,
			DiskController<TorrentFile> diskController) {
		TorrentFileSystem system = context.getFileSystem();
		_files = complete? system.getFiles() : system.getIncompleteFiles();
		this.context = context;
		_corruptedBytes = 0;
		partialBlocks = new BlockRangeMap();
		requestedRanges = new BlockRangeMap();
		pendingRanges = new BlockRangeMap();
		this.diskController = diskController;
		
		requestedAndPartial = 
			new MultiIterable<Integer>(partialBlocks.keySet(), 
					requestedRanges.keySet());
		
		if (complete) {
			verifiedBlocks = context.getFullBitSet();
			verified = context.getFullBitField();
		} else {
			verifiedBlocks = new BitSet(context.getMetaInfo().getNumBlocks());
			if (data != null && data instanceof SerialData)
				initialize((SerialData)data);
			verified = new BitFieldSet(verifiedBlocks, context.getMetaInfo().getNumBlocks());
		}
		
		missing = new NotView(verified);
	}
	
	/**
	 * populates various fields from data object that was 
	 * deserialized.
	 */
	private void initialize(SerialData data) {
		if (data.getPartialBlocks() != null) 
			partialBlocks.putAll(data.getPartialBlocks());
		
		if (data.getVerifiedBlocks() != null) 
			verifiedBlocks = data.getVerifiedBlocks();
		
		isVerifying = data.isVerifying();
	}
	
	
	public void writeBlock(NECallable<BTPiece> factory) {
		if (storedException != null)
			return;
		QUEUE.invokeLater(new WriteJob(factory),context.getMetaInfo().getURN());
	}
	
	/**
	 * This duplicates lots of code from VerifyingFile..
	 * the two should eventually be abstracted somehow.
	 */
	private class WriteJob implements Runnable {
		private final NECallable<BTPiece> factory;
		
		WriteJob(NECallable<BTPiece> factory) {
			this.factory = factory;
		}
		
		public void run() {
			if (storedException != null)
				return;
			
			BTPiece piece = factory.call();
			
			BTInterval in = piece.getInterval();
			byte [] data = piece.getData();
			
			synchronized(VerifyingFolder.this) {
				if (hasBlock(in.getId()))
					return;
				pendingRanges.addInterval(in);
				requestedRanges.removeInterval(in);
			}
			
			try {
				writeBlockImpl(in, data);
			} catch (IOException iox) {
				if (isOpen()) {
					storedException = iox;
					notifyDiskProblem();
				}
			} finally {
				synchronized(VerifyingFolder.this) {
					pendingRanges.removeInterval(in);
				}
			}
		}
	}
	/**
	 * Writes bytes to the underlying files.
	 * 
	 * @param in the BTInterval that is to be written
	 * @param buf
	 *            an array of byte containing the bytes to write
	 */
	private void writeBlockImpl(BTInterval in, byte[] buf) 
	throws IOException {
		
		long startOffset = (long)in.getId() * context.getMetaInfo().getPieceLength() + in.low;
		diskController.write(startOffset, buf);
		
		synchronized(this) {
			pendingRanges.removeInterval(in);
			partialBlocks.addInterval(in);
			if (!isCompleteBlock(in.getId(), partialBlocks))
				return;
		}
		
		boolean verified = verifyQuick(in.getId());
		
		synchronized(this) {
			partialBlocks.remove(in.getId());
			if (verified) 
				markPieceCompleted(in.getId());
			else 
				_corruptedBytes += getPieceSize(in.getId());
		}
		if (verified)
			handleVerified(in.getId());
	}
	
	private synchronized void markPieceCompleted(int blockId) {
		partialBlocks.remove(blockId);
		requestedRanges.remove(blockId);
		verifiedBlocks.set(blockId);
		bitFieldDirty = true;
		if (verifiedBlocks.cardinality() == context.getMetaInfo().getNumBlocks()) {
			verifiedBlocks = context.getFullBitSet();
			verified = context.getFullBitField();
			missing = new NotView(verified);
		}
	}
	
	private boolean verifyQuick(int pieceNum) throws IOException {
		try {
			return verify(pieceNum, false);
		} catch (InterruptedException impossible) {
			ErrorService.error(impossible);
			return false;
		}
	}
	
	/**
	 * @param pieceNum the piece to verify
	 * @param slow whether to not max out cpu
	 * @return if the block at pieceNum is ok.
	 */
	private boolean verify(int pieceNum, boolean slow) 
	throws IOException, InterruptedException {
		MessageDigest md = context.getMetaInfo().getMessageDigest();
		md.reset();
		int pieceSize = getPieceSize(pieceNum);
		byte [] buf = new byte[Math.min(65536,pieceSize)];
		int read = 0;
		long offset = (long)pieceNum * context.getMetaInfo().getPieceLength();
		while (read < pieceSize) {
			
			int readNow = diskController.read(offset, buf, 0, buf.length, 
					BittorrentSettings.TORRENT_FLUSH_VERIRY.getValue());
			if (readNow == 0)
				return false;
			
			long start = System.currentTimeMillis();
			md.update(buf, 0, readNow);
			
			if (slow && SystemUtils.getIdleTime() < URN.MIN_IDLE_TIME &&
					SharingSettings.FRIENDLY_HASHING.getValue()) {
				long interval = System.currentTimeMillis() - start;
				// go extra slow if there are active torrents
				interval *= QUEUE.size() > 0 ? 5 : 3; 
				if (interval > 0) 
					Thread.sleep(interval);
				else
					Thread.yield();
			}
			
			read += readNow;
			offset += readNow;
		}
		
		byte [] sha1 = md.digest();
		return context.getMetaInfo().verify(sha1, pieceNum);
	}
	
	/**
	 * performs various tasks after a block has been verified
	 * such as notifying any listeners or closing files.
	 */
	private void handleVerified(int pieceNum) throws IOException {
		closeAnyCompletedFiles(pieceNum);
		notifyOfChunkCompletion(pieceNum);
	}
	
	private void notifyOfChunkCompletion(int pieceNum) {
		DiskManagerListener t = listener;
		if (t != null)
			t.chunkVerified(pieceNum);
	}
	
	private void closeAnyCompletedFiles(int pieceNum) throws IOException {
		List<TorrentFile> possiblyCompleted = null;
		for (TorrentFile t : _files) {
			if (t.getBegin() > pieceNum)
				continue;
			if (t.getEnd() < pieceNum)
				break;
			
			if (possiblyCompleted == null)
				possiblyCompleted = new ArrayList<TorrentFile>();
			
			possiblyCompleted.add(t);
		}
		
		if (possiblyCompleted == null)
			return;
		
		for (TorrentFile file : possiblyCompleted) {
			boolean done = true;
			for(int i = file.getBegin(); i <= file.getEnd(); i++) {
				if (!hasBlock(i)) {
					done = false;
					break;
				}
			}
			if (done) 
				diskController.setReadOnly(file);
		}
	}
	
	private boolean isCompleteBlock(Interval in, int id) {
		return in.low == 0 && in.high == getPieceSize(id) - 1;
	}
	
	public synchronized boolean hasBlock(int block) {
		return verified.get(block);
	}

	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.TorrentFileManager#open(com.limegroup.bittorrent.ManagedTorrent)
	 */
	public void open(final DiskManagerListener torrent) throws IOException {
		this.listener = torrent;
		storedException = null;
		
		boolean wasVerifying = isVerifying;
		// whether the data on disk should be re-verified
		isVerifying |= (getBlockSize() == 0);
		
		List<TorrentFile> filesToVerify = diskController.open(_files, isComplete(), isVerifying);
		
		// verify any files that needed verification
		if (filesToVerify != null) {
			isVerifying = true;
			// pretend nothing was downloaded
			if (!wasVerifying) {
				synchronized(this) {
					verifiedBlocks.clear();
					partialBlocks.clear();
				}
			}
			verifyFiles(filesToVerify);
		} else
			isVerifying = false;

		// always verify any partial blocks that are large enough
		// (could happen if lw was shutdown during verification)
		for (int block : partialBlocks.keySet() ) {
			if (isCompleteBlock(block, partialBlocks)) {
				isVerifying = true;
				VERIFY_QUEUE.invokeLater(new VerifyJob(block),context.getMetaInfo().getURN());
			}
		}
		
		// if we had to verify anything, enqueue a notification
		// after we're done.
		if (isVerifying) {
			VERIFY_QUEUE.invokeLater(new Runnable(){
				public void run() {
					if (isOpen()) {
						isVerifying = false;
						_corruptedBytes = 0;
						torrent.verificationComplete();
					}
				}
			}, context.getMetaInfo().getURN());
		}
	}
	
	public boolean isVerifying() {
		return isVerifying;
	}

	/**
	 * @return true if the whole torrent has been written and verified
	 */
	public synchronized boolean isComplete() {
		return verified == context.getFullBitField();
	}

	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.TorrentFileManager#close()
	 */
	public void close() {
		LOG.debug("closing the file");
		synchronized(this) {
			pendingRanges.clear();
		}
		diskController.close();
		
		listener = null;
		// kill all jobs for this torrent
		URN urn = context.getMetaInfo().getURN();
		VERIFY_QUEUE.clear(urn);
		QUEUE.clear(urn);
		
	}

	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.TorrentFileManager#isOpen()
	 */
	public boolean isOpen() {
		return diskController != null & diskController.isOpen();
	}

	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.TorrentFileManager#requestPieceRead()
	 */
	public void requestPieceRead(BTInterval in, PieceReadListener c) {
		if (storedException != null)
				return;
		QUEUE.invokeLater(new SendJob(in, c),context.getMetaInfo().getURN());
	}
	
	private class SendJob implements Runnable {
		private final BTInterval in;
		private final PieceReadListener listener;
		SendJob(BTInterval in, PieceReadListener listener) {
			this.in = in;
			this.listener = listener;
		}
		
		public void run() {
			if (!isOpen())
				return;
			if (storedException != null)
				return;
			
			if (LOG.isDebugEnabled())
				LOG.debug("reading piece " + in);
			int length = in.high - in.low + 1;
			long position = (long)in.getId() * context.getMetaInfo().getPieceLength() + in.low;
			int offset = 0;
			byte[] buf = new byte[length];
			boolean success = false;
			try {
				do {
					offset += diskController.read(position + offset, buf, offset, length
							- offset, false);
				} while (offset < length);
				success = true;
			} catch (IOException bad) {
				if (isOpen()) {
					storedException = bad;
					notifyDiskProblem();
				}
			} finally {
				if (success)
					listener.pieceRead(in, buf);
				else
					listener.pieceReadFailed(in);
			}
		}
	}
	
	private void notifyDiskProblem() {
		DiskManagerListener t = listener;
		if (t != null)
			t.diskExceptionHappened();
	}
	
	/**
	 * returns a random available range that has preferrably not yet been
	 * requested
	 * 
	 * @param bs the BitBasedIntervalSet of available ranges
	 * @param exclude the set of ranges that the connection is already about to
	 * request
	 * @return a BTInterval that should be requested next.
	 */
	public synchronized BTInterval leaseRandom(BitField bs, Set<BTInterval> exclude) {
		if (isComplete())
			return null;
		
		if (LOG.isDebugEnabled())
			LOG.debug("leasing random chunk from available cardinality "+bs.cardinality());
		
		BitField interesting = new AndView(bs, missing);
		BTInterval leased = findRandom(interesting, exclude);
		
		if (leased != null) {
			if (leased.high - leased.low + 1 > BLOCK_SIZE)
				leased = new BTInterval(leased.low, leased.low + BLOCK_SIZE - 1, leased.getId());
			requestedRanges.addInterval(leased);
			
			if (LOG.isDebugEnabled())
				LOG.debug("assigning "+leased);
		}
		else if (LOG.isDebugEnabled())
			LOG.debug("couldn't find anything to assign "+exclude);
		
		return leased;
	}
	
	private BTInterval findRandom(BitField bs, Set<BTInterval> exclude) {
		
		// first try to complete any partial pieces that are not requested
		BTInterval ret = assignEndgame(bs, exclude, false);
		if (ret != null)
			return ret;
		LOG.debug("couldn't find partial, looking for unnassigned");
		
		// then see if the remote has any pieces that are neither 
		// partial nor already requested
		ret = findUnassigned(bs);
		if (ret != null)
			return ret;
		LOG.debug("couldn't find unassigned, looking for already requested");
		
		return assignEndgame(bs, exclude, true);
	}
	
	private BTInterval findUnassigned(BitField available) {
		int selected = -1;
		int current = 1;
		
		for (int i = available.nextSetBit(0); i >= 0; i = available.nextSetBit(i+1)) {
			if (pendingRanges.containsKey(i) || 
					partialBlocks.containsKey(i) ||
					requestedRanges.containsKey(i))
				continue;
			if (Math.random() < 1f/current++)
				selected = i;
		}
		
		if (selected == -1)
			return null;
		if (LOG.isDebugEnabled())
			LOG.debug("selecting unassigned piece "+selected);

		return new BTInterval(0,getPieceSize(selected) - 1,selected);
	}
	
	/**
	 * Picks an interval that is already requested by another connection.  This is
	 * referered to as "Endgame mode" and is done when there are no other pieces to 
	 * request. 
	 */
	private BTInterval assignEndgame(BitField bs, Set<BTInterval>exclude, boolean endgame) {
		
		BTInterval ret = null;
		
		// prepare a list of partial or requested blocks the remote host has
		Collection<Integer> available = null;
		for (int requested : requestedAndPartial) {
			if (!bs.get(requested) || 
				(!endgame && isCompleteBlock(requested,requestedRanges)) ||
				isCompleteBlock(requested, partialBlocks)) // during verification of block
				continue;

			if (available == null)
				available = new HashSet<Integer>(requestedRanges.size()+partialBlocks.size());
			available.add(requested);
		}

		if (available == null)
			return null;
	
		if (LOG.isDebugEnabled())
			LOG.debug("available partial and requested blocks to attempt: "+available);
		
		available = new ArrayList<Integer>(available);
		Collections.shuffle((List<Integer>)available);
		
		// go through and find a block that we can request something from.
		for (Iterator<Integer> iterator = available.iterator(); iterator.hasNext() && ret == null;) {
			int block = iterator.next();
			
			// figure out which parts of the chunks we need.
			IntervalSet needed = new IntervalSet();
			needed = needed.invert(getPieceSize(block));
			
			IntervalSet partial = partialBlocks.get(block);
			IntervalSet pending = pendingRanges.get(block);
			IntervalSet requested = requestedRanges.get(block);
			
			// get the parts of the block we're missing
			if (partial != null)
				needed.delete(partial);
			
			// don't request any parts pending write
			if (pending != null)
				needed.delete(pending);
			
			// exclude any specified intervals 
			for (Interval excluded : exclude) 
				needed.delete(excluded);
			
			// try not to request any parts that are already requested
			if (requested != null) {
				needed.delete(requested);
				
				// now, if we still have some parts of the chunk, get one of them
				// if not and this is the last partial chunk, doubly-assign some
				// part of it (a.k.a. endgame?)
				if (endgame && needed.isEmpty() && !iterator.hasNext()) {
					LOG.debug("endgame");
					needed = requested.clone();
					
					// exclude the specified intervals again
					for (Interval excluded : exclude) 
						needed.delete(excluded);
				}
			}
			
			if (needed.isEmpty()) 
				continue;
			
			ret = new BTInterval(needed.getFirst(),block);
			if (LOG.isDebugEnabled()) {
				LOG.debug("selected partial/requested interval "+ret+
						" with partial "+partialBlocks.get(ret.getId())+
						" requested "+requestedRanges.get(ret.getId())+
						" pending "+pendingRanges.get(ret.getId()));
			}
		}
		
		// couldn't find anything to assign.
		return ret;
	}

	/**
	 * @return whether the specified BlockRangeMap contains an Interval that 
	 * represents a complete piece.
	 */	
	private boolean isCompleteBlock(int pieceNum, BlockRangeMap toCheck) {
		IntervalSet set = toCheck.get(pieceNum);
		if (set == null)
			return false;
		if (set.getNumberOfIntervals() != 1)
			return false;
		Interval i = set.getFirst();
		return isCompleteBlock(i, pieceNum);
	}

	/**
	 * @return the size of the piece with given number.  All pieces
	 * except the last one have the same size.
	 */
	private int getPieceSize(int pieceNum) {
		BTMetaInfo info = context.getMetaInfo();
		if (pieceNum == info.getNumBlocks() - 1)
			return (int)(context.getFileSystem().getTotalSize() % 
					info.getPieceLength());
		else
			return info.getPieceLength();
	}
	
	/**
	 * Removes an interval from the internal list of already requested intervals.
	 * 
	 * Note that during endgame several connections may be requesting the same interval
	 * and as one of them fails that interval will no longer be considered requested.
	 * That's ok as it will only result in that interval requested again.
	 */
	public synchronized void releaseInterval(BTInterval in) {
		if (LOG.isDebugEnabled())
			LOG.debug("releasing "+in);
		requestedRanges.removeInterval(in);
	}

	/**
	 * Creates a bitfield
	 * 
	 * @return returns an array of byte where the i'th byte is 1 if we have
	 *         written and verified the i'th piece of the torrent and 0
	 *         otherwise
	 *         
	 */
	public synchronized byte[] createBitField() {
		if (bitField == null) 
			bitField = new byte[(context.getMetaInfo().getNumBlocks() + 7) / 8];
		
		if (bitFieldDirty) {
			if (isComplete()) {
				for(int i = 0; i < bitField.length; i++)
					bitField[i] = (byte)0xFF;
				int odd = context.getMetaInfo().getNumBlocks() % 8;
				if (odd != 0) 
					bitField[bitField.length - 1] <<= (8 - odd);
				
			} else {
				for(int i = verified.nextSetBit(0); i >= 0; i = verified.nextSetBit(i+1)) 
					bitField[i / 8] = (byte) (bitField[i / 8] | (1 << (7 - i % 8)));
			}
			bitFieldDirty = false;
		}
			
		return bitField;
	}

	/**
	 * verifies all the chunks that are associated with a list of files.
	 */
	private void verifyFiles(List<TorrentFile> l) {
		int lastSet;
		synchronized(this) {
			lastSet = verifiedBlocks.length();
		}
		for (TorrentFile f : l) {
			for (int i = Math.max(lastSet,f.getBegin()); i <= f.getEnd(); i++) 
				VERIFY_QUEUE.invokeLater(new VerifyJob(i),context.getMetaInfo().getURN());
		}
	}

	/**
	 * A task that checks an already existing file on disk
	 * against the .torrent metadata.  
	 */
	private class VerifyJob implements Runnable {
		private final int pieceNum;
		public VerifyJob(int pieceNum) {
			this.pieceNum = pieceNum;
		}
		
		public void run() {
			if (storedException != null ||
					!isOpen() ||
					hasBlock(pieceNum))
				return;
			try {
				
				if (verify(pieceNum, true)) {
					markPieceCompleted(pieceNum);
					handleVerified(pieceNum);
				} else 
					_corruptedBytes += getPieceSize(pieceNum);
			} catch (IOException bad) {
				storedException = bad;
			} catch (InterruptedException iex) { // should not happen
				storedException = new InterruptedIOException();
			} finally {
				if (storedException != null && isOpen())
					notifyDiskProblem();
			}
		}
	}

	/**
	 * @return number of bytes written and verified
	 */
	public synchronized long getVerifiedBlockSize() {
		BTMetaInfo info = context.getMetaInfo();
		long ret = verified.cardinality() * (long)info.getPieceLength();
		if (verified.get(info.getNumBlocks() - 1)) {
			ret = ret - info.getPieceLength() + 
				getPieceSize(info.getNumBlocks() -1 );
		}
		return ret;
	}
	
	/**
	 * @return number of bytes written
	 */
	public synchronized long getBlockSize() {
		long written = getVerifiedBlockSize();
		return written + partialBlocks.byteSize();
	}

	/**
	 * @return number of bytes corrupted
	 */
	public synchronized long getNumCorruptedBytes() {
		return _corruptedBytes;
	}
	
	public synchronized Serializable getSerializableObject() {
        return new SerialData((BitSet)verifiedBlocks.clone(), 
        		partialBlocks.clone(), 
        		isVerifying);
    }
    
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.TorrentFileManager#getAmountPending()
	 */
	public synchronized int getAmountPending() {
		return (int)pendingRanges.byteSize();
	}
	
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.TorrentFileManager#getNumMissing(com.limegroup.gnutella.util.BitField)
	 */
	public synchronized int getNumMissing(BitField other) {
		if (isComplete())
			return verified.cardinality() - other.cardinality();
		
		BitField theirMissing = new NotView(other);
		return (new AndView(verified, theirMissing)).cardinality();
	}
	
	/**
	 * @return true if the remote host has any pieces we miss
	 */
	public synchronized boolean containsAnyWeMiss(BitField other) {
		// if we are complete we miss nothing
		if (isComplete())
			return false;
		
		BitField interesting = new AndView(other,missing);
		return interesting.nextSetBit(0) > -1;
	}
	
	private static class BlockRangeMap extends HashMap<Integer, IntervalSet> {
		
		private static final long serialVersionUID = 4006274480019024111L;

		BlockRangeMap() {
			super();
		}
		
		private BlockRangeMap(int size) {
			super(size);
		}
		
		public void addInterval(BTInterval in) {
			IntervalSet s = get(in.getBlockId());
			if (s == null) {
				s = new IntervalSet();
				put(in.getBlockId(),s);
			}
			s.add(in);
		}
		
		public void removeInterval(BTInterval in) {
			IntervalSet s = get(in.getBlockId());
			if (s == null)
				return;
			s.delete(in);
			if (s.isEmpty())
				remove(in.getBlockId());
		}
		
		public long byteSize() {
			long ret = 0;
			for (IntervalSet set : values()) 
				ret += set.getSize();
			return ret;
		}
		
		public BlockRangeMap clone() {
			BlockRangeMap clone = new BlockRangeMap(size());
			for (Map.Entry<Integer, IntervalSet> e : entrySet())
				clone.put(e.getKey(), e.getValue().clone());
			return clone;
		}
	}
    
    /** Data that's serialized. */
    private static class SerialData implements Serializable {
    	private static final long serialVersionUID = -6901065516261232111l;
        private final BitSet verifiedBlocks;
        private final BlockRangeMap partialBlocks;
        private final boolean isVerifying;
        
        private SerialData(BitSet verifiedBlocks, 
        		BlockRangeMap partialBlocks, 
        		boolean isVerifying) {
            this.verifiedBlocks = verifiedBlocks;
            this.partialBlocks = partialBlocks;
            this.isVerifying = isVerifying;
        }

        public boolean isVerifying() {
            return isVerifying;
        }

        public BlockRangeMap getPartialBlocks() {
            return partialBlocks;
        }

        public BitSet getVerifiedBlocks() {
            return verifiedBlocks;
        }
    }
}
