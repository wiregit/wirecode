
// Commented for the Learning branch

package com.limegroup.bittorrent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
 * A VerifyingFolder object represents the folder we're downloading the files of a torrent to.
 * It saves them and checks that they are valid.
 * 
 * === Related objects ===
 * 
 * The object structure for a torrent looks like this:
 * 
 * ManagedTorrent - represents the torrent
 *  - BTMetaInfo - represents the .torrent file
 *  - VerifyingFolder - keeps the files of the torrent on the disk
 * 
 * Each ManagedTorrent has one BTMetaInfo object and one VerifyingFolder.
 * 
 * === Slow tasks and threads ===
 * 
 * Three things VerifyingFolder does are slow:
 * Saving data we've received to disk.
 * Hashing completed pieces.
 * Reading disk data to send it out.
 * 
 * To perform these tasks, VerifyingFolder has the nested classes WriteJob, SendJob, and VerifyJob.
 * A WriteJob writes data we've recieved to disk, and hashes a piece if it finishes one.
 * A SendJob reads torrent data from disk, puts it in a Piece message, and sends it to a connected peer.
 * A VerifyJob reads a piece from files already on the disk, and makes sure the hash is still good.
 * 
 * To keep the threads that perform these tasks, VerifyingFolder has RRProcessingQueue objects.
 * An RRProcessingQueue is an advanced form of ProcessingQueue.
 * A ProcessingQueue keeps a single list of Runnable objects, has a thread call run() on each one, and discards them.
 * An RRProcessingQueue has several named lists of Runnable objects.
 * It runs one from each list before returning to the first list again.
 * 
 * The RRProcessingQueue named QUEUE takes WriteJob and SendJob objects.
 * The RRProcessingQueue named VERIFY_QUEUE takes VerifyJob objects.
 * 
 * === Public methods ===
 * 
 * The open() method makes or opens all of the files of this torrent.
 * If we're done with the torrent, it will open all of the files for reading.
 * If we've just started downloading, it will make new empty files ready for writing.
 * Call close() to close all the files.
 * 
 * Call hasBlock(number) to find out if we have and can share a numbered piece.
 * Call writeBlock(location, data) to save torrent data we've received to disk.
 * Call sendPiece(location, data) to get torrent data from disk to send.
 * 
 * === Opening the files ===
 * 
 * Two methods in the program lead to the open() method here.
 * They are ManagedTorrent.saveFiles(), and ManagedTorrent.initializeFolder().
 * 
 * The open() method looks on the disk for all the files of this torrent, and opens them.
 * If we have the whole torrent, open() opens the files for reading to share them.
 * If we're just starting to download this torrent, open() creates new empty files in the right places to put data in.
 * 
 * If we open a torrent we downloaded a previous time the program ran, the files might not be the same.
 * open() calls verifyFiles(), which puts VerifyJob objects in the VERIFY_QUEUE.
 * The thread opens files to assemble each piece, hashes it, and marks it as done.
 * 
 * For a large finished torrent, this process could take minutes.
 * To see if it's going on, call isVerifying().
 * 
 * When it's done, an unnamed Runnable in open() calls torrent.verificationComplete().
 * Only at this time does the ManagedTorrent connects to remote computers to share this torrent.
 * 
 * === The bit field ===
 * 
 * BitTorrent programs tell each other what pieces they have with Bitfield messages.
 * The payload of a bit field message is an array of bytes.
 * Each byte has 8 bits.
 * Each bit represents a single piece.
 * If the bit is 0, that means the computer doesn't have, and needs that piece.
 * If the bit is 1, that means the computer has the piece, has checked its hash, and can share it.
 * 
 * VerifyingFolder has the code that generates our bit field for this torrent.
 * Call createBitField().
 * 
 * === Choosing what to ask for ===
 * 
 * BitTorrent programs need to choose what part of the torrent data to ask for next.
 * This important algorithm is located in leaseRandom().
 * Here's how it works:
 * 
 * First, it just looks at pieces that the remote computer has, and we have no parts of.
 * It randomly picks one towards the start of the file, and returns a BTInterval that clips around all of it.
 * 
 * If no such pieces exist, it looks into pieces that we have partially downloaded.
 * It finds the first missing portion of the first incomplete piece, and picks it.
 * 
 * The BitTorrent specification talks about programs sending out the rarest piece first.
 * Actually, most just pick pieces randomly, and research shows this is almost as efficient.
 * LimeWire's BitTorrent implementation here doesn't consider rarest at all, and picks requests randomly.
 * 
 * === Bit fields and striped patterns ===
 * 
 * Here are some of the types VerifyingFolder uses to express ranges of data in this torrent.
 * 
 * For the bit field, it uses BitSet.
 * A BitSet is a long list of bits.
 * You can read and set individual bits, count those that are set, and loop through them.
 * 
 * A LimeWire Interval object clips out a single range of data.
 * It has a low index and a high index that describe a single stripe.
 * 
 * An IntervalSet describes a striped pattern.
 * You can add and remove stripes, and it will merge touching stripes together.
 * 
 * A BTInterval object identifies a piece number, and a single stripe in that piece.
 * 
 * To describe the striped pattern across the entire torrent data, VerifyingFolder uses BlockRangeMap.
 * BlockRangeMap extends HashMap, giving it keys and values.
 * A key is a piece number.
 * The value is an IntervalSet object that describes the striped pattern in the piece.
 */
public class VerifyingFolder {

	/** A debugging log we can write lines of text to as the program runs. */
	private static final Log LOG = LogFactory.getLog(VerifyingFolder.class);

	/*
	 * A RRProcessingQueue looks like this:
	 * 
	 * Thread: "TorrentDiskQueue"
	 * Lists:  name1  name2  name3
	 *         -----  -----  -----
	 *         a      b      c
	 *         d             e
	 *         f
	 * 
	 * It keeps named list of Runnable objects.
	 * You can add a Runnable object to it with the call QUEUE.invokeLater(f, name1);
	 * The RRProcessingQueue's thread calls a.run() and discards a, then b.run() and discards b, and so on.
	 * When a list becomes empty, it removes it.
	 * When it runs out of objects, the thread stops.
	 * When you add another object, the thread starts up again.
	 */

	/**
	 * The RRProcessingQueue QUEUE has a thread named "TorrentDiskQueue" read and write from the disk, and hash pieces.
	 * 
	 * writeBlock() puts WriteJob objects in the QUEUE's "download" list.
	 * sendPiece() puts SendJob objects in the QUEUE's "upload" list.
	 */
	private static final RRProcessingQueue QUEUE = new RRProcessingQueue("TorrentDiskQueue");

	/**
	 * The RRProcessingQueue VERIFY_QUEUE has a thread named "TorrentVerifier" hash this torrent's data that is already saved on the hard drive.
	 * 
	 * verifyFiles() puts VerifyJob ojbects in the VERIFY_QUEUE's list named by this torrent's info hash.
	 * open() puts an unnamed objects in this VERIFY_QUEUE's list named by this torrent's info hash.
	 * 
	 * VERIFY_QUEUE is an RRProcessingQueue, but it only seems to be using one list of Runnable objects inside.
	 * The list is named by our torrent's info hash.
	 */
	private static final RRProcessingQueue VERIFY_QUEUE = new RRProcessingQueue("TorrentVerifier");

	/**
	 * A list of all the file names from the .torrent file.
	 * 
	 * _files is an array of TorrentFile objects.
	 * The VerifyingFolder makes them from reading the .torrent file.
	 * 
	 * Each TorrentFile object has the following information:
	 * The path in LimeWire's temporary folder where we'll save the file, like "C:\Documents and Settings\Kevin\Incomplete\File Name.ext".
	 * The file size.
	 * The number of the first piece that has data that is part of this file.
	 * The number of the last piece that has data that is part of this file.
	 */
	private final TorrentFile[] _files;

	/** Synchronize on DISK_LOCK while accessing files on the disk. */
	private final Object DISK_LOCK = new Object();

