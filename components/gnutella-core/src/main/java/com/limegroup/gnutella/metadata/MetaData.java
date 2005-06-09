
package com.limegroup.gnutella.metadata;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.limegroup.gnutella.xml.LimeXMLUtils;

/**
 * subclass for all data objects that contain the metadata 
 * of a given media file.
 */

public abstract class MetaData {
	
	protected MetaData(){} // use the factory instead of instantiating
	
    /** Creates MetaData for the file, if possible. */	
	public static MetaData parse(File f) throws IOException {
		if (LimeXMLUtils.isSupportedAudioFormat(f))
			return AudioMetaData.parseAudioFile(f);
		else if (LimeXMLUtils.isSupportedVideoFormat(f))
			return VideoMetaData.parseVideoMetaData(f);
		//TODO: add other media formats here			
	    else if (LimeXMLUtils.isSupportedMultipleFormat(f))
	        return parseMultipleFormat(f);
	        
		return null;
	}
	
	/** Figures out what kind of MetaData should exist for this file. */
	private static MetaData parseMultipleFormat(File f) throws IOException {
	    if(LimeXMLUtils.isASFFile(f)) {
	        ASFParser p = new ASFParser(f);
	        if(p.hasVideo())
	            return new WMVMetaData(p);
	        else if(p.hasAudio())
	            return new WMAMetaData(p);
        }
        
        return null;
    }
	
	/**
	 * Determines if all fields are valid.
	 */
	public abstract boolean isComplete();
	
	/**
	 * Writes the data to a NameValue list.
	 */
	public abstract List toNameValueList();
	
	/** 
	 * Retrieves the XML schema URI that this MetaData can be read with.
	 */
	public abstract String getSchemaURI();
	
	/**
	 * populates this's data fields with data read from the media file
	 * all subclasses need to implement it
	 * @throws IOException parsing failed
	 */
    protected abstract void parseFile(File f) throws IOException;
}