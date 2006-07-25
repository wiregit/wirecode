package com.limegroup.bittorrent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
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

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.Interval;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.util.AndView;
import com.limegroup.gnutella.util.BitField;
import com.limegroup.gnutella.util.BitFieldSet;
import com.limegroup.gnutella.util.FileUtils;
import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.util.IntervalSet;
import com.limegroup.gnutella.util.MultiIterable;
import com.limegroup.gnutella.util.BitSet;
import com.limegroup.gnutella.util.NotView;
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
	 * The instances RandomAccessFile for all files contained in this torrent
	 * LOCKING: this reference as well as the elements of the array 
	 */
	private RandomAccessFile[] _fos = null;
	
	/**
	 * a marker that the current file is currently opening.
	 */
	private static final RandomAccessFile[] OPENING = new RandomAccessFile[0];

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
	
	/** A view over which blocks we are missing - the inverse of verifiedBlocks */
	private BitField verified, missing;
	
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
		TorrentFileSystem system = info.getFileSystem();
		_files = complete? system.getFiles() : system.getIncompleteFiles();
		_info = info;
		_corruptedBytes = 0;
		partialBlocks = new BlockRangeMap();
		requestedRanges = new BlockRangeMap();
		pendingRanges = new BlockRangeMap();
		
		requestedAndPartial = 
			new MultiIterable<Integer>(partialBlocks.keySet(), 
					requestedRanges.keySet());
		
		if (complete) {
			verifiedBlocks = _info.getFullBitSet();
			verified = _info.getFullBitField();
		}else {
			verifiedBlocks = new BitSet(_info.getNumBlocks());
			if (data != null)
				initialize(data);
			verified = new BitFieldSet(verifiedBlocks, _info.getNumBlocks());
		}
		
		missing = new NotView(verified);
	}
	
	/**
	 * populates various fields from data object that was 
	 * deserialized.
	 */
	private void initialize(Map<String, Serializable> data) {
		BlockRangeMap partial = (BlockRangeMap) data.get("partial");
		if (partial != null) 
			partialBlocks.putAll(partial);
		
		BitSet verified = (BitSet) data.get("verified");
		if (verified != null) 
			verifiedBlocks = verified;
		
		Boolean wasVerifying = (Boolean) data.get("wasVerifying");
		isVerifying = wasVerifying == null ? false : wasVerifying.booleanValue();
	}
	
	
	public void writeBlock(BTPieceFactory factory) 
	throws IOException {
		IOException stored = storedException;
		if (stored != null)
			throw stored;
		QUEUE.invokeLater(new WriteJob(factory),_info.getURN());
	}
	
	/**
	 * This duplicates lots of code from VerifyingFile..
	 * the two should eventually be abstracted somehow.
	 */
	private class WriteJob implements Runnable {
		private final BTPieceFactory factory;
		
		WriteJob(BTPieceFactory factory) {
			this.factory = factory;
		}
		
		public void run() {
			if (storedException != null)
				return;
			
			BTPiece piece = factory.getPiece();
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
		
		long startOffset = (long)in.getId() * _info.getPieceLength() + in.low;
		int written = 0;
		for (int i = 0; i < _files.size() && written < buf.length; i++) {
			if (startOffset < _files.get(i).length()) {
				RandomAccessFile currentFile;
				synchronized(this) {
					if (!isOpen())
						throw new IOException("file closed");
					currentFile = _fos[i];
				}
				currentFile.seek(startOffset);
				int toWrite = (int) Math.min(_files.get(i).length()- startOffset,
						buf.length - written);
				
				if (currentFile.length() < startOffset + toWrite)
					currentFile.setLength(startOffset + toWrite);
				
				currentFile.write(buf, written, toWrite);
				startOffset += toWrite;
				written += toWrite;
			} 
			startOffset -= _files.get(i).length();
		}
		
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
		requestedRanges.remove(blockId);
		verifiedBlocks.set(blockId);
		bitFieldDirty = true;
		if (verifiedBlocks.cardinality() == _info.getNumBlocks()) {
			verifiedBlocks = _info.getFullBitSet();
			verified = _info.getFullBitField();
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
		MessageDigest md = _info.getMessageDigest();
		md.reset();
		int pieceSize = getPieceSize(pieceNum);
		byte [] buf = new byte[Math.min(65536,pieceSize)];
		int read = 0;
		long offset = (long)pieceNum * _info.getPieceLength();
		while (read < pieceSize) {
			
			int readNow = read(offset, buf, 0, buf.length, true);
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
		return _info.verify(sha1, pieceNum);
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
		List<TorrentFile> possiblyCompleted = null;
		for (TorrentFile t : _files) {
			if (t.begin > pieceNum)
				continue;
			if (t.end < pieceNum)
				break;
			
			if (possiblyCompleted == null)
				possiblyCompleted = new ArrayList<TorrentFile>();
			
			possiblyCompleted.add(t);
		}
		
		if (possiblyCompleted == null)
			return;
		
		for (TorrentFile file : possiblyCompleted) {
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
					int index = _files.indexOf(file);
					RandomAccessFile rf;
					synchronized(this) {
						if (!isOpen()) 
							return;
						rf = _fos[index];
						_fos[index] = null;
					}
					rf.close();
					rf = new RandomAccessFile(file.getPath(), "r");
					synchronized(this) {
						if(!isOpen())
							return;
						_fos[index] = rf;
					}
				} catch (FileNotFoundException bs) {
					ErrorService.error(bs);
				} catch (IOException ignored){}
			}
		}
	}
	
	private boolean isCompleteBlock(Interval in, int id) {
		return in.low == 0 && in.high == getPieceSize(id) - 1;
	}
	
	public synchronized boolean hasBlock(int block) {
		return verified.get(block);
	}

	/**
	 * Opens this VerifyingFolder for writing. MUST be called before anything
	 * else. If there is no completion size, this fails.
	 */
	public void open(final ManagedTorrent torrent) throws IOException {
		synchronized(this) {
			if (_fos != null)
				throw new IOException("Files already open(ing)!");
			_fos = OPENING;
		}
		RandomAccessFile []fos = new RandomAccessFile[_files.size()];
		
		this.torrent = torrent;
		storedException = null;
		
		// whether the data on disk should be re-verified
		isVerifying |= (getBlockSize() == 0);
		
		// position of the first byte of a file in the torrent
		long pos = 0;
		
		List<TorrentFile> filesToVerify = null;
		for (int i = 0; i < _files.size(); i++) {
			TorrentFile file = _files.get(i);

			// if the file is complete, just open it for reading and be done
			// with it
			if (isComplete()) {
				LOG.info("opening torrent in read-only mode");
				fos[i] = new RandomAccessFile(file, "r");
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
				fos[i] = new RandomAccessFile(file, "rw");
				
				// if a file exists, try to verify it
				if (isVerifying && file.length() > 0) {
					if (filesToVerify == null)
						filesToVerify = new ArrayList<TorrentFile>(_files.size());
					filesToVerify.add(file);
				}
			}

			// increment pos to point to the first byte of the next file
			pos += file.length();
		}
		
		synchronized(this) {
			Assert.that(_fos == OPENING);
			_fos = fos;
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
		return verified == _info.getFullBitField();
	}

	/**
	 * closes all internal RandomAccessFile objects
	 */
	public void close() {
		LOG.debug("closing the file");
		final RandomAccessFile [] fos;
		synchronized(this) {
			if (!isOpen())
				return;
			fos = _fos;
			_fos = null;
			pendingRanges.clear();
		}
		
		// close the files
		for (RandomAccessFile f : fos)
			IOUtils.close(f);
		
		torrent = null;
		// kill all jobs for this torrent
		VERIFY_QUEUE.clear(_info.getURN());
		QUEUE.clear(_info.getURN());
		
	}

	/**
	 * determines whether the files for this torrent are open.
	 */
	public synchronized boolean isOpen() {
		return _fos != null && _fos != OPENING;
	}

	public void requestPieceRead(BTInterval in, PieceReadListener c) throws IOException {
		IOException e = storedException;
		if (e != null)
			throw e;
		QUEUE.invokeLater(new SendJob(in, c),_info.getURN());
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
			long position = (long)in.getId() * _info.getPieceLength() + in.low;
			int offset = 0;
			byte[] buf = new byte[length];
			try {
				do {
					offset += read(position + offset, buf, offset, length
							- offset, false);
				} while (offset < length);
			} catch (IOException bad) {
				if (isOpen()) {
					storedException = bad;
					notifyDiskProblem();
				}
			}
			
			listener.pieceRead(in, buf);
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
	 * @param flush 
	 *            whether to flush any changes before reading
	 * @return
	 * @throws IOException
	 */
	private int read(long position, byte[] buf, int offset,
			int length, boolean flush) throws IOException {
		
		if (position < 0)
			throw new IllegalArgumentException("cannot seek negative position "+position);
		else if (offset + length > buf.length)
			throw new ArrayIndexOutOfBoundsException(
					"buffer to small to store supplied number of bytes");
		
		int read = 0;
		for (int i = 0; i < _files.size() && read < length; i++) {
			File f = _files.get(i);
			while (position < f.length() && read < length) {
				RandomAccessFile currentFile;
				synchronized(this) {
					if (!isOpen())
						throw new IOException("file closed");
					currentFile = _fos[i];
				}
				Assert.that(currentFile != null, "file being read & verified at the same time");
				if (flush)
					currentFile.getChannel().force(false);
				if (currentFile.length() < f.length() && position >= currentFile.length())
					return read;
				int toRead = (int) Math.min(currentFile.length() - position, length
						- read);
				currentFile.seek(position);
				int t_read = currentFile.read(buf, read + offset, toRead);
				if (t_read == -1)
					throw new IOException();
				position += t_read;
				read += t_read;
			}
			position -= f.length();
		}
		return read;
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
					needed = (IntervalSet)requested.clone();
					
					// exclude the specified intervals again
					for (Interval excluded : exclude) 
						needed.delete(excluded);
				}
			}
			
			if (needed.isEmpty()) 
				continue;
			
			ret = new BTInterval(needed.getFirst(),block);
			if (LOG.isDebugEnabled())
				LOG.debug("selected partial/requested interval "+ret);
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
		if (pieceNum == _info.getNumBlocks() - 1)
			return (int)(_info.getFileSystem().getTotalSize() % _info.getPieceLength());
		else
			return _info.getPieceLength();
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
			bitField = new byte[(_info.getNumBlocks() + 7) / 8];
		
		if (bitFieldDirty) {
			if (isComplete()) {
				for(int i = 0; i < bitField.length; i++)
					bitField[i] = (byte)0xFF;
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
		long ret = verified.cardinality() * (long)_info.getPieceLength();
		if (verified.get(_info.getNumBlocks() - 1)) {
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
		Map<String, Serializable> toWrite = new HashMap<String, Serializable>();
		toWrite.put("verified",(Serializable)verifiedBlocks.clone());
		toWrite.put("partial",(Serializable)partialBlocks.clone());
		toWrite.put("wasVerifying", new Boolean(isVerifying));
		return toWrite;
	}
	
	synchronized int getAmountPending() {
		return (int)pendingRanges.byteSize();
	}
	
	/**
	 * @return the number of pieces that we have verified
	 * and the other bitset doesn't have
	 */
	synchronized int getNumMissing(BitField other) {
		if (isComplete())
			return verified.cardinality() - other.cardinality();
		
		BitField theirMissing = new NotView(other);
		return (new AndView(verified, theirMissing)).cardinality();
	}
	
	/**
	 * @return true if the remote host has any pieces we miss
	 */
	synchronized boolean containsAnyWeMiss(BitField other) {
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
			IntervalSet s = get(in.blockId);
			if (s == null) {
				s = new IntervalSet();
				put(in.blockId,s);
			}
			s.add(in);
		}
		
		public void removeInterval(BTInterval in) {
			IntervalSet s = get(in.blockId);
			if (s == null)
				return;
			s.delete(in);
			if (s.isEmpty())
				remove(in.blockId);
		}
		
		public long byteSize() {
			long ret = 0;
			for (IntervalSet set : values()) 
				ret += (long)set.getSize();
			return ret;
		}
		
		public Object clone() {
			BlockRangeMap clone = new BlockRangeMap(size());
			for (Map.Entry<Integer, IntervalSet> e : entrySet())
				clone.put(e.getKey(), (IntervalSet)e.getValue().clone());
			return clone;
		}
	}
}
