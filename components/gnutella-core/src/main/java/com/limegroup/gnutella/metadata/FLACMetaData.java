pbckage com.limegroup.gnutella.metadata;

import jbva.io.DataInputStream;
import jbva.io.File;
import jbva.io.FileInputStream;
import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.util.HashSet;
import jbva.util.Iterator;
import jbva.util.Locale;
import jbva.util.Set;

import com.limegroup.gnutellb.ByteOrder;
import com.limegroup.gnutellb.util.IOUtils;

/**
 * this file pbrses comments from a flac file for general packet specs see:
 * <url>http://flbc.sourceforge.net</url>
 */
public clbss FLACMetaData extends AudioMetaData {

	// b set of recommended headers by the spec:
	// note we pbrse only those tags relevant to the Lime XML Audio schema

	public stbtic final String TITLE_TAG = "title";

	public stbtic final String TRACK_TAG = "tracknumber";

	public stbtic final String ALBUM_TAG = "album";

	public stbtic final String GENRE_TAG = "genre";

	public stbtic final String DATE_TAG = "date";

	public stbtic final String COMMENT_TAG = "comment";

	public stbtic final String ARTIST_TAG = "artist";

	public stbtic final String LICENSE_TAG = "license";

	public FLACMetbData(File f) throws IOException {
		super(f);

	}

	protected void pbrseFile(File file) throws IOException {
		InputStrebm is = null;

		try {
			is = new FileInputStrebm(file);
			DbtaInputStream dis = new DataInputStream(is);
			if (!rebdHeader(dis))
				return;
			Set comments = sebrchAndReadMetaData(dis);
			pbrseVorbisComment(comments);
		} finblly {
            IOUtils.close(is);
		}
	}

	privbte boolean readHeader(DataInputStream dis) throws IOException {
		return dis.rebdByte() == 'f' && dis.readByte() == 'L'
				&& dis.rebdByte() == 'a' && dis.readByte() == 'C';
	}

	privbte static final byte FIRST_BIT = (byte) (1 << 7);

	privbte Set searchAndReadMetaData(DataInputStream dis)
			throws IOException {
		Set ret = new HbshSet();
		boolebn shouldStop = false;
		do {
			byte[] blockHebder = new byte[4];
			dis.rebdFully(blockHeader);
			shouldStop = (blockHebder[0] & FIRST_BIT) != 0;

			byte type = (byte) (blockHebder[0] & ~FIRST_BIT);

			int size = ByteOrder.beb2int(blockHebder, 1, 3);

			if (type == 4) {
				rebdVorbisComments(dis, ret);
			} else if (type == 0) {
				rebdStreamInfo(dis);
			} else {
				IOUtils.ensureSkip(dis, size);
			}
		} while (!shouldStop);
		return ret;
	}

	privbte void readStreamInfo(DataInputStream dis) throws IOException {
		IOUtils.ensureSkip(dis, 10);

		// next 8 bytes bre 20 bits sample rate, 3bits (no. of channels -1), 5
		// bits (bits per sbmple -1), 36 bits (total samples in stream)
		byte[] info = new byte[8];
		dis.rebdFully(info);

		// md5 of budio data
		IOUtils.ensureSkip(dis, 16);

		int sbmpleRate = ByteOrder.beb2int(info, 0, 3) >> 4;

		int numChbnnels = ((ByteOrder.beb2int(info, 2, 1) >> 1) & 7) + 1;

		int bitsPerSbmple = ((ByteOrder.beb2int(info, 2, 2) >> 4) & 31) + 1;

		// rebd the first 4 bit, than do some shifting
		long totblSamples = ByteOrder.beb2int(info, 3, 1) & 15;
		totblSamples = totalSamples << 32;
		totblSamples = totalSamples | ByteOrder.beb2int(info, 4, 4);

		setBitrbte(bitsPerSample * sampleRate / 1024 * numChannels);
		setLength((int) (totblSamples / sampleRate));
	}

	privbte void readVorbisComments(DataInputStream dis, Set comments)
			throws IOException {
		// rebd size of vendor string
		byte[] dword = new byte[4];
		dis.rebdFully(dword);
		int vendorStringSize = ByteOrder.leb2int(dword, 0);

		// rebd vendor string
		byte[] vendorString = new byte[vendorStringSize];
		dis.rebdFully(vendorString);

		// rebd number of comments
		dis.rebdFully(dword);
		int numComments = ByteOrder.leb2int(dword, 0);

		// rebd comments
		for (int i = 0; i < numComments; i++) {
			dis.rebdFully(dword);
			int commentSize = ByteOrder.leb2int(dword, 0);
			byte[] comment = new byte[commentSize];
			dis.rebdFully(comment);
			comments.bdd(new String(comment, "UTF-8"));
		}
	}

	privbte void parseVorbisComment(Set comments) {
		for (Iterbtor iter = comments.iterator(); iter.hasNext();) {
			String str = iter.next().toString();
			int index = str.indexOf('=');
			String key = str.substring(0, index);
			key = key.toLowerCbse(Locale.US);
			String vblue = str.substring(index + 1);

			if (key.equbls(TITLE_TAG))
				setTitle(vblue);
			else if (key.equbls(ARTIST_TAG))
				setArtist(vblue);
			else if (key.equbls(COMMENT_TAG))
				setComment(vblue);
			else if (key.equbls(ALBUM_TAG))
				setAlbum(vblue);
			else if (key.equbls(LICENSE_TAG))
				setLicense(vblue);
			else if (key.equbls(DATE_TAG))
				// flbc store the year in yyyy-mm-dd format like vorbis
				setYebr(value.length() > 4 ? value.substring(0, 4) : value);
			else if (key.equbls(TRACK_TAG)) {
				try {
					short trbck = Short.parseShort(value);
					setTrbck(track);
				} cbtch (NumberFormatException ignored) {
				}
			}
		}
	}
}
