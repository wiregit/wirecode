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
import java.util.Arrays;
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
import com.limegroup.gnutella.util.BitField;
import com.limegroup.gnutella.util.BitFieldSet;
import com.limegroup.gnutella.util.BitSet;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.FileUtils;
import com.limegroup.gnutella.util.MultiCollection;
import com.limegroup.gnutella.util.StringUtils;

import com.limegroup.bittorrent.bencoding.BEncoder;
import com.limegroup.bittorrent.bencoding.Token;

/**
 * Contains information usually parsed in a .torrent file
 */
public class BTMetaInfo implements Serializable {
	private static final Log LOG = LogFactory.getLog(BTMetaInfo.class);

	static final long serialVersionUID = -2693983731217045071L;
	
	private static final ObjectStreamField[] serialPersistentFields = 
    	ObjectStreamClass.NO_FIELDS;

	/** a marker for a hash that has been verified */
	private static final byte [] VERIFIED_HASH = new byte[0];

	/** a single instance of the full bitset */
	private BitSet fullSet = new FullBitSet();
	private BitField fullBitField;

	/** a list the hashes for this file */
	private List<byte []> _hashes;

	/* the length of one piece */
	private int _pieceLength;

	/**
	 * Information about how the torrent looks on disk.
	 */
	private TorrentFileSystem fileSystem;
	
	/*
	 * the sha1-hash of te beencoded _infoMap Object
	 */
	private byte[] _infoHash;
	
	/**
	 * An URN representation of the infoHash;
	 */
	private URN _infoHashURN;

	/*
	 * The VerifyingFolder for this torrent. 
	 */
	private VerifyingFolder _folder;

	/*
	 * an array of URL[] containing any trackers. This field is non-final
	 * because at a later date we may want to be able to add trackers to a
	 * torrent
	 */
	private URL[] _trackers;

	/*
	 * FileDesc for the GUI
	 */
	private transient FileDesc _desc = null;
	
	/**
	 * @return piece length for this torrent
	 */
	public int getPieceLength() {
		return _pieceLength;
	}

	public TorrentFileSystem getFileSystem() {
		return fileSystem;
	}
	
	/**
	 * Verifies whether the given hash matches the expect hash of a piece
	 * @param sha1 the hash that was computed
	 * @param pieceNum the piece for which the hash was computed
	 * @return true if they match.
	 */
	public boolean verify(byte [] sha1, int pieceNum) {
		byte [] hash = _hashes.get(pieceNum);
		if (hash == VERIFIED_HASH)
			return true;
		boolean ok = Arrays.equals(sha1, hash);
		if (ok)
			_hashes.set(pieceNum, VERIFIED_HASH);
		return ok;
	}

	/**
	 * @return info hash
	 */
	public byte[] getInfoHash() {
		return _infoHash;
	}

	/**
	 * @return infohash URN
	 */
	public URN getURN() {
		return _infoHashURN;
	}
	
	/**
	 * @return FileDesc for the GUI.
	 */
	public FileDesc getFileDesc() {
		if (_desc == null) {
			Set s = new HashSet();
			s.add(getURN());
			_desc = new FakeFileDesc(fileSystem.getCompleteFile(),s);
		}
		return _desc;
	}

	/**
	 * @return VerifyingFolder
	 */
	public VerifyingFolder getVerifyingFolder() {
		return _folder;
	}

	/**
	 * Moves all files of this torrent to saving directory
	 * @throws IOException if failed
	 */
	public void moveToCompleteFolder() throws IOException {

		fileSystem.moveToCompleteFolder();
		fileSystem.addToLibrary();
		
		// purge the stored FakeFileDesc
		_desc = null;

		LOG.trace("saved files");
		initializeVerifyingFolder(null, true);
		LOG.trace("initialized folder");
	}

	/**
	 * @return number of pieces in this torrent
	 */
	public int getNumBlocks() {
		return (int) ((fileSystem.getTotalSize() + _pieceLength - 1) / _pieceLength);
	}

	public String getName() {
		return fileSystem.getName();
	}

	/**
	 * @return array of <tt>URL</tt> storing the addresses of the trackers
	 */
	public URL[] getTrackers() {
		return _trackers;
	}

	/**
	 * Returns which message digest was used to create _hashes.
	 * 
	 * @return new Instance of the message digest that was used
	 * 
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
	 * Reads a BTMetaInfo from byte []
	 * 
	 * @param torrent byte array with the contents of .torrent
	 * @return new instance of BTMetaInfo if all went well
	 * @throws IOException if parsing or reading failed.
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
		FileUtils.writeObject(fileSystem.getBaseFile().getParent()+
				File.separator +
				".dat"+fileSystem.getName(),
				this);
	}
	
	/**
	 * private utility method for initializing the VerifyingFolder
	 */
	private void initializeVerifyingFolder(Map data, boolean complete) {
		_folder = new VerifyingFolder(this, complete, data);
	}

