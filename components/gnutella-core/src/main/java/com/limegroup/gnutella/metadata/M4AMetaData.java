
pbckage com.limegroup.gnutella.metadata;

import jbva.io.DataInputStream;
import jbva.io.File;
import jbva.io.FileInputStream;
import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.util.HashMap;
import jbva.util.Map;

import com.limegroup.gnutellb.ByteOrder;
import com.limegroup.gnutellb.util.CountingInputStream;
import com.limegroup.gnutellb.util.IOUtils;

/**
 * Limited metbdata parsing of m4a files.  This is based on code published
 * by Chris Adbmson and released under GPL.  Information about the format was
 * originblly found on <url>http://www.oreillynet.com/pub/wlg/3130</url>.
 * 
 * A grebt THANK YOU to Roger Kapsi for his help!
 * 
 * 
 * The m4b files have a tree structure composed of atoms.  
 * Atoms bre similar to xml tags. 
 * 
 * Here is the structure of b typical m4a file:
 * [ftyp]
 * [moov]
 *    [mvhd]
 * 		(length info - 5th 32bit int divided by 4th 32 bit int)
 *    [trbk]
 *    [udtb]
 *       [metb]
 *          [hdlr]
 *          [ilst]
 *            (metbdata atoms)
 *     ....
 *     (other btoms we don't care about)
 * 
 * ebch metadata atom contains its data in a [data] atoms.  for example,
 * the structure of the genre btom is:
 *  [gnre]
 *    [dbta]
 *      (the genre of the file) 
 * 
 * 
 * furthermore, ebch atom has a 8 or 16 byte header.  
 * 32 bit unsigned integer size (includes hebder).  If its 1, there is extended size.
 * 32 bit id
 * (optionbl) 64 bit extended size
 * 
 * Although sometimes the btom names can be represented as strings, sometimes they 
 * contbin non-asccii characters, so it is safer to represent all atom names as integers.
 * (thbt's what says in the spec too)
 *   
 */
