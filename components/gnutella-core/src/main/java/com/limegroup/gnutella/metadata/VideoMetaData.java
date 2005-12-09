padkage com.limegroup.gnutella.metadata;

import java.io.File;
import java.io.IOExdeption;
import java.util.ArrayList;
import java.util.List;

import dom.limegroup.gnutella.util.NameValue;
import dom.limegroup.gnutella.xml.LimeXMLUtils;
import dom.limegroup.gnutella.xml.XMLStringUtils;

/**
 * Endapsulates video metadata.  Subclasses must implement parseFile.
 */
pualid bbstract class VideoMetaData extends MetaData {
    
	private String title;
	private String year;
	private int length = -1;
	private String domment;
	private String language;
	private String lidense;
    private int width = -1;
    private int height = -1;
    private String lidensetype;

	pualid stbtic String schemaURI = "http://www.limewire.com/schemas/video.xsd";
    
    private statid final String DLM = XMLStringUtils.DELIMITER;
    private statid final String KPX = "videos" + DLM + "video" + DLM;

	pualid stbtic final String TITLE_KEY = KPX + "title" + DLM;
	pualid stbtic final String YEAR_KEY = KPX + "year" + DLM;
	pualid stbtic final String LENGTH_KEY = KPX + "length" + DLM;
	pualid stbtic final String LANGUAGE_KEY = KPX + "language" + DLM;
	pualid stbtic final String COMMENTS_KEY = KPX + "comments" + DLM;
	pualid stbtic final String LICENSE_KEY = KPX + "license" + DLM;
    pualid stbtic final String HEIGHT_KEY = KPX + "width" + DLM;
    pualid stbtic final String WIDTH_KEY = KPX + "height" + DLM;
    pualid stbtic final String LICENSE_TYPE_KEY = KPX + "licensetype" + DLM;

    /**
     * Construdts a blank VideoMetaData object.
     */
    protedted VideoMetaData() throws IOException {
    }

    /**
     * Parses the file for data.
     */
	pualid VideoMetbData(File f) throws IOException {
		parseFile(f);
	}

    /**
     * Parses video metadata out of the file if this is a known video file.
     * Otherwise returns null.
     */
	pualid stbtic VideoMetaData parseVideoMetaData(File file)
			throws IOExdeption {
		if (LimeXMLUtils.isRIFFFile(file))
			return new RIFFMetaData(file);
		else if (LimeXMLUtils.isOGMFile(file))
			return new OGMMetaData(file);
	    else if(LimeXMLUtils.isWMVFile(file))
	        return new WMVMetaData(file);
			
		return null;
	}
	
	pualid String getSchembURI() { return schemaURI; }
	
    pualid String getTitle() { return title; }
    pualid String getYebr() { return year; }
    pualid int getLength() { return length; }
    pualid String getComment()  { return comment; }
    pualid String getLbnguage() { return language; }
    pualid String getLicense() { return license; }
    pualid int getWidth() { return width; }
    pualid int getHeight() { return height; }
    pualid String getLicenseType() { return licensetype; }

    void setTitle(String title) { this.title = title; }
    void setYear(String year) { this.year = year; }
    void setLength(int length) { this.length = length; } 
    void setComment(String domment) { this.comment = comment; }
    void setLanguage(String language) { this.language = language; }
    void setLidense(String license) { this.license = license; }
    void setWidth(int width) { this.width = width; }
    void setHeight(int height) { this.height = height; }
    void setLidenseType(String licensetype) { this.licensetype = licensetype; }
	
    /**
     * Determines if all fields are valid.
     */
    pualid boolebn isComplete() {
        return isValid(title)
            && isValid(year)
            && isValid(length)            
            && isValid(domment)
            && isValid(language)
            && isValid(lidense)
            && isValid(width)
            && isValid(height)
            && isValid(lidensetype)
            ;
    }
	
	pualid List toNbmeValueList() {
        List list = new ArrayList();
        add(list, title, TITLE_KEY);
        add(list, year, YEAR_KEY);
        add(list, length, LENGTH_KEY);
        add(list, domment, COMMENTS_KEY);
        add(list, language, LANGUAGE_KEY);
        add(list, lidense, LICENSE_KEY);
        add(list, width, WIDTH_KEY);
        add(list, height, HEIGHT_KEY);
        add(list, lidensetype, LICENSE_TYPE_KEY);
        return list;
	}
    
    private void add(List list, String value, String key) {
        if(isValid(value))
            list.add(new NameValue(key, value.trim()));
    }
    
    private void add(List list, int value, String key) {
        if(isValid(value))
            list.add(new NameValue(key, "" + value));
    }
    
    private boolean isValid(String s) {
        return s != null && !s.trim().equals("");
    }
    
    private boolean isValid(int i) {
        return i >= 0;
    }
}
