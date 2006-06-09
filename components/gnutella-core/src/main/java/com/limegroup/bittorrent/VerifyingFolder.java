package com.limegroup.bittorrent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.Interval;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.util.FileUtils;
import com.limegroup.gnutella.util.IntervalSet;
import com.limegroup.gnutella.util.MultiIterator;
import com.limegroup.gnutella.util.BitSet;
import com.limegroup.gnutella.util.RRProcessingQueue;
import com.limegroup.gnutella.util.SystemUtils;

/**
 * This class extends VerifyingFile for the simple reason that I would like to
 * add a feature to download from BitTorrent and Gnutella, possibly even at the
 * same time. Other than that it saves a couple of lines of code and wastes a
 * little memory on two empty IntervalSets and two or three unused class members
 * of the VerifyingFile class.
 */
public class VerifyingFolder {
	
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
	
	/*
	 * The files of this torrent as an array
	 */
	private final TorrentFile[] _files;

	/**
	 * Object to lock on during all disk operations.
	 */
	private final Object DISK_LOCK = new Object();
	
	/**
	 * The instances RandomAccessFile for all files contained in this torrent
	 * LOCKING: this reference as well as the elements of the array - DISK_LOCK
	 */
	private RandomAccessFile[] _fos = null;

	/**
	 * Mapping of the index of each partial block and the written ranges within 
	 */
	private BlockRangeMap partialBlocks;
	
	/**
	 * Mapping of the index of each requested block and the requested ranges within 
	 */
	private BlockRangeMap requestedRanges;
	
	/**
	 * Mapping of the index of each block pending write and 
	 * pending ranges within 
	 */
	private BlockRangeMap pendingRanges;
	
	/**
	 * A BitSet for all blocks that are verified.
	 */
	private BitSet verifiedBlocks;
	
	/** a cached bitfield. LOCKING: this*/
	private byte [] bitField;
	/** whether the cached bitfield is dirty LOCKING: this */
	private boolean bitFieldDirty = true;

	/*
	 * the number of corrupted bytes, encountered
	 */
	private long _corruptedBytes;

	private final BTMetaInfo _info;
	
	/**
	 * an exception indicating Disk operation failed.
	 * a nice feature would be to have a separate
	 * exception for each file so that multi-file torrents would 
	 * not fail if an operation on one of the files throws.
	 */
	private volatile IOException storedException;
	
	/**
	 * The <tt>ManagedTorrent</tt> this folder belongs to.
	 */
	private volatile ManagedTorrent torrent;
	
	/** Whether the files on disk are currently being verified */
	private volatile boolean isVerifying;

	/**
	 * constructs instance of this
	 * 
	 * @param info a BTMetaInfo for which to create this Folder.
	 * @param complete if the download is completed
	 */
	public VerifyingFolder(BTMetaInfo info, boolean complete, Map data) {
		_files = info.getFiles();
		_info = info;
		_corruptedBytes = 0;
		partialBlocks = new BlockRangeMap();
		requestedRanges = new BlockRangeMap();
		pendingRanges = new BlockRangeMap();
		
		if (complete) 
			verifiedBlocks = _info.getFullBitSet();
		else {
			verifiedBlocks = new BitSet(_info.getNumBlocks());
			if (data != null)
				initialize(data);
		}
	}
	
	/**
	 * populates various fields from data object that was 
	 * deserialized.
	 */
	private void initialize(Map data) {
		BlockRangeMap partial = (BlockRangeMap) data.get("partial");
		if (partial != null)
			partialBlocks = partial; 
		
		BitSet verified = (BitSet) data.get("verified");
		if (verified != null) 
			verifiedBlocks = verified;
		
		Boolean wasVerifying = (Boolean) data.get("wasVerifying");
		isVerifying = wasVerifying == null ? false : wasVerifying.booleanValue();
	}
	
	
	public void writeBlock(BTInterval in, byte [] data) 
	throws IOException {
		IOException stored = storedException;
		if (stored != null)
			throw stored;
		synchronized(this) {
			pendingRanges.addInterval(in);
			requestedRanges.removeInterval(in);
		}
		QUEUE.invokeLater(new WriteJob(in, data),_info.getURN());
	}
	
