
package com.limegroup.gnutella.mp3;

import java.io.*;

import com.limegroup.gnutella.util.*;
import com.sun.java.util.collections.*;

/**
 * Limited metadata parsing of m4a files.
 * 
 * The m4a files have a tree structure composed of atoms.  
 * Atoms are similar to xml tags. 
 * 
 * Here is the structure of a typical m4a file:
 * [ftyp]
 * [moov]
 *    [mvhd]
 *    [trak]
 *    [udta]
 *       [meta]
 *          [hdlr]
 *          [ilst]
 *            (metadata atoms)
 *     ....
 *     (other atoms we don't care about)
 * 
 * each metadata atom contains its data in a [data] atoms.  for example,
 * the structure of the genre atom is:
 *  [gnre]
 *    [data]
 *      (the genre of the file) 
 * 
 * furthermore, each atom has a 8 or 16 byte header.  
 * 32 bit unsigned integer size (includes header).  If its 1, there is extended size.
 * 32 bit id
 * (optional) 64 bit extended size
 * 
 * Although sometimes the atom names can be represented as strings, sometimes they 
 * contain non-asccii characters, so it is safer to represent all atom names as integers.
 * (that's what says in the spec too)
 */
public class M4AMetaData extends AudioMetaData {
	
	/**
	 * some atoms we don't care about
	 */
	private static final int FTYP_ATOM = 0x66767970;
	private static final int MOOV_ATOM = 0x6d6f6f76;
	private static final int MVHD_ATOM = 0x6d766864;
	private static final int TRAK_ATOM = 0x7672616b;
	private static final int UDTA_ATOM = 0x75647461;
	private static final int META_ATOM = 0x6d657461;
	private static final int HDLR_ATOM = 0x68646c72;
	
	/**
	 * this atom contains the metadata.
	 */
	private static final int ILST_ATOM= 0x696c7374;
	
	/**
	 * some metadata header atoms
	 */
	final static int NAME_ATOM = 0xa96e616d; //0xa9+ "nam"
    final static int ALBUM_ATOM = 0xa9616c62; //0xa9 + "alb"
    final static int ARTIST_ATOM = 0xa9415254; //0xa9 + "ART"
	final static int  GENRE_ATOM = 0x676e7265; //"gnre"
	final static int  TRACK_ATOM = 0x74726b6e; //"trkn"
	
	/**
	 * the data atom within each metadata atom
	 */
	final static int DATA_TAG = 0x64617461; //"data"
	
	
	
	InputStream _in;
	File _f;
	
	public M4AMetaData(File f) throws IOException{
		super(f);
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.mp3.MetaData#parseFile(java.io.File)
	 */
	protected void parseFile(File f) throws IOException {
		// TODO Auto-generated method stub
	}
	
	/**
	 * positions the stream past the current atom.
	 * the current stream position must be at the beginning of the atom
	 * 
	 * @param atomType the expected atom type, used for verification
	 * @param the <tt>DataInputStream</tt> to modify
	 * @throws IOException either reading failed, or the atom type didn't match
	 */
	private void skipAtom(int atomType, DataInputStream in) throws IOException {
		in.skip(enterAtom(atomType,in));
	}
	
	/**
	 * reads the atom headers and positions the stream at the beginning
	 * of the data of the atom.
	 * it assumes the current position is at the beginning of the atom
	 * 
	 * @param atomType the expected atom type, used for verification
	 * @param the <tt> DataInputStream </tt> to modify
	 * @throws IOException either reading failed, or the atom type didn't match
	 * @return the remaining size of the atom.
	 */
	private int enterAtom(int atomType, DataInputStream in) throws IOException {
		boolean extended = false;
		int size = in.readInt();
		if (size >= _f.length())
			throw new IOException ("invalid size field read");
		
		int type = in.readInt();
		if (type!=atomType)
			throw new IOException ("atom type mismatch, expected " +atomType+ " got "+ type);
		
		if (size == 1) {
			extended = true;
			size = (int)in.readLong();
		}
		
		size-= extended ? 16 : 8;
		
		return size;
	}
}
