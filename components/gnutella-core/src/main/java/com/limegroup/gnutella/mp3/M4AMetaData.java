
package com.limegroup.gnutella.mp3;

import java.io.*;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.ByteOrder;
import com.sun.java.util.collections.*;

/**
 * Limited metadata parsing of m4a files.  This is based on code published
 * by Chris Adamson and released under GPL.  Information about the format was
 * originally found on <url>http://www.oreillynet.com/pub/wlg/3130</url>.
 * 
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
	private static final int FTYP_ATOM = 0x66747970;
	private static final int MOOV_ATOM = 0x6d6f6f76;
	private static final int MVHD_ATOM = 0x6d766864;
	private static final int TRAK_ATOM = 0x7472616b;
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
    final static int DATE_ATOM = 0xa9646179; //0xa9 +"day" 
	final static int  GENRE_ATOM = 0x676e7265; //"gnre"
	final static int  TRACK_ATOM = 0x74726b6e; //"trkn"
	
	/**
	 * the data atom within each metadata atom
	 */
	final static int DATA_ATOM = 0x64617461; //"data"
	
	
	
	DataInputStream _in;
	File _f;
	
	HashMap _metaData;
	
	public M4AMetaData(File f) throws IOException{
		super(f);
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.mp3.MetaData#parseFile(java.io.File)
	 */
	protected void parseFile(File f) throws IOException {
		_f = f;
		_in = new DataInputStream(new FileInputStream(_f));
		_in = getMetaDataStream();
		_metaData = new HashMap();
		
		populateMetaDataMap();
		
		//the title, artist and album tags are in string format.
		//so we just set them
		byte []current = (byte []) _metaData.get(new Integer(NAME_ATOM));
		setTitle(current == null ? "" : new String(current));
		
		current = (byte []) _metaData.get(new Integer(ARTIST_ATOM));
		setArtist(current == null ? "" : new String(current));
		
		current = (byte []) _metaData.get(new Integer(ALBUM_ATOM));
		setAlbum(current == null ? "" : new String(current));
		
		
		//the genre is byte encoded the same way as with id3 tags
		//except that the table is shifted one position
		//for some reason the atom is much larger than a scalar type, 
		//and the actual value is held at the end... oh well.
		current = (byte []) _metaData.get(new Integer(GENRE_ATOM));
		short genreShort = (short) (ByteOrder.beb2short(current, current.length-2) -1);
		setGenre(MP3MetaData.getGenreString(genreShort));
		
		
		//the date is plaintext.  Store only the year
		current = (byte []) _metaData.get(new Integer(DATE_ATOM));
		if (current==null)
			setYear("");
		else {
			String year = new String(current);
			if (year.length()>4)
				year = year.substring(0,4);
			setYear(year);
		}
		
		//TODO: add more fields as we discover their meaning.
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
	
	/**
	 * skips through the headers of the file that we do not care about,
	 * loads the metadata atom into memory and returns a stream for it
	 * 
	 * @return a <tt>DataInputStream</tt> whose source is a copy of the
	 * atom containing the metadata atoms
	 */
	private DataInputStream getMetaDataStream() throws IOException{
		byte []ILST = null;
		try {
			skipAtom(FTYP_ATOM,_in);
			enterAtom(MOOV_ATOM,_in);
			skipAtom(MVHD_ATOM,_in);
			skipAtom(TRAK_ATOM,_in);
			enterAtom(UDTA_ATOM,_in);
			enterAtom(META_ATOM,_in);
			_in.skip(4); //no comment...
			skipAtom(HDLR_ATOM,_in);
			
			//at this point we are about to enter the ILST atom
			//which is the atom containing the metadata atoms.
			//for simplicity, lets load this atom in memory -
			//its pretty small and we don't have to worry about closing the stream
			ILST = new byte[enterAtom(ILST_ATOM,_in)];
			_in.readFully(ILST);
			
		}finally {
			//close the file before proceeding futher
			if (_in!=null)
				try {_in.close();}catch(IOException ignored){}
		}
		
		//create a ByteArrayInputStream and read from it.
		return new DataInputStream(new ByteArrayInputStream(ILST));
	}
	
	
	/**
	 * populates the metaData map with values read from the file
	 * @throws IOException parsing failed
	 */
	private void populateMetaDataMap() throws IOException {
		try {
			while (true) {
				int currentSize = _in.readInt();
				if (currentSize > _f.length())
					throw new IOException("invalid file size");
				int currentType = _in.readInt();
				
				switch(currentType) {
					case NAME_ATOM :
						_metaData.put(new Integer(NAME_ATOM), readDataAtom());break;
					case ARTIST_ATOM :
						_metaData.put(new Integer(ARTIST_ATOM), readDataAtom());break;
					case ALBUM_ATOM :
						_metaData.put(new Integer(ALBUM_ATOM), readDataAtom());break;
					case TRACK_ATOM :
						_metaData.put(new Integer(TRACK_ATOM), readDataAtom());break;
					case GENRE_ATOM :
						_metaData.put(new Integer(GENRE_ATOM), readDataAtom());break;
					case DATE_ATOM:
						_metaData.put(new Integer(DATE_ATOM), readDataAtom());break;
						//add more atoms as we learn their meaning
					default:
						//skip unknown atoms.
						_in.skip(currentSize-8);
				}
			}
		}catch(EOFException ignored) {}
		//let IOExceptions go through.
	}
	
	/**
	 * reads the data atom contained in a metadata atom.  Supposedly,
	 * the first 8 bytes are junk.
	 * @return the content of the data atom
	 * @throws IOException the data atom was not found or error occured
	 */
	private byte[] readDataAtom() throws IOException{
		int size = _in.readInt();
		if (_in.readInt() != DATA_ATOM)
			throw new IOException("data tag not found");
		byte [] res = new byte[size-16];
		_in.skip(8);
		_in.readFully(res);
		return res;
	}
}