	/**
	 * An array of RandomAccessFile objects we can use to read and write from the files of this torrent on the disk.
	 * 
	 * _fos is an array of RandomAccessFile objects.
	 * open() makes one for each file.
	 * 
	 * Synchronize on DISK_LOCK before using this _fos reference or the any of the RandomAccessFile objects in the array.
	 */
	private RandomAccessFile[] _fos = null;

	/** The parts of this torrent we've written to disk, but can't verify yet, because they are in incomplete pieces. */
	private BlockRangeMap partialBlocks;

	/** The parts of this torrent we've asked peers to send us with Request messages. */
	private BlockRangeMap requestedRanges;

	/** The parts of this torrent we've received, and need to write to disk. */
	private BlockRangeMap pendingRanges;

	/** The pieces that we've saved and verified. */
	private BitSet verifiedBlocks;

	/**
	 * The bit field that shows which pieces of this torrent we have saved and hash verified, ready for sharing.
	 * A byte array with 8 bits in each byte.
	 * Each bit represents a piece.
	 * If the bit is 0, we need that piece.
	 * If the bit is 1, we have it and can share it.
	 * 
	 * The BitTorrent Bitfield message carries a bitfield like this one as its payload.
	 */
	private byte[] bitField;

	/**
	 * True when bitField matches the information in verifiedBlocks.
	 * 
	 * When markPieceCompleted() updates verifiedBlocks but not bitField, it sets bitFieldDirty to true.
	 * When createBitField() remakes bitField from verifiedBlocks, it sets bitFieldDirty to false.
	 */
	private boolean bitFieldDirty = true;

	/** The total size in bytes of the pieces we downloaded that then didn't hash correctly. */
	private long _corruptedBytes;

	/** A link to the BTMetaInfo object that represents the bencoded data inside the .torrent file. */
	private final BTMetaInfo _info;

	/** If a thread gets an excepton, it will save it as storedException for the main thread to pick up and throw. */
	private volatile IOException storedException;

	/**
	 * True while this VerifyingFolder object is checking data we found on the disk.
	 * 
	 * If the program downloaded torrent data the last time it run, we will find files already on the disk.
	 * We need to hash their data now to make sure they're still good before sending any pieces out.
	 */
	private volatile boolean isVerifying;

	/**
	 * Make a new VerifyingFolder object that will save and hash data to disk, and read it to share it.
	 * Only BTMetaInfo.initializeVerifyingFolder() makes a new VerifyingFolder object with this constructor.
	 * 
	 * @param info     The BTMetaInfo that represents the data from inside the .torrent file, and which is making us
	 * @param complete True if we have the entire torrent saved and verified already
	 * @param data     A serialized object made from a VerifyingFolder object the last time the program ran
	 */
	public VerifyingFolder(BTMetaInfo info, boolean complete, Map data) {

		// Make the array of TorrentFile objects with the paths to them all
		_files = info.getFiles();

		// Save a reference back up to the BTMetaInfo object
		_info = info;

		// We haven't throw away any pieces because their hashes haven't matched yet
		_corruptedBytes = 0;

		// Make 3 BlockRangeMap objects which will describe the striped pattern within each piece of the torrent
		partialBlocks   = new BlockRangeMap(); // Stripes within incomplete pieces we've saved, but can't verify yet
		requestedRanges = new BlockRangeMap(); // Stripes we've asked our peers to send us
		pendingRanges   = new BlockRangeMap(); // Stripes we've received, and will have a thread write to disk in a moment

		// We have the whole torrent saved on disk already
		if (complete) {

			// Use a FullBitSet for verifiedBlocks, which will act like a BitSet but always return 1 when you query a bit
			verifiedBlocks = new FullBitSet();

		// We're still downloading this torrent
		} else {

			// Make a new BitSet with a 0 for each piece this torrent has
			verifiedBlocks = new BitSet(_info.getNumBlocks());

			// If we have a serialized VerifyingFolder from a previous time the program ran, initialize this one from it
			if (data != null) initialize(data);
		}
	}

	/**
	 * Set this VerifyingFolder object from data a previous instance of the program saved for the future.
	 * Sets partialBlocks, verifiedBlocks, and wasVerifying.
	 * 
	 * @param data A HashMap the earlier program made to save its VerifyingFolder object for this torrent for now
	 */
	private void initialize(Map data) {

		// Load the partialBlocks BlockRangeMap that shows the stripes in incomplete pieces
		BlockRangeMap partial = (BlockRangeMap)data.get("partial");
		if (partial != null) partialBlocks = partial;

		// Load the verifiedBlocks BitSet that identifies the pieces we've completed, hashed, and verified
		BitSet verified = (BitSet)data.get("verified");
		if (verified != null) verifiedBlocks = verified;

		// If the previous instance of the program saved isVerifying, use the value it saved
		Boolean wasVerifying = (Boolean)data.get("wasVerifying");
		isVerifying = wasVerifying == null ? false : wasVerifying.booleanValue();
	}

	/**
	 * Write torrent data to disk files, hash a finished piece, and send a Have message to all our peers.
	 * 
	 * @param in   The piece number and location within that piece of torrent data
	 * @param data The torrent data we downloaded
	 */
	public void writeBlock(BTInterval in, byte[] data) throws IOException {

		// If QUEUE's thread left an exception in storedException, grab it and throw it now
		IOException stored = storedException;
		if (stored != null) throw stored;

		// Only let one thread access requestedRanges and pendingRanges at a time
		synchronized (this) {

			// Move the interval we wrote from requestedRanges to pendingRanges
			requestedRanges.removeInterval(in);
			pendingRanges.addInterval(in);
		}

		// Write the data to the appropriate file on the disk
		QUEUE.invokeLater(new WriteJob(in, data), "download");
	}

	/**
	 * A WriteJob object writes torrent data to disk, hashes a finished piece, and sends a Have message to all our peers.
	 * writeBlock() makes WriteJob objects, and puts them in the "download" list in the QUEUE RRProcessingQueue.
	 */
	private class WriteJob implements Runnable {

		/**
		 * The piece number and location within that piece of some data we've received.
		 * This is where to write it.
		 */
		private final BTInterval in;

		/**
		 * The torrent data we've received.
		 * This is the data to write.
		 */
		private final byte[] data;

		/**
		 * Make a new WriteJob object for this VerifyingFolder's QUEUE RRProcessingQueue to run and discard.
		 * It will write torrent data to disk, hash a finished piece, and send Have messages to all our peers.
		 * 
		 * @param in   The piece number and range where some data exists in this torrent
		 * @param data The data
		 */
		WriteJob(BTInterval in, byte[] data) {

			// Save the given data and location
			this.in = in;
			this.data = data;
		}

		/**
		 * Write torrent data to disk, hash a finished piece, and send Have messages to all our peers.
		 * Our QUEUE RRProcessingQueue's thread named "TorrentDiskQueue" will call this run() method.
		 */
		public void run() {

			// freedPending will tell us if writeBlockImpl() finishes, or throws an exception
			boolean freedPending = false;

			try {

				// Write torrent data to disk, hash a finished piece, and send a Have message to all our peers
				writeBlockImpl(in, data);

				// writeBlockImpl returned without throwing an exception
				freedPending = true;

			} catch (IOException iox) {

				// If the files for this torrent are still open, save the exception writeBlockImpl threw us
				if (isOpen()) storedException = iox;

			} finally {

				// If writeBlockImpl threw an exception
				if (!freedPending) {

					synchronized (VerifyingFolder.this) {

						// Remove the interval from pendingRanges here, because writeBlockImpl() didn't do it
						pendingRanges.removeInterval(in);
					}
				}
			}
		}
	}

