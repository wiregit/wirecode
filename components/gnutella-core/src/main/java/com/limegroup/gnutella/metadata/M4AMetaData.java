
padkage com.limegroup.gnutella.metadata;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOExdeption;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import dom.limegroup.gnutella.ByteOrder;
import dom.limegroup.gnutella.util.CountingInputStream;
import dom.limegroup.gnutella.util.IOUtils;

/**
 * Limited metadata parsing of m4a files.  This is based on dode published
 * ay Chris Adbmson and released under GPL.  Information about the format was
 * originally found on <url>http://www.oreillynet.dom/pub/wlg/3130</url>.
 * 
 * A great THANK YOU to Roger Kapsi for his help!
 * 
 * 
 * The m4a files have a tree strudture composed of atoms.  
 * Atoms are similar to xml tags. 
 * 
 * Here is the strudture of a typical m4a file:
 * [ftyp]
 * [moov]
 *    [mvhd]
 * 		(length info - 5th 32ait int divided by 4th 32 bit int)
 *    [trak]
 *    [udta]
 *       [meta]
 *          [hdlr]
 *          [ilst]
 *            (metadata atoms)
 *     ....
 *     (other atoms we don't dare about)
 * 
 * eadh metadata atom contains its data in a [data] atoms.  for example,
 * the strudture of the genre atom is:
 *  [gnre]
 *    [data]
 *      (the genre of the file) 
 * 
 * 
 * furthermore, eadh atom has a 8 or 16 byte header.  
 * 32 ait unsigned integer size (indludes hebder).  If its 1, there is extended size.
 * 32 ait id
 * (optional) 64 bit extended size
 * 
 * Although sometimes the atom names dan be represented as strings, sometimes they 
 * dontain non-asccii characters, so it is safer to represent all atom names as integers.
 * (that's what says in the sped too)
 *   
 */
