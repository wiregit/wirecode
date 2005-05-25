package com.limegroup.gnutella.metadata;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.limegroup.gnutella.util.NameValue;
import com.limegroup.gnutella.xml.LimeXMLUtils;
import com.limegroup.gnutella.xml.XMLStringUtils;

public abstract class VideoMetaData extends MetaData {
    
	private String title;
	private String year;
	private int length = -1;
	private String comment;
	private String language;
	private String license;
    private int width = -1;
    private int height = -1;

	public static final String ISO_LATIN_1 = "8859_1";

	public static final String UNICODE = "Unicode";

	public static String schemaURI = "http://www.limewire.com/schemas/video.xsd";

	public static final String KEY_PREFIX = "videos" + XMLStringUtils.DELIMITER
			+ "video" + XMLStringUtils.DELIMITER;

	public static final String TITLE_KEY = KEY_PREFIX + "title"
			+ XMLStringUtils.DELIMITER;

	public static final String YEAR_KEY = KEY_PREFIX + "year"
			+ XMLStringUtils.DELIMITER;

	public static final String LENGTH_KEY = KEY_PREFIX + "length"
			+ XMLStringUtils.DELIMITER;

	public static final String LANGUAGE_KEY = KEY_PREFIX + "language"
			+ XMLStringUtils.DELIMITER;

	public static final String COMMENTS_KEY = KEY_PREFIX + "comments"
			+ XMLStringUtils.DELIMITER;

	public static final String LICENSE_KEY = KEY_PREFIX + "license"
			+ XMLStringUtils.DELIMITER;
    
    public static final String HEIGHT_KEY = KEY_PREFIX + "width"
            + XMLStringUtils.DELIMITER;
    
    public static final String WIDTH_KEY = KEY_PREFIX + "height"
            + XMLStringUtils.DELIMITER;

    protected VideoMetaData() throws IOException {
    }

	public VideoMetaData(File f) throws IOException {
		parseFile(f);
	}

	public static VideoMetaData parseVideoMetaData(File file)
			throws IOException {
		if (LimeXMLUtils.isRIFFFile(file))
			return new RIFFMetaData(file);
		else if (LimeXMLUtils.isOGMFile(file))
			return new OGMMetaData(file);
	    else if(LimeXMLUtils.isWMVFile(file))
	        return new WMVMetaData(file);
			
		return null;
	}
	
	public String getSchema() { return schemaURI; }
	
    public String getTitle() { return title; }
    public String getYear() { return year; }
    public int getLength() { return length; }
    public String getComment()  { return comment; }
    public String getLanguage() { return language; }
    public String getLicense() { return license; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    void setTitle(String title) { this.title = title; }
    void setYear(String year) { this.year = year; }
    void setLength(int length) { this.length = length; } 
    void setComment(String comment) { this.comment = comment; }
    void setLanguage(String language) { this.language = language; }
    void setLicense(String license) { this.license = license; }
    void setWidth(int width) { this.width = width; }
    void setHeight(int height) { this.height = height; }
	
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
            && isValid(height);
    }
	
	public List toNameValueList() {
        List list = new ArrayList();
        add(list, title, TITLE_KEY);
        add(list, year, YEAR_KEY);
        add(list, length, LENGTH_KEY);
        add(list, comment, COMMENTS_KEY);
        add(list, language, LANGUAGE_KEY);
        add(list, license, LICENSE_KEY);
        add(list, width, WIDTH_KEY);
        add(list, height, HEIGHT_KEY);
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