	/**
	 * Write torrent data to disk.
	 * If this finishes a piece, hash and verify it, and send a Have message to all our peers.
	 * 
	 * @param in  The piece number and location within the piece of the data
	 * @param buf The data to write
	 */
	private void writeBlockImpl(BTInterval in, byte[] buf) throws IOException {

		// If our bitfield say we already have this entire piece, we don't need to write this data within it
		if (hasBlock(in.getId())) return;

		// Only let one thread write to disk at a time for each torrent
		synchronized (DISK_LOCK) { // Each VerifyingFolder has a DISK_LOCK, so two different torrents are allowed to do this at the same time

			// Make sure our files are open
			if (!isOpen()) throw new IOException("file closed");

			// Calculate where in the data of the whole torrent the given data starts
			long startOffset = (long)in.getId() * _info.getPieceLength() + in.low; // (piece number * piece size) + offset in that piece

			// written will count how many bytes we've written to disk
			int written = 0;

			// Loop i for each file that makes up this torrent, if it's a single-file torrent, this loop will just run once
			for (int i = 0; i < _files.length && written < buf.length; i++) {

				// The given data is within this file, and startOffset gets us to the right place in it
				if (startOffset < _files[i].LENGTH) {

					// Move the file pointer for this file to the starting offset
					_fos[i].seek(startOffset);

					// Calculate how many bytes we'll write
					int toWrite = (int)Math.min(        // Write the smaller of the two things we calculate
						_files[i].LENGTH - startOffset, // The size of the whole rest of the file
						buf.length - written);          // The amount of data we have left in the buffer

					// If we're writing less than the entire remainder of this file, set its length to be just what we'll write
					if (_fos[i].length() < startOffset + toWrite) _fos[i].setLength(startOffset + toWrite);

					// Write the data to the file
					_fos[i].write(buf, written, toWrite);

					// Move startOffset past the data we wrote
					startOffset += toWrite;

					// Count the bytes we wrote
					written += toWrite;
				}

				// Start startOffset from the end of this file
				startOffset -= _files[i].LENGTH;
			}
		}

		// If this writing operation completed a piece, we'll set shouldVerify to true
		boolean shouldVerify;

		synchronized (this) {

			// Remove the stripe we wrote from pendingRanges, the parts we requested from our peers
			pendingRanges.removeInterval(in);

			// Add the stripe to partialBlocks, the parts we've written but haven't verified yet
			shouldVerify = addBlockPart(in);                    // Returns true if that completed a piece
			if (shouldVerify) partialBlocks.remove(in.blockId); // If that completed a piece, remove it from partialBlocks, we'll verify its hash now
		}

		// The data we just wrote completed a piece, so now we can hash it
		if (shouldVerify) {

			// Set verified to true if the piece's hash matches
			boolean verified = false;

			try {

				// Read the piece data from the files we wrote on the disk, compute the SHA1 hash, and compare it to the hash in the .torrent file
				verified = verify(in.getId(), false); // Pass false to have the thread not pause, go full speed without waiting

			} catch (InterruptedException impossible) { ErrorService.error(impossible); }

			synchronized (this) {

				// We hashed the piece and found it valid
				if (verified) {

					// Mark this piece done in verifiedBlocks
					markPieceCompleted(in.getId());

				// The piece is corrupted, its hash didn't match
				} else {

					// Add the piece's size to _corruptedBytes, our count of how much data we've downloaded that then didn't hash correctly
					_corruptedBytes += getPieceSize(in.getId());
				}
			}

			// We finished downloading a piece, computed its hash, and it matched
			if (verified) handleVerified(in.getId()); // Close any files we finished, and send all our peers a Have message
		}
	}

	/**
	 * Record that we've finished downloading a piece and verified its hash.
	 * 
	 * @param blockId The number of the piece we have saved and checked
	 */
	private synchronized void markPieceCompleted(int blockId) {

		// Set the bit we have for the given piece number in verifiedBlocks to 1
		verifiedBlocks.set(blockId);

		// Make a note that with that change, our bit field that we send out in Bitfield messages isn't up to date
		bitFieldDirty = true; // So, we'll have to generate it again instead of just sending out what we've cached

		// If we've downloaded and checked the whole torrent, replace verifiedBlocks with a FullBitSet object
		if (isComplete()) verifiedBlocks = new FullBitSet(); // A FullBitSet object acts like a BitSet, but always returns true instead of actually checking bits
	}

	/**
	 * Compute the SHA1 hash of a piece we saved to disk, and match it against the hash in the .torrent file.
	 * 
	 * @param pieceNum The number of the piece we've saved to disk
	 * @param slow     true to sleep and yield this thread between parts of the hashing operation
	 * @return         true if the hash matches, false if the data is corrupt
	 */
	private boolean verify(int pieceNum, boolean slow) throws IOException, InterruptedException {

		// Get a new SHA1 object that we'll use to hash the data of a piece on the disk
		MessageDigest md = _info.getMessageDigest(); // Just returns a new SHA1 object
		md.reset(); // Reset it, this shouldn't really be necessary since it's new

		// Figure out how big the piece is
		int pieceSize = getPieceSize(pieceNum); // They're all the same size except for the last one

		// Make a 64 KB buffer, or smaller if this is the last piece and it's smaller than that
		byte[] buf = new byte[Math.min(65536, pieceSize)];

		// Count the number of bytes we hash
		int read = 0;

		// Start offset at the start of the piece, measured from the start of all of the data of the torrent
		long offset = (long)pieceNum * _info.getPieceLength();

		// Loop until we've read the whole piece
		while (read < pieceSize) {

			// Read buf.length bytes from offset into the torrent data we've saved into the buf buffer
			int readNow = read(offset, buf, 0, buf.length); // Returns the number of bytes we read
			if (readNow == 0) return false; // Unable to read anything, return false, piece not verified

			// Get the time before this hashing operation
			long start = System.currentTimeMillis();

			// Hash readNow bytes in buf, starting at 0, the start
			md.update(buf, 0, readNow);

			// If the caller has requested slow hashing, wait a little before hashing the next 64 KB
			if (slow && SystemUtils.getIdleTime() < URN.MIN_IDLE_TIME && SharingSettings.FRIENDLY_HASHING.getValue()) {
				long interval = System.currentTimeMillis() - start;
				interval *= QUEUE.size() > 0 ? 5 : 3; // Go extra slow if there are active torrents
				if (interval > 0) {
					Thread.sleep(interval); // Pause this thread for an interval of time, making other threads run instead
				} else {
					Thread.yield(); // Don't pause this thread, but allow other threads to jump in right now
				}
			}

			// Count that we've hashed readNow more bytes
			read += readNow;

			// Move offset forward past them
			offset += readNow;
		}

		// Get the SHA1 hash the md object computed
		byte[] sha1 = md.digest();

		// If the hash we computed matches the one in the .torrent file, return true
		return Arrays.equals(sha1, _info.getHash(pieceNum));
	}

	/**
	 * Close any files the piece we saved completed.
	 * Send a Have message with the piece number to all our peers sharing this torrent.
	 * 
	 * @param pieceNum The number of the piece we downloaded and verified
	 */
	private void handleVerified(int pieceNum) {

		// If that piece finished a file, close it
		closeAnyCompletedFiles(pieceNum);

		// Send a Have message with the given piece number to all our connections sharing this torrent
		notifyOfChunkCompletion(pieceNum);
	}

	/**
	 * Send a Have message with the given piece number to all our connections sharing this torrent.
	 * If the VerifyingFolder has downloaded the complete torrent, move it into our "Shared" folder.
	 * 
	 * @param pieceNum The number of the piece we got, saved, and verified
	 */
	private void notifyOfChunkCompletion(int pieceNum) {

		// Just call the next method
		_info.notifyOfComplete(pieceNum);
	}