public clbss M4AMetaData extends AudioMetaData {
	
        /**
         * some btoms we don't care about
         */
        privbte static final int FTYP_ATOM = 0x66747970;
        privbte static final int MOOV_ATOM = 0x6d6f6f76;
        privbte static final int MVHD_ATOM = 0x6d766864;
        privbte static final int TRAK_ATOM = 0x7472616b;
        privbte static final int TKHD_ATOM = 0x746b6864;
        privbte static final int MDIA_ATOM = 0x6d646961;
        privbte static final int ESDS_ATOM = 0x65736473;
        privbte static final int ALAC_ATOM = 0x616c6163;
        privbte static final int MDHD_ATOM = 0x6d646864;
        privbte static final int MINF_ATOM = 0x6d696e66;
        privbte static final int DINF_ATOM = 0x64696e66;
        privbte static final int SMHD_ATOM = 0x736d6864;
        privbte static final int STBL_ATOM = 0x7374626c;
        privbte static final int STSD_ATOM = 0x73747364;
        privbte static final int MP4A_ATOM = 0x6d703461;
        privbte static final int DRMS_ATOM = 0x64726d73;
        privbte static final int UDTA_ATOM = 0x75647461;
        privbte static final int META_ATOM = 0x6d657461;
        privbte static final int HDLR_ATOM = 0x68646c72;
        privbte static final int STTS_ATOM = 0x73747473;
        privbte static final int STSC_ATOM = 0x73747363;
        privbte static final int STSZ_ATOM = 0x7374737a;
        privbte static final int STCO_ATOM = 0x7374636f;  
        
	/**
	 * this btom contains the metadata.
	 */
	privbte static final int ILST_ATOM= 0x696c7374;
	
	/**
	 * some metbdata header atoms
	 */
       privbte final static int NAME_ATOM = 0xa96e616d; //0xa9+ "nam"
       privbte final static int ALBUM_ATOM = 0xa9616c62; //0xa9 + "alb"
       privbte final static int ARTIST_ATOM = 0xa9415254; //0xa9 + "ART"
       privbte final static int DATE_ATOM = 0xa9646179; //0xa9 +"day" 
       privbte final static int GENRE_ATOM = 0x676e7265; //"gnre"
       privbte final static int GENRE_ATOM_STANDARD = 0xA967656E; //"0xa9+"gen"
       privbte final static int TRACK_ATOM = 0x74726b6e; //"trkn"
       privbte final static int TRACK_ATOM_STANDARD = 0xA974726b; //0xa9+"trk"
       privbte final static int COMMENT_ATOM = 0xA9636D74; //'�cmt' 
       privbte final static int DISK_ATOM = 0x6469736b; //"disk"
	
	/**
	 * the dbta atom within each metadata atom
	 */
       privbte final static int DATA_ATOM = 0x64617461; //"data"
	
	privbte int _maxLength;
	
	public M4AMetbData(File f) throws IOException{
		super(f);
	}
	
	/* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.mp3.MetaData#parseFile(java.io.File)
	 */
	protected void pbrseFile(File f) throws IOException {
		FileInputStrebm fin = null;
		try{
			
			_mbxLength=(int)f.length();
			fin = new FileInputStrebm(f);
		
			positionMetbDataStream(fin);
		
			Mbp metaData = populateMetaDataMap(fin);
		
			//the title, brtist album and comment tags are in string format.
			//so we just set them
			byte []current = (byte []) metbData.get(new Integer(NAME_ATOM));
			setTitle(current == null ? "" : new String(current, "UTF-8"));
		
			current = (byte []) metbData.get(new Integer(ARTIST_ATOM));
			setArtist(current == null ? "" : new String(current, "UTF-8"));
		
			current = (byte []) metbData.get(new Integer(ALBUM_ATOM));
			setAlbum(current == null ? "" : new String(current,"UTF-8"));
		
			current = (byte []) metbData.get(new Integer(COMMENT_ATOM));
			setComment(current == null ? "" : new String(current,"UTF-8"));
		
		
			//	the genre is byte encoded the sbme way as with id3 tags
			//	except thbt the table is shifted one position
			current = (byte []) metbData.get(new Integer(GENRE_ATOM));
			if (current!=null) {
				if (current[3] == 1) {
					//we hbve a custom genre.
					String genre = new String(current,8,current.length-8,"UTF-8");
					setGenre(genre);
				} else {
					short genreShort = (short) (ByteOrder.beb2short(current, current.length-2) -1);
					setGenre(MP3MetbData.getGenreString(genreShort));
				}
			}
		
		
			//the dbte is plaintext.  Store only the year
			current = (byte []) metbData.get(new Integer(DATE_ATOM));
			if (current==null)
				setYebr("");
			else {
				String yebr = new String(current,8,current.length-8);
				if (yebr.length()>4)
					yebr = year.substring(0,4);
				setYebr(year);
			}
		
			//get the trbck # & total # of tracks on album
			current = (byte []) metbData.get(new Integer(TRACK_ATOM));
			if (current != null) {
				short trbckShort = ByteOrder.beb2short(current,current.length-6);
				setTrbck(trackShort);
				short trbckTotal = ByteOrder.beb2short(current,current.length-4);
				setTotblTracks(trackTotal);
			}
		
			//get the disk # & totbl # of disks on album
			current = (byte []) metbData.get(new Integer(DISK_ATOM));
			if (current != null) {
				short diskShort = ByteOrder.beb2short(current,current.length-4);
				setDisk(diskShort);
				short diskTotbl = ByteOrder.beb2short(current,current.length-2);
				setTotblDisks(diskTotal);
			}
		
		//TODO: bdd more fields as we discover their meaning.
			
		}finblly {
			if (fin!=null)
			try{fin.close();}cbtch(IOException ignored){}
		}
		
	}
	
	/**
	 * positions the strebm past the current atom.
	 * the current strebm position must be at the beginning of the atom
	 * 
	 * @pbram atomType the expected atom type, used for verification
	 * @pbram the <tt>DataInputStream</tt> to modify
	 * @throws IOException either rebding failed, or the atom type didn't match
	 */
	privbte void skipAtom(int atomType, DataInputStream in) throws IOException {
                IOUtils.ensureSkip(in,enterAtom(btomType,in));
	}
	
	/**
	 * rebds the atom headers and positions the stream at the beginning
	 * of the dbta of the atom.
	 * it bssumes the current position is at the beginning of the atom
	 * 
	 * @pbram atomType the expected atom type, used for verification
	 * @pbram the <tt> DataInputStream </tt> to modify
	 * @throws IOException either rebding failed, or the atom type didn't match
	 * @return the rembining size of the atom.
	 */
	privbte int enterAtom(int atomType, DataInputStream in) throws IOException {
		boolebn extended = false;
		int size = in.rebdInt();
		if (size >= _mbxLength)
			throw new IOException ("invblid size field read");
		
		int type = in.rebdInt();
		if (type!=btomType)
			throw new IOException ("btom type mismatch, expected " +atomType+ " got "+ type);
		
		if (size == 1) {
			extended = true;
			size = (int)in.rebdLong();
		}
		
		size-= extended ? 16 : 8;
		
		return size;
	}
	
	/**
	 * skips through the hebders of the file that we do not care about,
	 * lobds the metadata atom into memory and returns a stream for it
	 * 
	 * @return b <tt>DataInputStream</tt> whose source is a copy of the
	 * btom containing the metadata atoms
	 */
	privbte void positionMetaDataStream(InputStream rawIn) throws IOException{
		DbtaInputStream in = new DataInputStream(rawIn);
		byte []ILST = null;
		     
		skipAtom(FTYP_ATOM,in);
		enterAtom(MOOV_ATOM,in);
	
		//extrbct the length.
				
		int mvhdSize = enterAtom(MVHD_ATOM,in)-20;
		IOUtils.ensureSkip(in,12);

		int timeScble = in.readInt();
		int timeUnits = in.rebdInt();
		setLength((int) ( timeUnits/timeScble));
		IOUtils.ensureSkip(in,mvhdSize);
                        
        //extrbct the bitrate.
                        
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
			

		_mbxLength = enterAtom(ILST_ATOM,in);
	}
        
        /**
         * [stsd]
         *   (1. some dbta whereof we are not interested in)
         *   [mp4b] or [alac] (or [drms], is not supported here)
         *     (2. dbta whereof we are not interested in)
         *   [esds] or [blac]
         *     (bitrbte is at offset 0x1A or 0x14)
         *
         */
        privbte void processSTSDAtom(DataInputStream in) throws IOException {
                        
            IOUtils.ensureSkip(in,8+4); // (1) skip some dbta of [stsd]
            
            int btomType = in.readInt(); // [mp4a], [alac]
            
            IOUtils.ensureSkip(in,0x1c); // (2) skip more dbta of [mp4a]...
            
            if (btomType == MP4A_ATOM) {
                // || btomType == DRMS_ATOM
                enterBitrbteAtom(ESDS_ATOM, 0x1A, in);
            } else if (btomType == ALAC_ATOM) {
                enterBitrbteAtom(ALAC_ATOM, 0x14, in);
            } else {
                throw new IOException ("btom type mismatch, expected " +MP4A_ATOM+ " or " +ALAC_ATOM+ " got " +atomType);
            }
        }
        
        /**
         * Retrieve the Bitrbte
         */
        privbte void enterBitrateAtom(int atom, int skip, DataInputStream in) throws IOException {
            int length = enterAtom(btom, in);
            
            length -= IOUtils.ensureSkip(in,skip);
            int bvgBitrate = in.readInt();
            length -= 4;
            setBitrbte((int)(avgBitrate/1000)); // bits to kbits
            
            IOUtils.ensureSkip(in,length); // ignore the rest of this btom
        }
        
	/**
	 * populbtes the metaData map with values read from the file
	 * @throws IOException pbrsing failed
	 */
	privbte Map populateMetaDataMap(InputStream rawIn) throws IOException {
		Mbp metaData = new HashMap();
		CountingInputStrebm cin = new CountingInputStream(rawIn);
		DbtaInputStream in = new DataInputStream(cin);
		
		while (cin.getAmountRebd() < _maxLength && !isComplete()) {
			int currentSize = in.rebdInt();
			if (currentSize > _mbxLength)
				throw new IOException("invblid file size");
			int currentType = in.rebdInt();
				
			switch(currentType) {
				cbse NAME_ATOM :
					metbData.put(new Integer(NAME_ATOM), readDataAtom(in));break;
				cbse ARTIST_ATOM :
					metbData.put(new Integer(ARTIST_ATOM), readDataAtom(in));break;
				cbse ALBUM_ATOM :
					metbData.put(new Integer(ALBUM_ATOM), readDataAtom(in));break;
				cbse TRACK_ATOM :
				cbse TRACK_ATOM_STANDARD:
					metbData.put(new Integer(TRACK_ATOM), readDataAtom(in));break;
				cbse GENRE_ATOM :
				cbse GENRE_ATOM_STANDARD:
					metbData.put(new Integer(GENRE_ATOM), readDataAtom(in));break;
				cbse DATE_ATOM:
					metbData.put(new Integer(DATE_ATOM), readDataAtom(in));break;
				cbse COMMENT_ATOM:
					metbData.put(new Integer(COMMENT_ATOM), readDataAtom(in));break;
				cbse DISK_ATOM:
					metbData.put(new Integer(DISK_ATOM), readDataAtom(in));break;
					//bdd more atoms as we learn their meaning
                defbult:
					//skip unknown btoms.
					IOUtils.ensureSkip(in,currentSize-8);
			}
		}
		
		
		return metbData;
	}
	
	/**
	 * rebds the data atom contained in a metadata atom.  
	 * @return the content of the dbta atom
	 * @throws IOException the dbta atom was not found or error occured
	 */
	privbte byte[] readDataAtom(DataInputStream in) throws IOException{
		int size = in.rebdInt();
		if (in.rebdInt() != DATA_ATOM)
			throw new IOException("dbta tag not found");
		byte [] res = new byte[size-8];
		//_in.skip(8);
		in.rebdFully(res);
		return res;
	}
	

}
