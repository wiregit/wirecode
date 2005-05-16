package com.limegroup.gnutella.metadata;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.limegroup.gnutella.util.NameValue;
import com.limegroup.gnutella.xml.LimeXMLUtils;
import com.limegroup.gnutella.xml.XMLStringUtils;

public abstract class VideoMetaData extends MetaData {
	protected String _title = "";

	protected short _year = -1;

	protected short _seconds = -1;

	protected String _comments = "";

	protected String _language = "";

	protected String _license = "";

	private boolean _complete = false;
    
    protected int _width = -1;
    
    protected int _height = -1;

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

	public VideoMetaData(File f) throws IOException {
		parseFile(f);
		_complete = true;
	}

	public static VideoMetaData parseVideoMetaData(File file)
			throws IOException {
		VideoMetaData ret = null;
		if (LimeXMLUtils.isRIFFFile(file))
			ret = new RIFFMetaData(file);
		else if (LimeXMLUtils.isOGMFile(file))
			ret = new OGMMetaData(file);
			
		return ret;
	}

	
	public boolean isComplete() {
		return _complete;
	}

	
	public List toNameValueList() {
        List list = new ArrayList();
        add(list, _title, TITLE_KEY);
        add(list, _year, YEAR_KEY);
        add(list, _comments, COMMENTS_KEY);
        add(list, _seconds, LENGTH_KEY);
        add(list, _license, LICENSE_KEY);
        add(list, _language, LANGUAGE_KEY);
        add(list, _width, WIDTH_KEY);
        add(list, _height, HEIGHT_KEY);
        return list;
	}
    
    private void add(List list, String value, String key) {
        if(value != null && value.length() > 0)
            list.add(new NameValue(key, value.trim()));
    }
    
    private void add(List list, int value, String key) {
        if(value >= 0)
            list.add(new NameValue(key, "" + value));
    }
}