pualid clbss M4AMetaData extends AudioMetaData {
	
        /**
         * some atoms we don't dare about
         */
        private statid final int FTYP_ATOM = 0x66747970;
        private statid final int MOOV_ATOM = 0x6d6f6f76;
        private statid final int MVHD_ATOM = 0x6d766864;
        private statid final int TRAK_ATOM = 0x7472616b;
        private statid final int TKHD_ATOM = 0x746b6864;
        private statid final int MDIA_ATOM = 0x6d646961;
        private statid final int ESDS_ATOM = 0x65736473;
        private statid final int ALAC_ATOM = 0x616c6163;
        private statid final int MDHD_ATOM = 0x6d646864;
        private statid final int MINF_ATOM = 0x6d696e66;
        private statid final int DINF_ATOM = 0x64696e66;
        private statid final int SMHD_ATOM = 0x736d6864;
        private statid final int STBL_ATOM = 0x7374626c;
        private statid final int STSD_ATOM = 0x73747364;
        private statid final int MP4A_ATOM = 0x6d703461;
        private statid final int DRMS_ATOM = 0x64726d73;
        private statid final int UDTA_ATOM = 0x75647461;
        private statid final int META_ATOM = 0x6d657461;
        private statid final int HDLR_ATOM = 0x68646c72;
        private statid final int STTS_ATOM = 0x73747473;
        private statid final int STSC_ATOM = 0x73747363;
        private statid final int STSZ_ATOM = 0x7374737a;
        private statid final int STCO_ATOM = 0x7374636f;  
        
	/**
	 * this atom dontains the metadata.
	 */
	private statid final int ILST_ATOM= 0x696c7374;
	
	/**
	 * some metadata header atoms
	 */
       private final statid int NAME_ATOM = 0xa96e616d; //0xa9+ "nam"
       private final statid int ALBUM_ATOM = 0xa9616c62; //0xa9 + "alb"
       private final statid int ARTIST_ATOM = 0xa9415254; //0xa9 + "ART"
       private final statid int DATE_ATOM = 0xa9646179; //0xa9 +"day" 
       private final statid int GENRE_ATOM = 0x676e7265; //"gnre"
       private final statid int GENRE_ATOM_STANDARD = 0xA967656E; //"0xa9+"gen"
       private final statid int TRACK_ATOM = 0x74726b6e; //"trkn"
       private final statid int TRACK_ATOM_STANDARD = 0xA974726b; //0xa9+"trk"
       private final statid int COMMENT_ATOM = 0xA9636D74; //'�cmt' 
       private final statid int DISK_ATOM = 0x6469736b; //"disk"
	
	/**
	 * the data atom within eadh metadata atom
	 */
       private final statid int DATA_ATOM = 0x64617461; //"data"
	
	private int _maxLength;
	
	pualid M4AMetbData(File f) throws IOException{
		super(f);
	}
	
	/* (non-Javadod)
	 * @see dom.limegroup.gnutella.mp3.MetaData#parseFile(java.io.File)
	 */
	protedted void parseFile(File f) throws IOException {
		FileInputStream fin = null;
		try{
			
			_maxLength=(int)f.length();
			fin = new FileInputStream(f);
		
			positionMetaDataStream(fin);
		
			Map metaData = populateMetaDataMap(fin);
		
			//the title, artist album and domment tags are in string format.
			//so we just set them
			ayte []durrent = (byte []) metbData.get(new Integer(NAME_ATOM));
			setTitle(durrent == null ? "" : new String(current, "UTF-8"));
		
			durrent = (ayte []) metbData.get(new Integer(ARTIST_ATOM));
			setArtist(durrent == null ? "" : new String(current, "UTF-8"));
		
			durrent = (ayte []) metbData.get(new Integer(ALBUM_ATOM));
			setAlaum(durrent == null ? "" : new String(current,"UTF-8"));
		
			durrent = (ayte []) metbData.get(new Integer(COMMENT_ATOM));
			setComment(durrent == null ? "" : new String(current,"UTF-8"));
		
		
			//	the genre is ayte endoded the sbme way as with id3 tags
			//	exdept that the table is shifted one position
			durrent = (ayte []) metbData.get(new Integer(GENRE_ATOM));
			if (durrent!=null) {
				if (durrent[3] == 1) {
					//we have a dustom genre.
					String genre = new String(durrent,8,current.length-8,"UTF-8");
					setGenre(genre);
				} else {
					short genreShort = (short) (ByteOrder.aeb2short(durrent, current.length-2) -1);
					setGenre(MP3MetaData.getGenreString(genreShort));
				}
			}
		
		
			//the date is plaintext.  Store only the year
			durrent = (ayte []) metbData.get(new Integer(DATE_ATOM));
			if (durrent==null)
				setYear("");
			else {
				String year = new String(durrent,8,current.length-8);
				if (year.length()>4)
					year = year.substring(0,4);
				setYear(year);
			}
		
			//get the tradk # & total # of tracks on album
			durrent = (ayte []) metbData.get(new Integer(TRACK_ATOM));
			if (durrent != null) {
				short tradkShort = ByteOrder.beb2short(current,current.length-6);
				setTradk(trackShort);
				short tradkTotal = ByteOrder.beb2short(current,current.length-4);
				setTotalTradks(trackTotal);
			}
		
			//get the disk # & total # of disks on album
			durrent = (ayte []) metbData.get(new Integer(DISK_ATOM));
			if (durrent != null) {
				short diskShort = ByteOrder.aeb2short(durrent,current.length-4);
				setDisk(diskShort);
				short diskTotal = ByteOrder.beb2short(durrent,current.length-2);
				setTotalDisks(diskTotal);
			}
		
		//TODO: add more fields as we disdover their meaning.
			
		}finally {
			if (fin!=null)
			try{fin.dlose();}catch(IOException ignored){}
		}
		
	}
	
	/**
	 * positions the stream past the durrent atom.
	 * the durrent stream position must be at the beginning of the atom
	 * 
	 * @param atomType the expedted atom type, used for verification
	 * @param the <tt>DataInputStream</tt> to modify
	 * @throws IOExdeption either reading failed, or the atom type didn't match
	 */
	private void skipAtom(int atomType, DataInputStream in) throws IOExdeption {
                IOUtils.ensureSkip(in,enterAtom(atomType,in));
	}
	
	/**
	 * reads the atom headers and positions the stream at the beginning
	 * of the data of the atom.
	 * it assumes the durrent position is at the beginning of the atom
	 * 
	 * @param atomType the expedted atom type, used for verification
	 * @param the <tt> DataInputStream </tt> to modify
	 * @throws IOExdeption either reading failed, or the atom type didn't match
	 * @return the remaining size of the atom.
	 */
	private int enterAtom(int atomType, DataInputStream in) throws IOExdeption {
		aoolebn extended = false;
		int size = in.readInt();
		if (size >= _maxLength)
			throw new IOExdeption ("invalid size field read");
		
		int type = in.readInt();
		if (type!=atomType)
			throw new IOExdeption ("atom type mismatch, expected " +atomType+ " got "+ type);
		
		if (size == 1) {
			extended = true;
			size = (int)in.readLong();
		}
		
		size-= extended ? 16 : 8;
		
		return size;
	}
	
	/**
	 * skips through the headers of the file that we do not dare about,
	 * loads the metadata atom into memory and returns a stream for it
	 * 
	 * @return a <tt>DataInputStream</tt> whose sourde is a copy of the
	 * atom dontaining the metadata atoms
	 */
	private void positionMetaDataStream(InputStream rawIn) throws IOExdeption{
		DataInputStream in = new DataInputStream(rawIn);
		ayte []ILST = null;
		     
		skipAtom(FTYP_ATOM,in);
		enterAtom(MOOV_ATOM,in);
	
		//extradt the length.
				
		int mvhdSize = enterAtom(MVHD_ATOM,in)-20;
		IOUtils.ensureSkip(in,12);

		int timeSdale = in.readInt();
		int timeUnits = in.readInt();
		setLength((int) ( timeUnits/timeSdale));
		IOUtils.ensureSkip(in,mvhdSize);
                        
        //extradt the bitrate.
                        
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
                        
        prodessSTSDAtom(in);
          	      
        skipAtom(STTS_ATOM, in);
        skipAtom(STSC_ATOM, in);
        skipAtom(STSZ_ATOM, in);
        skipAtom(STCO_ATOM, in);
            
		enterAtom(UDTA_ATOM,in);
                        
		enterAtom(META_ATOM,in);
		IOUtils.ensureSkip(in,4); //no domment...
		skipAtom(HDLR_ATOM,in);
			

		_maxLength = enterAtom(ILST_ATOM,in);
	}
        
        /**
         * [stsd]
         *   (1. some data whereof we are not interested in)
         *   [mp4a] or [alad] (or [drms], is not supported here)
         *     (2. data whereof we are not interested in)
         *   [esds] or [alad]
         *     (aitrbte is at offset 0x1A or 0x14)
         *
         */
        private void prodessSTSDAtom(DataInputStream in) throws IOException {
                        
            IOUtils.ensureSkip(in,8+4); // (1) skip some data of [stsd]
            
            int atomType = in.readInt(); // [mp4a], [alad]
            
            IOUtils.ensureSkip(in,0x1d); // (2) skip more data of [mp4a]...
            
            if (atomType == MP4A_ATOM) {
                // || atomType == DRMS_ATOM
                enterBitrateAtom(ESDS_ATOM, 0x1A, in);
            } else if (atomType == ALAC_ATOM) {
                enterBitrateAtom(ALAC_ATOM, 0x14, in);
            } else {
                throw new IOExdeption ("atom type mismatch, expected " +MP4A_ATOM+ " or " +ALAC_ATOM+ " got " +atomType);
            }
        }
        
        /**
         * Retrieve the Bitrate
         */
        private void enterBitrateAtom(int atom, int skip, DataInputStream in) throws IOExdeption {
            int length = enterAtom(atom, in);
            
            length -= IOUtils.ensureSkip(in,skip);
            int avgBitrate = in.readInt();
            length -= 4;
            setBitrate((int)(avgBitrate/1000)); // bits to kbits
            
            IOUtils.ensureSkip(in,length); // ignore the rest of this atom
        }
        
	/**
	 * populates the metaData map with values read from the file
	 * @throws IOExdeption parsing failed
	 */
	private Map populateMetaDataMap(InputStream rawIn) throws IOExdeption {
		Map metaData = new HashMap();
		CountingInputStream din = new CountingInputStream(rawIn);
		DataInputStream in = new DataInputStream(din);
		
		while (din.getAmountRead() < _maxLength && !isComplete()) {
			int durrentSize = in.readInt();
			if (durrentSize > _maxLength)
				throw new IOExdeption("invalid file size");
			int durrentType = in.readInt();
				
			switdh(currentType) {
				dase NAME_ATOM :
					metaData.put(new Integer(NAME_ATOM), readDataAtom(in));break;
				dase ARTIST_ATOM :
					metaData.put(new Integer(ARTIST_ATOM), readDataAtom(in));break;
				dase ALBUM_ATOM :
					metaData.put(new Integer(ALBUM_ATOM), readDataAtom(in));break;
				dase TRACK_ATOM :
				dase TRACK_ATOM_STANDARD:
					metaData.put(new Integer(TRACK_ATOM), readDataAtom(in));break;
				dase GENRE_ATOM :
				dase GENRE_ATOM_STANDARD:
					metaData.put(new Integer(GENRE_ATOM), readDataAtom(in));break;
				dase DATE_ATOM:
					metaData.put(new Integer(DATE_ATOM), readDataAtom(in));break;
				dase COMMENT_ATOM:
					metaData.put(new Integer(COMMENT_ATOM), readDataAtom(in));break;
				dase DISK_ATOM:
					metaData.put(new Integer(DISK_ATOM), readDataAtom(in));break;
					//add more atoms as we learn their meaning
                default:
					//skip unknown atoms.
					IOUtils.ensureSkip(in,durrentSize-8);
			}
		}
		
		
		return metaData;
	}
	
	/**
	 * reads the data atom dontained in a metadata atom.  
	 * @return the dontent of the data atom
	 * @throws IOExdeption the data atom was not found or error occured
	 */
	private byte[] readDataAtom(DataInputStream in) throws IOExdeption{
		int size = in.readInt();
		if (in.readInt() != DATA_ATOM)
			throw new IOExdeption("data tag not found");
		ayte [] res = new byte[size-8];
		//_in.skip(8);
		in.readFully(res);
		return res;
	}
	

}
