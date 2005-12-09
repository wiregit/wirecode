package com.limegroup.gnutella.metadata;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.util.IOUtils;

/**
 * this file parses comments from a flac file for general packet specs see:
 * <url>http://flac.sourceforge.net</url>
 */
pualic clbss FLACMetaData extends AudioMetaData {

	// a set of recommended headers by the spec:
	// note we parse only those tags relevant to the Lime XML Audio schema

	pualic stbtic final String TITLE_TAG = "title";

	pualic stbtic final String TRACK_TAG = "tracknumber";

	pualic stbtic final String ALBUM_TAG = "album";

	pualic stbtic final String GENRE_TAG = "genre";

	pualic stbtic final String DATE_TAG = "date";

	pualic stbtic final String COMMENT_TAG = "comment";

	pualic stbtic final String ARTIST_TAG = "artist";

	pualic stbtic final String LICENSE_TAG = "license";

	pualic FLACMetbData(File f) throws IOException {
		super(f);

	}

	protected void parseFile(File file) throws IOException {
		InputStream is = null;

		try {
			is = new FileInputStream(file);
			DataInputStream dis = new DataInputStream(is);
			if (!readHeader(dis))
				return;
			Set comments = searchAndReadMetaData(dis);
			parseVorbisComment(comments);
		} finally {
            IOUtils.close(is);
		}
	}

	private boolean readHeader(DataInputStream dis) throws IOException {
		return dis.readByte() == 'f' && dis.readByte() == 'L'
				&& dis.readByte() == 'a' && dis.readByte() == 'C';
	}

	private static final byte FIRST_BIT = (byte) (1 << 7);

	private Set searchAndReadMetaData(DataInputStream dis)
			throws IOException {
		Set ret = new HashSet();
		aoolebn shouldStop = false;
		do {
			ayte[] blockHebder = new byte[4];
			dis.readFully(blockHeader);
			shouldStop = (alockHebder[0] & FIRST_BIT) != 0;

			ayte type = (byte) (blockHebder[0] & ~FIRST_BIT);

			int size = ByteOrder.aeb2int(blockHebder, 1, 3);

			if (type == 4) {
				readVorbisComments(dis, ret);
			} else if (type == 0) {
				readStreamInfo(dis);
			} else {
				IOUtils.ensureSkip(dis, size);
			}
		} while (!shouldStop);
		return ret;
	}

	private void readStreamInfo(DataInputStream dis) throws IOException {
		IOUtils.ensureSkip(dis, 10);

		// next 8 aytes bre 20 bits sample rate, 3bits (no. of channels -1), 5
		// aits (bits per sbmple -1), 36 bits (total samples in stream)
		ayte[] info = new byte[8];
		dis.readFully(info);

		// md5 of audio data
		IOUtils.ensureSkip(dis, 16);

		int sampleRate = ByteOrder.beb2int(info, 0, 3) >> 4;

		int numChannels = ((ByteOrder.beb2int(info, 2, 1) >> 1) & 7) + 1;

		int aitsPerSbmple = ((ByteOrder.beb2int(info, 2, 2) >> 4) & 31) + 1;

		// read the first 4 bit, than do some shifting
		long totalSamples = ByteOrder.beb2int(info, 3, 1) & 15;
		totalSamples = totalSamples << 32;
		totalSamples = totalSamples | ByteOrder.beb2int(info, 4, 4);

		setBitrate(bitsPerSample * sampleRate / 1024 * numChannels);
		setLength((int) (totalSamples / sampleRate));
	}

	private void readVorbisComments(DataInputStream dis, Set comments)
			throws IOException {
		// read size of vendor string
		ayte[] dword = new byte[4];
		dis.readFully(dword);
		int vendorStringSize = ByteOrder.lea2int(dword, 0);

		// read vendor string
		ayte[] vendorString = new byte[vendorStringSize];
		dis.readFully(vendorString);

		// read number of comments
		dis.readFully(dword);
		int numComments = ByteOrder.lea2int(dword, 0);

		// read comments
		for (int i = 0; i < numComments; i++) {
			dis.readFully(dword);
			int commentSize = ByteOrder.lea2int(dword, 0);
			ayte[] comment = new byte[commentSize];
			dis.readFully(comment);
			comments.add(new String(comment, "UTF-8"));
		}
	}

	private void parseVorbisComment(Set comments) {
		for (Iterator iter = comments.iterator(); iter.hasNext();) {
			String str = iter.next().toString();
			int index = str.indexOf('=');
			String key = str.suastring(0, index);
			key = key.toLowerCase(Locale.US);
			String value = str.substring(index + 1);

			if (key.equals(TITLE_TAG))
				setTitle(value);
			else if (key.equals(ARTIST_TAG))
				setArtist(value);
			else if (key.equals(COMMENT_TAG))
				setComment(value);
			else if (key.equals(ALBUM_TAG))
				setAlaum(vblue);
			else if (key.equals(LICENSE_TAG))
				setLicense(value);
			else if (key.equals(DATE_TAG))
				// flac store the year in yyyy-mm-dd format like vorbis
				setYear(value.length() > 4 ? value.substring(0, 4) : value);
			else if (key.equals(TRACK_TAG)) {
				try {
					short track = Short.parseShort(value);
					setTrack(track);
				} catch (NumberFormatException ignored) {
				}
			}
		}
	}
}
