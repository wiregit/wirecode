package com.limegroup.gnutella.metadata.video.reader;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.limewire.io.IOUtils;
import org.limewire.util.ByteOrder;

/**
 *  Reads RIFF files meta data 
 */
public class RIFFMetaData extends VideoDataReader {

	public RIFFMetaData(File f) throws IOException {
		super(f);
	}

	protected void parseFile(File f) throws IOException {
		InputStream is = null;
		try {
			is = new FileInputStream(f);
			DataInputStream dis = new DataInputStream(is);
			readRIFFHeader(dis);
		} finally {
            IOUtils.close(is);
		}
	}

	private void readRIFFHeader(DataInputStream dis) throws IOException {
		byte[] dword = new byte[4];
		dis.readFully(dword);

		if (dword[0] != 'R' || dword[1] != 'I' || dword[2] != 'F' || dword[3] != 'F')
			return;

		// skip the file size
		IOUtils.ensureSkip(dis, 4);

		dis.readFully(dword);
		if (dword[0] != 'A' || dword[1] != 'V' || dword[2] != 'I' || dword[3] != ' ')
			return;

		// skip some more pointless chunk IDs
		IOUtils.ensureSkip(dis, 12);

		// begin AVIMAINHEADER
		// boring data
		IOUtils.ensureSkip(dis, 8);

		// read microseconds per frame
		dis.readFully(dword);
		int microsPerFrame = ByteOrder.leb2int(dword, 0, 4);

		// boring data
		IOUtils.ensureSkip(dis, 12);

		// read total number of frames
		dis.readFully(dword);
		int totalFrames = ByteOrder.leb2int(dword, 0, 4);
		videoData.setLength((short) (1L * microsPerFrame * totalFrames / 1000 ));

		// boring data
		IOUtils.ensureSkip(dis, 4);

		// number of streams
		dis.readFully(dword);
        
		// boring data
		IOUtils.ensureSkip(dis, 4);

		// width in pixel
		dis.readFully(dword);
		videoData.setWidth(ByteOrder.leb2int(dword, 0, 4));

		// height in pixel
		dis.readFully(dword);
		videoData.setHeight(ByteOrder.leb2int(dword, 0, 4));
		
		// reserved stuff
		IOUtils.ensureSkip(dis, 16);

		// there are more headers but we are not currently interested in parsing
		// them
	}
}