	/**
	 * This duplicates lots of code from VerifyingFile..
	 * the two should eventually be abstracted somehow.
	 */
	private class WriteJob implements Runnable {
		private final BTInterval in;
		private final byte [] data;
		
		WriteJob(BTInterval in, byte [] data) {
			this.in = in;
			this.data = data;
		}
		
		public void run() {
			if (storedException != null)
				return;
			
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
		// is the chunk verified?
		if (hasBlock(in.getId()))
			return;
		
		synchronized(DISK_LOCK) {
			if (!isOpen())
				throw new IOException("file closed");
			long startOffset = (long)in.getId() * _info.getPieceLength() + in.low;
			int written = 0;
			for (int i = 0; i < _files.length && written < buf.length; i++) {
				if (startOffset < _files[i].LENGTH) {
					_fos[i].seek(startOffset);
					int toWrite = (int) Math.min(_files[i].LENGTH - startOffset,
							buf.length - written);
					
					if (_fos[i].length() < startOffset + toWrite)
						_fos[i].setLength(startOffset + toWrite);
					
					_fos[i].write(buf, written, toWrite);
					startOffset += toWrite;
					written += toWrite;
				} 
				startOffset -= _files[i].LENGTH;
			}
		}
		
		boolean shouldVerify;
		synchronized(this) {
			pendingRanges.removeInterval(in);
			shouldVerify = addBlockPart(in);
			if (shouldVerify)
				partialBlocks.remove(in.blockId);
		}
		if (shouldVerify){
			boolean verified = false;
			try {
				verified = verify(in.getId(), false);
			} catch (InterruptedException impossible) {
				ErrorService.error(impossible);
			}
			synchronized(this) {
				if (verified) {
					markPieceCompleted(in.getId());
				} else 
					_corruptedBytes += getPieceSize(in.getId());
			}
			if (verified)
				handleVerified(in.getId());
		}
	}
	
	private synchronized void markPieceCompleted(int blockId) {
		verifiedBlocks.set(blockId);
		bitFieldDirty = true;
		if (isComplete()) 
			verifiedBlocks = _info.getFullBitSet();
	}
	
