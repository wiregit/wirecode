package com.limegroup.bittorrent;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;

import com.limegroup.bittorrent.bencoding.Token;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.security.SHA1;
import com.limegroup.gnutella.util.FileUtils;
import com.limegroup.gnutella.util.GenericsUtils;

/**
 * Contains information usually parsed in a .torrent file
 */
public class BTMetaInfo implements Serializable {

	static final long serialVersionUID = -2693983731217045071L;
	
	private static final ObjectStreamField[] serialPersistentFields = 
    	ObjectStreamClass.NO_FIELDS;

	/** a marker for a hash that has been verified */
	private static final byte [] VERIFIED_HASH = new byte[0];

	/** a list the hashes for this file */
	private List<byte []> _hashes;

	/** the length of one piece */
	private int _pieceLength;

	/**
	 * Information about how the torrent looks on disk.
	 */
	private TorrentFileSystem fileSystem;
	
	/**
	 * the sha1-hash of te beencoded _infoMap Object
	 */
	private byte[] _infoHash;
	
	/**
	 * An URN representation of the infoHash;
	 */
	private URN _infoHashURN;

	/**
	 * an array of URL[] containing any trackers. This field is non-final
	 * because at a later date we may want to be able to add trackers to a
	 * torrent
	 */
	private URI[] _trackers;

	/**
	 * FileDesc for the GUI
	 */
	private FileDesc _desc = null;
	
	/**
	 * Object that can save/restore the diskManager
	 */
	private Serializable diskManagerData;
	
	/**
	 * The current <tt>TorrentContext</tt>
	 */
	private TorrentContext context;
	
	/**
	 * The amount of data uploaded in previous session(s)
	 * only set once during deserialization
	 */
	private long uploadedBefore;
	
	/**
	 * The amount of data uploaded this session
	 */
	private volatile long uploadedNow;
	
	/**
	 * The ratio from previous sessions
	 */
	private float historicRatio;
    
    /**
     * A handle to the .torrent file on disk 
     */
    private File torrentMetaDataFile;
	
	/**
	 * @return piece length for this torrent
	 */
	public int getPieceLength() {
		return _pieceLength;
	}

	public TorrentFileSystem getFileSystem() {
		return fileSystem;
	}
	
	public Serializable getDiskManagerData() {
		return diskManagerData;
	}
	
	public void setContext(TorrentContext context) {
		if (context == null) // initialize cross-session ratio
			initRatio(context);
		this.context = context;
	}
    
    public void setTorrentMetaDataFile(File f) {
        this.torrentMetaDataFile = f;
    }
	
	private void initRatio(TorrentContext context) {
		if (historicRatio == 0) 
			return;
		uploadedBefore = (long)
		(context.getDiskManager().getBlockSize() * historicRatio);
	}
	
	long getAmountUploaded() {
		return uploadedNow;
	}
	
	void countUploaded(int uploaded) {
		uploadedNow += uploaded;
	}
	
