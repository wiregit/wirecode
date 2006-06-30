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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bitzi.util.Base32;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.security.SHA1;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.util.BitSet;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.FileUtils;
import com.limegroup.gnutella.util.MultiIterable;

import com.limegroup.bittorrent.bencoding.BEncoder;
import com.limegroup.bittorrent.bencoding.Token;

/**
 * This class wraps a .torrent file. 
 */
public class BTMetaInfo implements Serializable {
	private static final Log LOG = LogFactory.getLog(BTMetaInfo.class);

	static final long serialVersionUID = -2693983731217045071L;
	
	private static final ObjectStreamField[] serialPersistentFields = 
    	ObjectStreamClass.NO_FIELDS;

	/*
	 * for creating FakeFileDescs
	 */
	private static final Set FAKE_URN_SET = new HashSet();

	static {
		// initialize FAKE_URN_SET.
		try {
			FAKE_URN_SET
					.add(URN
							.createSHA1Urn("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
		} catch (IOException ioe) {
			ErrorService.error(ioe);
		}
	}
	
	private BitSet fullSet = new FullBitSet();

	/* a two dimensional array storing the hashes for this file */
	private byte[][] _hashes;

	/* the length of one piece */
	private int _pieceLength;

	/* the name of the torrent */
	private String _name;

	/*
	 * an array of TorrentFile[] storing the path and the size of of every file
	 */
	private List<TorrentFile> _files;
	
	/**
	 * any new folders that are contained in the torrent
	 */
	private Collection<File> _folders = new HashSet<File>();
	
	private Iterable<File> _filesAndFolders = 
		new MultiIterable<File>(_files,_folders);
	
	/*
	 * A <tt> File </tt> pointing to the file/directory where the incomplete
	 * torrent is written.
	 */
	private File _incompleteFile;

	/*
	 * A <tt> File </tt> pointing to the file/directory where the completed
	 * torrent will be moved.
	 */
	private File _completeFile;

	/*
	 * the sha1-hash of te beencoded _infoMap Object
	 */
	private byte[] _infoHash;
	
	/**
	 * An URN representation of the infoHash;
	 */
	private URN _infoHashURN;

	/*
	 * The VerifyingFolder for this torrent. The VerifyingFolder should not be
	 * serialized to disk.
	 */
	private VerifyingFolder _folder;

	/*
	 * an array of URL[] containing any trackers. This field is non-final
	 * because at a later date we may want to be able to add trackers to a
	 * torrent
	 */
	private URL[] _trackers;

	/*
	 * the total length of this torrent.
	 */
	private long _totalSize;
	
	/*
	 * The ManagedTorrent associated with this torrent
	 */
	private transient ManagedTorrent _torrent = null;

	/*
	 * FileDesc for the GUI
	 */
	private transient FileDesc _desc = null;

	/**
	 * Accessor for PECE_LENGTH
	 * 
	 * @return long piece length for this torrent
	 */
	public int getPieceLength() {
		return _pieceLength;
	}

	/**
	 * Acessor for the files belonging to this torrent
	 * 
	 * @return array of TorrentFile storing the path and the length for each
	 *         file
	 */
	public List<TorrentFile> getIncompleteFiles() {
		List<TorrentFile> ret = new ArrayList<TorrentFile>(_files);
		updateReferences(_incompleteFile,ret);
		return ret;
	}
	
	public List<TorrentFile> getFiles() {
		return _files;
	}
	
	public Iterable<File> getFilesAndFolders() {
		return _filesAndFolders;
	}
	
	boolean conflicts(File f) {
		return _files.contains(f) || _folders.contains(f);
	}

	/**
	 * Accessor for the base file of the torrent, this may be a directory.
	 * 
	 * @return <tt>File</tt> the parent file for all files in this torrent.
	 */
	public File getBaseFile() {
		return _incompleteFile;
	}

	/**
	 * Accessor for the base file of a complete torrent, returns null if torrent
	 * is not complete yet.
	 * 
	 * @return <tt>File</tt> the parent file for all files in this torrent or
	 *         the only file in this torrent
	 */
	public File getCompleteFile() {
		return _completeFile;
	}
	
	void setCompleteFile(File f) {
		updateReferences(f, _files);
		updateFolderReferences(f);
		_completeFile = f;
	}
	
	/**
	 * @return FileDesc for the GUI.
	 */
	public FileDesc getFileDesc() {
		if (_desc == null)
			_desc = new FakeFileDesc(_completeFile == null ? _incompleteFile
					: _completeFile);
		return _desc;
	}

	/**
	 * Accessor for the hash of a certain piece.
	 * 
	 * @param the
	 *            int identifying the piece for which to return the hash
	 * @return the hash of the piece
	 */
	public byte[] getHash(int pieceNum) {
		return _hashes[pieceNum];
	}

	/**
	 * Accessor for the info hash
	 * 
	 * @return info hash
	 */
	public byte[] getInfoHash() {
		return _infoHash;
	}
	
	public URN getURN() {
		return _infoHashURN;
	}

	/**
	 * Accessor for the VerifyingFolder
	 * 
	 * @return VerifyingFolder
	 */
	public VerifyingFolder getVerifyingFolder() {
		return _folder;
	}

	/**
	 * Moves all files of this torrent to saving directory
	 * 
	 * @return true if successful, false if not
	 */
	public boolean moveToCompleteFolder() {
		
		if (!saveFile(_incompleteFile,
				_completeFile))
			return false;

		// purge the stored FakeFileDesc
		_desc = null;

		LOG.trace("saved files");
		initializeVerifyingFolder(null, true);
		LOG.trace("initialized folder");
		return true;
	}

	/**
	 * Associates this meta info with a ManagedTorrent
	 * 
	 * @param torrent
	 *            the ManagedTorrent we will inform about completed ranges,
	 *            etc...
	 */
	public void setManagedTorrent(ManagedTorrent torrent) {
		_torrent = torrent;
	}

	/**
	 * Accessor for the total size of this torrent
	 * 
	 * @return long size of the torrent
	 */
	public long getTotalSize() {
		return _totalSize;
	}
	
	private static long calculateTotalSize(List<TorrentFile> files) {
		long ret = 0;
		for (File f : files)
			ret += f.length();
		return ret;
	}

	/**
	 * helper calculating the number of pieces in a torrent
	 * 
	 * @return number of blocks
	 */
	public int getNumBlocks() {
		return (int) ((_totalSize + _pieceLength - 1) / _pieceLength);
	}

	/**
	 * Accessor for the torrent's name
	 * 
	 * @return <tt>String</tt>, the name
	 */
	public String getName() {
		return _name;
	}

	/**
	 * Accessor for the trackers
	 * 
	 * @return array of <tt>URL</tt> storing the addresses of the trackers
	 */
	public URL[] getTrackers() {
		return _trackers;
	}

	/**
	 * Returns which message digest was used to create _hashes.
	 * 
	 * @return new Instance of the message digest that was used
	 */
	public MessageDigest getMessageDigest() {
		return new SHA1();
	}

	/**
	 * Creates a new bitfield identifying the available ranges in a super sparse
	 * manner
	 * 
	 * @return array of byte, the bitfield
	 */
	public byte[] createBitField() {
		return _folder.createBitField();
	}

	/**
	 * Reads a BTMetaInfo from any InputStream
	 * 
	 * @param is
	 *            the <tt>InputStream</tt> to read from
	 * @return new instance of BTMetaInfo if all went well
	 * @throws IOException
	 *             if we couldn't read the BTMetaInfo from the InputStream
	 */
	public static BTMetaInfo readFromBytes(byte []torrent)
			throws IOException {
		Object metaInfo = Token.parse(torrent);
		return new BTMetaInfo(metaInfo);
	}

	/**
	 * Saves the torrent data in the incomplete folder for this torrent
	 * for easier resuming.
	 */
	public void saveInfoMapInIncomplete() 
	throws IOException {
		FileUtils.writeObject(_incompleteFile.getParent()+
				File.separator +
				".dat"+_name,
				this);
	}
	
	/**
	 * private helper, copies file, updates FileManager and _files
	 * 
	 * @param incFile
	 *            the <tt>File</tt> from incomplete directory
	 * @param completeDest
	 *            the <tt>File</tt> representing incFile's parent in the
	 *            complete directory
	 * @return true if successful
	 */
	private boolean saveFile(File incFile, File completeDest) {
		try {
			completeDest = completeDest.getCanonicalFile();
		} catch (IOException ioe) {
			if (LOG.isDebugEnabled())
				LOG.debug(ioe);
			return false;
		}

		FileUtils.setWriteable(completeDest.getParentFile());
		if (incFile.isDirectory())
			return saveDirectory(incFile, completeDest);
		long incLength = incFile.length();
		// set parent to writeable


		// overwrite complete file and make complete File writeable
		completeDest.delete();
		FileUtils.setWriteable(completeDest);
		if (!FileUtils.forceRename(incFile, completeDest)) {
			LOG.debug("could not rename file " + incFile);
			return false;
		}

		// there have been problems with FileUtils.forceRename()
		if (incLength != completeDest.length()) {
			LOG.debug("length of complete file does not match incomplete file "
					+ completeDest + " , " + incLength + ":"
					+ completeDest.length());
			return false;
		}
		
		File torrentFolder = incFile.getParentFile();
		FileUtils.deleteRecursive(torrentFolder);

		// Add file to library.
		// first check if it conflicts with the saved dir....
		RouterService.getFileManager().removeFileIfShared(completeDest);
		RouterService.getFileManager().addFileIfShared(completeDest);

		return true;
	}

	/**
	 * private utility method for initializing the VerifyingFolder
	 */
	private void initializeVerifyingFolder(Map data, boolean complete) {
		_folder = new VerifyingFolder(this, complete, data);
	}

	/**
	 * Saves a directory
	 * 
	 * @param incFile
	 *            the directory to move
	 * @param completeParent
	 *            the destination to move the directory to
	 * @return true if successful
	 */
	private boolean saveDirectory(File incFile, File completeDir) {

		// we will delete completeDir if it exists and if it is not a directory
		if (completeDir.exists()) {
			// completeDir is not a directory
			if (!completeDir.isDirectory()) {
				if (!(completeDir.delete() && completeDir.mkdirs())) {
					if (LOG.isDebugEnabled())
						LOG.debug("could not create complete dir " + completeDir);
					return false;
				}
			}
		} else if (!completeDir.mkdirs()) {
			// completeDir does not exist...
			if (LOG.isDebugEnabled())
				LOG.debug("could not create complete dir " + completeDir);
			return false;
		}

		FileUtils.setWriteable(completeDir);

		// we inform the filemanager about the new directory here,
		// it is still empty at this point
		RouterService.getFileManager().addFileIfShared(completeDir);

		for (File f : incFile.listFiles()) {
			if (!saveFile(f, new File(completeDir, f.getName()))) 
				return false;
		}

		// remove the empty directory from the incomplete folder.
		// all its contents have been moved to the shared folder
		FileUtils.deleteRecursive(incFile);

		return true;
	}

	/**
	 * Updates the files in the torrent to start at the 
	 * new base path.
	 * @param completeBase
	 *            the top file in the torrent
	 */
	private void updateReferences(File completeBase, List<TorrentFile> l) {
		int offset = _completeFile.getAbsolutePath().length();
		String newPath = completeBase.getAbsolutePath();
		for (int i = 0; i < l.size(); i++) {
			TorrentFile current = l.get(i);
			TorrentFile updated = new TorrentFile(current.length(), newPath
					+ current.getPath().substring(offset));
			l.set(i,updated);
		}
	}
	
	/**
	 * Updates the files in the torrent to start at the 
	 * new base path.
	 */
	private void updateFolderReferences(File completeBase) {
		int offset = _completeFile.getAbsolutePath().length();
		String newPath = completeBase.getAbsolutePath();
		Collection<File> newFolders = new HashSet<File>(_folders.size());
		for (File f: _folders) 
			newFolders.add(new File(newPath + f.getPath().substring(offset)));
		_folders.clear();
		_folders.addAll(newFolders); // the reference is held in the MultiIterable
	}

	/**
	 * private constructor for BTMetaInfo, called by readFromInputStream().
	 * 
	 * @param t_metaInfo
	 *            the Object to create the BTMetaInfo from, must be a
	 *            <tt>Map</tt>
	 * @throws ValueException
	 */
	private BTMetaInfo(Object t_metaInfo) throws ValueException {
		_completeFile = null;

		// meta info must be map
		if (!(t_metaInfo instanceof Map))
			throw new ValueException("Unknown type of MetaInfo");

		Map metaInfo = (Map) t_metaInfo;

		// get the trackers, we only expect one tracker, more trackers may be
		// added by the addTracker() method, we will throw an exception if the
		// tracker is invalid or does not even exist.
		Object t_announce = metaInfo.get("announce");
		if (!(t_announce instanceof byte []))
			throw new ValueException("bad metainfo - no tracker");
		String url = getString((byte[])t_announce);
		try {
			// Note: this kills UDP trackers so we will eventually
			// use a different object.
			_trackers = new URL[] { new URL(url) };
		} catch (MalformedURLException mue) {
			throw new ValueException("bad metainfo - bad tracker");
		}

		// TODO: add proper support for multi-tracker torrents later.

		// the main data from the meta info
		Object t_info = metaInfo.get("info");
		if (!(t_info instanceof Map))
			throw new ValueException("bad metainfo - bad info");
		Map info = (Map) t_info;

		// the hashes from the meta info
		Object t_pieces = info.get("pieces");
		if (!(t_pieces instanceof byte []))
			throw new ValueException("bad metainfo - no pieces key found");
		_hashes = parsePieces((byte [])t_pieces);
		
		// create the info hash, we could create the info hash while reading it
		// but that would make the code a lot more complex. This works well too,
		// because the order of a list is not changed during the process of
		// decoding or encoding it and Maps are always sorted alphanumerically
		// when encoded.
		// So the data we encoded is always exactly the same as the data before
		// we decoded it. This is intended that way by the protocol.
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			BEncoder.encodeDict(baos, info);
		} catch (IOException ioe) {
			ErrorService.error(ioe);
		}
		
		MessageDigest md = new SHA1();
		_infoHash = md.digest(baos.toByteArray());
		
		try {
			_infoHashURN = URN.createSHA1UrnFromBytes(_infoHash);
		} catch (IOException impossible) {
			ErrorService.error(impossible);
		}

		// piece length, also very important.
		Object t_pieceLength = info.get("piece length");
		if (!(t_pieceLength instanceof Long))
			throw new ValueException("bad metainfo - illegal piece length");
		_pieceLength = (int)((Long) t_pieceLength).longValue();
		if (_pieceLength <= 0)
			throw new ValueException("bad metainfo - illegal piece length");

		// name of the torrent, also specifying the directory under which to
		// save the torrents, as per extension spec, name.urf-8 specifies the
		// utf-8 name of the torrent
		String name = null;
		Object t_name = info.get("name");
		if (!(t_name instanceof byte []))
			throw new ValueException("bad metainfo - bad name");

		// if possible prefer the name.utf-8 key but we are already sure that
		// we have a safe name value to fallback to
		Object t_name_utf8 = info.get("name.utf-8");
		if (t_name_utf8 instanceof byte[]) {
			try {
				name = new String((byte [])t_name_utf8, 
						Constants.UTF_8_ENCODING);
			} catch (UnsupportedEncodingException uee) {
				// fail silently
			}
		}
		
		if (name == null)
			name = getString((byte [])t_name);

		_name = CommonUtils.convertFileName(name);

		// we need to check the name of the torrent, security risk!
		if (_name.length() == 0)
			throw new ValueException("bad torrent name");

		// read the files belonging to the torrent
		if (info.containsKey("files") == info.containsKey("length"))
			throw new ValueException("single/multiple file mix");

		File incompleteDir = SharingSettings.INCOMPLETE_DIRECTORY.getValue();
		try {
			incompleteDir = incompleteDir.getCanonicalFile();
		} catch (IOException iox){}
		
		_incompleteFile = new File(incompleteDir, 
				Base32.encode(_infoHash)+File.separator+_name);
		_completeFile = new File(SharingSettings.getSaveDirectory(), _name);
		
		if (info.containsKey("files")) {
			Object t_files = info.get("files");
			if (!(t_files instanceof List))
				throw new ValueException("bad metainfo - bad files value");
			List<TorrentFile> files = parseFiles((List) t_files, _completeFile
					.getAbsolutePath());
			if (files.size() == 0)
				throw new ValueException("bad metainfo - bad files value " + t_files);
			
			// add the beginning and ending chunks for each file.
			long position = 0;
			for (TorrentFile file : files) {
				file.begin = (int) (position / _pieceLength);
				position += file.length();
				file.end = (int) (position / _pieceLength);
			}
			
			_files = files;
			_folders.add(_completeFile);
		} else {
			Object t_length = info.get("length");
			if (!(t_length instanceof Long))
				throw new ValueException("bad metainfo - bad file length");
			long length = ((Long) t_length).longValue();
			_files = new ArrayList<TorrentFile>(1);
			try {
				TorrentFile f = new TorrentFile(length, _completeFile
						.getCanonicalPath());
				f.begin = 0;
				f.end = _hashes.length;
				_files.add(f);
			} catch (IOException bad) {
				throw new ValueException("bad path");
			}
			
		}
		
		_totalSize = calculateTotalSize(_files);

		initializeVerifyingFolder(null, false);
	}
	
	private static String getString(byte [] bytes) {
		try {
			return new String(bytes, Constants.ASCII_ENCODING);
		} catch (UnsupportedEncodingException impossible) {
			ErrorService.error(impossible);
			return null;
		}
	}

	/**
	 * overrides serialization methods, so we can save the _writtenRanges.
	 * 
	 * @param out
	 *            the <tt>ObjectOutputStream</tt> to write to
	 * @throws IOException
	 */
	private synchronized void writeObject(ObjectOutputStream out)
			throws IOException {
		Map toWrite = new HashMap();
		
		toWrite.put("_hashes",_hashes);
		toWrite.put("_pieceLength",new Integer(_pieceLength));
		toWrite.put("_name",_name);
		toWrite.put("_files",_files);
		toWrite.put("_folders",_folders);
		toWrite.put("_completeFile",_completeFile);
		toWrite.put("_incompleteFile",_incompleteFile);
		toWrite.put("_infoHash",_infoHash);
		toWrite.put("_trackers",_trackers);
		toWrite.put("_totalSize",new Long(_totalSize));
		
		toWrite.put("folder data",_folder.getSerializableObject());
		
		out.writeObject(toWrite);
	}

	/**
	 * Overrides serialization method to initialize the VerifyingFolder
	 * 
	 * @param in
	 *            the <tt>ObjectInputStream</tt> this is deserialized from
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private synchronized void readObject(ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		Map toRead = (Map) in.readObject();
		
		_hashes = (byte [][]) toRead.get("_hashes");
		Integer pieceLength = (Integer)toRead.get("_pieceLength"); 
		_name = (String) toRead.get("_name");
		_files = (List<TorrentFile>) toRead.get("_files");
		_folders = (Collection<File>) toRead.get("_folders");
		_incompleteFile = (File) toRead.get("_incompleteFile");
		_completeFile = (File) toRead.get("_completeFile");
		_infoHash = (byte []) toRead.get("_infoHash");
		_infoHashURN = URN.createSHA1UrnFromBytes(_infoHash);
		_trackers = (URL []) toRead.get("_trackers");
		Long totalSize = (Long)toRead.get("_totalSize");
		
		Map folderData = (Map) toRead.get("folder data");
		
		if (_hashes == null || pieceLength == null ||
				_name == null || _files == null || _folders == null ||
				_incompleteFile == null || _completeFile == null ||
				 _infoHash == null || _trackers == null ||
				totalSize == null || folderData == null)
			throw new IOException("cannot read BTMetaInfo");
		
		_pieceLength = pieceLength.intValue();
		_totalSize = totalSize.longValue();
		initializeVerifyingFolder(folderData, false);
		fullSet = new FullBitSet();
	}

	/**
	 * Parse a list of files from the meta info
	 * 
	 * @param files
	 *            a <tt>List</tt> containing the file info
	 * @param basePath
	 *            the <tt>String</tt> identifying the torrent's saving folder
	 * @return List of <tt>TorrentFile</tt>
	 * @throws ValueException
	 */
	private List<TorrentFile> parseFiles(List files, String basePath)
			throws ValueException {
		ArrayList<TorrentFile> ret = new ArrayList<TorrentFile>();
		for (Iterator iter = files.iterator(); iter.hasNext();) {
			Object t_file = iter.next();
			if (!(t_file instanceof Map))
				throw new ValueException("bad metainfo - bad file value");
			ret.add(parseFile((Map) t_file, basePath));
		}
		return ret;
	}

	/**
	 * Utility method to parse a single file from the meta info
	 * 
	 * @param file
	 *            the <tt>Map</tt> containing the file info
	 * @param basePath
	 *            the <tt>String</tt> identifying the torrent
	 * @return instance of <tt>TorrentFile</tt>
	 * @throws ValueException
	 */
	private TorrentFile parseFile(Map file, String basePath)
			throws ValueException {

		Object t_length = file.get("length");

		if (!(t_length instanceof Long))
			throw new ValueException("bad metainfo - bad file length");

		long length = ((Long) t_length).longValue();

		Object t_path = file.get("path");
		if (!(t_path instanceof List))
			throw new ValueException("bad metainfo - bad path");
		
		List path = (List) t_path;
		if (path.isEmpty())
			throw new ValueException("bad metainfo - bad path");

		Object t_path_utf8 = file.get("path.utf-8");
		// the extension specs introduced the utf-8 path.
		if ( ! (t_path_utf8 instanceof List)) {
			// invalid - ignore
			t_path_utf8 = null;
		}

		StringBuffer paths = new StringBuffer(basePath);
		
		int numParsed = 0;
		// prefer the utf8 path if possible
		if (t_path_utf8 != null) {
			
			List pathUtf8 = (List) t_path_utf8;
			if (pathUtf8.size() == path.size()) {
				for (Iterator iter = pathUtf8.iterator(); iter.hasNext();) {
					Object t_next = iter.next();
					if ( ! (t_next instanceof byte []))
						break; // fall through to regular path
					
					String pathElement;
					try {
						pathElement = new String((byte []) t_next, Constants.UTF_8_ENCODING);
						paths.append(File.separator);
						paths.append(CommonUtils.convertFileName(pathElement));
						_folders.add(new File(paths.toString()));
						numParsed++;
					} catch (UnsupportedEncodingException uee) {
						break; // fall through silently
					}
				}
			} // else # of files different? weird...
		} 
		
		// if not all elements were found in
		if (numParsed < path.size()) {
			for (int i = numParsed; i < path.size(); i++) {
				Object next = path.get(i);
				if (! (next instanceof byte []))
					throw new ValueException("bad paths");
				String pathElement = getString((byte [])next);
				paths.append(File.separator);
				paths.append(CommonUtils.convertFileName(pathElement));
			}
		}
		return new TorrentFile(length, paths.toString());
	}

	/**
	 * parse the hashes
	 * 
	 * @param str
	 *            the String containing the hashes in raw form.
	 * @return two dimensional byte array containing the hashes.
	 * @throws ValueException
	 */
	private static byte[][] parsePieces(byte [] pieces) throws ValueException {
		if (pieces.length % 20 != 0)
			throw new ValueException("bad metainfo - bad pieces key");
		byte[][] ret = new byte[pieces.length / 20][20];

		int k = 0;
		for (int i = 0; i < pieces.length; i += 20) {
			System.arraycopy(pieces, i, ret[k++], 0, 20);
		}
		return ret;
	}

	public class FakeFileDesc extends FileDesc {
		public FakeFileDesc(File file) {
			super(file, FAKE_URN_SET, Integer.MAX_VALUE);
		}
	}
	
	public BitSet getFullBitSet() {
		return fullSet;
	}
	
	private class FullBitSet extends BitSet {
		public void set(int i) {}
		public void clear(int i){}
		public boolean get(int i) {
			return true;
		}
		public int cardinality() {
			return getNumBlocks();
		}
		public int length() {
			return getNumBlocks();
		}
		public int nextSetBit(int i) {
			if (i >= cardinality())
				return -1;
			return i;
		}
	}

}
