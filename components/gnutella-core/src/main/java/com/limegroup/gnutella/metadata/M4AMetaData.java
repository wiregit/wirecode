
package com.limegroup.gnutella.metadata;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.limewire.io.CountingInputStream;
import org.limewire.io.IOUtils;
import org.limewire.util.ByteOrder;


/**
 * Limited metadata parsing of m4a files.  This is based on code published
 * by Chris Adamson and released under GPL.  Information about the format was
 * originally found on <url>http://www.oreillynet.com/pub/wlg/3130</url>.
 * 
 * A great THANK YOU to Roger Kapsi for his help!
 * 
 * 
 * The m4a files have a tree structure composed of atoms.  
 * Atoms are similar to xml tags. 
 * 
 * Here is the structure of a typical m4a file:
 * [ftyp]
 * [moov]
 *    [mvhd]
 * 		(length info - 5th 32bit int divided by 4th 32 bit int)
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
 * 
 * furthermore, each atom has a 8 or 16 byte header.  
 * 32 bit unsigned integer size (includes header).  If its 1, there is extended size.
 * 32 bit id
 * (optional) 64 bit extended size
 * 
 * Although sometimes the atom names can be represented as strings, sometimes they 
 * contain non-asccii characters, so it is safer to represent all atom names as integers.
 * (that's what says in the spec too)
 *   
 */
public class M4AMetaData extends AudioMetaData {
	
        /**
         * some atoms we don't care about
         */
        private static final int FTYP_ATOM = 0x66747970;
        private static final int MOOV_ATOM = 0x6d6f6f76;
        private static final int MVHD_ATOM = 0x6d766864;
        private static final int TRAK_ATOM = 0x7472616b;
        private static final int TKHD_ATOM = 0x746b6864;
        private static final int MDIA_ATOM = 0x6d646961;
        private static final int ESDS_ATOM = 0x65736473;
        private static final int ALAC_ATOM = 0x616c6163;
        private static final int MDHD_ATOM = 0x6d646864;
        private static final int MINF_ATOM = 0x6d696e66;
        private static final int DINF_ATOM = 0x64696e66;
        private static final int SMHD_ATOM = 0x736d6864;
        private static final int STBL_ATOM = 0x7374626c;
        private static final int STSD_ATOM = 0x73747364;
        private static final int MP4A_ATOM = 0x6d703461;
        @SuppressWarnings("unused")
        private static final int DRMS_ATOM = 0x64726d73;
        private static final int UDTA_ATOM = 0x75647461;
        private static final int META_ATOM = 0x6d657461;
        private static final int HDLR_ATOM = 0x68646c72;
        private static final int STTS_ATOM = 0x73747473;
        private static final int STSC_ATOM = 0x73747363;
        private static final int STSZ_ATOM = 0x7374737a;
        private static final int STCO_ATOM = 0x7374636f;  
        
	/**
	 * this atom contains the metadata.
	 */
	private static final int ILST_ATOM= 0x696c7374;
	
	/**
	 * some metadata header atoms
	 */
       private final static int NAME_ATOM = 0xa96e616d; //0xa9+ "nam"
       private final static int ALBUM_ATOM = 0xa9616c62; //0xa9 + "alb"
       private final static int ARTIST_ATOM = 0xa9415254; //0xa9 + "ART"
       private final static int DATE_ATOM = 0xa9646179; //0xa9 +"day" 
       private final static int GENRE_ATOM = 0x676e7265; //"gnre"
       private final static int GENRE_ATOM_STANDARD = 0xA967656E; //"0xa9+"gen"
       private final static int TRACK_ATOM = 0x74726b6e; //"trkn"
       private final static int TRACK_ATOM_STANDARD = 0xA974726b; //0xa9+"trk"
       private final static int COMMENT_ATOM = 0xA9636D74; //'�cmt' 
       private final static int DISK_ATOM = 0x6469736b; //"disk"
	
	/**
	 * the data atom within each metadata atom
	 */
       private final static int DATA_ATOM = 0x64617461; //"data"
	
	private int _maxLength;
	
