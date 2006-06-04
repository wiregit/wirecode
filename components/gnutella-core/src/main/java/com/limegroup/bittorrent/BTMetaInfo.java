
// Commented for the Learning branch

package com.limegroup.bittorrent;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.security.SHA1;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.FileUtils;

import com.limegroup.bittorrent.bencoding.BEncoder;
import com.limegroup.bittorrent.bencoding.Token;

/**
 * A BTMetaInfo object represents a .torrent file, and the bencoded information inside.
 * 
 * Give the constructor a clump of Java objects made from parsing bencoded data.
 * Then, call methods like getPieceLength() and getFiles() to easily read the information in the bencoded data.
 * 
 * A BTMetaInfo object has references to other objects that are made for each torrent the program is sharing.
 * _torrent links up to the ManagedTorrent object that represents the torrent.
 * _folder links to a VerifyingFolder that can check hashes and produce the bit field.
 * 
 * This class has some code that goes beyond representing the data inside a .torrent.
 * readObject() and writeObject() do serialization to disk.
 * addTracker(address) lets you list another tracker alongside those that came from the .torrent file.
 * getFiles() returns TorrentFile objects that have paths like "C:\Documents and Settings\Kevin\Incomplete\Torrent Name\Folder\Name.ext".
 */
public class BTMetaInfo implements Serializable {

	/** A debugging log we can write lines of text to as the program runs. */
	private static final Log LOG = LogFactory.getLog(BTMetaInfo.class);

	/** A number that identifies this version of this type of object when it's serialized to a file on the disk. */
	static final long serialVersionUID = -2693983731217045071L;

	/** Not used. */
	private static final ObjectStreamField[] serialPersistentFields = ObjectStreamClass.NO_FIELDS;

