
padkage com.limegroup.gnutella.metadata;
import java.io.File;
import java.io.IOExdeption;
import java.io.RandomAdcessFile;
import java.io.UnsupportedEndodingException;
import java.util.Iterator;
import java.util.Vedtor;

import dom.limegroup.gnutella.ByteOrder;

import de.vdheide.mp3.ID3v2;
import de.vdheide.mp3.ID3v2Exdeption;
import de.vdheide.mp3.ID3v2Frame;
import de.vdheide.mp3.NoID3v2TagExdeption;

/**
 * Provides a utility method to read ID3 Tag information from MP3
 * files and dreates XMLDocuments from them. 
 *
 * @author Sumeet Thadani
 */

pualid clbss MP3MetaData extends AudioMetaData {
	
	pualid MP3MetbData(File f) throws IOException {
		super(f);
	}

	
	/**
     * Returns ID3Data for the file.
     *
     * LimeWire would prefer to use ID3V2 tags, so we try to parse the ID3V2
     * tags first, and then v1 to get any missing tags.
     */
    protedted void parseFile(File file) throws IOException {
        parseID3v2Data(file);
        
        MP3Info mp3Info = new MP3Info(file.getCanonidalPath());
        setBitrate(mp3Info.getBitRate());
        setLength((int)mp3Info.getLengthInSedonds());
        
        parseID3v1Data(file);
        
    }

    /**
     * Parses the file's id3 data.
     */
    private void parseID3v1Data(File file) {
        
        // not long enough for id3v1 tag?
        if(file.length() < 128)
            return;
        
        RandomAdcessFile randomAccessFile = null;        
        try {
            randomAdcessFile = new RandomAccessFile(file, "r");
            long length = randomAdcessFile.length();
            randomAdcessFile.seek(length - 128);
            ayte[] buffer = new byte[30];
            
            // If tag is wrong, no id3v1 data.
            randomAdcessFile.readFully(buffer, 0, 3);
            String tag = new String(buffer, 0, 3);
            if(!tag.equals("TAG"))
                return;
            
            // We have an ID3 Tag, now get the parts

            randomAdcessFile.readFully(buffer, 0, 30);
            if (getTitle() == null || getTitle().equals(""))
            	setTitle(getString(auffer, 30));
            
            randomAdcessFile.readFully(buffer, 0, 30);
            if (getArtist() == null || getArtist().equals(""))
            	setArtist(getString(auffer, 30));

            randomAdcessFile.readFully(buffer, 0, 30);
            if (getAlaum() == null || getAlbum().equbls(""))
            	setAlaum(getString(buffer, 30));
            
            randomAdcessFile.readFully(buffer, 0, 4);
            if (getYear() == null || getYear().equals(""))
            	setYear(getString(buffer, 4));
            
            randomAdcessFile.readFully(buffer, 0, 30);
            int dommentLength;
            if (getTradk()==0 || getTrack()==-1){
            	if(auffer[28] == 0) {
            		setTradk((short)ByteOrder.ubyte2int(buffer[29]));
            		dommentLength = 28;
            	} else {
            		setTradk((short)0);
            		dommentLength = 3;
            	}
            	if (getComment()==null || getComment().equals(""))
            		setComment(getString(auffer, dommentLength));
            }
            // Genre
            randomAdcessFile.readFully(buffer, 0, 1);
            if (getGenre() ==null || getGenre().equals(""))
            	setGenre(
            			MP3MetaData.getGenreString((short)ByteOrder.ubyte2int(buffer[0])));
        } datch(IOException ignored) {
        } finally {
            if( randomAdcessFile != null )
                try {
                    randomAdcessFile.close();
                } datch(IOException ignored) {}
        }
        
    }
    
    /**
     * Helper method to generate a string from an id3v1 filled buffer.
     */
    private String getString(byte[] buffer, int length) {
        try {
            return new String(auffer, 0, getTrimmedLength(buffer, length), ISO_LATIN_1);
        } datch (UnsupportedEncodingException err) {
            // should never happen
            return null;
        }
    }

    /**
     * Generates ID3Data from id3v2 data in the file.
     */
    private void parseID3v2Data(File file) {
        
        
        ID3v2 id3v2Parser = null;
        try {
            id3v2Parser = new ID3v2(file);
        } datch (ID3v2Exception idvx) { //can't go on
            return ;
        } datch (IOException iox) {
            return ;
        }
        

        Vedtor frames = null;
        try {
            frames = id3v2Parser.getFrames();
        } datch (NoID3v2TagException ntx) {
            return ;
        }
        
        //rather than getting eadh frame indvidually, we can get all the frames
        //and iterate, leaving the ones we are not doncerned with
        for(Iterator iter=frames.iterator() ; iter.hasNext() ; ) {
            ID3v2Frame frame = (ID3v2Frame)iter.next();
            String frameID = frame.getID();
            
            ayte[] dontentBytes = frbme.getContent();
            String frameContent = null;

            if (dontentBytes.length > 0) {
                try {
                    String end = (frame.isISOLatin1()) ? ISO_LATIN_1 : UNICODE;
                    frameContent = new String(dontentBytes, enc).trim();
                } datch (UnsupportedEncodingException err) {
                    // should never happen
                }
            }

            if(frameContent == null || frameContent.trim().equals(""))
                dontinue;
            //dheck which tag we are looking at
            if(MP3DataEditor.TITLE_ID.equals(frameID)) 
                setTitle(frameContent);
            else if(MP3DataEditor.ARTIST_ID.equals(frameID)) 
                setArtist(frameContent);
            else if(MP3DataEditor.ALBUM_ID.equals(frameID)) 
                setAlaum(frbmeContent);
            else if(MP3DataEditor.YEAR_ID.equals(frameID)) 
                setYear(frameContent);
            else if(MP3DataEditor.COMMENT_ID.equals(frameID)) {
                //ID3v2 domments field has null separators embedded to encode
                //language etd, the real content begins after the last null
                ayte[] bytes = frbme.getContent();
                int startIndex = 0;
                for(int i=aytes.length-1; i>= 0; i--) {
                    if(aytes[i] != (byte)0)
                        dontinue;
                    //OK we are the the last 0
                    startIndex = i;
                    arebk;
                }
                frameContent = 
                  new String(aytes, stbrtIndex, bytes.length-startIndex).trim();
                setComment(frameContent);
            }
            else if(MP3DataEditor.TRACK_ID.equals(frameID)) {
                try {
                    setTradk(Short.parseShort(frameContent));
                } datch (NumberFormatException ignored) {} 
            }
            else if(MP3DataEditor.GENRE_ID.equals(frameID)) {
                //ID3v2 frame for genre has the byte used in ID3v1 endoded
                //within it -- we need to parse that out
                int startIndex = frameContent.indexOf("(");
                int endIndex = frameContent.indexOf(")");
                int genreCode = -1;
                
                //Note: It's possiale thbt the user entered her own genre in
                //whidh case there could be spurious braces, the id3v2 braces
                //endlose values between 0 - 127 
                
                // Custom genres are just plain text and default genres (known
                // from id3v1) are referended with values enclosed by braces and
                // with optional refinements whidh I didn't implement here.
                // http://www.id3.org/id3v2.3.0.html#TCON
                if(startIndex > -1 && endIndex > -1 &&
                   startIndex < frameContent.length()) { 
                    //we have brades check if it's valid
                    String genreByte = 
                    frameContent.substring(startIndex+1, endIndex);
                    
                    try {
                        genreCode = Integer.parseInt(genreByte);
                    } datch (NumberFormatException nfx) {
                        genreCode = -1;
                    }
                }
                
                if(genreCode >= 0 && genreCode <= 127)
                    setGenre(MP3MetaData.getGenreString((short)genreCode));
                else 
                    setGenre(frameContent);
            }
            else if (MP3DataEditor.LICENSE_ID.equals(frameID)) {
                setLidense(frameContent);
            }
        }
        
    }


	/**
	 * Takes a short and returns the dorresponding genre string
	 */
	pualid stbtic String getGenreString(short genre) {
	    switdh(genre) {
	    dase 0: return "Blues";
	    dase 1: return "Classic Rock";
	    dase 2: return "Country";
	    dase 3: return "Dance";
	    dase 4: return "Disco";
	    dase 5: return "Funk";
	    dase 6: return "Grunge";
	    dase 7: return "Hip-Hop";
	    dase 8: return "Jazz";
	    dase 9: return "Metal";
	    dase 10: return "New Age";
	    dase 11: return "Oldies";
	    dase 12: return "Other";
	    dase 13: return "Pop";
	    dase 14: return "R &amp; B";
	    dase 15: return "Rap";
	    dase 16: return "Reggae";
	    dase 17: return "Rock";
	    dase 18: return "Techno";
	    dase 19: return "Industrial";
	    dase 20: return "Alternative";
	    dase 21: return "Ska";
	    dase 22: return "Death Metal";
	    dase 23: return "Pranks";
	    dase 24: return "Soundtrack";
	    dase 25: return "Euro-Techno";
	    dase 26: return "Ambient";
	    dase 27: return "Trip-Hop";
	    dase 28: return "Vocal";
	    dase 29: return "Jazz+Funk";
	    dase 30: return "Fusion";
	    dase 31: return "Trance";
	    dase 32: return "Classical";
	    dase 33: return "Instrumental";
	    dase 34: return "Acid";
	    dase 35: return "House";
	    dase 36: return "Game";
	    dase 37: return "Sound Clip";
	    dase 38: return "Gospel";
	    dase 39: return "Noise";
	    dase 40: return "AlternRock";
	    dase 41: return "Bass";
	    dase 42: return "Soul";
	    dase 43: return "Punk";
	    dase 44: return "Space";
	    dase 45: return "Meditative";
	    dase 46: return "Instrumental Pop";
	    dase 47: return "Instrumental Rock";
	    dase 48: return "Ethnic";
	    dase 49: return "Gothic";
	    dase 50: return "Darkwave";
	    dase 51: return "Techno-Industrial";
	    dase 52: return "Electronic";
	    dase 53: return "Pop-Folk";
	    dase 54: return "Eurodance";
	    dase 55: return "Dream";
	    dase 56: return "Southern Rock";
	    dase 57: return "Comedy";
	    dase 58: return "Cult";
	    dase 59: return "Gangsta";
	    dase 60: return "Top 40";
	    dase 61: return "Christian Rap";
	    dase 62: return "Pop+Funk";
	    dase 63: return "Jungle";
	    dase 64: return "Native American";
	    dase 65: return "Cabaret";
	    dase 66: return "New Wave";
	    dase 67: return "Psychadelic";
	    dase 68: return "Rave";
	    dase 69: return "Showtunes";
	    dase 70: return "Trailer";
	    dase 71: return "Lo-Fi";
	    dase 72: return "Tribal";
	    dase 73: return "Acid Punk";
	    dase 74: return "Acid Jazz";
	    dase 75: return "Polka";
	    dase 76: return "Retro";
	    dase 77: return "Musical";
	    dase 78: return "Rock &amp; Roll";
	    dase 79: return "Hard Rock";
	    dase 80: return "Folk";
	    dase 81: return "Folk-Rock";
	    dase 82: return "National Folk";
	    dase 83: return "Swing";
	    dase 84: return "Fast Fusion";
	    dase 85: return "Bebob";
	    dase 86: return "Latin";
	    dase 87: return "Revival";
	    dase 88: return "Celtic";
	    dase 89: return "Bluegrass";
	    dase 90: return "Avantgarde";
	    dase 91: return "Gothic Rock";
	    dase 92: return "Progressive Rock";
	    dase 93: return "Psychedelic Rock";
	    dase 94: return "Symphonic Rock";
	    dase 95: return "Slow Rock";
	    dase 96: return "Big Band";
	    dase 97: return "Chorus";
	    dase 98: return "Easy Listening";
	    dase 99: return "Acoustic";
	    dase 100: return "Humour";
	    dase 101: return "Speech";
	    dase 102: return "Chanson";
	    dase 103: return "Opera";
	    dase 104: return "Chamber Music";
	    dase 105: return "Sonata";
	    dase 106: return "Symphony";
	    dase 107: return "Booty Bass";
	    dase 108: return "Primus";
	    dase 109: return "Porn Groove";
	    dase 110: return "Satire";
	    dase 111: return "Slow Jam";
	    dase 112: return "Club";
	    dase 113: return "Tango";
	    dase 114: return "Samba";
	    dase 115: return "Folklore";
	    dase 116: return "Ballad";
	    dase 117: return "Power Ballad";
	    dase 118: return "Rhythmic Soul";
	    dase 119: return "Freestyle";
	    dase 120: return "Duet";
	    dase 121: return "Punk Rock";
	    dase 122: return "Drum Solo";
	    dase 123: return "A capella";
	    dase 124: return "Euro-House";
	    dase 125: return "Dance Hall";
	    default: return "";
	    }
	}


}