	public M4AMetaData(File f) throws IOException{
		super(f);
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.mp3.MetaData#parseFile(java.io.File)
	 */
	protected void parseFile(File f) throws IOException {
		FileInputStream fin = null;
		try{
			
			_maxLength=(int)f.length();
			fin = new FileInputStream(f);
		
			positionMetaDataStream(fin);
		
			Map<Integer, byte[]> metaData = populateMetaDataMap(fin);
		
			//the title, artist album and comment tags are in string format.
			//so we just set them
			byte []current = metaData.get(new Integer(NAME_ATOM));
			setTitle(current == null ? "" : new String(current, "UTF-8"));
		
			current = metaData.get(new Integer(ARTIST_ATOM));
			setArtist(current == null ? "" : new String(current, "UTF-8"));
		
			current = metaData.get(new Integer(ALBUM_ATOM));
			setAlbum(current == null ? "" : new String(current,"UTF-8"));
		
			current = metaData.get(new Integer(COMMENT_ATOM));
			setComment(current == null ? "" : new String(current,"UTF-8"));
		
		
			//	the genre is byte encoded the same way as with id3 tags
			//	except that the table is shifted one position
			current = metaData.get(new Integer(GENRE_ATOM));
			if (current!=null) {
				if (current[3] == 1) {
					//we have a custom genre.
					String genre = new String(current,8,current.length-8,"UTF-8");
					setGenre(genre);
				} else {
					short genreShort = (short) (ByteOrder.beb2short(current, current.length-2) -1);
					setGenre(MP3MetaData.getGenreString(genreShort));
				}
			}
		
		
			//the date is plaintext.  Store only the year
			current = metaData.get(new Integer(DATE_ATOM));
			if (current==null)
				setYear("");
			else {
				String year = new String(current,8,current.length-8);
				if (year.length()>4)
					year = year.substring(0,4);
				setYear(year);
			}
		
			//get the track # & total # of tracks on album
			current = metaData.get(new Integer(TRACK_ATOM));
			if (current != null) {
				short trackShort = ByteOrder.beb2short(current,current.length-6);
				setTrack(trackShort);
				short trackTotal = ByteOrder.beb2short(current,current.length-4);
				setTotalTracks(trackTotal);
			}
		
			//get the disk # & total # of disks on album
			current = metaData.get(new Integer(DISK_ATOM));
			if (current != null) {
				short diskShort = ByteOrder.beb2short(current,current.length-4);
				setDisk(diskShort);
				short diskTotal = ByteOrder.beb2short(current,current.length-2);
				setTotalDisks(diskTotal);
			}
		
		//TODO: add more fields as we discover their meaning.
			
		} finally {
            IOUtils.close(fin);
		}
		
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
                IOUtils.ensureSkip(in,enterAtom(atomType,in));
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
		if (size >= _maxLength)
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
	private void positionMetaDataStream(InputStream rawIn) throws IOException{
		DataInputStream in = new DataInputStream(rawIn);		     
		skipAtom(FTYP_ATOM,in);
		enterAtom(MOOV_ATOM,in);
	
		//extract the length.
				
		int mvhdSize = enterAtom(MVHD_ATOM,in)-20;
		IOUtils.ensureSkip(in,12);

		int timeScale = in.readInt();
		int timeUnits = in.readInt();
		setLength(timeUnits/timeScale);
		IOUtils.ensureSkip(in,mvhdSize);
                        
        //extract the bitrate.
                        
		enterAtom(TRAK_ATOM, in);
        skipAtom(TKHD_ATOM, in);
        enterAtom(MDIA_ATOM, in);
        skipAtom(MDHD_ATOM, in);
        skipAtom(HDLR_ATOM, in);
        enterAtom(MINF_ATOM, in);
        skipAtom(SMHD_ATOM, in);
        skipAtom(DINF_ATOM, in);
        enterAtom(STBL_ATOM, in);
        enterAtom(STSD_ATOM, in);
                        
        processSTSDAtom(in);
          	      
        skipAtom(STTS_ATOM, in);
        skipAtom(STSC_ATOM, in);
        skipAtom(STSZ_ATOM, in);
        skipAtom(STCO_ATOM, in);
            
		enterAtom(UDTA_ATOM,in);
                        
		enterAtom(META_ATOM,in);
		IOUtils.ensureSkip(in,4); //no comment...
		skipAtom(HDLR_ATOM,in);
			

		_maxLength = enterAtom(ILST_ATOM,in);
	}
        
        /**
         * [stsd]
         *   (1. some data whereof we are not interested in)
         *   [mp4a] or [alac] (or [drms], is not supported here)
         *     (2. data whereof we are not interested in)
         *   [esds] or [alac]
         *     (bitrate is at offset 0x1A or 0x14)
         *
         */
        private void processSTSDAtom(DataInputStream in) throws IOException {
                        
            IOUtils.ensureSkip(in,8+4); // (1) skip some data of [stsd]
            
            int atomType = in.readInt(); // [mp4a], [alac]
            
            IOUtils.ensureSkip(in,0x1c); // (2) skip more data of [mp4a]...
            
            if (atomType == MP4A_ATOM) {
                // || atomType == DRMS_ATOM
                enterBitrateAtom(ESDS_ATOM, 0x1A, in);
            } else if (atomType == ALAC_ATOM) {
                enterBitrateAtom(ALAC_ATOM, 0x14, in);
            } else {
                throw new IOException ("atom type mismatch, expected " +MP4A_ATOM+ " or " +ALAC_ATOM+ " got " +atomType);
            }
        }
        
        /**
         * Retrieve the Bitrate
         */
        private void enterBitrateAtom(int atom, int skip, DataInputStream in) throws IOException {
            int length = enterAtom(atom, in);
            
            length -= IOUtils.ensureSkip(in,skip);
            int avgBitrate = in.readInt();
            length -= 4;
            setBitrate(avgBitrate/1000); // bits to kbits
            
            IOUtils.ensureSkip(in,length); // ignore the rest of this atom
        }
        
	/**
	 * populates the metaData map with values read from the file
	 * @throws IOException parsing failed
	 */
	private Map<Integer, byte[]> populateMetaDataMap(InputStream rawIn) throws IOException {
		Map<Integer, byte[]> metaData = new HashMap<Integer, byte[]>();
		CountingInputStream cin = new CountingInputStream(rawIn);
		DataInputStream in = new DataInputStream(cin);
		
		while (cin.getAmountRead() < _maxLength && !isComplete()) {
			int currentSize = in.readInt();
			if (currentSize > _maxLength)
				throw new IOException("invalid file size");
			int currentType = in.readInt();
				
			switch(currentType) {
				case NAME_ATOM :
					metaData.put(new Integer(NAME_ATOM), readDataAtom(in));break;
				case ARTIST_ATOM :
					metaData.put(new Integer(ARTIST_ATOM), readDataAtom(in));break;
				case ALBUM_ATOM :
					metaData.put(new Integer(ALBUM_ATOM), readDataAtom(in));break;
				case TRACK_ATOM :
				case TRACK_ATOM_STANDARD:
					metaData.put(new Integer(TRACK_ATOM), readDataAtom(in));break;
				case GENRE_ATOM :
				case GENRE_ATOM_STANDARD:
					metaData.put(new Integer(GENRE_ATOM), readDataAtom(in));break;
				case DATE_ATOM:
					metaData.put(new Integer(DATE_ATOM), readDataAtom(in));break;
				case COMMENT_ATOM:
					metaData.put(new Integer(COMMENT_ATOM), readDataAtom(in));break;
				case DISK_ATOM:
					metaData.put(new Integer(DISK_ATOM), readDataAtom(in));break;
					//add more atoms as we learn their meaning
                default:
					//skip unknown atoms.
					IOUtils.ensureSkip(in,currentSize-8);
			}
		}
		
		
		return metaData;
	}
	
	/**
	 * reads the data atom contained in a metadata atom.  
	 * @return the content of the data atom
	 * @throws IOException the data atom was not found or error occured
	 */
	private byte[] readDataAtom(DataInputStream in) throws IOException{
		int size = in.readInt();
		if (in.readInt() != DATA_ATOM)
			throw new IOException("data tag not found");
		byte [] res = new byte[size-8];
		//_in.skip(8);
		in.readFully(res);
		return res;
	}
	

}
