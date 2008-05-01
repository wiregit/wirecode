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
import java.util.ListIterator;
import java.util.Set;

import org.limewire.io.InvalidDataException;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.serial.BTDiskManagerMemento;
import com.limegroup.gnutella.downloader.serial.BTMetaInfoMemento;
import com.limegroup.gnutella.downloader.serial.BTMetaInfoMementoImpl;
import com.limegroup.gnutella.http.URIUtils;
import com.limegroup.gnutella.security.SHA1;

/**
 * Contains information usually parsed in a .torrent file
 */
public class BTMetaInfoImpl implements BTMetaInfo {        
    
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
    
	/* (non-Javadoc)
     * @see com.limegroup.bittorrent.BTMetaInfo#getPieceLength()
     */
	public int getPieceLength() {
		return _pieceLength;
	}

	/* (non-Javadoc)
     * @see com.limegroup.bittorrent.BTMetaInfo#getFileSystem()
     */
	public TorrentFileSystem getFileSystem() {
		return fileSystem;
	}
	
	/* (non-Javadoc)
     * @see com.limegroup.bittorrent.BTMetaInfo#getDiskManagerData()
     */
	public BTDiskManagerMemento getDiskManagerData() {
		return diskManagerData;
	}
    
    /* (non-Javadoc)
     * @see com.limegroup.bittorrent.BTMetaInfo#isPrivate()
     */
    public boolean isPrivate() {
        return isPrivate;
    }
	
	/* (non-Javadoc)
     * @see com.limegroup.bittorrent.BTMetaInfo#setContext(com.limegroup.bittorrent.TorrentContext)
     */
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
	
	public long getAmountUploaded() {
		return uploadedNow;
	}
	
	public void countUploaded(int uploaded) {
		uploadedNow += uploaded;
	}
	
	public float getRatio() {
		long downloaded = context.getDiskManager().getBlockSize();
		if (downloaded == 0)
			return 0;
		return (uploadedBefore + uploadedNow) * 1f / downloaded;
	}
	
	/* (non-Javadoc)
     * @see com.limegroup.bittorrent.BTMetaInfo#verify(byte[], int)
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

	/* (non-Javadoc)
     * @see com.limegroup.bittorrent.BTMetaInfo#getInfoHash()
     */
	public byte[] getInfoHash() {
		return _infoHash;
	}

	/* (non-Javadoc)
     * @see com.limegroup.bittorrent.BTMetaInfo#getURN()
     */
	public URN getURN() {
		return _infoHashURN;
	}
	
	/* (non-Javadoc)
     * @see com.limegroup.bittorrent.BTMetaInfo#getFileDesc()
     */
	public FileDesc getFileDesc() {
		if (_desc == null) {
			Set<URN> s = new HashSet<URN>();
			s.add(getURN());
			_desc = new FakeFileDesc(fileSystem.getCompleteFile(),s);
		}
		return _desc;
	}

	/* (non-Javadoc)
     * @see com.limegroup.bittorrent.BTMetaInfo#resetFileDesc()
     */
	public void resetFileDesc() {
		_desc = null;
	}

	/* (non-Javadoc)
     * @see com.limegroup.bittorrent.BTMetaInfo#getNumBlocks()
     */
	public int getNumBlocks() {
		return (int) ((fileSystem.getTotalSize() + _pieceLength - 1) / _pieceLength);
	}

	/* (non-Javadoc)
     * @see com.limegroup.bittorrent.BTMetaInfo#getName()
     */
	public String getName() {
		return fileSystem.getName();
	}

	/* (non-Javadoc)
     * @see com.limegroup.bittorrent.BTMetaInfo#getTrackers()
     */
	public URI[] getTrackers() {
		return _trackers;
	}

	/* (non-Javadoc)
     * @see com.limegroup.bittorrent.BTMetaInfo#getMessageDigest()
     */
	public MessageDigest getMessageDigest() {
		return new SHA1();
	}
        
    public BTMetaInfoImpl(BTMetaInfoMemento memento) throws InvalidDataException {
        _hashes =  memento.getHashes();
        // make sure all 0-length hashes are correctly the VERIFIED_HASH
        for(ListIterator<byte[]> hashIter = _hashes.listIterator(); hashIter.hasNext(); ) {
            byte[] next = hashIter.next();
            if(next.length == 0)
                hashIter.set(VERIFIED_HASH);
        }
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
	public BTMetaInfoImpl(BTData data) throws IOException {
		try {
			URI trackerURI = URIUtils.toURI(data.getAnnounce());
			validateURI(trackerURI);
			_trackers = new URI[] { trackerURI };
		} catch (URISyntaxException e) {
            //URIUtils.error(e);
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
    
    
	
	/* (non-Javadoc)
     * @see com.limegroup.bittorrent.BTMetaInfo#toMemento()
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
