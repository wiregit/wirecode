package com.limegroup.gnutella.metadata;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.limegroup.gnutella.util.NameValue;
import com.limegroup.gnutella.xml.LimeXMLNames;
import com.limegroup.gnutella.xml.LimeXMLUtils;

/**
 * Encapsulates video metadata.  Subclasses must implement parseFile.
 */
public abstract class VideoMetaData extends MetaData {
    
	private String title;
	private String year;
	private int length = -1;
	private String comment;
	private String language;
	private String license;
    private int width = -1;
    private int height = -1;
    private String licensetype;

    /**
     * Constructs a blank VideoMetaData object.
     */
    protected VideoMetaData() {}

    /**
     * Parses the file for data.
     */
	public VideoMetaData(File f) throws IOException {
		parseFile(f);
	}

    /**
     * Parses video metadata out of the file if this is a known video file.
     * Otherwise returns null.
     */
	public static VideoMetaData parseVideoMetaData(File file)
			throws IOException {
		if (LimeXMLUtils.isRIFFFile(file))
			return new RIFFMetaData(file);
		else if (LimeXMLUtils.isOGMFile(file))
			return new OGMMetaData(file);
	    else if(LimeXMLUtils.isWMVFile(file))
	        return new WMVMetaData(file);
        else if(LimeXMLUtils.isMPEGFile(file))
            return new MPEGMetaData(file);
        else if (LimeXMLUtils.isQuickTimeFile(file))
            return new MOVMetaData(file);
			
		return null;
	}
	
	public String getSchemaURI() { return LimeXMLNames.VIDEO_SCHEMA; }
	
    public String getTitle() { return title; }
    public String getYear() { return year; }
    public int getLength() { return length; }
    public String getComment()  { return comment; }
    public String getLanguage() { return language; }
    public String getLicense() { return license; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public String getLicenseType() { return licensetype; }

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
    public boolean isComplete() {
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
	
	public List<NameValue<String>> toNameValueList() {
        List<NameValue<String>> list = new ArrayList<NameValue<String>>();
        add(list, title, LimeXMLNames.VIDEO_TITLE);
        add(list, year, LimeXMLNames.VIDEO_YEAR);
        add(list, length, LimeXMLNames.VIDEO_LENGTH);
        add(list, comment, LimeXMLNames.VIDEO_COMMENTS);
        add(list, language, LimeXMLNames.VIDEO_LANGUAGE);
        add(list, license, LimeXMLNames.VIDEO_LICENSE);
        add(list, width, LimeXMLNames.VIDEO_WIDTH);
        add(list, height, LimeXMLNames.VIDEO_HEIGHT);
        add(list, licensetype, LimeXMLNames.VIDEO_LICENSETYPE);
        return list;
	}
    
    private void add(List<NameValue<String>> list, String value, String key) {
        if(isValid(value))
            list.add(new NameValue<String>(key, value.trim()));
    }
    
    private void add(List<NameValue<String>> list, int value, String key) {
        if(isValid(value))
            list.add(new NameValue<String>(key, "" + value));
    }
    
    private boolean isValid(String s) {
        return s != null && !s.trim().equals("");
    }
    
    private boolean isValid(int i) {
        return i >= 0;
    }
}