	/**
	 * If the piece we just saved finishes a file, close it for reading and writing and open it for just reading.
	 * Only handleVerified() calls this method.
	 * 
	 * @param The piece number we just got and verified
	 */
	private void closeAnyCompletedFiles(int pieceNum) {

		// Loop for each of the files in this torrent
		List possiblyCompleted = null;
		for (int i = 0; i < _files.length; i++) {

			// If this file starts in a piece after the one we saved, move to the next file
			if (_files[i].begin > pieceNum) continue;

			// If this file ends before the piece we saved, leave the loop
			if (_files[i].end < pieceNum) break;

			// Add this file to a list of those this piece may have completed
			if (possiblyCompleted == null) possiblyCompleted = new ArrayList();
			possiblyCompleted.add(new Integer(i));
		}

		// We didn't find any files that might have been completed by the piece we saved
		if (possiblyCompleted == null) return;

		// Loop for each file this piece may have completed
		for (Iterator iter = possiblyCompleted.iterator(); iter.hasNext(); ) {
			int index = ((Integer)iter.next()).intValue();
			TorrentFile file = _files[index]; // Get the TorrentFile object that has the path to the file

			// If we have every piece that this file is a part of, we'll set done to true
			boolean done = true;

			// Loop for each piece that has part of this file in it
			for (int i = file.begin; i <= file.end; i++) {

				// If we don't have that piece, we can't have this whole file
				if (!hasBlock(i)) {

					// Record the file isn't done, and don't bother checking the other pieces
					done = false;
					break;
				}
			}

			// This file is done, we have all the pieces it's in
			if (done) {

				/*
				 * decide if files should be moved to the save
				 * location as they are completed.. cool but not trivial
				 */

				try {

					synchronized (DISK_LOCK) {

						// If our files are open
						if (isOpen()) {

							// Close this one, and then open it with only read access
							_fos[index].close(); // Before, we had read and write access
							_fos[index] = new RandomAccessFile(file.PATH, "r");
						}
					}

				// The file isn't there anymore
				} catch (FileNotFoundException bs) {

					// Log that exception in the ErrorService
					ErrorService.error(bs);

				// Catch and ignore IOExceptions
				} catch (IOException ignored) {}
			}
		}
	}

	/**
	 * Determine if the stripe we just wrote completed a piece, meaning it's time for us to hash and verify it.
	 * Adds the given interval to partialBlocks.
	 * 
	 * Notification that the program has written a part of a piece of this torrent to disk.
	 * Only writeBlockImpl() calls this method.
	 * 
	 * @return true if the given interval completes the whole piece.
	 *         false if there are still parts of the piece missing.
	 */
	private boolean addBlockPart(BTInterval in) {

		// If the given interval is the entire piece, yes, the piece is complete
		if (isCompleteBlock(in, in.getId())) return true;

		// Record that we've written this part of the torrent in partialBlocks
		partialBlocks.addInterval(in);

		// Get the striped pattern that shows what parts of the piece we've written to disk
		IntervalSet set = partialBlocks.getSet(in);

		// If there is a single stripe
		if (set.getNumberOfIntervals() == 1) { // If there are 2 stripes, there must be unwritten data between them

			// Get it
			Interval one = set.getFirst();

			// If it covers the entire piece, return true
			if (isCompleteBlock(one,in.getId())) return true;
		}

		// No, we haven't finished writing all of the data of the piece yet
		return false;
	}

	/**
	 * Determine if a given interval clips out an entire piece.
	 * Its low index has to be 0, and its high index points to the last byte in the piece.
	 * The last piece may be smaller than the standard piece size.
	 * 
	 * Only addBlockPart() above calls this method.
	 * 
	 * @param in A BTInterval object that clips out a range of data
	 * @param id The piece number this is from
	 * @return   True if the interval clips out the entire piece
	 */
	private boolean isCompleteBlock(Interval in, int id) {

		// Return true if
		return
			in.low == 0 &&                   // The interval starts at the start of the piece, and
			in.high == getPieceSize(id) - 1; // Ends by pointing to the last byte in the piece
	}

	/**
	 * Determine if we have a piece of this torrent.
	 * 
	 * @param block The piece number
	 */
	public synchronized boolean hasBlock(int block) {

		// Look it up in verifiedBlocks, our BitSet that shows which pieces are saved and verified
		return verifiedBlocks.get(block);
	}

	/**
	 * Have this VerifyingFolder object open files on the disk for each of the files described by the torrent.
	 * Makes a RandomAccessFile object in the _fos array for each file in the torrent, and opens it.
	 */
	public void open() throws IOException {

		// Call the next open() method, not giving it a MangedTorrent file
		open(null);
	}

	/**
	 * Have this VerifyingFolder object open files on the disk for each of the files described by the torrent.
	 * Makes a RandomAccessFile object in the _fos array for each file in the torrent, and opens it.
	 * We can use a RandomAccessFile object to read and write data in each file.
	 * 
	 * A .torrent describes one file or several files that the program will save to disk.
	 * This open() method opens all of the files.
	 * 
	 * The file names and paths to where they should go are in the _files array of TorentFile objects.
	 * open() sets up _fos, an array that has a RandomAccessFile object for each one.
	 * 
	 * If we're just starting to download this torrent, there won't be any saved files at all yet.
	 * The RandomAccessFile objects will let us write each file as pieces of it come in.
	 * 
	 * If we're done with the whole torrent, all the files are there.
	 * This method opens the RandomAccessFile objects with read-only permissions.
	 * 
	 * If we've partially downloaded this torrent, it's a mixture between the two.
	 * We can use the RandomAccessFile objects to read the parts of each file that are present, and write in the missing parts.
	 * 
	 * If we're opening files we saved the last time, we need to be sure they haven't changed since then.
	 * To do this, open() creates a list named filesToVerify of files that it opens that have data in them.
	 * It gives that list to verifyFiles(), which hashes piece data and adds 1s to our verifiedBlocks BitSet.
	 * 
	 * @param torrent The ManagedTorrent object that represents this torrent file.
	 *                When we're done hashing the files we find on disk, we'll call torrent.verificationComplete().
	 */
	public void open(final ManagedTorrent torrent) throws IOException {

		synchronized (DISK_LOCK) {

			// Make sure the files aren't already open
			if (_fos != null) throw new IOException("Files already open!");

			// Make an array that has a RandomAccessFile object for each file this torrent describes
			_fos = new RandomAccessFile[_files.length]; // _files.length is the number of files this torrent describes
		}

		// If a thread gets an exception, it will put it in storedException for this thread to pick up
		storedException = null;

		// If we haven't saved any data of this torrent yet, set isVerifying to true
		isVerifying |= (getBlockSize() == 0); // If isVerifying is already true, leave it true

		// position of the first byte of a file in the torrent
		long pos = 0;

		// If we find a file already on the disk, we'll add its path to this list
		List filesToVerify = null;

		// Loop for each file listed in the .torrent
		for (int i = 0; i < _files.length; i++) {

			// Wrap the path to where we'll save the file in LimeWire's "Incomplete" folder in a new Java File object
			File file = new File(_files[i].PATH);

			// We've already have this entire torrent saved
			if (isComplete()) {

				// Make a note that we're going to open all the files in read-only mode
				LOG.info("opening torrent in read-only mode");

				synchronized (DISK_LOCK) {

					// Make a new RandomAccessFile object for this file
					_fos[i] = new RandomAccessFile(file, "r"); // Give it the file object with the path, and "r" for read-only mode
				}

			// We need to download more to get this whole torrent
			} else {

				// Make a note that we're going to open all the files for reading and writing
				LOG.info("opening torrent in read-write");

				// If there isn't already a file at the path we generated from the .torrent file
				if (!file.exists()) {

					/*
					 * Ensure that the directory this file is in exists & is writeable.
					 * make sure this doesn't allow trickery with names
					 */

					// Make sure the folders up to this path exist, or make them
					File parentFile = file.getParentFile();
					if (parentFile != null) {
						parentFile.mkdirs();
						FileUtils.setWriteable(parentFile);
					}

					// Make the file at that location
					file.createNewFile();

					/*
					 * a file is missing, so this must be a new download.
					 * if it was not, we need to reverify every file.
					 */

					// If we haven't already determined that this is a new download
					if (!isVerifying) {

						/*
						 * pretend nothing was downloaded
						 */

						synchronized (this) {

							// Record that we've written nothing, and verified nothing
							partialBlocks.clear();
							verifiedBlocks.clear();
						}

						// Have this VerifyingFolder report that it is opening files and checking them right now
						isVerifying = true;

						// Restart the loop from the beginning
						i = -1;   // Set i to -1, the i++ will make it 0, just like the first time the loop ran
						continue; // Go to the start of the loop now
					}
				}

				// If this file is marked read-only, remove that status from it
				FileUtils.setWriteable(file);

				synchronized (DISK_LOCK) {

					// Make a new RandomAccessFile object that will open the file at the given path for reading and writing
					_fos[i] = new RandomAccessFile(file, "rw"); // Pass "rw" to get read and write access
				}

				// If this file is already on the disk, add it to our list of files we'll verify
				if (isVerifying &&       // We're allowed to hash file data now
					file.length() > 0) { // This file is already saved on the disk

					// Add the TorrentFile object with the path to this file to filesToVerify, a list of files we'll hash
					if (filesToVerify == null) filesToVerify = new ArrayList(_files.length);
					filesToVerify.add(_files[i]);
				}
			}

			// Increment pos to point to the first byte of the next file
			pos += _files[i].LENGTH; // Add the length of this file to it
		}

		// We found files on the disk to verify
		if (filesToVerify != null) {

			// Have this VerifyingFolder object report that it is hasing file data it opened
			isVerifying = true;

			// Open the files we found on disk, hash their pieces, and set bits to 1 in verifiedBlocks of those that match
			verifyFiles(filesToVerify); // Adds a lot of VerifyJob objects to VERIFY_QUEUE

			// The caller gave us a link up to the ManagedTorrent object that this VerifyingFolder is for
			if (torrent != null) {

				// Have the VERIFY_QUEUE's "TorrentVerifier" thread run this code
				VERIFY_QUEUE.invokeLater(new Runnable() {
					public void run() {

						/*
						 * When we add this Runnable to the VERIFY_QUEUE, it will go after all the VerifyJob objects.
						 * So, when the VERIFY_QUEUE runs it, that means it's done with all the VerifyJob objectes before it.
						 */

						// If our files are still open
						if (isOpen()) {

							// Record that we're done hashing the file data we opened
							isVerifying = false;

							// Now that we've opened all our files, start connecting to remote computers to share this torrent
							torrent.verificationComplete();
						}
					}

				// Put this Runnable object in the VERIFY_QUEUE in a list for this VerifyingFolder's URN
				}, _info.getURN());
			}

		// We didn't find any files on the disk for this torrent
		} else {

			// Have this VerifyingFolder object report that it isn't verifying already saved files right now
			isVerifying = false;
		}
	}

