
package com.limegroup.gnutella.mp3;

import com.limegroup.gnutella.xml.LimeXMLUtils;
import com.sun.java.util.collections.List;
import java.io.*;

/**
 * interface for all data objects that contain the metadata 
 * of a given media file.
 */

public abstract class MetaData {
	
	public static MetaData parse(File f) throws IOException {
		if (LimeXMLUtils.isSupportedAudioFormat(f))
			return AudioMetaData.parseAudioFile(f);
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
	 * Writes the data to an XML string.
	 *
	 * @param complete if true, the data is a complete XML string, otherwise
	 * it is an excerpt of an XML string.
	 */
	public abstract String toXML(String path, boolean complete);
}