	/**
	 * @param pieceNum the piece to verify
	 * @param slow whether to not max out cpu
	 * @return if the block at pieceNum is ok.
	 */
	private boolean verify(int pieceNum, boolean slow) 
	throws IOException, InterruptedException {
		MessageDigest md = _info.getMessageDigest();
		md.reset();
		int pieceSize = getPieceSize(pieceNum);
		byte [] buf = new byte[Math.min(65536,pieceSize)];
		int read = 0;
		long offset = (long)pieceNum * _info.getPieceLength();
		while (read < pieceSize) {
			
			int readNow = read(offset, buf, 0, buf.length);
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
		return Arrays.equals(sha1, _info.getHash(pieceNum));
	}
	
	/**
	 * performs various tasks after a block has been verified
	 * such as notifying any listeners or closing files.
	 */
	private void handleVerified(int pieceNum) {
		closeAnyCompletedFiles(pieceNum);
		notifyOfChunkCompletion(pieceNum);
	}
	
	private void notifyOfChunkCompletion(int pieceNum) {
		ManagedTorrent t = torrent;
		if (t != null)
			t.notifyOfComplete(pieceNum);
	}
	
	private void closeAnyCompletedFiles(int pieceNum) {
		List possiblyCompleted = null;
		for (int i = 0; i < _files.length; i++) {
			if (_files[i].begin > pieceNum)
				continue;
			if (_files[i].end < pieceNum)
				break;
			
			if (possiblyCompleted == null)
				possiblyCompleted = new ArrayList();
			
			possiblyCompleted.add(new Integer(i));
		}
		
		if (possiblyCompleted == null)
			return;
		
		for (Iterator iter = possiblyCompleted.iterator(); iter.hasNext();) {
			int index = ((Integer) iter.next()).intValue();
			TorrentFile file = _files[index];
			boolean done = true;
			for(int i = file.begin; i <= file.end; i++) {
				if (!hasBlock(i)) {
					done = false;
					break;
				}
			}
			if (done) {
				// TODO: decide if files should be moved to the save
				// location as they are completed.. cool but not trivial
				try {
					synchronized(DISK_LOCK) {
						if (isOpen()) {
							_fos[index].close();
							_fos[index] = new RandomAccessFile(file.PATH, "r");
						}
					}
				} catch (FileNotFoundException bs) {
					ErrorService.error(bs);
				} catch (IOException ignored){}
			}
		}
	}
	
	/**
	 * Notifies that part of a block has been written to disk.
	 * 
	 * @return true if the block is now complete and ready to be verified.
	 */
	private boolean addBlockPart(BTInterval in) {
		// shortcut
		if (isCompleteBlock(in, in.getId()))
			return true;
		
		partialBlocks.addInterval(in);
		IntervalSet set = partialBlocks.getSet(in);
		if (set.getNumberOfIntervals() == 1) {
			Interval one = set.getFirst();
			if (isCompleteBlock(one,in.getId()))
				return true;
		}
		
		return false;
	}
	
	private boolean isCompleteBlock(Interval in, int id) {
		return in.low == 0 && in.high == getPieceSize(id) - 1;
	}
	
	public synchronized boolean hasBlock(int block) {
		return verifiedBlocks.get(block);
	}

	/**
	 * Opens this VerifyingFolder for writing. MUST be called before anything
	 * else. If there is no completion size, this fails.
	 */
	public void open(final ManagedTorrent torrent) throws IOException {
		synchronized(DISK_LOCK) {
			if (_fos != null)
				throw new IOException("Files already open!");
			
			_fos = new RandomAccessFile[_files.length];
		}
		
		this.torrent = torrent;
		storedException = null;
		
		// whether the data on disk should be re-verified
		isVerifying |= (getBlockSize() == 0);
		
		// position of the first byte of a file in the torrent
		long pos = 0;
		
		List filesToVerify = null;
		for (int i = 0; i < _files.length; i++) {
			File file = new File(_files[i].PATH);

			// if the file is complete, just open it for reading and be done
			// with it
			if (isComplete()) {
				LOG.info("opening torrent in read-only mode");
				synchronized(DISK_LOCK) {
					_fos[i] = new RandomAccessFile(file, "r");
				}
			} else {
				LOG.info("opening torrent in read-write");
				if (!file.exists()) {

					// Ensure that the directory this file is in exists & is
					// writeable.
					// TODO: make sure this doesn't allow trickery with names
					File parentFile = file.getParentFile();
					if (parentFile != null) {
						parentFile.mkdirs();
						FileUtils.setWriteable(parentFile);
					}
					
					file.createNewFile();
					
					// a file is missing, so this must be a new download.
					// if it was not, we need to reverify every file.
					if (!isVerifying) {
						// pretend nothing was downloaded
						synchronized(this) {
							partialBlocks.clear(); 
							verifiedBlocks.clear();
						}
						isVerifying = true;
						i = -1; // restart the loop
						continue;
					}

				} 
				
				FileUtils.setWriteable(file);
				synchronized(DISK_LOCK) {
					_fos[i] = new RandomAccessFile(file, "rw");
				}
				
				// if a file exists, try to verify it
				if (isVerifying && file.length() > 0) {
					if (filesToVerify == null)
						filesToVerify = new ArrayList(_files.length);
					filesToVerify.add(_files[i]);
				}
			}

			// increment pos to point to the first byte of the next file
			pos += _files[i].LENGTH;
		}
		
		// verify any files that needed verification
		if (filesToVerify != null) {
			isVerifying = true;
			verifyFiles(filesToVerify);
			if (torrent != null) {
				VERIFY_QUEUE.invokeLater(new Runnable(){
					public void run() {
						if (isOpen()) {
							isVerifying = false;
							torrent.verificationComplete();
						}
					}
				}, _info.getURN());
			}
		} else
			isVerifying = false;
	}
	
	boolean isVerifying() {
		return isVerifying;
	}

	/**
	 * @return true if the whole torrent has been written and verified
	 */
	synchronized boolean isComplete() {
		return verifiedBlocks.cardinality() == _info.getNumBlocks();
	}

	/**
	 * closes all internal RandomAccessFile objects
	 */
	public void close() {
		LOG.debug("closing the file");
		synchronized(DISK_LOCK) {
			if (_fos == null)
				return;
			for (int i = 0; i < _fos.length; i++) {
				try {
					if (_fos[i] != null)
						_fos[i].close();
				} catch (IOException ioe) {
					// ignored
				}
			}
			_fos = null;
			pendingRanges.clear();
		}
		torrent = null;
		// kill all jobs for this torrent
		VERIFY_QUEUE.clear(_info.getURN());
		QUEUE.clear(_info.getURN());
	}

	/**
	 * determines whether the files for this torrent are open.
	 */
	public boolean isOpen() {
		synchronized(DISK_LOCK) {
			return _fos != null;
		}
	}

	public void sendPiece(BTInterval in, BTConnection c) throws IOException {
		IOException e = storedException;
		if (e != null)
			throw e;
		QUEUE.invokeLater(new SendJob(in, c),_info.getURN());
	}
	
	private class SendJob implements Runnable {
		private final BTInterval in;
		private final BTConnection c;
		SendJob(BTInterval in, BTConnection c) {
			this.in = in;
			this.c = c;
		}
		
		public void run() {
			if (!isOpen())
				return;
			if (storedException != null)
				return;
			
			if (LOG.isDebugEnabled())
				LOG.debug("sending piece " + in);
			int length = in.high - in.low + 1;
			long position = (long)in.getId() * _info.getPieceLength() + in.low;
			int offset = 0;
			byte[] buf = new byte[length];
			try {
				do {
					offset += read(position + offset, buf, offset, length
							- offset);
				} while (offset < length);
			} catch (IOException bad) {
				if (isOpen()) {
					storedException = bad;
					notifyDiskProblem();
				}
			}
			
			c.pieceRead(in, buf);
		}
	}
	
	private void notifyDiskProblem() {
		ManagedTorrent t = torrent;
		if (t != null)
			t.diskExceptionHappened();
	}
	/**
	 * reads a number of bytes from a torrent must obtain monitor 
	 * before reading if you want to make sure the VerifyingFolder can't be
	 * closed while you are trying to read
	 * 
	 * @param position
	 *            the position in the file where to start reading
	 * @param buf
	 *            the array to write the read bytes to
	 * @param offset
	 *            the offset in the array where to start storing the bytes read
	 * @param length
	 *            the number of bytes to read to the array
	 * @return
	 * @throws IOException
	 */
	private int read(long position, byte[] buf, int offset,
			int length) throws IOException {
		
		if (position < 0)
			throw new IllegalArgumentException("cannot seek negative position "+position);
		else if (offset + length > buf.length)
			throw new ArrayIndexOutOfBoundsException(
					"buffer to small to store supplied number of bytes");
		
		int read = 0;
		synchronized(DISK_LOCK) {
			if (!isOpen())
				throw new IOException("file closed");
			for (int i = 0; i < _files.length && read < length; i++) {
				while (position < _files[i].LENGTH && read < length) {
					if (_fos[i].length() < _files[i].LENGTH && position >= _fos[i].length())
						return read;
					int toRead = (int) Math.min(_fos[i].length() - position, length
							- read);
					_fos[i].seek(position);
					int t_read = _fos[i].read(buf, read + offset, toRead);
					if (t_read == -1)
						throw new IOException();
					position += t_read;
					read += t_read;
				}
				position -= _files[i].LENGTH;
			}
		}
		return read;
	}

	/**
	 * returns a random available range that has preferrably not yet been
	 * requested
	 * 
	 * @param bbis
	 *            the BitBasedIntervalSet of available ranges
	 * @return an array with the chunk id as first element, 
	 * offset and size within that chunk as second and third elements.
	 */
	public synchronized BTInterval leaseRandom(BitSet bs) {
		if (isComplete())
			return null;
		
		if (LOG.isDebugEnabled())
			LOG.debug("leasing random chunk from available cardinality "+bs.cardinality());
		
		// see which pieces we don't have
		BitSet remote;
		if (bs.cardinality() == _info.getNumBlocks()) {
			remote = (BitSet) verifiedBlocks.clone();
			remote.flip(0, _info.getNumBlocks());
		} else {
			remote = (BitSet) bs.clone();
			remote.andNot(verifiedBlocks);
		}
		
		// if possible, do not request any chunks which are currently
		// being requested
		MultiIterator iter = new MultiIterator(new Iterator[]{
				pendingRanges.keySet().iterator(),
				requestedRanges.keySet().iterator(),
				partialBlocks.keySet().iterator()
		});
		while(iter.hasNext()) {
			Integer element = (Integer) iter.next();
			remote.clear(element.intValue());
		}
		
		if (remote.cardinality() > 0) {
			// the remote has new chunks we can get
			int selected = -1;
			int current = 1;
			for (int i = remote.nextSetBit(0); i >= 0; i = remote.nextSetBit(i+1)) {
				if (Math.random() < 1f/current++)
					selected = i;
			}
			
			if (LOG.isDebugEnabled())
				LOG.debug("selecting piece "+selected);
			
			BTInterval ret = new BTInterval(0,getPieceSize(selected) - 1,selected);
			requestedRanges.addInterval(ret);
			return ret;
		} 
		
		// prepare a list of partial or requested blocks the remote host has
		Collection available = new HashSet(requestedRanges.size()+partialBlocks.size());
		available.addAll(requestedRanges.keySet());
		available.addAll(partialBlocks.keySet());
		for (Iterator iterator = available.iterator(); iterator.hasNext();) {
			Integer block = (Integer) iterator.next();
			// if the other side doesn't have this block, its not an option
			if (!bs.get(block.intValue()) || verifiedBlocks.get(block.intValue())) 
				iterator.remove();
		}
		
		if (LOG.isDebugEnabled())
			LOG.debug("available partial blocks to attempt: "+available);
		
		available = new ArrayList(available);
		Collections.shuffle((List)available);
		
		// go through and find a block that we can request something from.
		for (Iterator iterator = available.iterator(); iterator.hasNext();) {
			Integer block = (Integer) iterator.next();
			
			// figure out which parts of the chunks we need.
			IntervalSet needed = new IntervalSet();
			needed = needed.invert(getPieceSize(block.intValue()));
			
			IntervalSet partial = partialBlocks.getSet(block);
			IntervalSet pending = pendingRanges.getSet(block);
			IntervalSet requested = requestedRanges.getSet(block);
			
			// get the parts of the block we're missing
			if (partial != null)
				needed.delete(partial);
			
			// don't request any parts pending write
			if (pending != null)
				needed.delete(pending);
			
			// try not to request any parts that are already requested
			if (requested != null) {
				needed.delete(requested);
				
				// now, if we still have some parts of the chunk, get one of them
				// if not and this is the last partial chunk, doubly-assign some
				// part of it (is this endgame?)
				if (needed.isEmpty() && !iterator.hasNext()) 
					needed = requested;
			}
			
			if (needed.isEmpty()) 
				continue;
			
			BTInterval ret = new BTInterval(needed.getFirst(),block.intValue());
			if (LOG.isDebugEnabled())
				LOG.debug("selected partial/requested interval "+ret);
			return ret;
		}
		
		// couldn't find anything to assign.
		return null;
	}
	
	/**
	 * @return the size of the piece with given number.  All pieces
	 * except the last one have the same size.
	 */
	private int getPieceSize(int pieceNum) {
		if (pieceNum == _info.getNumBlocks() - 1)
			return (int)(_info.getTotalSize() % _info.getPieceLength());
		else
			return _info.getPieceLength();
	}
	
	/**
	 * removes chunk from the internal list of already requested chunks
	 * 
	 * @param pieceNum
	 */
	public synchronized void releaseChunk(int pieceNum) {
		requestedRanges.remove(new Integer(pieceNum));
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
			bitField = new byte[(_info.getNumBlocks() + 7) / 8];
		
		if (bitFieldDirty) {
			if (isComplete()) {
				for(int i = 0; i < bitField.length; i++)
					bitField[i] = (byte)0xFF;
			} else {
				for(int i = verifiedBlocks.nextSetBit(0); i >= 0; i = verifiedBlocks.nextSetBit(i+1)) 
					bitField[i / 8] = (byte) (bitField[i / 8] | (1 << (7 - i % 8)));
			}
			bitFieldDirty = false;
		}
			
		return bitField;
	}

	/**
	 * verifies all the chunks that are associated with a list of files.
	 */
	private void verifyFiles(List l) {
		int lastSet;
		synchronized(this) {
			lastSet = verifiedBlocks.length();
		}
		for (Iterator iter = l.iterator(); iter.hasNext();) {
			TorrentFile f = (TorrentFile) iter.next();
			for (int i = Math.max(lastSet,f.begin); i <= f.end; i++) 
				VERIFY_QUEUE.invokeLater(new VerifyJob(i),_info.getURN());
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
				}
				if (SystemUtils.getIdleTime() < URN.MIN_IDLE_TIME &&
						SharingSettings.FRIENDLY_HASHING.getValue()) {
					
				}
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
	synchronized long getVerifiedBlockSize() {
		long ret = verifiedBlocks.cardinality() * (long)_info.getPieceLength();
		if (verifiedBlocks.get(_info.getNumBlocks() - 1)) {
			ret = ret - _info.getPieceLength() + 
				getPieceSize(_info.getNumBlocks() -1 );
		}
		return ret;
	}
	
	/**
	 * @return number of bytes written
	 */
	synchronized long getBlockSize() {
		long written = getVerifiedBlockSize();
		return written + partialBlocks.byteSize();
	}

	/**
	 * @return number of bytes corrupted
	 */
	synchronized long getNumCorruptedBytes() {
		return _corruptedBytes;
	}
	
	synchronized Map getSerializableObject() {
		Map toWrite = new HashMap();
		toWrite.put("verified",verifiedBlocks.clone());
		toWrite.put("partial",partialBlocks.clone());
		toWrite.put("wasVerifying", new Boolean(isVerifying));
		return toWrite;
	}
	
	synchronized int getAmountPending() {
		return (int)pendingRanges.byteSize();
	}
	
	/**
	 * @return the number of pieces that we have verified
	 * and the other host doesn't have
	 */
	synchronized int getNumMissing(BitSet other) {
		if (isComplete())
			return verifiedBlocks.cardinality() - other.cardinality();
		BitSet clone = (BitSet)verifiedBlocks.clone();
		clone.andNot(other);
		return clone.cardinality();
	}
	
	/**
	 * @return the # of pieces that we do not have that
	 * the other host has
	 */
	synchronized boolean containsAnyWeMiss(BitSet other) {
		// if we are complete we miss nothing
		if (isComplete())
			return false;
		
		// if they are complete we miss what we don't have
		if (other == _info.getFullBitSet())
			return verifiedBlocks.cardinality() < _info.getNumBlocks();
		
	    for(int i=other.nextSetBit(0); i>=0; i=other.nextSetBit(i+1)) {
	    	if (!verifiedBlocks.get(i))
	    		return true;
	    }
		return false;
	}
	
	private static class BlockRangeMap extends HashMap {
		public void addInterval(BTInterval in) {
			IntervalSet s = (IntervalSet) get(in.blockId);
			if (s == null) {
				s = new IntervalSet();
				put(in.blockId,s);
			}
			s.add(in);
		}
		
		public void removeInterval(BTInterval in) {
			IntervalSet s = (IntervalSet) get(in.blockId);
			if (s == null)
				return;
			s.delete(in);
			if (s.isEmpty())
				remove(in.blockId);
		}
		
		public IntervalSet getSet(BTInterval in) {
			return (IntervalSet) get(in.blockId);
		}
		
		public IntervalSet getSet(Integer id) {
			return (IntervalSet) get(id);
		}
		
		public long byteSize() {
			long ret = 0;
			for (Iterator iter = values().iterator(); iter.hasNext();) {
				IntervalSet set = (IntervalSet) iter.next();
				ret += (long)set.getSize();
			}
			return ret;
		}
	}
}