	/**
	 * Determine if this VerifyingFolder object is hashing data it found on disk.
	 * 
	 * When the programs runs, it will look on the disk for torrent files it saved the last time.
	 * If it finds some, it opens each one, hashes it, and makes sure the data is good.
	 * This process can take several minutes for a large torrent.
	 * Call this method to find out if this VerifyingFolder is doing it now.
	 * 
	 * @return True if this VerifyingFolder object is hashing files it found already saved on the disk from last time.
	 *         False if it is finished doing this, or didn't need to because there weren't any saved files.
	 */
	boolean isVerifying() {

		// Return the flag that open() sets
		return isVerifying;
	}

	/**
	 * Determine if we have this whole torrent.
	 * 
	 * @return true if we do, false if we are still missing some pieces
	 */
	synchronized boolean isComplete() {

		// If verifiedBlocks has as many 1s as there are pieces, return true
		return verifiedBlocks.cardinality() == _info.getNumBlocks();
	}

	/**
	 * Close all the files we have open to read and write the files of this torrent.
	 * Closes all the RandomAccessFile objects this VerifyingFolder has in its _fos list.
	 */
	public void close() {

		// Make a note we're closing all the files
		LOG.debug("closing the file");

		// Only let one thread perform disk activity at a time
		synchronized (DISK_LOCK) {

			// If we don't have our array of RandomAccessFile objects, leave without doing anything
			if (_fos == null) return;

			// Loop for each file we have open
			for (int i = 0; i < _fos.length; i++) {

				try {

					// Close the RandomAccessFile object
					if (_fos[i] != null) _fos[i].close();

				// Exceptions don't matter because we're closing files
				} catch (IOException ioe) {}
			}

			// Discard the entire array
			_fos = null;
		}

		// Clear all the VerifyJob objects from VERIFY_QUEUE, they would need the files to be open to work anyway
		VERIFY_QUEUE.clear(_info.getURN());
	}

	/**
	 * Determine whether the files for this torrent are open.
	 * 
	 * @return true if they are, false if they are closed
	 */
	public boolean isOpen() {

		// Only let one thread perform disk activity at a time
		synchronized (DISK_LOCK) {

			// If we have our _fos array of RandomAccessFile objects, they're open
			return _fos != null;
		}
	}

	/**
	 * Read torrent data we have saved on disk, put it in a Piece message, and send it to a remote computer.
	 * 
	 * @param in The piece number and range within that piece of the data we want to send
	 * @param c  The remote computer to send it to
	 */
	public void sendPiece(BTInterval in, BTConnection c) throws IOException {

		// If a previous call to SendJob.run() left an exception, get it and throw it now
		IOException e = storedException;
		if (e != null) throw e;

		// Read data we have saved, put it in a Piece message, and send it to the remote computer
		QUEUE.invokeLater(new SendJob(in, c), "upload");
	}

	/**
	 * A SendJob object reads torrent data from disk, puts it in a Piece message, and sends it to a remote computer.
	 * sendPiece() makes SendJob objects, and puts them in the "upload" list in the QUEUE RRProcessingQueue.
	 */
	private class SendJob implements Runnable {

		/**
		 * The piece number and range within that piece to send.
		 * We have this part of the torrent's data saved in the appropriate files on the disk.
		 */
		private final BTInterval in;

		/** The remote computer to send the data to. */
		private final BTConnection c;

		/**
		 * Make a new SendJob, which will send data from this torrent that we have saved to a remote computer.
		 * 
		 * @param in The piece number and range within that piece to send
		 * @param c  The remote computer to send it to
		 */
		SendJob(BTInterval in, BTConnection c) {

			// Save the given objects in this new SendJob
			this.in = in;
			this.c = c;
		}

		/**
		 * Read data we have saved on disk, put it in a Piece message, and send it to the remote computer.
		 * Our QUEUE RRProcessingQueue's thread named "TorrentDiskQueue" will call this run() method.
		 */
		public void run() {

			// If our files aren't open, we can't send anything
			if (!isOpen()) return;

			// If the thread left us an exception, throw it
			IOException iex = storedException;
			if (iex != null) c.handleIOException(iex);

			// Make a note that we're going to upload a piece
			if (LOG.isDebugEnabled()) LOG.debug("sending piece " + in);

			// Calculate how much data we're going to send
			int length = in.high - in.low + 1; // high points to the last byte, not beyond it, so we have to add 1

			// Calculate the distance from the start of the torrent's data where the data we'll send starts
			long position = (long)in.getId() * _info.getPieceLength() + in.low;

			// offset will record how much data we've read from the files we've saved to disk
			int offset = 0;

			// Make a byte array large enough to hold all the data we'll send
			byte[] buf = new byte[length];

			try {

				do {

					// Read the torrent data we have saved in files on the disk
					offset += read(position + offset, buf, offset, length - offset); // Move offset past the data we've read

				// Loop, calling read() multiple times, until we've filled our buffer
				} while (offset < length);

			// read() threw us an IOException
			} catch (IOException bad) {

				// If our files are still open, save it
				if (isOpen()) storedException = bad;

				// Give it to the BTConnection object
				c.handleIOException(bad);
			}

			// Put the data we read in a new Piece message, and send it to the remote computer c
			c.pieceRead(in, buf);
		}
	}