	/**
	 * A list of a single all-0 SHA1 hash to use in place of a list of actual file hashes.
	 * 
	 * FAKE_URN_SET is a HashSet that contains a single URN object, holding a SHA1 hash with 20 0 bytes.
	 * Written in base 32, 20 bytes of 0s looks like "urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA".
	 * 
	 * A FakeFileDesc object uses this list of URNs instead of some that would describe real files.
	 */
	private static final Set FAKE_URN_SET = new HashSet();
	static {
		try {
			FAKE_URN_SET.add(URN.createSHA1Urn("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
		} catch (IOException ioe) {
			ErrorService.error(ioe);
		}
	}

	/**
	 * The SHA1 hashes of the file pieces.
	 * _hashes is an array of 20-byte arrays.
	 */
	private byte[][] _hashes;

	/**
	 * The size in bytes of the pieces this torrent has broken its data into.
	 * All of the pieces are this size except for the last one, which is probably smaller.
	 * 
	 * In the .torrent file, this is the bencoded number value of "info"."piece length".
	 */
	private int _pieceLength;

	/**
	 * The name of the file this .torrent describes, like "File Name.ext".
	 * For a multifile torrent, this is the name of the folder we should save all the files in, like "Folder Name".
	 * 
	 * In the .torrent file, this is the bencoded string value of "info"."name".
	 */
	private String _name;

	/**
	 * An array of TorrentFile objects that contain information about the files listed in this .torrent file.
	 * If this is a single-file .torrent, there is just 1 TorrentFile object in the array.
	 * 
	 * A TorrentFile object in this list is like this:
	 * 
	 * _files[0].LENGTH  is the size of the file, in bytes
	 * _files[0].PATH    is the path to where we'll save it like "C:\Documents and Settings\Kevin\Incomplete\Torrent Name\Folder\Name.ext"
	 * _files[0].begin   is the piece number where the file's data begins
	 * _files[0].end     is the piece number where the file's data ends
	 */
	private TorrentFile[] _files;

	/**
	 * The path to where we're saving this torrent's file, like "C:\Documents and Settings\Kevin\Incomplete\File Name.ext".
	 * For a multifile torrent, this is the path to the folder, like "C:\Documents and Settings\Kevin\Incomplete\Folder Name".
	 * 
	 * This is where we'll save the torrent as we're downloading it.
	 * A Java File object that holds the path.
	 */
	private File _incompleteFile;

	/**
	 * The path to where we'll move this torrent's file when we've downloaded it completely, like "C:\Documents and Settings\Kevin\Shared\File Name.ext".
	 * For a multifile torrent, this is the path to the folder, like "C:\Documents and Settings\Kevin\Shared\Folder Name".
	 * 
	 * This is where we'll move the torrent when it's done.
	 * A Java File object that holds the path.
	 */
	private File _completeFile;

	/**
	 * The "info" dictionary of the .torrent file.
	 * A Java HashMap that contains other Java objects we made by parsing the bencoded data.
	 * 
	 * A map containing the _infoMap of this torrent, - the bencoded value of
	 * this map is a unique identifier for the torrent. It is not necessary to
	 * store it, but at a later date we may want to add support for certain
	 * extensions stored in this data field
	 */
	private Map _infoMap;

	/**
	 * The info hash, the hash BitTorrent uses to identify this torrent and its file.
	 * 
	 * To compute the info hash, take the SHA1 hash of the value of "info" in the bencoded data of the .torrent file.
	 * The value of "info" is a bencoded dictionary.
	 */
	private byte[] _infoHash;

	/**
	 * The torrent's info hash as a text URN, like "urn:sha1:JAZSGOLT6UP4I5N5KGJRZPSF6RZCEJKQ".
	 * The info hash is the SHA1 hash of the "info" section of the bencoded data of the .torrent file.
	 */
	private URN _infoHashURN;

	/**
	 * This torrent's VerifyingFolder object, which keeps track of which pieces we have and validates their hashes.
	 * 
	 * Don't serialize _folder to disk.
	 */
	private VerifyingFolder _folder;

	/**
	 * An array of Java URL objects, each of which is the address of a tracker.
	 * In the .torrent file, these addresses are listed under "announce".
	 * 
	 * First, the BTMetaInfo constructor adds the tracker listed under "announce" in the .torrent file.
	 * Then, TorrentManager.download() calls addTracker(url) to add more that we find out about.
	 */
	private URL[] _trackers;

	/** Always null, not used. */
	private Set _locations = null;

	/**
	 * The total size in bytes of the files this .torrent file describes.
	 * If this is a single file torrent, _totalSize is the file size.
	 * If this is a multi-file torrent, _totalSize is the size of all the files totaled, and the size of the data block made by putting all the files together.
	 */
	private long _totalSize;

	/** A link to the ManagedTorrent object that represents this torrent. */
	private transient ManagedTorrent _torrent = null;

	/**
	 * A FakeFileDesc object with the save path, like "C:\Documents and Settings\Kevin\Shared\File Name.ext".
	 * This is the kind of object the GUI needs to be able to read the path and list the file.
	 */
	private transient FileDesc _desc = null;

	/**
	 * Get the size in bytes of the pieces this torrent has broken its data into.
	 * All of the pieces are this size except for the last one, which is probably smaller.
	 * 
	 * In the .torrent file, this is the bencoded number value of "info"."piece length".
	 * 
	 * @return The size of a piece
	 */
	public int getPieceLength() {

		// Get the piece size we parsed
		return _pieceLength;
	}

	/**
	 * Not used.
	 * 
	 * @return null
	 */
	public Set getLocations() {
		return _locations;
	}

	/**
	 * Get an array of TorrentFile objects that contain information about the files listed in this multifile .torrent file.
	 * If this is a single-file .torrent, there is just 1 TorrentFile object in the array.
	 * 
	 * A TorrentFile object in this list is like this:
	 * 
	 * files[0].LENGTH  is the size of the file, in bytes
	 * files[0].PATH    is the path to where we'll save it like "C:\Documents and Settings\Kevin\Incomplete\Torrent Name\Folder\Name.ext"
	 * files[0].begin   is the piece number where the file's data begins
	 * files[0].end     is the piece number where the file's data ends
	 * 
	 * @return An ArrayList of TorrentFile objects
	 */
	public TorrentFile[] getFiles() {

		// Return the ArrayList that the BTMetaInfo constructor made
		return _files;
	}

	/**
	 * Get the path to where we're saving this torrent's file, like "C:\Documents and Settings\Kevin\Incomplete\File Name.ext".
	 * For a multifile torrent, this is the path to the folder, like "C:\Documents and Settings\Kevin\Incomplete\Folder Name".
	 * This is where we'll save the torrent as we're downloading it.
	 * 
	 * @return A Java File object that holds the path.
	 */
	public File getBaseFile() {

		// Return the path
		return _incompleteFile;
	}

	/**
	 * Get the path to where we'll move this torrent's file when we've downloaded it completely, like "C:\Documents and Settings\Kevin\Shared\File Name.ext".
	 * For a multifile torrent, this is the path to the folder, like "C:\Documents and Settings\Kevin\Shared\Folder Name".
	 * 
	 * @return A Java File object that holds the path
	 */
	public File getCompleteFile() {

		// If we haven't composed the path, yet, do it and return it
		if (_completeFile == null) _completeFile = new File(SharingSettings.getSaveDirectory(), _name); // Get the path to the "Shared" folder
		return _completeFile;
	}

	/**
	 * Make a FileDesc object with the save path, like "C:\Documents and Settings\Kevin\Shared\File Name.ext".
	 * This is the kind of object the GUI needs to be able to read the path and list the file.
	 * 
	 * Returns a new FileDesc object that is actually a FakeFileDesc.
	 * FakeFileDesc is a nested class in this BTMetaInfo class.
	 * 
	 * @return A FakeFileDesc object that has the path
	 */
	public FileDesc getFileDesc() {

		// If we haven't made _desc yet, make it now
		if (_desc == null) {

			// Wrap the path like "C:\Documents and Settings\Kevin\Shared\File Name.ext" into a new FakeFileDesc object
			_desc = new FakeFileDesc(
				_completeFile == null ? // If we haven't composed the path in the "Shared" folder yet
				_incompleteFile :       // Use the path with the "Incomplete" folder instead, otherwise
				_completeFile);         // Use the "Shared" folder path
		}

		// Return the FileDesc object the GUI needs to read the path
		return _desc;
	}

	/**
	 * Get the hash of a piece of the file this .torrent describes.
	 * 
	 * @param pieceNum The piece number, 0 for the first piece
	 * @return         A 20-byte array with the SHA1 hash of that piece of file data
	 */
	public byte[] getHash(int pieceNum) {

		// Look up the 20-byte array in the _hashes array of them
		return _hashes[pieceNum];
	}

	/**
	 * Get the info hash, the hash BitTorrent uses to identify this torrent and its file.
	 * 
	 * To compute the info hash, take the SHA1 hash of the value of "info" in the bencoded data of the .torrent file.
	 * The value of "info" is a bencoded dictionary.
	 * 
	 * @return The 20-byte SHA1 info hash of this .torrent file
	 */
	public byte[] getInfoHash() {

		// Return the hash the BTMetaInfo constructor computed
		return _infoHash;
	}

	/**
	 * Get the torrent's info hash as a text URN, like "urn:sha1:JAZSGOLT6UP4I5N5KGJRZPSF6RZCEJKQ".
	 * The info hash is the SHA1 hash of the "info" section of the bencoded data of the .torrent file.
	 * 
	 * @return A String like "urn:sha1:JAZSGOLT6UP4I5N5KGJRZPSF6RZCEJKQ"
	 */
	public URN getURN() {

		// Return the String we composed
		return _infoHashURN;
	}

	/**
	 * Get the VerifyingFolder object this torrent is using to check piece hashes and save files to disk.
	 * 
	 * @return The VerifyingFolder object
	 */
	public VerifyingFolder getVerifyingFolder() {

		// Return the VerifyingFolder object we saved
		return _folder;
	}

	/**
	 * Move the torrent file or folder we downloaded from the "Incomplete" folder to the "Shared" folder.
	 * Adds it to the program's library of files we're sharing.
	 * Replaces "Incomplete" with "Shared" in the paths in the _files list, and remakes the VerifyingFolder object.
	 * 
	 * @return false on error
	 */
	public boolean moveToCompleteFolder() {

		// Move the torrent file or folder we downloaded from the "Incomplete" folder to the "Shared" folder, and add it to the program's library of files we're sharing
		if (!saveFile(_incompleteFile, SharingSettings.DIRECTORY_FOR_SAVING_FILES.getValue())) return false; // Returns false on error

		// Make a Java File object with the destionation path, like "C:\Documents and Settings\Kevin\Shared\File Name.ext"
		_completeFile = new File(SharingSettings.DIRECTORY_FOR_SAVING_FILES.getValue(), _name);

		// We don't need the FakeFileDesc to show to LimeWire's GUI anymore
		_desc = null;

		// Replace "Incomplete" with "Shared" in the paths to files in the _files list
		updateReferences(_completeFile);

		// Remake this BTMetaInfo object's VerifyingFolder object now that we've moved the files
		LOG.trace("saved files");
		initializeVerifyingFolder(null, true); // true, we're done downloading this torrent
		LOG.trace("initialized folder");

		// Report success
		return true;
	}

	/**
	 * Send a Have message with the given piece number to all our connections sharing this torrent.
	 * If the VerifyingFolder has downloaded the complete torrent, move it into our "Shared" folder.
	 * 
	 * Have this BTMetaInfo object tell its ManagedTorrent object that we've downloaded and verified a piece of this torrent.
	 * VerifyingFolder.notifyOfChunkCompletion(pieceNum) calls this.
	 * Calls ManagedTorrent.notifyOfComplete(pieceNum).
	 * 
	 * @param pieceNum The piece we just got
	 */
	public void notifyOfComplete(int pieceNum) {

		// If we have a reference to our ManagedTorrent object, tell it
		if (_torrent != null) _torrent.notifyOfComplete(pieceNum);
	}

	/**
	 * Give this BTMetaInfo object a reference to the ManagedTorrent that represents the same .torrent file.
	 * We'll inform this ManagedTorrent object about ranges of completed data.
	 * 
	 * @param torrent The ManagedTorrent reference to save
	 */
	public void setManagedTorrent(ManagedTorrent torrent) {

		// Save the given reference
		_torrent = torrent;
	}

	/**
	 * Get the total size in bytes of the files this .torrent file describes.
	 * 
	 * If this is a single file torrent, _totalSize is the file size.
	 * If this is a multi-file torrent, _totalSize is the size of all the files totaled, and the size of the data block made by putting all the files together.
	 * 
	 * @return The number of bytes of data of this .torrent file describes
	 */
	public long getTotalSize() {

		// Return the size we read or totaled
		return _totalSize;
	}

	/**
	 * Total the lengths in an array of TorrentFile objects.
	 * This is the total size of all of the files that a .torrent file describes.
	 * 
	 * @param files An array of TorrentFile objects
	 * @return      The total of the length number each one keeps
	 */
	private static long calculateTotalSize(TorrentFile[] files) {

		// Loop for each TorrentFile in the array
		long ret = 0; // Start our total at 0
		for (int i = 0; i < files.length; i++) {

			// Add this TorrentFile's LENGTH to our total
			ret += files[i].LENGTH;
		}

		// Return the total size we summed
		return ret;
	}

	/**
	 * Calculate the number of pieces this .torrent has broken its file into.
	 * 
	 * @return The number of pieces
	 */
	public int getNumBlocks() {

		// Calculate the number of pieces, including the last fragment piece
		return (int)((_totalSize + _pieceLength - 1) / _pieceLength);
	}

	/**
	 * Get the "info" dictionary of the .torrent file.
	 * 
	 * @return A Java HashMap that contains other Java objects we made by parsing the bencoded data
	 */
	public Map getInfo() {

		// Return the HashMap we parsed
		return _infoMap;
	}

	/**
	 * Get the name of the file this .torrent describes, like "File Name.ext".
	 * For a multifile torrent, this is the name of the folder we should save all the files in, like "Folder Name".
	 * 
	 * In the .torrent file, this is the bencoded string value of "info"."name".
	 * 
	 * @return The name text as a String
	 */
	public String getName() {

		// Return the name text we parsed
		return _name;
	}

	/**
	 * Get the Web addresses of the trackers described in this .torrent file.
	 * These addresses are listed under "announce" in the bencoded data.
	 * 
	 * @return An array of Java URL objects
	 */
	public URL[] getTrackers() {

		// Return the array we made
		return _trackers;
	}

	/**
	 * Add the Web address of another tracker to the list of them this BTMetaInfo keeps.
	 * A BTMetaInfo is made from a .torrent file, and starts out with the one tracker address there.
	 * TorrentManager.download() calls this to add more to the list.
	 * 
	 * @param url The Web address of another tracker keeping track of this torrent
	 * @return    false if we already have that tracker
	 *            true if we didn't, and added it
	 */
	public boolean addTracker(URL url) {

		// Make sure we don't already have that tracker
		for (int i = 0; i < _trackers.length; i++) if (_trackers[i].equals(url)) return false;

		// Add it to the _trackers list, and return true
		URL[] newTrackers = new URL[_trackers.length + 1];
		System.arraycopy(_trackers, 0, newTrackers, 0, _trackers.length);
		newTrackers[_trackers.length] = url;
		_trackers = newTrackers;
		return true;
	}

	/**
	 * Returns a new SHA1 object to signify that BTMetaInfo and BitTorrent use SHA1 to hash the "info" part of the .torrent file into the info hash.
	 * 
	 * @return A new blank SHA1 object
	 */
	public MessageDigest getMessageDigest() {

		// Make and return a new empty SHA1 object
		return new SHA1();
	}

	/**
	 * Make a bitfield that shows what pieces of this torrent we have.
	 * The bitfield will have 1 bit for each piece in the torrent.
	 * If a bit is 1, it means we have that piece and have checked its hash, 0 means we still need it.
	 * 
	 * Gets the bitfield from the VerifyingFolder object for this torrent.
	 * Sends it to the remote computer in a Bit Field message.
	 * 
	 * @return A byte array with the bit field
	 */
	public byte[] createBitField() {

		// Have our VerifyingFolder object compose the bit field, and return it
		return _folder.createBitField();
	}

	/**
	 * Turn the data of a .torrent file into a new BTMetaInfo object that represents it.
	 * 
	 * @param torrent A byte array with the contents of a .torrent file we opened or downloaded
	 * @return        A new BTMetaInfo that represents it
	 */
	public static BTMetaInfo readFromBytes(byte[] torrent) throws IOException {

		// Parse the bencoded data of the .torrent file into a Java object that references others to express its structure
		Object metaInfo = Token.parse(torrent); // Returns a Java Map that contains other Java objects

		// Use the clump of Java objects to make a BTMetaInfo object
		return new BTMetaInfo(metaInfo);
	}

	/**
	 * Move a file or folder from the "Incomplete" folder to the "Shared" folder, and add it to the program's library of files we're sharing.
	 * 
	 * @param incFile        The path to a file or folder in the "Incomplete" folder
	 * @param completeParent The path to the folder in the "Shared" folder where we should move it
	 * @return               True if successful, false on error
	 */
	private boolean saveFile(File incFile, File completeParent) {

		// Call getCanonicalFile() to resolve any navigational codes in the path, like "./"
		try {
			completeParent = completeParent.getCanonicalFile();
		} catch (IOException ioe) {
			if (LOG.isDebugEnabled()) LOG.debug(ioe);
			return false;
		}

		// Make sure we can write to the destination folder
		FileUtils.setWriteable(completeParent);

		// If the given source path points to a folder, have saveDirectory() move it instead of us
		if (incFile.isDirectory()) return saveDirectory(incFile, completeParent);

		/*
		 * If control reaches here, we're moving a file
		 */

		// Get the size of the file on the disk in bytes
		long incLength = incFile.length();

		// Make completeFile the file's destination path
		File completeFile = new File(completeParent, incFile.getName());

		// Move the file from incFile to completeFile
		completeFile.delete();                               // Delete a file that's already there
		FileUtils.setWriteable(completeFile);                // There shouldn't be a file there to set writable
		if (!FileUtils.forceRename(incFile, completeFile)) { // Move the file from incFile to completeFile
			LOG.debug("could not rename file " + incFile);
			return false;
		}

		// Make sure that didn't change the file size
		if (incLength != completeFile.length()) {
			LOG.debug("length of complete file does not match incomplete file " + completeFile + " , " + incLength + ":" + completeFile.length());
			return false;
		}

		// Add the file to LimeWire's Library of files we're sharing on Gnutella
		RouterService.getFileManager().removeFileIfShared(completeFile);
		RouterService.getFileManager().addFileIfShared(completeFile);

		// Report success
		return true;
	}

	/**
	 * Make a new VerifyingFolder object from this BTMetaInfo one.
	 * Saves it as _folder.
	 * 
	 * @param data     A Java Map that represents the serialized form of a VerifyingFolder object we read from a file on the disk
	 * @param complete True if we're done downloading this torrent
	 */
	private void initializeVerifyingFolder(Map data, boolean complete) {

		// Make a new VerifyingFolder object that represents the save folder and can check the hash of file pieces
		_folder = new VerifyingFolder(
			this,     // Give the constructor a link back to this BTMetaInfo object
			complete, // True if we're done downloading this torrent
			data);    // Serialized disk data, use this instead if we have it
	}

	/**
	 * Move a folder of files from the "Incomplete" folder to the "Shared" folder, and add it to the program's library of files we're sharing.
	 * 
	 * @param incFile        The path to a folder in the "Incomplete" folder
	 * @param completeParent The path to the folder in the "Shared" folder where we should move it
	 * @return               True if successful, false on error
	 */
	private boolean saveDirectory(File incFile, File completeParent) {

		// Compose the folder's destination path
		File completeDir = new File(completeParent, incFile.getName());

		// Make the destination folder, deleting a file already there
		if (completeDir.exists()) { // If there is a file at the destination path, delete it
			if (!completeDir.isDirectory()) {
				if (!(completeDir.delete() && completeDir.mkdirs())) {
					LOG.debug("could not create complete dir " + completeDir);
					return false;
				}
			}
		} else if (!completeDir.mkdirs()) { // The path is open, but we can't make a folder there
			LOG.debug("could not create complete dir " + completeDir);
			return false;
		}
		FileUtils.setWriteable(completeDir); // Set the destination folder writable

		// Share the folder before we start putting files into it
		RouterService.getFileManager().addFileIfShared(completeDir);

		// Loop for each file and folder in the source folder
		File[] files = incFile.listFiles();
		for (int i = 0; i < files.length; i++) {

			// Move it into the destination folder we made
			if (!saveFile(files[i], completeDir)) return false; // Return false on error
		}

		// Delete the source folder we emptied
		FileUtils.deleteRecursive(incFile);

		// Report success
		return true;
	}

	/**
	 * Replace "Incomplete" with "Shared" in the paths to files in the _files list.
	 * 
	 * @param completeBase The path we moved this torrent to when it finished, like "C:\Documents and Settings\Kevin\Shared\File Name.ext"
	 */
	private void updateReferences(File completeBase) {

		// _incompleteFile is like "C:\Documents and Settings\Kevin\Incomplete\File Name.ext", get the length of that string
		int offset = _incompleteFile.getAbsolutePath().length();

		// Make newPath like "C:\Documents and Settings\Kevin\Shared\File Name.ext"
		String newPath = completeBase.getAbsolutePath();

		// Loop for each of the files this .torrent file describes
		for (int i = 0; i < _files.length; i++) { // If this is a single-file torrent, this loop will just run once

			// Change the path of this file from the "Incomplete" folder to the "Shared" folder
			_files[i] = new TorrentFile(_files[i].LENGTH, newPath + _files[i].PATH.substring(offset));
		}
	}

	/**
	 * Make a new BTMetaInfo object to hold the information in a .torrent file.
	 * 
	 * The program has already opened a .torrent file, and parsed its bencoded dictionary into a Java HashMap object.
	 * This constructor looks through that clump of objects, making a BTMetaInfo object with the information in the .torrent file.
	 * 
	 * This constructor takes a Java object.
	 * Token.parse() turned the bencoded data from a .torrent file, and parsed it into a clump of Java objects.
	 * Since a .torrent contains a bencoded dictionary, t_metaInfo will be a Java HashMap.
	 * 
	 * This constructor is private, and only readFromBytes() calls it.
	 * To make a new BTMetaInfo object, call the static method BTMetaInfo.readFromBytes().
	 * 
	 * @param  t_metaInfo     A Java HashMap that contains other Java objects.
	 * @throws ValueException A part of the .torent file we looked for is missing or wrong.
	 */
	private BTMetaInfo(Object t_metaInfo) throws ValueException {

		// We don't know where we'll save the torrent yet
		_completeFile = null;

		// Make sure the given object is a Map, and cast it that way
		if (!(t_metaInfo instanceof Map)) throw new ValueException("Unknown type of MetaInfo");
		Map metaInfo = (Map)t_metaInfo;

		/*
		 * get the trackers, we only expect one tracker, more trackers may be
		 * added by the addTracker() method, we will throw an exception if the
		 * tracker is invalid or does not even exist.
		 */

		// Get the value of the "announce" key
		Object t_announce = metaInfo.get("announce"); // t_announce is the value of "accounce", the Web address of the tracker
		if (!(t_announce instanceof byte[])) throw new ValueException("bad metainfo - no tracker");
		String url = getString((byte[])t_announce); // Convert the ASCII bytes into a String

		try {

			/*
			 * Note: this kills UDP trackers so we will eventually use a different object.
			 */

			// Turn the Web address into a Java URL object, and save that as the only entry in _trackers, an array of URL objects
			_trackers = new URL[] { new URL(url) };

		} catch (MalformedURLException mue) { throw new ValueException("bad metainfo - bad tracker"); }

		/*
		 * add proper support for multi-tracker torrents later.
		 */

		/*
		 * In the .torrent file, there are two main sections:
		 * "announce" has the address of the tracker
		 * "info" has the information about the file
		 */

		// Look up the "info" dictionary item, which has all the information about the files this torrent describes
		Object t_info = metaInfo.get("info");
		if (!(t_info instanceof Map)) throw new ValueException("bad metainfo - bad info");
		Map info = (Map)t_info;

		// Within "info", get "pieces", the hashes of all the file pieces
		Object t_pieces = info.get("pieces");
		if (!(t_pieces instanceof byte[])) throw new ValueException("bad metainfo - no pieces key found");

		// Split apart the 20-byte SHA1 hashes, and store them in the _hashes array of 20-byte arrays
		_hashes = parsePieces((byte[])t_pieces); // Now, you can look up the hash of piece 0 with _hashes[0]

		// Within "info", get "piece length", the size in bytes of the pieces the torrent has broken this file into
		Object t_pieceLength = info.get("piece length");
		if (!(t_pieceLength instanceof Long)) throw new ValueException("bad metainfo - illegal piece length");
		_pieceLength = (int)((Long)t_pieceLength).longValue();
		if (_pieceLength <= 0) throw new ValueException("bad metainfo - illegal piece length");

		/*
		 * name of the torrent, also specifying the directory under which to
		 * save the torrents, as per extension spec, name.urf-8 specifies the
		 * utf-8 name of the torrent
		 */

		// Within "info", get "name", the file or folder name we should save this torrent to
		String name = null;
		Object t_name = info.get("name");
		if (!(t_name instanceof byte[])) throw new ValueException("bad metainfo - bad name");
		if (info.containsKey("name.utf-8")) {                                  // "info" may also have the key "name.utf-8", the name in UTF-8, which is better than ASCII
			try {
				name = new String((byte [])info.get("name.utf-8"), Constants.UTF_8_ENCODING);
			} catch (UnsupportedEncodingException uee) {}
		}
		if (name == null) name = getString((byte[])t_name);                    // Otherwise, it's just regular ASCII
		_name = CommonUtils.convertFileName(name);                             // Replace characters that can't be in a file name to underscore
		if (_name.length() == 0) throw new ValueException("bad torrent name"); // Make sure the torrent has a file name

		// A .torent can have "files" or "length", but it can't have both, and it can't have neither
		if (info.containsKey("files") == info.containsKey("length")) throw new ValueException("single/multiple file mix");

		// Make a new Java File object that has the path to the file or folder we'll save this torrent at
		_incompleteFile = new File(
			SharingSettings.INCOMPLETE_DIRECTORY.getValue(), // The path to LimeWire's temporary folder, like "C:\Documents and Settings\Kevin\Incomplete"
			_name);                                          // The folder or file name from the .torrent file, like "File Name.ext" or "Folder Name"

		// "info" has the key "files", this .torrent describes a list of files we'll save in a folder
		if (info.containsKey("files")) {

			// Get the value of "files", which is a list
			Object t_files = info.get("files");
			if (!(t_files instanceof List)) throw new ValueException("bad metainfo - bad files value");

			// Parse "info"."files" into an ArrayList of TorrentFile objects
			List files = parseFiles((List)t_files, _incompleteFile.getAbsolutePath());
			if (files.size() == 0) throw new ValueException("bad metainfo - bad files value " + t_files);

			/*
			 * files is an ArrayList of TorrentFile objects.
			 * A TorrentFile object has the path where we'll save a file, and it's length.
			 * It also has begin and end, the piece numbers where the file starts and finishes.
			 * These piece numbers aren't in the .torrent file, but we can calculate them.
			 * We know the piece size, and the file size.
			 */

			// Calculate the piece numbers where each file starts and ends
			long position = 0;                                         // position is our length in bytes into the file, starting at 0, the start
			for (Iterator iter = files.iterator(); iter.hasNext(); ) { // Loop for each TorrentFile object we parsed
				TorrentFile file = (TorrentFile)iter.next();

				// Save the piece number where this file begins and ends
				file.begin = (int)(position / _pieceLength);
				position += file.LENGTH;
				file.end = (int)(position / _pieceLength); // If the file ends on a piece boundary, end will be the next piece
			}

			// Move the TorrentFile objects into _files
			_files = new TorrentFile[files.size()];
			files.toArray(_files);

		// "info" has the key "length", identifying this .torrent file as a single-file torrent
		} else {

			// Get the value of "length", the size of the file in bytes
			Object t_length = info.get("length");
			if (!(t_length instanceof Long)) throw new ValueException("bad metainfo - bad file length");
			long length = ((Long)t_length).longValue();

			// Setup _files an an array TorentFile objects that only has 1 for the 1 file described here
			_files = new TorrentFile[1];

			try {

				// Make a new TorrentFile object to represent the single file described in this .torrent
				_files[0] = new TorrentFile(
					length,                              // The file size
					_incompleteFile.getCanonicalPath()); // The path to LimeWire's temporary folder, like "C:\Documents and Settings\Kevin\Incomplete"

				// The file starts in piece 0
				_files[0].begin = 0;

				// Set end to the number of pieces
				_files[0].end = _hashes.length;

			// getCanonicalPath() threw an exception
			} catch (IOException ie) { throw new ValueException("bad metainfo - file path"); }
		}

		/*
		 * The info hash is the hash of the "info" dictionary in a .torrent file.
		 * BitTorrent programs use the info hash to identify the torrent.
		 * The info hash isn't in the .torrent, because any program that has the .torrent can calculate it for itself.
		 */

		/*
		 * create the info hash, we could create the info hash while reading it
		 * but that would make the code a lot more complex. This works well too,
		 * because the order of a list is not changed during the process of
		 * decoding or encoding it and Maps are always sorted alphanumerically
		 * when encoded.
		 * So the data we encoded is always exactly the same as the data before
		 * we decoded it. This is intended that way by the protocol.
		 */

		// Turn info, the Java HashMap we parsed the "info" dictionary into, back into bencoded data
		ByteArrayOutputStream baos = new ByteArrayOutputStream(); // Make a ByteArrayOutputStream that will grow as we add data to it
		try {
			BEncoder.encodeDict(baos, info);
		} catch (IOException ioe) { ErrorService.error(ioe); }

		// Compute the SHA1 hash of the bencoded data
		MessageDigest md = new SHA1(); // LimeWire's SHA1 class extend's Java's MessageDigest class
		_infoHash = md.digest(baos.toByteArray());

		// Turn it into a URN object that holds text like "urn:sha1:JAZSGOLT6UP4I5N5KGJRZPSF6RZCEJKQ"
		try {
			_infoHashURN = URN.createSHA1UrnFromBytes(_infoHash);
		} catch (IOException impossible) { ErrorService.error(impossible); }

		// Save the parsed "info" HashMap as _infoMap, and make sure no one else can change it
		_infoMap = Collections.unmodifiableMap(info);

		// Add up the length of all the files, and save the total size
		_totalSize = calculateTotalSize(_files);

		// Make a new VerifyingFolder object from this BTMetaInfo one
		initializeVerifyingFolder(null, false); // false, we're not done downloading this torrent yet
	}

	/**
	 * Convert a byte array of ASCII text characters into a Java String object.
	 * 
	 * @param bytes A byte array that contains ASCII text.
	 * @return      The text converted to a Java String object.
	 *              null on error.
	 */
	private static String getString(byte[] bytes) {

		try {

			// Make a new String, using the default ASCII encoding
			return new String(bytes, Constants.ASCII_ENCODING);

		} catch (UnsupportedEncodingException impossible) {

			// Java couldn't make that conversion, return null instead.
			ErrorService.error(impossible);
			return null;
		}
	}

	/**
	 * Save this BTMetaInfo object to data.
	 * 
	 * @param out An ObjectOutputStream object we can call writeObject() on to give it objects
	 */
	private synchronized void writeObject(ObjectOutputStream out) throws IOException {

		// List all the parts of this BTMetaInfo object that we want to save in a HashMap
		Map toWrite = new HashMap();
		toWrite.put("_hashes", _hashes);
		toWrite.put("_pieceLength", new Integer(_pieceLength));
		toWrite.put("_name", _name);
		toWrite.put("_files", _files);
		toWrite.put("_completeFile", _completeFile);
		toWrite.put("_incompleteFile", _incompleteFile);
		toWrite.put("_infoMap", _infoMap);
		toWrite.put("_infoHash", _infoHash);
		toWrite.put("_trackers", _trackers);
		toWrite.put("_totalSize", new Long(_totalSize));
		toWrite.put("folder data", _folder.getSerializableObject());

		// Serialize the HashMap to the given ObjectOutputStream
		out.writeObject(toWrite);
	}

	/**
	 * Read data to make all the parts of this BTMetaInfo object.
	 * 
	 * @param in An ObjectInputStream object we can call readObject() on to get objects from
	 */
	private synchronized void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {

		// Read the HashMap we made and saved with all the other objects inside
		Map toRead = (Map)in.readObject();

		// Get each unserialized object out of it, and save it in the member variables of this BTMetaInfo object
		_hashes = (byte[][])toRead.get("_hashes");
		Integer pieceLength = (Integer)toRead.get("_pieceLength");
		_name = (String)toRead.get("_name");
		_files = (TorrentFile[])toRead.get("_files");
		_incompleteFile = (File)toRead.get("_incompleteFile");
		_completeFile = (File)toRead.get("_completeFile");
		_infoMap = (Map)toRead.get("_infoMap");
		_infoHash = (byte[])toRead.get("_infoHash");
		_trackers = (URL[])toRead.get("_trackers");
		Long totalSize = (Long)toRead.get("_totalSize");
		Map folderData = (Map)toRead.get("folder data");

		// Make sure we got everything
		if (_hashes == null ||
			pieceLength == null ||
			_name == null ||
			_files == null ||
			_incompleteFile == null ||
			_completeFile == null ||
			_infoMap == null ||
			_infoHash == null ||
			_trackers == null ||
			totalSize == null ||
			folderData == null)
			throw new IOException("cannot read BTMetaInfo");

		// Save numbers
		_pieceLength = pieceLength.intValue();
		_totalSize = totalSize.longValue();

		// Make the VerifyingFolder object from this BTMetaInfo one
		initializeVerifyingFolder(folderData, false); // false, we're not done downloading this torrent yet
	}

	/**
	 * Parse "info"."files" into an ArrayList of TorrentFile objects.
	 * 
	 * In a multifile .torrent, "info"."files" is a list.
	 * Each item in the list has the save path like "Folder\Folder\Name.ext" and size of a file.
	 * parseFiles() turns each list item into a TorrentFile object, and returns an ArrayList of them.
	 * The paths in the TorentFile objects are complete with the path to LimeWire's Incomplete folder and the "Torrent Name" folder.
	 * 
	 * @param files    A Java ArrayList with the Java objects we made from reading the "info"."files" part of the .torrent.
	 * @param basePath The path to LimeWire's temporary folder, and the folder name from "info"."name".
	 *                 Like "C:\Documents and Settings\Kevin\Incomplete\Folder Name".
	 * @return         An ArrayList of TorrentFile objects.
	 *                 Each TorrentFile object has information about a file listed in the .torrent.
	 *                 It has the path to where we'll save it on disk, like "C:\Documents and Settings\Kevin\Incomplete\Torrent Name\Folder\Folder\File.ext".
	 *                 It also has the file size.
	 */
	private static List parseFiles(List files, String basePath) throws ValueException {

		// Make an empty ArrayList for us to fill and return
		ArrayList ret = new ArrayList();

		// Loop for each item in the "files" list
		for (Iterator iter = files.iterator(); iter.hasNext(); ) {
			Object t_file = iter.next();
			if (!(t_file instanceof Map)) throw new ValueException("bad metainfo - bad file value");

			// Parse the file's path and size, wrap those in a TorrentFile object, and add it to ret
			ret.add(parseFile((Map)t_file, basePath));
		}

		// Return the ArrayList of TorrentFile objects we made
		return ret;
	}

	/**
	 * Compose the save path and get the file size of a single file listed in a multifile .torrent.
	 * 
	 * In a multifile .torrent, "info"."files" is a list.
	 * Each item in the list is a dictionary with keys and values like this:
	 * 
	 *   length      521859
	 *   path        "Folder" "Subfolder" "Another Subfolder" "File Name.ext"
	 *   path.utf-8  "Folder" "Subfolder" "Another Subfolder" "File Name.ext"
	 * 
	 * length is a bencoded number, while path and path.utf-8 are bencoded lists.
	 * length is the size of the file
	 * path is the subfolders you should save it in, like "Folder\Subfolder\Another Subfolder\File Name.ext".
	 * In the .torrent, "info"."name" contains the folder name for this torrent, like "Folder Name".
	 * LimeWire settings have the path to the temporary save folder, like "C:\Documents and Settings\Kevin\Incomplete".
	 * From all this, we can put together the complete path to the file, like:
	 * "C:\Documents and Settings\Kevin\Incomplete\Folder Name\Folder\Subfolder\Another Subfolder\File Name.ext"
	 * 
	 * Both "path" and "path.utf-8" should contain the same information.
	 * All .torrent files will have "path", while some will also have "path.utf-8".
	 * "path" is encoded in regular ASCII, while "path.utf-8" is in UTF-8, allowing non English-language characters.
	 * If we have "path.utf-8", this parseFile() method uses it instead of "path".
	 * 
	 * @param file     The Java HashMap we made by parsing a file item in the "info"."files" list in the .torrent file.
	 * @param basePath The path to LimeWire's temporary folder, and the folder name from "info"."name".
	 *                 Like "C:\Documents and Settings\Kevin\Incomplete\Folder Name".
	 * @return         A TorrentFile object with the complete save path for the file, and its size.
	 */
	private static TorrentFile parseFile(Map file, String basePath) throws ValueException {

		// Read "length", the file size
		Object t_length = file.get("length");
		if (!(t_length instanceof Long)) throw new ValueException("bad metainfo - bad file length");
		long length = ((Long)t_length).longValue();

		// Read "path", the path and file name in a bencoded list of strings, like "Folder", "Subfolder", "File.ext"
		Object t_path = file.get("path");
		if (!(t_path instanceof List)) throw new ValueException("bad metainfo - bad path");
		List path = (List)t_path;
		if (path.isEmpty()) throw new ValueException("bad metainfo - bad path");

		/*
		 * The value of "path" is a bencoded list of strings, like:
		 * 
		 * "Folder" "Subfolder" "File Name.ext"
		 * 
		 * You can put these together to make the end of the path, like "Folder\Subfolder\File Name.ext".
		 * 
		 * The .torrent file may have another key alongside "path", named "path.utf-8".
		 * It has the same path, but encoded in UTF-8 instead of regular ASCII.
		 * A .torrent file that has "path.utf-8" also has the same information in "path" so BitTorrent programs that don't understand "path.utf-8" can still understand it.
		 * We do understand it, though, so we'll use it instead of "path" if it's there.
		 */

		// If the key "path.utf-8" is present, the path is written in UTF-8 in addition to ASCII
		Object t_path_utf8 = file.get("path.utf-8");
		if (!(t_path_utf8 instanceof List)) t_path_utf8 = null; // "path.utf-8" not found, set t_path_utf8 to null to use "path" instead

		// Make a new StringBuffer that we can add text to
		StringBuffer paths = new StringBuffer(basePath); // Start it out with the path to the folder we're going to save this torrent in

		// Count the number of paths we parse
		int numParsed = 0;

		// The .torrent has the path in UTF-8
		if (t_path_utf8 != null) {
			List pathUtf8 = (List)t_path_utf8;

			// Only take it if it has the same number of folders as the ASCII path in "path"
			if (pathUtf8.size() == path.size()) {

				// Loop for each folder name in the "path.utf-8" bencoded list of bencoded strings
				for (Iterator iter = pathUtf8.iterator(); iter.hasNext(); ) {
					Object t_next = iter.next();
					if (!(t_next instanceof byte[])) break; // Leave the loop to use "path" instead

					// Make a string to hold this folder name
					String pathElement;

					try {

						// Read the bencoded string using UTF-8 encoding
						pathElement = new String((byte[])t_next, Constants.UTF_8_ENCODING);

						// Add "\" and the folder name to the paths string we're building
						paths.append(File.separator);
						paths.append(CommonUtils.convertFileName(pathElement));

						// Count that we parsed another path element
						numParsed++;

					// Leave the loop to use "path" instead
					} catch (UnsupportedEncodingException uee) { break; }
				}
			}
		}

		// If the loop above didn't find all the elements that "path" has, use it instead
		if (numParsed < path.size()) {

			// Loop for each string in "path"
			for (int i = numParsed; i < path.size(); i++) {
				Object next = path.get(i);
				if (!(next instanceof byte[])) throw new ValueException("bad paths");
				String pathElement = getString((byte[])next);

				// Add "\" and the path element to paths
				paths.append(File.separator);
				paths.append(CommonUtils.convertFileName(pathElement));
			}
		}

		/*
		 * Now, paths is like this:
		 * "C:\Documents and Settings\Kevin\Incomplete\Folder\Subfolder\File Name.ext"
		 */

		// Store the complete path we composed and the length of the file in a new TorrentFile object, and return it
		return new TorrentFile(length, paths.toString());
	}

	/**
	 * Split the pieces byte array of hashes into individual hashes.
	 * 
	 * @param pieces A byte array with the SHA1 hashes of all the pieces, one after the other with nothing between them
	 * @return       A 2-D array with each 20-byte SHA1 hash presented seprately
	 */
	private static byte[][] parsePieces(byte[] pieces) throws ValueException {

		// Make sure the given array is exactly a multiple of 20 bytes long
		if (pieces.length % 20 != 0) throw new ValueException("bad metainfo - bad pieces key");

		// Allocate a 2-D byte array exactly the right size
		byte[][] ret = new byte[pieces.length / 20][20];

		// Move i down the given array in 20-byte steps
		int k = 0; // The index in our array we're filling
		for (int i = 0; i < pieces.length; i += 20) {

			// Copy the next 20-byte SHA1 hash from pieces into the next space in ret
			System.arraycopy(pieces, i, ret[k++], 0, 20); // ret[k++] returns ret[k], and then increments the value of k for the next time
		}

		// Return the array we made
		return ret;
	}

	/**
	 * A FakeFileDesc object holds a path like "C:\Documents and Settings\Kevin\Incomplete\Folder\Subfolder\File Name.ext" that the GUI can read.
	 * LimeWire's GUI needs a FileDesc object to be able to show a file in its list.
	 * FakeFileDesc extends FileDesc to fill this requirement.
	 */
	public class FakeFileDesc extends FileDesc {

		/**
		 * Make a new FakeFileDesc object.
		 * A FakeFileDesc object holds a path, and satisifies the minimum requirements of being a FileDesc object.
		 * The GUI needs a FileDesc object to list a file, and will be able to get the path from this FakeFileDesc object.
		 * 
		 * @param file A Java File object that has the path this FakeFileDesc object will hold
		 */
		public FakeFileDesc(File file) {

			// Call the FileDesc constructor
			super(
				file,               // The path for the GUI to read
				FAKE_URN_SET,       // The FileDesc constructor needs a Set of URN objects, give it the one we made with a single URN that has a hash that's all 0s
				Integer.MAX_VALUE); // Instead of a real file index, pass the largest int
		}
	}
}
