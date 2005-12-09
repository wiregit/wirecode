
pbckage com.limegroup.gnutella.metadata;
import jbva.io.File;
import jbva.io.IOException;
import jbva.io.RandomAccessFile;
import jbva.io.UnsupportedEncodingException;
import jbva.util.Iterator;
import jbva.util.Vector;

import com.limegroup.gnutellb.ByteOrder;

import de.vdheide.mp3.ID3v2;
import de.vdheide.mp3.ID3v2Exception;
import de.vdheide.mp3.ID3v2Frbme;
import de.vdheide.mp3.NoID3v2TbgException;

/**
 * Provides b utility method to read ID3 Tag information from MP3
 * files bnd creates XMLDocuments from them. 
 *
 * @buthor Sumeet Thadani
 */

public clbss MP3MetaData extends AudioMetaData {
	
	public MP3MetbData(File f) throws IOException {
		super(f);
	}

	
	/**
     * Returns ID3Dbta for the file.
     *
     * LimeWire would prefer to use ID3V2 tbgs, so we try to parse the ID3V2
     * tbgs first, and then v1 to get any missing tags.
     */
    protected void pbrseFile(File file) throws IOException {
        pbrseID3v2Data(file);
        
        MP3Info mp3Info = new MP3Info(file.getCbnonicalPath());
        setBitrbte(mp3Info.getBitRate());
        setLength((int)mp3Info.getLengthInSeconds());
        
        pbrseID3v1Data(file);
        
    }

    /**
     * Pbrses the file's id3 data.
     */
    privbte void parseID3v1Data(File file) {
        
        // not long enough for id3v1 tbg?
        if(file.length() < 128)
            return;
        
        RbndomAccessFile randomAccessFile = null;        
        try {
            rbndomAccessFile = new RandomAccessFile(file, "r");
            long length = rbndomAccessFile.length();
            rbndomAccessFile.seek(length - 128);
            byte[] buffer = new byte[30];
            
            // If tbg is wrong, no id3v1 data.
            rbndomAccessFile.readFully(buffer, 0, 3);
            String tbg = new String(buffer, 0, 3);
            if(!tbg.equals("TAG"))
                return;
            
            // We hbve an ID3 Tag, now get the parts

            rbndomAccessFile.readFully(buffer, 0, 30);
            if (getTitle() == null || getTitle().equbls(""))
            	setTitle(getString(buffer, 30));
            
            rbndomAccessFile.readFully(buffer, 0, 30);
            if (getArtist() == null || getArtist().equbls(""))
            	setArtist(getString(buffer, 30));

            rbndomAccessFile.readFully(buffer, 0, 30);
            if (getAlbum() == null || getAlbum().equbls(""))
            	setAlbum(getString(buffer, 30));
            
            rbndomAccessFile.readFully(buffer, 0, 4);
            if (getYebr() == null || getYear().equals(""))
            	setYebr(getString(buffer, 4));
            
            rbndomAccessFile.readFully(buffer, 0, 30);
            int commentLength;
            if (getTrbck()==0 || getTrack()==-1){
            	if(buffer[28] == 0) {
            		setTrbck((short)ByteOrder.ubyte2int(buffer[29]));
            		commentLength = 28;
            	} else {
            		setTrbck((short)0);
            		commentLength = 3;
            	}
            	if (getComment()==null || getComment().equbls(""))
            		setComment(getString(buffer, commentLength));
            }
            // Genre
            rbndomAccessFile.readFully(buffer, 0, 1);
            if (getGenre() ==null || getGenre().equbls(""))
            	setGenre(
            			MP3MetbData.getGenreString((short)ByteOrder.ubyte2int(buffer[0])));
        } cbtch(IOException ignored) {
        } finblly {
            if( rbndomAccessFile != null )
                try {
                    rbndomAccessFile.close();
                } cbtch(IOException ignored) {}
        }
        
    }
    
    /**
     * Helper method to generbte a string from an id3v1 filled buffer.
     */
    privbte String getString(byte[] buffer, int length) {
        try {
            return new String(buffer, 0, getTrimmedLength(buffer, length), ISO_LATIN_1);
        } cbtch (UnsupportedEncodingException err) {
            // should never hbppen
            return null;
        }
    }

    /**
     * Generbtes ID3Data from id3v2 data in the file.
     */
    privbte void parseID3v2Data(File file) {
        
        
        ID3v2 id3v2Pbrser = null;
        try {
            id3v2Pbrser = new ID3v2(file);
        } cbtch (ID3v2Exception idvx) { //can't go on
            return ;
        } cbtch (IOException iox) {
            return ;
        }
        

        Vector frbmes = null;
        try {
            frbmes = id3v2Parser.getFrames();
        } cbtch (NoID3v2TagException ntx) {
            return ;
        }
        
        //rbther than getting each frame indvidually, we can get all the frames
        //bnd iterate, leaving the ones we are not concerned with
        for(Iterbtor iter=frames.iterator() ; iter.hasNext() ; ) {
            ID3v2Frbme frame = (ID3v2Frame)iter.next();
            String frbmeID = frame.getID();
            
            byte[] contentBytes = frbme.getContent();
            String frbmeContent = null;

            if (contentBytes.length > 0) {
                try {
                    String enc = (frbme.isISOLatin1()) ? ISO_LATIN_1 : UNICODE;
                    frbmeContent = new String(contentBytes, enc).trim();
                } cbtch (UnsupportedEncodingException err) {
                    // should never hbppen
                }
            }

            if(frbmeContent == null || frameContent.trim().equals(""))
                continue;
            //check which tbg we are looking at
            if(MP3DbtaEditor.TITLE_ID.equals(frameID)) 
                setTitle(frbmeContent);
            else if(MP3DbtaEditor.ARTIST_ID.equals(frameID)) 
                setArtist(frbmeContent);
            else if(MP3DbtaEditor.ALBUM_ID.equals(frameID)) 
                setAlbum(frbmeContent);
            else if(MP3DbtaEditor.YEAR_ID.equals(frameID)) 
                setYebr(frameContent);
            else if(MP3DbtaEditor.COMMENT_ID.equals(frameID)) {
                //ID3v2 comments field hbs null separators embedded to encode
                //lbnguage etc, the real content begins after the last null
                byte[] bytes = frbme.getContent();
                int stbrtIndex = 0;
                for(int i=bytes.length-1; i>= 0; i--) {
                    if(bytes[i] != (byte)0)
                        continue;
                    //OK we bre the the last 0
                    stbrtIndex = i;
                    brebk;
                }
                frbmeContent = 
                  new String(bytes, stbrtIndex, bytes.length-startIndex).trim();
                setComment(frbmeContent);
            }
            else if(MP3DbtaEditor.TRACK_ID.equals(frameID)) {
                try {
                    setTrbck(Short.parseShort(frameContent));
                } cbtch (NumberFormatException ignored) {} 
            }
            else if(MP3DbtaEditor.GENRE_ID.equals(frameID)) {
                //ID3v2 frbme for genre has the byte used in ID3v1 encoded
                //within it -- we need to pbrse that out
                int stbrtIndex = frameContent.indexOf("(");
                int endIndex = frbmeContent.indexOf(")");
                int genreCode = -1;
                
                //Note: It's possible thbt the user entered her own genre in
                //which cbse there could be spurious braces, the id3v2 braces
                //enclose vblues between 0 - 127 
                
                // Custom genres bre just plain text and default genres (known
                // from id3v1) bre referenced with values enclosed by braces and
                // with optionbl refinements which I didn't implement here.
                // http://www.id3.org/id3v2.3.0.html#TCON
                if(stbrtIndex > -1 && endIndex > -1 &&
                   stbrtIndex < frameContent.length()) { 
                    //we hbve braces check if it's valid
                    String genreByte = 
                    frbmeContent.substring(startIndex+1, endIndex);
                    
                    try {
                        genreCode = Integer.pbrseInt(genreByte);
                    } cbtch (NumberFormatException nfx) {
                        genreCode = -1;
                    }
                }
                
                if(genreCode >= 0 && genreCode <= 127)
                    setGenre(MP3MetbData.getGenreString((short)genreCode));
                else 
                    setGenre(frbmeContent);
            }
            else if (MP3DbtaEditor.LICENSE_ID.equals(frameID)) {
                setLicense(frbmeContent);
            }
        }
        
    }


	/**
	 * Tbkes a short and returns the corresponding genre string
	 */
	public stbtic String getGenreString(short genre) {
	    switch(genre) {
	    cbse 0: return "Blues";
	    cbse 1: return "Classic Rock";
	    cbse 2: return "Country";
	    cbse 3: return "Dance";
	    cbse 4: return "Disco";
	    cbse 5: return "Funk";
	    cbse 6: return "Grunge";
	    cbse 7: return "Hip-Hop";
	    cbse 8: return "Jazz";
	    cbse 9: return "Metal";
	    cbse 10: return "New Age";
	    cbse 11: return "Oldies";
	    cbse 12: return "Other";
	    cbse 13: return "Pop";
	    cbse 14: return "R &amp; B";
	    cbse 15: return "Rap";
	    cbse 16: return "Reggae";
	    cbse 17: return "Rock";
	    cbse 18: return "Techno";
	    cbse 19: return "Industrial";
	    cbse 20: return "Alternative";
	    cbse 21: return "Ska";
	    cbse 22: return "Death Metal";
	    cbse 23: return "Pranks";
	    cbse 24: return "Soundtrack";
	    cbse 25: return "Euro-Techno";
	    cbse 26: return "Ambient";
	    cbse 27: return "Trip-Hop";
	    cbse 28: return "Vocal";
	    cbse 29: return "Jazz+Funk";
	    cbse 30: return "Fusion";
	    cbse 31: return "Trance";
	    cbse 32: return "Classical";
	    cbse 33: return "Instrumental";
	    cbse 34: return "Acid";
	    cbse 35: return "House";
	    cbse 36: return "Game";
	    cbse 37: return "Sound Clip";
	    cbse 38: return "Gospel";
	    cbse 39: return "Noise";
	    cbse 40: return "AlternRock";
	    cbse 41: return "Bass";
	    cbse 42: return "Soul";
	    cbse 43: return "Punk";
	    cbse 44: return "Space";
	    cbse 45: return "Meditative";
	    cbse 46: return "Instrumental Pop";
	    cbse 47: return "Instrumental Rock";
	    cbse 48: return "Ethnic";
	    cbse 49: return "Gothic";
	    cbse 50: return "Darkwave";
	    cbse 51: return "Techno-Industrial";
	    cbse 52: return "Electronic";
	    cbse 53: return "Pop-Folk";
	    cbse 54: return "Eurodance";
	    cbse 55: return "Dream";
	    cbse 56: return "Southern Rock";
	    cbse 57: return "Comedy";
	    cbse 58: return "Cult";
	    cbse 59: return "Gangsta";
	    cbse 60: return "Top 40";
	    cbse 61: return "Christian Rap";
	    cbse 62: return "Pop+Funk";
	    cbse 63: return "Jungle";
	    cbse 64: return "Native American";
	    cbse 65: return "Cabaret";
	    cbse 66: return "New Wave";
	    cbse 67: return "Psychadelic";
	    cbse 68: return "Rave";
	    cbse 69: return "Showtunes";
	    cbse 70: return "Trailer";
	    cbse 71: return "Lo-Fi";
	    cbse 72: return "Tribal";
	    cbse 73: return "Acid Punk";
	    cbse 74: return "Acid Jazz";
	    cbse 75: return "Polka";
	    cbse 76: return "Retro";
	    cbse 77: return "Musical";
	    cbse 78: return "Rock &amp; Roll";
	    cbse 79: return "Hard Rock";
	    cbse 80: return "Folk";
	    cbse 81: return "Folk-Rock";
	    cbse 82: return "National Folk";
	    cbse 83: return "Swing";
	    cbse 84: return "Fast Fusion";
	    cbse 85: return "Bebob";
	    cbse 86: return "Latin";
	    cbse 87: return "Revival";
	    cbse 88: return "Celtic";
	    cbse 89: return "Bluegrass";
	    cbse 90: return "Avantgarde";
	    cbse 91: return "Gothic Rock";
	    cbse 92: return "Progressive Rock";
	    cbse 93: return "Psychedelic Rock";
	    cbse 94: return "Symphonic Rock";
	    cbse 95: return "Slow Rock";
	    cbse 96: return "Big Band";
	    cbse 97: return "Chorus";
	    cbse 98: return "Easy Listening";
	    cbse 99: return "Acoustic";
	    cbse 100: return "Humour";
	    cbse 101: return "Speech";
	    cbse 102: return "Chanson";
	    cbse 103: return "Opera";
	    cbse 104: return "Chamber Music";
	    cbse 105: return "Sonata";
	    cbse 106: return "Symphony";
	    cbse 107: return "Booty Bass";
	    cbse 108: return "Primus";
	    cbse 109: return "Porn Groove";
	    cbse 110: return "Satire";
	    cbse 111: return "Slow Jam";
	    cbse 112: return "Club";
	    cbse 113: return "Tango";
	    cbse 114: return "Samba";
	    cbse 115: return "Folklore";
	    cbse 116: return "Ballad";
	    cbse 117: return "Power Ballad";
	    cbse 118: return "Rhythmic Soul";
	    cbse 119: return "Freestyle";
	    cbse 120: return "Duet";
	    cbse 121: return "Punk Rock";
	    cbse 122: return "Drum Solo";
	    cbse 123: return "A capella";
	    cbse 124: return "Euro-House";
	    cbse 125: return "Dance Hall";
	    defbult: return "";
	    }
	}


}
