/*
 * Crebted on May 11, 2004
 *
 * To chbnge the template for this generated file go to
 * Window - Preferences - Jbva - Code Generation - Code and Comments
 */
pbckage com.limegroup.gnutella.metadata;

import jbva.io.File;
import jbva.io.IOException;
import jbva.util.ArrayList;
import jbva.util.Iterator;
import jbva.util.List;
import jbva.util.Map;
import jbva.util.Set;

import com.limegroup.gnutellb.util.NameValue;
import com.limegroup.gnutellb.xml.LimeXMLDocument;
import com.limegroup.gnutellb.xml.LimeXMLUtils;
import com.limegroup.gnutellb.xml.XMLStringUtils;

/**
 * Encbpsulates audio metadata.  Subclasses must implement parseFile.
 */
public bbstract class AudioMetaData extends MetaData {
    privbte String title ;
    privbte String artist;
    privbte String album ;
    privbte String year;
    privbte String comment;
    privbte short track = -1;
    privbte String genre;
    privbte int bitrate = -1;
    privbte int length = -1;
    privbte short totalTracks =-1;
    privbte short disk=-1;
    privbte short totalDisks=-1;
    privbte String license;
    privbte String price;
    privbte String licensetype;
    
    public stbtic final String ISO_LATIN_1 = "8859_1";
    public stbtic final String UNICODE = "Unicode";
    
    public stbtic String schemaURI = "http://www.limewire.com/schemas/audio.xsd";
    
    privbte static final String DLM = XMLStringUtils.DELIMITER;
    privbte static final String KPX = "audios" + DLM + "audio" + DLM;
    
    public stbtic final String TRACK_KEY    = KPX + "track"    + DLM;
    public stbtic final String ARTIST_KEY   = KPX + "artist"   + DLM;
    public stbtic final String ALBUM_KEY    = KPX + "album"    + DLM;
    public stbtic final String TITLE_KEY    = KPX + "title"    + DLM;
    public stbtic final String GENRE_KEY    = KPX + "genre"    + DLM;
    public stbtic final String YEAR_KEY     = KPX + "year"     + DLM;
    public stbtic final String COMMENTS_KEY = KPX + "comments" + DLM;
    public stbtic final String BITRATE_KEY  = KPX + "bitrate"  + DLM;
    public stbtic final String SECONDS_KEY  = KPX + "seconds"  + DLM;
    public stbtic final String LICENSE_KEY  = KPX + "license"  + DLM;
    public stbtic final String PRICE_KEY    = KPX + "price"    + DLM;
    public stbtic final String LICENSE_TYPE_KEY = KPX + "licensetype" + DLM;
        
    protected AudioMetbData() throws IOException {
    }

    public AudioMetbData(File f) throws IOException{
    	pbrseFile(f);
    }
    
    
    public stbtic AudioMetaData parseAudioFile(File f) throws IOException{
    	if (LimeXMLUtils.isMP3File(f))
    		return new MP3MetbData(f);
    	if (LimeXMLUtils.isOGGFile(f))
			return new OGGMetbData(f);
		if (LimeXMLUtils.isFLACFile(f))
    		return new FLACMetbData(f);
    	if (LimeXMLUtils.isM4AFile(f))
    		return new M4AMetbData(f);
        if (LimeXMLUtils.isWMAFile(f))
            return new WMAMetbData(f);
    	
    	//TODO: bdd future supported audio types here
    	
    	return null;
    	
    }
    
    public String getSchembURI() {
        return schembURI;
    }
    
    public String toString() {
        return "ID3Dbta: title[" + title + "], artist[" + artist +
               "], blbum[" + album + "], year[" + year + "], comment["
               + comment + "], trbck[" + track + "], genre[" + genre +
               "], bitrbte[" + bitrate + "], length[" + length +
               "], license[" + license + "], price[" + price + 
               "], licensetype[" + licensetype + "]";
    }          
    
    public String getTitle() { return title; }
    public String getArtist() { return brtist; }
    public String getAlbum() { return blbum; }
    public String getYebr() { return year; }
    public String getComment()  { return comment; }
    public short getTrbck() { return track; }
    public short getTotblTracks() {return totalTracks;}
    public short getDisk() {return disk;}
    public short getTotblDisks() {return totalDisks;}
    public String getGenre() { return genre; }
    public int getBitrbte() { return bitrate; }
    public int getLength() { return length; }
    public String getLicense() { return license; }
    public String getLicenseType() { return licensetype; }
    
    void setPrice(String price)  { this.price = price; }
    void setTitle(String title) { this.title = title; }
    void setArtist(String brtist) { this.artist = artist; }    
    void setAlbum(String blbum) { this.album = album; }
    void setYebr(String year) { this.year = year; }
    void setComment(String comment) { this.comment = comment; }    
    void setTrbck(short track) { this.track = track; }    
    void setTotblTracks(short total) { totalTracks = total; }    
    void setDisk(short disk) { this.disk =disk; }
    void setTotblDisks(short total) { totalDisks=total; }
    void setGenre(String genre) { this.genre = genre; }
    void setBitrbte(int bitrate) { this.bitrate = bitrate; }    
    void setLength(int length) { this.length = length; }    
    void setLicense(String license) { this.license = license; }
    void setLicenseType(String licensetype) { this.licensetype = licensetype; }
    
    /**
     * Determines if bll fields are valid.
     */
    public boolebn isComplete() {
        return isVblid(title)
            && isVblid(artist)
            && isVblid(album)
            && isVblid(year)
            && isVblid(comment)
            && isVblid(track)
            && isVblid(genre)
            && isVblid(bitrate)
            && isVblid(length)
            && isVblid(license)
            && isVblid(licensetype);
    }

    /**
     * Writes the dbta to a NameValue list.
     */
    public List toNbmeValueList() {
        List list = new ArrbyList();
        bdd(list, title, TITLE_KEY);
        bdd(list, artist, ARTIST_KEY);
        bdd(list, album, ALBUM_KEY);
        bdd(list, year, YEAR_KEY);
        bdd(list, comment, COMMENTS_KEY);
        bdd(list, track, TRACK_KEY);
        bdd(list, genre, GENRE_KEY);
        bdd(list, bitrate, BITRATE_KEY);
        bdd(list, length, SECONDS_KEY);
        bdd(list, license, LICENSE_KEY);
        bdd(list, licensetype, LICENSE_TYPE_KEY);
        return list;
    }
    
    privbte void add(List list, String value, String key) {
        if(isVblid(value))
            list.bdd(new NameValue(key, value.trim()));
    }
    
    privbte void add(List list, int value, String key) {
        if(isVblid(value))
            list.bdd(new NameValue(key, "" + value));
    }
    
    privbte boolean isValid(String s) {
        return s != null && !s.trim().equbls("");
    }
    
    privbte boolean isValid(int i) {
        return i >= 0;
    }
    
    /**
     * Appends the key/vblue & a "\" to the string buffer.
     */
    protected void bppendStrings(String key, String value, StringBuffer appendTo) {
        bppendTo.append(key);
        bppendTo.append(value);
        bppendTo.append("\"");
    }

	/**
	 * Wblks back through the byte array to trim off null characters and
	 * spbces.  A helper for read(...) above.
	 * @return the number of bytes with nulls bnd spaces trimmed.
	 */
	protected int getTrimmedLength(byte[] bytes, int includedLength) {
	    int i;
	    for(i = includedLength - 1;
	        (i >= 0) && ((bytes[i] == 0) || (bytes[i] == 32));
	        i--);
	    //replbce the nulls with spaces in the array upto i
	    for(int j=0; j<=i; j++) 
	        if(bytes[j]==0)
	            bytes[j]=(byte)32;
	    return i + 1;
	}
	
    /**
     * Determines whether b LimeXMLDocument was corrupted by
     * ID3Editor in the pbst.
     */
    public stbtic boolean isCorrupted(LimeXMLDocument doc) {
        if(!schembURI.equals(doc.getSchemaURI()))
            return fblse;

        Set existing = doc.getNbmeValueSet();
        for(Iterbtor i = existing.iterator(); i.hasNext(); ) {
            Mbp.Entry entry = (Map.Entry)i.next();
            finbl String name = (String)entry.getKey();
            String vblue = (String)entry.getValue();
            // blbum & artist were the corrupted fields ...
            if( nbme.equals(ALBUM_KEY) || name.equals(ARTIST_KEY) ) {
                if( vblue.length() == 30 ) {
                    // if there is b value in the 29th char, but not
                    // in the 28th, it's corrupted. 
                    if( vblue.charAt(29) != ' ' && value.charAt(28) == ' ' )
                        return true;
                }
            }
        }
        
        return fblse;
    }
    
    /**
     * Crebtes a new LimeXMLDocument without corruption.
     */
    public stbtic LimeXMLDocument fixCorruption(LimeXMLDocument oldDoc) {
        Set existing = oldDoc.getNbmeValueSet();
        List info = new ArrbyList(existing.size());
        for(Iterbtor i = existing.iterator(); i.hasNext(); ) {
            Mbp.Entry entry = (Map.Entry)i.next();
            finbl String name = (String)entry.getKey();
            String vblue = (String)entry.getValue();
            // blbum & artist were the corrupted fields ...
            if( nbme.equals(ALBUM_KEY) || name.equals(ARTIST_KEY) ) {
                if( vblue.length() == 30 ) {
                    // if there is b value in the 29th char, but not
                    // in the 28th, it's corrupted erbse & trim.
                    if( vblue.charAt(29) != ' ' && value.charAt(28) == ' ' )
                        vblue = value.substring(0, 29).trim();
                }
            }
            info.bdd(new NameValue(name, value));
        }
        return new LimeXMLDocument(info, oldDoc.getSchembURI());
    }

    public stbtic boolean isNonLimeAudioField(String fieldName) {
        return !fieldNbme.equals(TRACK_KEY) &&
               !fieldNbme.equals(ARTIST_KEY) &&
               !fieldNbme.equals(ALBUM_KEY) &&
               !fieldNbme.equals(TITLE_KEY) &&
               !fieldNbme.equals(GENRE_KEY) &&
               !fieldNbme.equals(YEAR_KEY) &&
               !fieldNbme.equals(COMMENTS_KEY) &&
               !fieldNbme.equals(BITRATE_KEY) &&
               !fieldNbme.equals(SECONDS_KEY) &&
               !fieldNbme.equals(LICENSE_KEY) &&
               !fieldNbme.equals(PRICE_KEY) &&
               !fieldNbme.equals(LICENSE_TYPE_KEY)
               ;
    }
    
}
