package com.limegroup.bittorrent;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.http.URIUtils;
import org.limewire.io.InvalidDataException;

import com.limegroup.bittorrent.bencoding.Token;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.serial.BTDiskManagerMemento;
import com.limegroup.gnutella.downloader.serial.BTMetaInfoMemento;
import com.limegroup.gnutella.downloader.serial.BTMetaInfoMementoImpl;
import com.limegroup.gnutella.security.SHA1;

/**
 * Contains information usually parsed in a .torrent file
 */
public class BTMetaInfo {

    private static final Log LOG = LogFactory.getLog(BTMetaInfo.class);
    
	/** a marker for a hash that has been verified */
	private static final byte [] VERIFIED_HASH = new byte[0];

	/** a list the hashes for this file */
	private final List<byte []> _hashes;

	/** the length of one piece */
	private final int _pieceLength;

	/**
	 * Information about how the torrent looks on disk.
	 */
	private final TorrentFileSystem fileSystem;
	
	/**
	 * the sha1-hash of te beencoded _infoMap Object
	 */
	private final byte[] _infoHash;
	
	/**
	 * An URN representation of the infoHash;
	 */
	private final URN _infoHashURN;

	/**
	 * an array of URL[] containing any trackers. This field is non-final
	 * because at a later date we may want to be able to add trackers to a
	 * torrent
	 */
    private final URI[] _trackers;

	/**
	 * FileDesc for the GUI
	 */
	private FileDesc _desc = null;
	
	/**
	 * Object that can save/restore the diskManager
	 */
	private final BTDiskManagerMemento diskManagerData;
	
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
	private final float historicRatio;
    
    /**
     * Whether this torrent has the private flag set
     */
    private final boolean isPrivate;
    
	/**
	 * @return piece length for this torrent
	 */
	public int getPieceLength() {
		return _pieceLength;
	}

	public TorrentFileSystem getFileSystem() {
		return fileSystem;
	}
	
	public BTDiskManagerMemento getDiskManagerData() {
		return diskManagerData;
	}
    
    public boolean isPrivate() {
        return isPrivate;
    }
	
	public void setContext(TorrentContext context) {
		if (context == null) // initialize cross-session ratio
			initRatio(context);
		this.context = context;
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
	 * Reads a BTMetaInfo from byte []
	 * 
	 * @param torrent byte array with the contents of .torrent
	 * @return new instance of BTMetaInfo if all went well
	 * @throws IOException if parsing or reading failed.
	 */
	public static BTMetaInfo readFromBytes(byte []torrent) throws IOException {
	    try {
	        Object metaInfo = Token.parse(torrent);
	        if(!(metaInfo instanceof Map))
	            throw new ValueException("metaInfo not a Map!");
	        return new BTMetaInfo(new BTDataImpl((Map)metaInfo));
	    } catch (IOException bad) {
	        LOG.error("read failed", bad);
	        throw bad;
	    }
	}
    
    public BTMetaInfo(BTMetaInfoMemento memento) throws InvalidDataException {
        _hashes =  memento.getHashes();
		Integer pieceLength = memento.getPieceLength();
		fileSystem = new TorrentFileSystem(memento.getFileSystem());
		_infoHash = memento.getInfoHash();
        try {
            _infoHashURN = URN.createSHA1UrnFromBytes(_infoHash);
        } catch (IOException e) {
            throw new InvalidDataException(e);
        }
        _trackers = memento.getTrackers();
		Float ratio = memento.getRatio();
        diskManagerData = memento.getFolderData();
		
		if (_hashes == null || pieceLength == null || fileSystem == null ||
				 _infoHash == null || _trackers == null ||
                 diskManagerData == null || ratio == null)
			throw new InvalidDataException("cannot read BTMetaInfo");
        
        if (_trackers.length == 0)
            throw new InvalidDataException("no trackers");
        for (URI uri : _trackers) {
            try {
                validateURI(uri);
            } catch (ValueException e) {
                throw new InvalidDataException(e);
            }
        }

        historicRatio = ratio.floatValue();
		_pieceLength = pieceLength.intValue();
        
        isPrivate = memento.isPrivate();   
    }

    /**
	 * Constructs a BTMetaInfo based on the BTData.
	 */
	public BTMetaInfo(BTData data) throws IOException {
		try {
			URI trackerURI = new URI(data.getAnnounce());
			validateURI(trackerURI);
			_trackers = new URI[] { trackerURI };
		} catch (URISyntaxException e) {
            URIUtils.error(e);
            throw new ValueException("bad tracker: " + data.getAnnounce());
		}

        isPrivate = data.isPrivate();
        
		// TODO: add proper support for multi-tracker torrents later.
		_infoHash = data.getInfoHash();
		
		try {
			_infoHashURN = URN.createSHA1UrnFromBytes(_infoHash);
		} catch (IOException impossible) {
		    throw new RuntimeException(impossible);
		}

		_hashes = parsePieces(data.getPieces());
        data.clearPieces(); // save memory.
        
		_pieceLength = (int)data.getPieceLength().longValue();
		if (_pieceLength <= 0)
			throw new ValueException("bad metainfo - illegal piece length: " + data.getPieceLength());

		diskManagerData = null;
		historicRatio = 0;
		fileSystem = new TorrentFileSystem(data, _hashes.size(), _pieceLength, _infoHash);
	}

    private static void validateURI(URI check) throws ValueException {
        if (check == null)
            throw new ValueException("null URI");
        if (!"http".equalsIgnoreCase(check.getScheme()))
            throw new ValueException("unsupported tracker protocol: "+check.getScheme());
        boolean hostOk = false;
        hostOk = check.getHost() != null; // validity will be checked upon request
        if (!hostOk)
            throw new ValueException("invalid host");
    }
    
    
	
	/**
	 * Serializes this, including information about the written ranges.
	 */
	public synchronized BTMetaInfoMemento toMemento() {
        BTMetaInfoMemento memento = new BTMetaInfoMementoImpl();
        memento.setFileSystem(fileSystem.toMemento());
        memento.setFolderData(context.getDiskManager().toMemento());
        memento.setHashes(_hashes);
        memento.setInfoHash(_infoHash);
        memento.setPieceLength(_pieceLength);
        memento.setPrivate(isPrivate);
        memento.setRatio(getRatio());
        memento.setTrackers(_trackers);
        return memento;
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