	float getRatio() {
		long downloaded = context.getDiskManager().getBlockSize();
		if (downloaded == 0)
			return 0;
		return (uploadedBefore + uploadedNow) * 1f / downloaded;
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
			Set<URN> s = new HashSet<URN>();
			s.add(getURN());
			_desc = new FakeFileDesc(fileSystem.getCompleteFile(),s);
		}
		return _desc;
	}

	public void resetFileDesc() {
		_desc = null;
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
	public URI[] getTrackers() {
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
     * Returns the .torrent meta data file. Can be null.
     */
    public File getTorrentMetaDataFile() {
        return torrentMetaDataFile;
    }

	/**
	 * Reads a BTMetaInfo from byte []
	 * 
	 * @param torrent byte array with the contents of .torrent
	 * @return new instance of BTMetaInfo if all went well
	 * @throws IOException if parsing or reading failed.
	 */
	public static BTMetaInfo readFromBytes(byte []torrent) throws IOException {
		Object metaInfo = Token.parse(torrent);
        if(!(metaInfo instanceof Map))
            throw new ValueException("metaInfo not a Map!");
        return new BTMetaInfo(new BTData((Map)metaInfo));
	}

	/**
	 * Constructs a BTMetaInfo based on the BTData.
	 */
	private BTMetaInfo(BTData data) throws IOException {
		try {
			URI trackerURI = new URI(data.getAnnounce());
			if (!"http".equalsIgnoreCase(trackerURI.getScheme()))
				throw new ValueException("unsupported tracker protocol: "+trackerURI.getScheme());
			_trackers = new URI[] { trackerURI };
		} catch (URIException mue) {
			throw new ValueException("bad tracker: " + data.getAnnounce());
		}

		// TODO: add proper support for multi-tracker torrents later.
		_infoHash = data.getInfoHash();
		
		try {
			_infoHashURN = URN.createSHA1UrnFromBytes(_infoHash);
		} catch (IOException impossible) {
			ErrorService.error(impossible);
		}

		_hashes = parsePieces(data.getPieces());
        data.clearPieces(); // save memory.
        
		_pieceLength = (int)data.getPieceLength().longValue();
		if (_pieceLength <= 0)
			throw new ValueException("bad metainfo - illegal piece length: " + data.getPieceLength());


		fileSystem = new TorrentFileSystem(data, _hashes.size(), _pieceLength, _infoHash);
	}
    
    // keys used between read/write object.
    private static enum SerialKeys {
        HASHES, PIECE_LENGTH, FILE_SYSTEM, INFO_HASH, TRACKERS, RATIO, FOLDER_DATA, TORRENT_METAFILE;
    }
	
	/**
	 * Serializes this, including information about the written ranges.
	 */
	private synchronized void writeObject(ObjectOutputStream out)
			throws IOException {
		Map<SerialKeys,Serializable> toWrite = new EnumMap<SerialKeys, Serializable>(SerialKeys.class);
		
		toWrite.put(SerialKeys.HASHES,(Serializable)_hashes);
		toWrite.put(SerialKeys.PIECE_LENGTH, _pieceLength);
		toWrite.put(SerialKeys.FILE_SYSTEM,fileSystem);
		toWrite.put(SerialKeys.INFO_HASH,_infoHash);
		toWrite.put(SerialKeys.TRACKERS,_trackers);
		toWrite.put(SerialKeys.RATIO, getRatio());		
		toWrite.put(SerialKeys.FOLDER_DATA,context.getDiskManager().getSerializableObject());
        
        if(torrentMetaDataFile != null) {
            String filePath = torrentMetaDataFile.getAbsolutePath();
            try {
                filePath = FileUtils.getCanonicalPath(torrentMetaDataFile.getAbsoluteFile());
            } catch (IOException ignore) {}
           
            toWrite.put(SerialKeys.TORRENT_METAFILE, filePath);
        }
		
		out.writeObject(toWrite);
	}

	/**
	 * Overrides serialization method to initialize the VerifyingFolder
	 */
	private synchronized void readObject(ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		Object read = in.readObject();
		Map<SerialKeys, Serializable> toRead = 
			GenericsUtils.scanForMap(read, 
                    SerialKeys.class, Serializable.class, 
					GenericsUtils.ScanMode.EXCEPTION);
		
		_hashes = (List<byte[]>) toRead.get(SerialKeys.HASHES);
		Integer pieceLength = (Integer)toRead.get(SerialKeys.PIECE_LENGTH);
		fileSystem = (TorrentFileSystem) toRead.get(SerialKeys.FILE_SYSTEM);
		_infoHash = (byte []) toRead.get(SerialKeys.INFO_HASH);
		_infoHashURN = URN.createSHA1UrnFromBytes(_infoHash);
		_trackers = (URI []) toRead.get(SerialKeys.TRACKERS);
		Float ratio = (Float)toRead.get(SerialKeys.RATIO);
        diskManagerData = toRead.get(SerialKeys.FOLDER_DATA); 
		
		if (_hashes == null || pieceLength == null || fileSystem == null ||
				 _infoHash == null || _trackers == null ||
                 diskManagerData == null || ratio == null)
			throw new IOException("cannot read BTMetaInfo");
        
        String filePath = (String)toRead.get(SerialKeys.TORRENT_METAFILE);
        if(filePath != null) {
            torrentMetaDataFile = new File(filePath);
            if (!FileUtils.isFilePhysicallyShareable(torrentMetaDataFile)) {
                torrentMetaDataFile = null;
            }
        }
		
		historicRatio = ratio.floatValue();
		_pieceLength = pieceLength.intValue();
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
		public FakeFileDesc(File file, Set<? extends URN> s) {
			super(file, s, Integer.MAX_VALUE);
		}
	}
}