	/**
	 * Read data we have saved to disk as part of this torrent.
	 * 
	 * In a multifile torrent, the data of all the files are placed together into one large block.
	 * position and length measure into this combined block.
	 * On the disk, however, we've named and saved the files individually.
	 * This read() method opens several files if necessary to get the requested data.
	 * 
	 * @param position The position from the start of the torrent data to start reading
	 * @param buf      A destination buffer for the data
	 * @param offset   Put the data this far into the buffer
	 * @param length   The number of bytes we can write there
	 * @return         The number of bytes we read
	 */
	private int read(long position, byte[] buf, int offset, int length) throws IOException {

		// Make sure position is 0 or more, and buf is big enough
		if (position < 0) {
			throw new IllegalArgumentException("cannot seek negative position " + position);
		} else if (offset + length > buf.length) {
			throw new ArrayIndexOutOfBoundsException("buffer to small to store supplied number of bytes");
		}

		// Count how many bytes we read
		int read = 0;

		// Only let one thread do this at a time
		synchronized (DISK_LOCK) {

			// The files have to be open for us to read from them
			if (!isOpen()) throw new IOException("file closed");

			// Loop for each saved file we made for this torrent
			for (int i = 0; i < _files.length && read < length; i++) {

				// Loop while position is still within this file, and we still need to read more data
				while (position < _files[i].LENGTH && read < length) {

					// If this file isn't as long as it will be when it's done, and position is at or beyond the end, leave now
					if (_fos[i].length() < _files[i].LENGTH && position >= _fos[i].length()) return read; // Return the number of bytes we read

					// Calculate how many bytes we'll read from this file
					int toRead = (int)Math.min(      // Whichever is smaller
						_fos[i].length() - position, // The end of the saved file, or
						length - read);              // The amount of space we have left in the buffer

					// Copy toRead bytes of data from position in the _fos[i] file into buf
					_fos[i].seek(position);
					int t_read = _fos[i].read(buf, read + offset, toRead);
					if (t_read == -1) throw new IOException(); // End of file

					// Move position past the bytes we read
					position += t_read;

					// Count the bytes we read
					read += t_read;
				}

				// Make position less so it points to the same place in the torrent's data, but from the start of the next file
				position -= _files[i].LENGTH;
			}
		}

		// Return the number of bytes we read
		return read;
	}

	/**
	 * Choose which part of this torrent to ask for.
	 * 
	 * Only ManagedTorrent.request() calls this method.
	 * It's going to send a remote computer a Reqeust message, and needs to know what to ask for.
	 * 
	 * The BitTorrent specification talks about programs sending out the rarest piece first.
	 * Actually, most just pick pices randomly, and research shows this is almost as efficient.
	 * LimeWire's BitTorrent implementation here doesn't to rarest at all, it just does random.
	 * 
	 * @param bs A BitSet that shows which pieces the remote computer has, and we need.
	 * @return   A BTInterval with the piece number, and range within that piece, that we should ask for.
	 *           null if we can't find anything we need from this computer.
	 */
	public synchronized BTInterval leaseRandom(BitSet bs) {

		// If we have the whole torrent, we shouldn't be asking for anything
		if (isComplete()) return null;

		// Make a note that contains the number of pieces we could use from the remote computer
		if (LOG.isDebugEnabled()) LOG.debug("leasing random chunk from available cardinality " + bs.cardinality());

		// See which pieces we don't have and the remote computer does
		BitSet clone = (BitSet)bs.clone();
		clone.andNot(verifiedBlocks); // Removes verifiedBlocks from clone, this shouldn't be necessary because bs has already had what we have removed

		/*
		 * if possible, do not request any chunks which are currently
		 * being requested
		 */

		// Make an iterator that will move through all the keys in pendingRanges, then in requestedRanges, then in partialBlocks
		MultiIterator iter = new MultiIterator(new Iterator[] {

			// Prepare an array of the Iterator objects to pass the MultiIterator constructor
			pendingRanges.keySet().iterator(),   // The piece numbers that we have data for, a thread will write it to disk
			requestedRanges.keySet().iterator(), // The piece numbers that we have sent Request messages to get data in
			partialBlocks.keySet().iterator()    // The piece numbers of pieces that we're still missing stripes from
		});

		// Loop for each piece number we're saving, have requested, or have partial data for
		while (iter.hasNext()) {

			// Set the bit for that piece in clone to 0, this will make us not request from it
			Integer element = (Integer)iter.next();
			clone.clear(element.intValue());
		}

		// If that leaves us with something to choose from
		if (LOG.isDebugEnabled()) LOG.debug("after removing pending, partial and requesting ranges, the remote has cardinality " + clone.cardinality());
		if (clone.cardinality() > 0) {

			// the remote has new chunks we can get
			int selected = -1;
			int current = 1;

			// Loop with i set to the index of each 1 in the clone BitSet, these are the pieces we're choosing from amongst
			for (int i = clone.nextSetBit(0); i >= 0; i = clone.nextSetBit(i + 1)) {

				/*
				 * Tour Point: How we decide which piece to request.
				 * 
				 * This loop runs once for each piece we need that the remote computer has.
				 * i is the piece number.
				 * Suppose this remote computer only has 3 pieces that we need, with the numbers 568, 789, and 1052.
				 * This loop will run 3 times, with i equal to 568 the first time, and 789 and 1052 the later times.
				 * 
				 * current starts at 1, and gets incremented at the end for the next loop.
				 * So, current will be 1, 2, and 3.
				 * 
				 * The fraction (1f / current) gets smaller and smaller as the loop runs.
				 * The first time, it's certainty, 1, then it's only half, then a third, then a fourth, and so on.
				 * 
				 * Math.random() returns a random floating point number between 0.0 and 1.0.
				 * It will be a different random number every time.
				 * 
				 * The complete statement in the if is (Math.random() < (1f / current)).
				 * The first time the loop runs, the fraction will be 1, and the condition will be satisified.
				 * selected = i will run, making us choose the first needed piece.
				 * 
				 * The loop keeps running, for every piece we need.
				 * If the random number falls within the rapidly shrinking envelope, we'll pick that piece number instead.
				 * 
				 * For the pieces near the end of the file, the fraction is very small.
				 * It's unlikely we'll pick one of them instead of the earlier one we selected.
				 * 
				 * So, this method is likely to pick a piece early in the file.
				 * This will cause files to generally be downloaded from start to finish.
				 */

				// If a random number falls under a shrinking fraction, pick this piece
				if (Math.random() < 1f / current++) selected = i;
			}

			// Log the piece we selected
			if (LOG.isDebugEnabled()) LOG.debug("selecting piece " + selected);

			// Make a new BTInterval object which has the piece number, and clips out all of the data of the piece
			BTInterval ret = new BTInterval(0, getPieceSize(selected) - 1, selected);

			// Add it to the ranges we've send Request messages for, and return it
			requestedRanges.addInterval(ret);
			return ret;
		}

		/*
		 * prepare a list of partial or requested blocks the remote host has
		 */

		// Make a list of the piece numbers we're still missing parts of, and those we've sent Request messages to get data in
		Set available = new LinkedHashSet(partialBlocks.size() + requestedRanges.size());
		available.addAll(partialBlocks.keySet());
		available.addAll(requestedRanges.keySet());

		// Loop for each piece number we're missing parts of, or are requesting parts of
		for (Iterator iterator = available.iterator(); iterator.hasNext(); ) {
			Integer block = (Integer)iterator.next();

			// If this remote computer doesn't have this piece, remove it from the available list
			if (!bs.get(block.intValue())) iterator.remove();
		}

		// Make a note of how many pieces we're choosing from now
		if (LOG.isDebugEnabled()) LOG.debug("available partial blocks to attempt: " + available);

		// Loop through the pieces we're missing parts of or requesting parts of that the remote computer has
		for (Iterator iterator = available.iterator(); iterator.hasNext(); ) {
			Integer block = (Integer)iterator.next();

			// Figure out which parts of this piece we need
			IntervalSet needed = new IntervalSet(); // An IntervalSet is a striped pattern, needed starts out empty
			needed = needed.invert(getPieceSize(block.intValue())); // Now, needed is a single stripe across the entire piece

			// Make more IntervalSet objects, which keep the striped pattern within this piece
			IntervalSet partial   = partialBlocks.getSet(block);   // The parts we've saved to disk
			IntervalSet pending   = pendingRanges.getSet(block);   // The parts we've received, and a thread will save soon
			IntervalSet requested = requestedRanges.getSet(block); // The parts we've requested with Request messages

			// Remove the parts we have from what we need
			if (partial != null) needed.delete(partial); // The parts we saved
			if (pending != null) needed.delete(pending); // The parts a thread is going to save

			// Remove the parts we currently have Request messages out for from what we need
			if (requested != null) {
				needed.delete(requested);

				/*
				 * now, if we still have some parts of the chunk, get one of them
				 * if not and this is the last partial chunk, doubly-assign some
				 * part of it (is this endgame?)
				 */

				// If we don't need any part of this piece, but it's the last one in our list
				if (needed.isEmpty() &&    // If we don't have anything to ask for, and
					!iterator.hasNext()) { // This is the last piece in our list

					// Put out an additional request for this part of this piece
					LOG.debug("requesting part of a block that is already requested...");
					needed = requested;
				}
			}

			// We don't need any part of this piece, loop again to find the next one
			if (needed.isEmpty()) continue;

			// Get the first stripe from the needed stripe pattern, and make a BTInterval from it
			BTInterval ret = new BTInterval(needed.getFirst(), block.intValue());

			// We're choosing this one, return it
			if (LOG.isDebugEnabled()) LOG.debug("selected partial/requested interval " + ret);
			return ret;
		}

		// We couldn't find anything to ask for
		return null;
	}

