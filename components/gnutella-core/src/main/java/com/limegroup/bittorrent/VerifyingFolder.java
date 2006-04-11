package com.limegroup.bittorrent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.downloader.Interval;
import com.limegroup.gnutella.util.FileUtils;
import com.limegroup.gnutella.util.IntervalSet;
import com.limegroup.gnutella.util.MultiIterator;
import com.limegroup.gnutella.util.ProcessingQueue;

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
	 * The thread that does the actual verification & writing
	 */
	private static final ProcessingQueue QUEUE = new ProcessingQueue(
			"TorrentVerifier");
	
	/*
	 * The files of this torrent as an array
	 */
	private final TorrentFile[] _files;

	/*
	 * The instances RandomAccessFile for all files contained in this torrent
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

	/*
	 * the number of corrupted bytes, encountered
	 */
	private long _corruptedBytes;

	private final BTMetaInfo _info;
	
	/**
	 * an exception indicating something failed.
	 */
	private volatile IOException storedException;

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
		verifiedBlocks = new BitSet(_info.getNumBlocks());
		requestedRanges = new BlockRangeMap();
		pendingRanges = new BlockRangeMap();
		
		if (complete) 
			verifiedBlocks.set(0,_info.getNumBlocks() - 1);
		else if (data != null)
			initialize(data);
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
	}
	
	
	public void writeBlock(BTInterval in, byte [] data) 
	throws IOException {
			IOException stored = storedException;
			if (stored != null)
				throw stored;
			synchronized(this) {
				requestedRanges.removeInterval(in);
				pendingRanges.addInterval(in);
			}
			QUEUE.add(new WriteJob(in, data));
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
			boolean freedPending = false;
			try {
				writeBlockImpl(in, data);
				freedPending = true;
			} catch (IOException iox) {
				storedException = iox;
			} finally {
				if (!freedPending) {
					synchronized(VerifyingFolder.this) {
						pendingRanges.removeInterval(in);
					}
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
	private synchronized void writeBlockImpl(BTInterval in, byte[] buf) 
	throws IOException {
		// is the chunk verified?
		if (hasBlock(in.getId()))
			return;
		
		long startOffset = (long)in.getId() * _info.getPieceLength() + in.low;
		int written = 0;
		for (int i = 0; i < _files.length && written < buf.length; i++) {
			if (startOffset < _files[i].LENGTH) {
				if (startOffset < 0) {
					System.out.println("startOffset:"+startOffset+" length:"+_files[i].LENGTH+" i:"+i+" interval ("+in+") written:"+written);
					System.exit(1);
				}
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
		
		pendingRanges.removeInterval(in);
		if (addBlockPart(in)){
			partialBlocks.remove(in.blockId);
			if (verify(in.getId())) {
				verifiedBlocks.set(in.getId());
				handleVerified(in.getId());
			} else 
				_corruptedBytes += buf.length;
		}
	}
	
	/**
	 * @return if the block at pieceNum is ok.
	 */
	private synchronized boolean verify(int pieceNum) throws IOException {
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
			md.update(buf, 0, readNow);
			read += readNow;
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
		_info.notifyOfComplete(pieceNum);
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
					_fos[index].close();
					_fos[index] = new RandomAccessFile(file.PATH, "r");
				} catch (FileNotFoundException bs) {
					ErrorService.error(bs);
				} catch (IOException ignored){}
			}
		}
	}
	
	/**
	 * Notifies that part of a block has been written to disk.
	 * 
	 * @param pieceNum the block number
	 * @param offset the offset within the block
	 * @param length the length of the written data
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
	public synchronized void open() throws IOException {
		if (_fos != null)
			throw new IOException("Files already open!");
		
		_fos = new RandomAccessFile[_files.length];
		
		// whether the data on disk should be re-verified
		boolean doVerification = this.getBlockSize() == 0;
		
		// position of the first byte of a file in the torrent
		long pos = 0;
		
		for (int i = 0; i < _files.length; i++) {
			File file = new File(_files[i].PATH);

			// if the file is complete, just open it for reading and be done
			// with it
			if (isComplete()) {
				_fos[i] = new RandomAccessFile(file, "r");
			} else {
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
					if (!doVerification) {
						partialBlocks.clear(); // pretend nothing was downloaded
						verifiedBlocks.clear();
						doVerification = true;
						i = -1; // restart the loop
						continue;
					}

				} 
				
				FileUtils.setWriteable(file);
				_fos[i] = new RandomAccessFile(file, "rw");
				
				// if a file exists, try to verify it
				if (doVerification && file.length() > 0) 
					verifyFile(_files[i]);
			}

			// increment pos to point to the first byte of the next file
			pos += _files[i].LENGTH;
		}
	}

	/**
	 * @return true if the whole torrent has been written and verified
	 */
	boolean isComplete() {
		return verifiedBlocks.cardinality() == _info.getNumBlocks();
	}

	/**
	 * closes all internal RandomAccessFile objects
	 */
	public synchronized void close() {
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
	}

	/**
	 * determines whether the files for this torrent are open.
	 */
	public synchronized boolean isOpen() {
		return _fos != null;
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
	public synchronized int read(long position, byte[] buf, int offset,
			int length) throws IOException {
		
		if (position < 0)
			throw new IllegalArgumentException("cannot seek negative position "+position);
		else if (offset + length > buf.length)
			throw new ArrayIndexOutOfBoundsException(
					"buffer to small to store supplied number of bytes");
		
		int read = 0;
		for (int i = 0; i < _files.length && read < length; i++) {
			while (position < _files[i].LENGTH && read < length) {
				if (_fos[i].length() < _files[i].LENGTH && position >= _fos[i].length())
					return read;
				int toRead = (int) Math.min(_fos[i].length() - position, length
						- read);
				_fos[i].seek(position);
				int t_read = _fos[i].read(buf, read + offset, toRead);
				if (t_read == -1)
					throw new IOException("read -1 with offset:"+offset+ " read:"+read+" toRead"+toRead+" position:"+position+" length"+_files[i].LENGTH+" i"+i+" physical length:"+_fos[i].length());
				position += t_read;
				read += t_read;
			}
			position -= _files[i].LENGTH;
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
		BitSet clone = (BitSet)bs.clone();
		clone.andNot(verifiedBlocks);
		
		// if possible, do not request any chunks which are currently
		// being requested
		MultiIterator iter = new MultiIterator(new Iterator[]{
				pendingRanges.keySet().iterator(),
				requestedRanges.keySet().iterator(),
				partialBlocks.keySet().iterator()
		});
		while(iter.hasNext()) {
			Integer element = (Integer) iter.next();
			clone.clear(element.intValue());
		}
		
		if (LOG.isDebugEnabled())
			LOG.debug("after removing pending, partial and requesting ranges, the remote has cardinality "+clone.cardinality());
		
		if (clone.cardinality() > 0) {
			// the remote has new chunks we can get
			int selected = -1;
			int current = 1;
			for (int i = clone.nextSetBit(0); i >= 0; i = clone.nextSetBit(i+1)) {
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
		Set available = new LinkedHashSet(partialBlocks.size() + requestedRanges.size());
		available.addAll(partialBlocks.keySet());
		available.addAll(requestedRanges.keySet());
		for (Iterator iterator = available.iterator(); iterator.hasNext();) {
			Integer block = (Integer) iterator.next();
			// if the other side doesn't have this block, its not an option
			if (!bs.get(block.intValue())) 
				iterator.remove();
		}
		
		if (LOG.isDebugEnabled())
			LOG.debug("available partial blocks to attempt: "+available);
		
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
				if (needed.isEmpty() && !iterator.hasNext()) {
					LOG.debug("requesting part of a block that is already requested...");
					needed = requested;
				}
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
	 * TODO: CACHE THIS!!!!
	 * 
	 * @return returns an array of byte where the i'th byte is 1 if we have
	 *         written and verified the i'th piece of the torrent and 0
	 *         otherwise
	 *         
	 */
	public synchronized byte[] createBitField() {
		byte[] field = new byte[(_info.getNumBlocks() + 7) / 8];
		for(int i = verifiedBlocks.nextSetBit(0); i >= 0; i = verifiedBlocks.nextSetBit(i+1)) 
			field[i / 8] = (byte) (field[i / 8] | (1 << (7 - i % 8)));
			
		return field;
	}

	/**
	 * verifies all the chunks that are associated with a file
	 * TODO: figure out which thread this should be happening in.
	 */
	private void verifyFile(TorrentFile f) {
		for (int i = f.begin; i <= f.end; i++)
			verifyDelayed(i);
	}
	
	/**
	 * this is a temp solution to ensure that verifying resumed torrents
	 * 1. doesn't max out the cpu
	 * 2. has less priority than verifying currently downloaded chunks
	 * a proper solution would eventually have explicitly coded priorities.
	 * This method is meant to be used within tight loops.
	 */
	private void verifyDelayed(int i ) {
		// 
		try {
			wait(QUEUE.size() > 0 ? 10 : 10);
		} catch (InterruptedException killed) {
			return;
		}
		QUEUE.add(new VerifyJob(i));
	}
	
	private class VerifyJob implements Runnable {
		
		private final int pieceNum;
		public VerifyJob(int pieceNum) {
			this.pieceNum = pieceNum;
		}
		
		public void run() {
			if (storedException != null)
				return;
			try {
				synchronized(VerifyingFolder.this) {
					if (verify(pieceNum)) { 
						verifiedBlocks.set(pieceNum);
						handleVerified(pieceNum);
					}
				}
			} catch (IOException bad) {
				storedException = bad;
			}
		}
	}

	/**
	 * @return number of bytes written and verified
	 */
	synchronized long getVerifiedBlockSize() {
		return verifiedBlocks.cardinality() * (long)_info.getPieceLength();
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
		toWrite.put("verified",verifiedBlocks);
		toWrite.put("partial",partialBlocks);
		return toWrite;
	}
	
	synchronized int getAmountPending() {
		return (int)pendingRanges.byteSize();
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
