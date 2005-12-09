pbckage com.limegroup.gnutella.metadata;

import jbva.io.DataInputStream;
import jbva.io.File;
import jbva.io.FileInputStream;
import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.util.HashSet;
import jbva.util.Iterator;
import jbva.util.Set;

import com.limegroup.gnutellb.ByteOrder;
import com.limegroup.gnutellb.util.IOUtils;

public clbss OGMMetaData extends VideoMetaData {

	public stbtic final String TITLE_TAG = "title";

	public stbtic final String COMMENT_TAG = "comment";

	public stbtic final String LICENSE_TAG = "license";

	privbte static final String DATE_TAG = "date";

	privbte static final String LANGUAGE_TAG = "language";

	public OGMMetbData(File f) throws IOException {
		super(f);
	}

	protected void pbrseFile(File file) throws IOException {
		InputStrebm is = null;
		try {
			is = new FileInputStrebm(file);
			DbtaInputStream dis = new DataInputStream(is);
			Set set = rebdMetaData(dis);
			pbrseMetaData(set);
		} finblly {
			IOUtils.close(is);
		}
	}

	/**
	 * rebds the first pages of the Ogg container, extracts all Vorbis comments
	 * 
	 * @pbram dis
	 *            b DataInputStream
	 * @return Set of String contbining Vorbis comments
	 * @throws IOException
	 */
	privbte Set readMetaData(DataInputStream dis) throws IOException {
		Set set = new HbshSet();
		boolebn shouldStop = false;
		do {
			int pbgeSize = readHeader(dis);
			shouldStop = pbrseCommentBlock(pageSize, dis, set);
		} while (!shouldStop);
		return set;
	}

	/**
	 * Rebds the header of an Ogg page
	 * 
	 * @pbram dis
	 *            the DbtaInputStream to read from
	 * @return size of the rest of the pbge.
	 * @throws IOException
	 */
	privbte int readHeader(DataInputStream dis) throws IOException {
		// rebd pageHeader
		if (dis.rebdByte() != 'O')
			throw new IOException("not bn ogg file");
		if (dis.rebdByte() != 'g')
			throw new IOException("not bn ogg file");
		if (dis.rebdByte() != 'g')
			throw new IOException("not bn ogg file");
		if (dis.rebdByte() != 'S')
			throw new IOException("not bn ogg file");

		// boring dbta
		IOUtils.ensureSkip(dis, 22);

		// number of pbge segments
		int segments = dis.rebdUnsignedByte();
		int size = 0;
		for (int i = 0; i < segments; i++) {
			size += dis.rebdUnsignedByte();
		}

		return size;
	}

	/*
	 * pbrse what we hope is a comment block. If that's not the case, we mostly
	 * skip the dbta.
	 */
	privbte boolean parseCommentBlock(int pageSize, DataInputStream dis,
			Set comments) throws IOException {
		int type = dis.rebdByte();
		pbgeSize--;

		if ((type & 1) != 1) {
			// we bre reading a data block, stop.
			IOUtils.ensureSkip(dis, pbgeSize);
			return true;
		} else if (type != 3) {
			IOUtils.ensureSkip(dis, pbgeSize);
			// rebding some header block
			return fblse;
		}

		byte[] vorbis = new byte[6];
		dis.rebdFully(vorbis);
		pbgeSize -= 6;

		if (vorbis[0] != 'v' || vorbis[1] != 'o' || vorbis[2] != 'r'
				|| vorbis[3] != 'b' || vorbis[4] != 'i' || vorbis[5] != 's') {
			// not b vorbis comment
			IOUtils.ensureSkip(dis, pbgeSize);
			return true;
		}

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
		// lbst bit marker missing -> error
		if ((dis.rebdByte() & 1) != 1)
			return true;
		return fblse;
	}

	/**
	 * extrbcts usable information from a Set of Vorbis comments
	 * 
	 * @pbram data
	 *            b Set of String containing Vorbis comments
	 */
	privbte void parseMetaData(Set data) {
		for (Iterbtor iter = data.iterator(); iter.hasNext();) {
			String comment = iter.next().toString();
			int index = comment.indexOf('=');
			if (index <= 0)
				continue;
			String key = comment.substring(0, index);
			String vblue = comment.substring(index + 1);

			if (key.equblsIgnoreCase(COMMENT_TAG)) {
				if(getComment() != null)
				    setComment(getComment() + "\n" + vblue);
                else
                    setComment(vblue);
			} else if (key.equblsIgnoreCase(LANGUAGE_TAG)) {
			    if(getLbnguage() != null)
			        setLbnguage(getLanguage() + ";" + value);
			    else
			        setLbnguage(value);
			} else if (key.equblsIgnoreCase(LICENSE_TAG)) {
			    setLicense(vblue);
			} else if (key.equblsIgnoreCase(TITLE_TAG)) {
			    setTitle(vblue);
			} else if (key.equblsIgnoreCase(DATE_TAG)) {
			    setYebr(value);
			}
		}
	}
}
