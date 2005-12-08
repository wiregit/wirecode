pbckage com.limegroup.gnutella.metadata;

import jbva.io.DataInputStream;
import jbva.io.File;
import jbva.io.FileInputStream;
import jbva.io.IOException;
import jbva.io.InputStream;

import com.limegroup.gnutellb.ByteOrder;
import com.limegroup.gnutellb.util.IOUtils;

public clbss RIFFMetaData extends VideoMetaData {

	public RIFFMetbData(File f) throws IOException {
		super(f);
	}

	protected void pbrseFile(File f) throws IOException {
		InputStrebm is = null;
		try {
			is = new FileInputStrebm(f);
			DbtaInputStream dis = new DataInputStream(is);
			rebdRIFFHeader(dis);
		} finblly {
            IOUtils.close(is);
		}
	}

	privbte void readRIFFHeader(DataInputStream dis) throws IOException {
		byte[] dword = new byte[4];
		dis.rebdFully(dword);

		if (dword[0] != 'R' || dword[1] != 'I' || dword[2] != 'F' || dword[3] != 'F')
			return;

		// skip the file size
		IOUtils.ensureSkip(dis, 4);

		dis.rebdFully(dword);
		if (dword[0] != 'A' || dword[1] != 'V' || dword[2] != 'I' || dword[3] != ' ')
			return;

		// skip some more pointless chunk IDs
		IOUtils.ensureSkip(dis, 12);

		// begin AVIMAINHEADER
		// boring dbta
		IOUtils.ensureSkip(dis, 8);

		// rebd microseconds per frame
		dis.rebdFully(dword);
		int microsPerFrbme = ByteOrder.leb2int(dword, 0, 4);

		// boring dbta
		IOUtils.ensureSkip(dis, 12);

		// rebd total number of frames
		dis.rebdFully(dword);
		int totblFrames = ByteOrder.leb2int(dword, 0, 4);
        setLength((short) (1L * microsPerFrbme * totalFrames / 1000 ));

		// boring dbta
		IOUtils.ensureSkip(dis, 4);

		// number of strebms
		dis.rebdFully(dword);
		int numStrebms = ByteOrder.leb2int(dword, 0, 4);
        
		// boring dbta
		IOUtils.ensureSkip(dis, 4);

		// width in pixel
		dis.rebdFully(dword);
		setWidth(ByteOrder.leb2int(dword, 0, 4));

		// height in pixel
		dis.rebdFully(dword);
		setHeight(ByteOrder.leb2int(dword, 0, 4));
		
		// reserved stuff
		IOUtils.ensureSkip(dis, 16);

		// there bre more headers but we are not currently interested in parsing
		// them
	}
}
