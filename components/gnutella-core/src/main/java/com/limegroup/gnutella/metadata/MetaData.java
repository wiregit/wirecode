
package com.limegroup.gnutella.metadata;

import com.limegroup.gnutella.xml.LimeXMLUtils;
import java.util.List;
import java.io.*;

/**
 * subclass for all data objects that contain the metadata 
 * of a given media file.
 */

public abstract class MetaData {
	
	protected MetaData(){} // use the factory instead of instantiating
	
	public static MetaData parse(File f) throws IOException {
		if (LimeXMLUtils.isSupportedAudioFormat(f))
			return AudioMetaData.parseAudioFile(f);
		//TODO: add other media formats here
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
	 * populates this's data fields with data read from the media file
	 * all subclasses need to implement it
	 * @throws IOException parsing failed
	 */
    protected abstract void parseFile(File f) throws IOException;
}