	/**
	 * Find out how big a numbered piece is.
	 * All the pieces in a torrent are the same size except for the last one, which is probably smaller.
	 * 
	 * @param pieceNum The piece number
	 * @return         The size of that piece
	 */
	private int getPieceSize(int pieceNum) {

		// The given piece number is the last piece
		if (pieceNum == _info.getNumBlocks() - 1) {

			// Get the remainder from dividing the file size with the piece size
			return (int)(_info.getTotalSize() % _info.getPieceLength());

		// The given piece number is for a piece before the last one
		} else {

			// Return the piece size
			return _info.getPieceLength();
		}
	}

	/**
	 * Remove a piece number from our list of the pieces we've asked a peer to send.
	 * 
	 * Only BTConnection.clearRequest() calls this method.
	 * We've sent a Cancel message to a remote computer, so we're not going to get the piece we asked for.
	 * 
	 * @param pieceNum The number of the piece we requested, and then cancelled
	 */
	public synchronized void releaseChunk(int pieceNum) {

		// Remove the piece number from requestedRanges, our record of what we're waiting for our connections to send us
		requestedRanges.remove(new Integer(pieceNum));
	}

	/**
	 * Get the bit field that shows which pieces of this torrent we have, and which we need.
	 * The bit field will have a bit for each piece.
	 * If the bit is 0, we need it, if the bit is 1, we have it saved and hash verified, and can share it.
	 * 
	 * The BitTorrent Bitfield message has a bit field like this as its payload.
	 * 
	 * @return A byte array with the bitfield
	 */
	public synchronized byte[] createBitField() {

		// The first time this is called, make a new byte array large enough to have a bit for each piece of this torrent
		if (bitField == null) bitField = new byte[(_info.getNumBlocks() + 7) / 8]; // Add 7 and divide by 8 to round up to the next 8 bits, which are a byte

		// If we've gotten another piece since the last time we made the bit field
		if (bitFieldDirty) { // We have to remake it

			// If we have this entire torrent downloaded and verified
			if (isComplete()) {

				// Set all the bits to 1
				for (int i = 0; i < bitField.length; i++) bitField[i] = (byte)0xFF;

			// We only have some pieces of this torrent
			} else {

				// Loop for each bit set to 1 in verifiedBlocks
				for (
					int i = verifiedBlocks.nextSetBit(0);   // (1) Start i at the index of the first 1 in verifiedBlocks
					i >= 0;                                 // (3) Stop the loop when nextSetBit() returned -1 because we're done
					i = verifiedBlocks.nextSetBit(i + 1)) { // (2) After the loop, move i to the next 1, returns -1 if there are no more

					// Flip the bit at i in bitField to 1
					bitField[i / 8] =          // (4) Set the byte we composed back into the bitField array
						(byte)(bitField[i / 8] // (1) Look up the byte in bitField that contains the bit at i
						|                      // (3) Combine them, setting the bit at i to 1
						(1 << (7 - i % 8)));   // (2) Make a byte that just has a 1 in the right place
				}
			}

			// Record that bitField matches the information in verifiedBlocks
			bitFieldDirty = false;
		}

		// Return the bit field we made now, or earlier
		return bitField;
	}

	/**
	 * Set bits to 1 in verifiedBlocks to mark the pieces of this torrent we have on disk.
	 * Loops through the files we have on disk for this torrent.
	 * Assembles the pieces they occupy in the torrent data, and hashes each piece.
	 * Validates the hashes, and sets 1s in the verifiedBlocks BitSet.
	 * 
	 * Only VerifyingFolder.open() calls this method.
	 * 
	 * @param l A list of Java File objects with the paths to files on the disk for this torrent
	 */
	private void verifyFiles(List l) {

		// Get the distance to the last set bit, plus 1
		int lastSet;
		synchronized (this) {
			lastSet = verifiedBlocks.length(); // If the BitSet is 011010000, the length is 5, the size of the (01101)0000 part
		}

		// Loop for each file on the disk we have for this torrent
		for (Iterator iter = l.iterator(); iter.hasNext(); ) {
			TorrentFile f = (TorrentFile)iter.next();

			// Loop for each piece that has a part of this file
			for (
				int i = Math.max( // Start i at whichever is bigger
					lastSet,      // The piece beyond the last one we have (do) this doesn't make any sense as a starting point
					f.begin);     // The piece that this file starts in 
				i <= f.end;       // Stop looping when i moves beyond the last piece this file is in
				i++) {            // Move to the next piece

				// Read this piece from the files that can make it, hash it, and set a 1 in verifiedBlocks if it hashes correctly
				VERIFY_QUEUE.invokeLater(new VerifyJob(i), _info.getURN());
			}
		}
	}

	/**
	 * A VerifyJob reads the data of a piece from files on disk, and checks its hash.
	 * If the data is found and the hash is good, it sets a 1 in our verifiedBlocks BitSet, and sends all our peers a Have message.
	 */
	private class VerifyJob implements Runnable {

		/** The piece number to hash and check. */
		private final int pieceNum;

		/**
		 * Make a new VerifyJob object that will open, read, hash, and check a piece we have saved.
		 * Only the verifyFiles() method above does this.
		 * 
		 * @param pieceNum The number of the piece to check
		 */
		public VerifyJob(int pieceNum) {

			// Save the given piece number
			this.pieceNum = pieceNum;
		}

		/**
		 * Read the data of a piece we have saved on disk, and check its hash.
		 * If it's here and valid, sets its bit to 1 in verifiedBlocks, and sends a Have message to all our peers.
		 * 
		 * Our VERIFY_QUEUE RRProcessingQueue's thread named "TorrentVerifier" will call this run() method.
		 */
		public void run() {

			// Check things before running
			if (storedException != null || // If a thread got an exception and left it here for us, or
				!isOpen() ||               // Our saved files aren't open, or
				hasBlock(pieceNum))        // We don't have the requested block number saved and verified at all yet
				return;                    // Leave without doing anything

			try {

				// Compute the SHA1 hash of a piece we saved to disk, and match it against the hash in the .torrent file
				if (verify(pieceNum, true)) { // true to go slowly, have the thread sleep and yield between hashing data

					// The hash is good
					markPieceCompleted(pieceNum); // Set its bit in verifiedBlocks to 1
					handleVerified(pieceNum);     // Send all our connections a Have message
				}

				// Does nothing
				if (SystemUtils.getIdleTime() < URN.MIN_IDLE_TIME && SharingSettings.FRIENDLY_HASHING.getValue()) {}

			// The verify() method wasn't able to read a file on the disk
			} catch (IOException bad) {

				// Catch the exception and store it for our main thread to pick up
				storedException = bad;

			// Another thread interrupted this one
			} catch (InterruptedException iex) {

				// Save an exception for that
				storedException = new InterruptedIOException(); // Save an InterruptedIOException instead of an IOException
			}
		}
	}

	/**
	 * Calculate how many bytes of this torrent we've saved and verified.
	 * 
	 * Totals the size of all the pieces we've hashed and checked.
	 * Deals with the last piece, which is probably smaller than the standard piece size for this torrent.
	 * 
	 * @return The size of the data in bytes
	 */
	synchronized long getVerifiedBlockSize() {

		// Calculate the number of bytes we've saved and verified
		long ret = verifiedBlocks.cardinality() * (long)_info.getPieceLength();

		// The last piece is probably smaller, if it was in that count
		if (verifiedBlocks.get(_info.getNumBlocks() - 1)) {

			// Make ret smaller to show the real size of the final block
			ret = ret - _info.getPieceLength() + getPieceSize(_info.getNumBlocks() - 1);
		}

		// Return the total we calculated
		return ret;
	}

