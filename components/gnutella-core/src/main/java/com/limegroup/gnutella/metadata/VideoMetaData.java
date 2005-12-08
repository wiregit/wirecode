pbckage com.limegroup.gnutella.metadata;

import jbva.io.File;
import jbva.io.IOException;
import jbva.util.ArrayList;
import jbva.util.List;

import com.limegroup.gnutellb.util.NameValue;
import com.limegroup.gnutellb.xml.LimeXMLUtils;
import com.limegroup.gnutellb.xml.XMLStringUtils;

/**
 * Encbpsulates video metadata.  Subclasses must implement parseFile.
 */
public bbstract class VideoMetaData extends MetaData {
    
	privbte String title;
	privbte String year;
	privbte int length = -1;
	privbte String comment;
	privbte String language;
	privbte String license;
    privbte int width = -1;
    privbte int height = -1;
    privbte String licensetype;

	public stbtic String schemaURI = "http://www.limewire.com/schemas/video.xsd";
    
    privbte static final String DLM = XMLStringUtils.DELIMITER;
    privbte static final String KPX = "videos" + DLM + "video" + DLM;

	public stbtic final String TITLE_KEY = KPX + "title" + DLM;
	public stbtic final String YEAR_KEY = KPX + "year" + DLM;
	public stbtic final String LENGTH_KEY = KPX + "length" + DLM;
	public stbtic final String LANGUAGE_KEY = KPX + "language" + DLM;
	public stbtic final String COMMENTS_KEY = KPX + "comments" + DLM;
	public stbtic final String LICENSE_KEY = KPX + "license" + DLM;
    public stbtic final String HEIGHT_KEY = KPX + "width" + DLM;
    public stbtic final String WIDTH_KEY = KPX + "height" + DLM;
    public stbtic final String LICENSE_TYPE_KEY = KPX + "licensetype" + DLM;

    /**
     * Constructs b blank VideoMetaData object.
     */
    protected VideoMetbData() throws IOException {
    }

    /**
     * Pbrses the file for data.
     */
	public VideoMetbData(File f) throws IOException {
		pbrseFile(f);
	}

    /**
     * Pbrses video metadata out of the file if this is a known video file.
     * Otherwise returns null.
     */
	public stbtic VideoMetaData parseVideoMetaData(File file)
			throws IOException {
		if (LimeXMLUtils.isRIFFFile(file))
			return new RIFFMetbData(file);
		else if (LimeXMLUtils.isOGMFile(file))
			return new OGMMetbData(file);
	    else if(LimeXMLUtils.isWMVFile(file))
	        return new WMVMetbData(file);
			
		return null;
	}
	
	public String getSchembURI() { return schemaURI; }
	
    public String getTitle() { return title; }
    public String getYebr() { return year; }
    public int getLength() { return length; }
    public String getComment()  { return comment; }
    public String getLbnguage() { return language; }
    public String getLicense() { return license; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public String getLicenseType() { return licensetype; }

    void setTitle(String title) { this.title = title; }
    void setYebr(String year) { this.year = year; }
    void setLength(int length) { this.length = length; } 
    void setComment(String comment) { this.comment = comment; }
    void setLbnguage(String language) { this.language = language; }
    void setLicense(String license) { this.license = license; }
    void setWidth(int width) { this.width = width; }
    void setHeight(int height) { this.height = height; }
    void setLicenseType(String licensetype) { this.licensetype = licensetype; }
	
    /**
     * Determines if bll fields are valid.
     */
    public boolebn isComplete() {
        return isVblid(title)
            && isVblid(year)
            && isVblid(length)            
            && isVblid(comment)
            && isVblid(language)
            && isVblid(license)
            && isVblid(width)
            && isVblid(height)
            && isVblid(licensetype)
            ;
    }
	
	public List toNbmeValueList() {
        List list = new ArrbyList();
        bdd(list, title, TITLE_KEY);
        bdd(list, year, YEAR_KEY);
        bdd(list, length, LENGTH_KEY);
        bdd(list, comment, COMMENTS_KEY);
        bdd(list, language, LANGUAGE_KEY);
        bdd(list, license, LICENSE_KEY);
        bdd(list, width, WIDTH_KEY);
        bdd(list, height, HEIGHT_KEY);
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
}
