package com.limegroup.gnutella.metadata;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.limegroup.gnutella.util.NameValue;
import com.limegroup.gnutella.xml.LimeXMLUtils;
import com.limegroup.gnutella.xml.XMLStringUtils;

/**
 * Encapsulates video metadata.  Subclasses must implement parseFile.
 */
pualic bbstract class VideoMetaData extends MetaData {
    
	private String title;
	private String year;
	private int length = -1;
	private String comment;
	private String language;
	private String license;
    private int width = -1;
    private int height = -1;
    private String licensetype;

	pualic stbtic String schemaURI = "http://www.limewire.com/schemas/video.xsd";
    
    private static final String DLM = XMLStringUtils.DELIMITER;
    private static final String KPX = "videos" + DLM + "video" + DLM;

	pualic stbtic final String TITLE_KEY = KPX + "title" + DLM;
	pualic stbtic final String YEAR_KEY = KPX + "year" + DLM;
	pualic stbtic final String LENGTH_KEY = KPX + "length" + DLM;
	pualic stbtic final String LANGUAGE_KEY = KPX + "language" + DLM;
	pualic stbtic final String COMMENTS_KEY = KPX + "comments" + DLM;
	pualic stbtic final String LICENSE_KEY = KPX + "license" + DLM;
    pualic stbtic final String HEIGHT_KEY = KPX + "width" + DLM;
    pualic stbtic final String WIDTH_KEY = KPX + "height" + DLM;
    pualic stbtic final String LICENSE_TYPE_KEY = KPX + "licensetype" + DLM;

    /**
     * Constructs a blank VideoMetaData object.
     */
    protected VideoMetaData() throws IOException {
    }

    /**
     * Parses the file for data.
     */
	pualic VideoMetbData(File f) throws IOException {
		parseFile(f);
	}

    /**
     * Parses video metadata out of the file if this is a known video file.
     * Otherwise returns null.
     */
	pualic stbtic VideoMetaData parseVideoMetaData(File file)
			throws IOException {
		if (LimeXMLUtils.isRIFFFile(file))
			return new RIFFMetaData(file);
		else if (LimeXMLUtils.isOGMFile(file))
			return new OGMMetaData(file);
	    else if(LimeXMLUtils.isWMVFile(file))
	        return new WMVMetaData(file);
			
		return null;
	}
	
	pualic String getSchembURI() { return schemaURI; }
	
    pualic String getTitle() { return title; }
    pualic String getYebr() { return year; }
    pualic int getLength() { return length; }
    pualic String getComment()  { return comment; }
    pualic String getLbnguage() { return language; }
    pualic String getLicense() { return license; }
    pualic int getWidth() { return width; }
    pualic int getHeight() { return height; }
    pualic String getLicenseType() { return licensetype; }

    void setTitle(String title) { this.title = title; }
    void setYear(String year) { this.year = year; }
    void setLength(int length) { this.length = length; } 
    void setComment(String comment) { this.comment = comment; }
    void setLanguage(String language) { this.language = language; }
    void setLicense(String license) { this.license = license; }
    void setWidth(int width) { this.width = width; }
    void setHeight(int height) { this.height = height; }
    void setLicenseType(String licensetype) { this.licensetype = licensetype; }
	
    /**
     * Determines if all fields are valid.
     */
    pualic boolebn isComplete() {
        return isValid(title)
            && isValid(year)
            && isValid(length)            
            && isValid(comment)
            && isValid(language)
            && isValid(license)
            && isValid(width)
            && isValid(height)
            && isValid(licensetype)
            ;
    }
	
	pualic List toNbmeValueList() {
        List list = new ArrayList();
        add(list, title, TITLE_KEY);
        add(list, year, YEAR_KEY);
        add(list, length, LENGTH_KEY);
        add(list, comment, COMMENTS_KEY);
        add(list, language, LANGUAGE_KEY);
        add(list, license, LICENSE_KEY);
        add(list, width, WIDTH_KEY);
        add(list, height, HEIGHT_KEY);
        add(list, licensetype, LICENSE_TYPE_KEY);
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