	/**
	 * Calculate how many bytes of this torrent we've saved.
	 * 
	 * We haven't verified all this data yet.
	 * This total includes the size of all the complete pieces we've verified, and all the data we've saved in partial pieces.
	 * 
	 * @return The size of the data in bytes
	 */
	synchronized long getBlockSize() {

		// Start out with the total size of all the pieces we've saved and hashed
		long written = getVerifiedBlockSize();

		// To that, add the stripes we've saved in pieces that aren't done yet
		return written + partialBlocks.byteSize();
	}

	/**
	 * Calculate how many bytes of data we've downloaded for this torrent, but then discarded because they didn't hash correctly.
	 * 
	 * This data size doesn't have anything to do with the size of the torrent or how much of it we've saved.
	 * For instance, if we repeatedly get bad data for even 1 piece, getNumCorruptedBytes() could return a size larger than the torrent.
	 * 
	 * When writeBlockImpl() finishes a piece, it hashes it.
	 * If the hash doesn't match the value in the .torrent file, it adds the size of the piece to the count this method returns.
	 * 
	 * @return The number of bytes of corrupted data we threw out
	 */
	synchronized long getNumCorruptedBytes() {

		// Return the count writeBlockImpl() has been adding to
		return _corruptedBytes;
	}

	/**
	 * Make a HashMap with some information from this VerifyingFolder object.
	 * We'll serialize it to disk, and open it later.
	 * 
	 * Includes the verifiedBlocks BitSet, partialBlocks BlockRangeMap, and isVerifying boolean.
	 * 
	 * @return A HashMap
	 */
	synchronized Map getSerializableObject() {

		// Put our verifiedBlocks BitSet, partialBlocks BlockRangeMap, and isVerifying boolean in a new HashMap, and return it
		Map toWrite = new HashMap();
		toWrite.put("verified", verifiedBlocks.clone());
		toWrite.put("partial", partialBlocks.clone());
		toWrite.put("wasVerifying", new Boolean(isVerifying));
		return toWrite;
	}

	/**
	 * Find out how many bytes of torrent data we've received, and are waiting for a thread to write to disk.
	 * 
	 * @return The number of bytes
	 */
	synchronized int getAmountPending() {

		// Total the widths of all the stripes in pendingRanges, the pieces we're waiting to write to disk
		return (int)pendingRanges.byteSize();
	}

	/**
	 * Calculate how many pieces we have that a given BitSet doesn't.
	 * 
	 * @return The number of pieces the computer that has the given BitSet needs from us
	 */
	synchronized int getNumMissing(BitSet other) {

		// If we have all the pieces, we can give the remote computer everything it needs
		if (isComplete())
			return
				verifiedBlocks.cardinality() - // The number of pieces there are, minus
				other.cardinality();           // The number of pieces the remote computer has

		// Copy the BitSet that tells what we have
		BitSet clone = (BitSet)verifiedBlocks.clone();

		// Only keep our 1s that the remote computer doesn't have
		clone.andNot(other); // Now it only shows the pieces we have that the remote computer needs

		// Return the number of 1s that are left, these are those we could give this remote computer
		return clone.cardinality();
	}

	/**
	 * A BlockRangeMap describes the striped pattern within all the pieces of a torrent.
	 * 
	 * BlockRangeMap extends HashMap, giving it keys and values.
	 * A key is a piece number.
	 * The value is an IntervalSet object which describes the striped pattern within that piece.
	 */
	private static class BlockRangeMap extends HashMap {

		/**
		 * Add a single stripe to this BlockRangeMap.
		 * The BlockRangeMap holds the striped pattern within each piece of all the pieces of the torrent.
		 * addInterval() adds a single stripe to a single piece.
		 * 
		 * @param in The piece number, and low and high bounds of the stripe within the piece
		 */
		public void addInterval(BTInterval in) {

			// Look up the IntervalSet for the given BTInterval's piece number
			IntervalSet s = (IntervalSet)get(in.blockId); // s has the striped pattern within the single numbered piece

			// We don't have an IntervalSet for this piece number yet
			if (s == null) {

				// Make a new one
				s = new IntervalSet(); // It starts out empty, with no stripes
				put(in.blockId, s);    // List it in this BlockRangeMap under the piece number
			}

			// Add the given stripe to the pattern for this piece
			s.add(in);
		}

		/**
		 * Remove a single stripe from this BlockRangeMap.
		 * This BlockRangeMap holds the striped pattern within each piece of all the pieces of the torrent.
		 * removeInterval() clears the stripes from an area within a single piece.
		 * 
		 * @param in The piece number, and low and high ranges of the area to clear of stripes
		 */
		public void removeInterval(BTInterval in) {

			// Look up the IntervalSet this BlockRangeMap has for the given piece number
			IntervalSet s = (IntervalSet)get(in.blockId);
			if (s == null) return; // We don't have any stripes in that piece, there's nothing for us to clear

			// Clear the specified area of stripes
			s.delete(in); // delete() can remove multiple stripes, and shorten stripes that overlap the given area

			// If that removed all the stripes in the piece, remove our IntervalSet object for it
			if (s.isEmpty()) remove(in.blockId);
		}

		/**
		 * Get the striped pattern within a single numbered piece.
		 * 
		 * @param in A BTInterval object that has the piece number to look up
		 * @return   An IntervalSet object that describes the striped pattern within it
		 */
		public IntervalSet getSet(BTInterval in) {

			// Look up the IntervalSet in this HashMap for the given piece number
			return (IntervalSet)get(in.blockId); // Calls HashMap.get()
		}

		/**
		 * Get the striped pattern within a single numbered piece.
		 * 
		 * @param id The piece number as an Integer object
		 * @return   An IntervalSet object that describes the striped pattern within it
		 */
		public IntervalSet getSet(Integer id) {

			// Look up the IntervalSet in this HashMap for the given piece number
			return (IntervalSet)get(id); // Calls HashMap.get()
		}

		/**
		 * Calculate the total width of all the stripes in all the striped patterns this BlockRangeMap has.
		 * This BlockRangeMap has a striped pattern for each numbered piece of the torrent.
		 * 
		 * @return The total size, in bytes
		 */
		public long byteSize() {

			// The total we'll return
			long ret = 0;

			// Loop for each IntervalSet in this BlockRangeMap, we have one for each piece number
			for (Iterator iter = values().iterator(); iter.hasNext(); ) { // Calls HashMap.values()
				IntervalSet set = (IntervalSet)iter.next();

				// Have set total the width of all its stripes, and add that to the value we'll return
				ret += (long)set.getSize();
			}

			// Return the total we calculated
			return ret;
		}
	}

	/**
	 * A FullBitSet object is a BitSet that has all 1s.
	 * Instead of having a big array of bytes with bits set all to 1, it just returns true when you call the method to query a bit.
	 */
	private class FullBitSet extends BitSet {

		/** Does nothing. */
		public void set(int i) {}

		/** Does nothing. */
		public void clear(int i) {}

		/**
		 * Determine if a bit in this FullBitSet is set to 1.
		 * 
		 * @param i The bit index
		 * @return  true, all the bits are set to 1
		 */
		public boolean get(int i) {

			// Always return true
			return true;
		}

		/**
		 * Count the number of bits set to true.
		 * 
		 * @return The number of blocks in this torrent, we have them all
		 */
		public int cardinality() {

			// Return the number of blocks in this torrent, we have them all
			return _info.getNumBlocks();
		}

		/**
		 * Count how many bits this bit set has.
		 * 
		 * @return The number of blocks in this torrent, a bit set has a bit for each one
		 */
		public int length() {

			// Return the number of blocks in this torrent, our bit set has a bit for each one
			return _info.getNumBlocks();
		}
	}
}