	/**
	 * Constructs a BTMetaInfo from an Object parsed from BEncoded data.
	 * 
	 * @throws ValueException if the structure of the object does not match
	 * what we understand or expect.
	 */
	private BTMetaInfo(Object t_metaInfo) throws ValueException {

		// meta info must be map
		if (!(t_metaInfo instanceof Map))
			throw new ValueException("Unknown type of MetaInfo");

		Map metaInfo = (Map) t_metaInfo;

		// get the trackers, we only expect one tracker, we will throw if the
		// tracker is invalid or does not even exist.
		Object t_announce = metaInfo.get("announce");
		if (!(t_announce instanceof byte []))
			throw new ValueException("bad metainfo - no tracker");
		String url = StringUtils.getASCIIString((byte[])t_announce);
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
		baos = null;
		
		try {
			_infoHashURN = URN.createSHA1UrnFromBytes(_infoHash);
		} catch (IOException impossible) {
			ErrorService.error(impossible);
		}

		// the hashes from the meta info
		Object t_pieces = info.remove("pieces");
		if (!(t_pieces instanceof byte []))
			throw new ValueException("bad metainfo - no pieces key found");
		_hashes = parsePieces((byte [])t_pieces);
		t_pieces = null; // this byte [] is usually big.
		
		// piece length
		Object t_pieceLength = info.get("piece length");
		if (!(t_pieceLength instanceof Long))
			throw new ValueException("bad metainfo - illegal piece length");
		_pieceLength = (int)((Long) t_pieceLength).longValue();
		if (_pieceLength <= 0)
			throw new ValueException("bad metainfo - illegal piece length");


		fileSystem = new TorrentFileSystem(info, _hashes.size(), _pieceLength, _infoHash);
		fullBitField = new BitFieldSet(fullSet, getNumBlocks());
		initializeVerifyingFolder(null, false);
	}
	
	/**
	 * Serializes this, including information about the written ranges.
	 */
	private synchronized void writeObject(ObjectOutputStream out)
			throws IOException {
		Map toWrite = new HashMap();
		
		toWrite.put("_hashes",_hashes);
		toWrite.put("_pieceLength",new Integer(_pieceLength));
		toWrite.put("_fileSystem",fileSystem);
		toWrite.put("_infoHash",_infoHash);
		toWrite.put("_trackers",_trackers);
		
		toWrite.put("folder data",_folder.getSerializableObject());
		
		out.writeObject(toWrite);
	}

	/**
	 * Overrides serialization method to initialize the VerifyingFolder
	 */
	private synchronized void readObject(ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		Map toRead = (Map) in.readObject();
		
		_hashes = (List<byte[]>) toRead.get("_hashes");
		Integer pieceLength = (Integer)toRead.get("_pieceLength");
		fileSystem = (TorrentFileSystem) toRead.get("_fileSystem");
		_infoHash = (byte []) toRead.get("_infoHash");
		_infoHashURN = URN.createSHA1UrnFromBytes(_infoHash);
		_trackers = (URL []) toRead.get("_trackers");
		
		Map folderData = (Map) toRead.get("folder data");
		
		if (_hashes == null || pieceLength == null || fileSystem == null ||
				 _infoHash == null || _trackers == null ||
				folderData == null)
			throw new IOException("cannot read BTMetaInfo");
		
		_pieceLength = pieceLength.intValue();
		initializeVerifyingFolder(folderData, false);
		fullSet = new FullBitSet();
		fullBitField = new BitFieldSet(fullSet,getNumBlocks());
	}

	/**
	 * parse the hashes
	 * 
	 * @param pieces the byte [] containing the hashes in raw form.
	 * @return List<byte[]> containing the hashes.
	 * @throws ValueException if parsing fails.
	 */
	private static List<byte[]> parsePieces(byte [] pieces) throws ValueException {
		if (pieces.length % 20 != 0)
			throw new ValueException("bad metainfo - bad pieces key");
		List<byte[]> ret = new ArrayList<byte[]>(pieces.length / 20);

		for (int i = 0; i < pieces.length; i += 20) {
			byte [] hash = new byte[20];
			System.arraycopy(pieces,i, hash, 0, 20);
			ret.add(hash);
		}
		return ret;
	}

	public static class FakeFileDesc extends FileDesc {
		public FakeFileDesc(File file, Set s) {
			super(file, s, Integer.MAX_VALUE);
		}
	}

	/**
	 * @return a <tt>BitSet</tt> to be used to represent a complete torrent.
	 */
	public BitSet getFullBitSet() {
		return fullSet;
	}
	
	public BitField getFullBitField() {
		return fullBitField;
	}
	
	/**
	 * A bitset that has fixed size and every bit in it is set.
	 */
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
