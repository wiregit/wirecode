
package com.limegroup.gnutella.mp3;

import java.io.*;

import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.util.*;

import com.sun.java.util.collections.*;

/**
 * Superclass of all readers that extract metadata from media files
 */
public class MetaDataReader {
	
	/**
	 * Generates an XML description of the file's id3 data.
	 * @param asString true if String we return is the only document associated
	 * with the file. 
	 */
	public static String readDocument(File file, boolean asString) throws IOException {
		
		MetaData data = MetaData.parse(file);
		return data.toXML(file.getCanonicalPath(), asString);
	    
	}

	/**
	 * Generates a LimeXMLDocument of the file's id3 data.
	 */
	public static LimeXMLDocument readDocument(File file) throws IOException {
		
		MetaData data = MetaData.parse(file);
		List nameValList = data.toNameValueList();
		if(nameValList.isEmpty())
			throw new IOException("invalid/no data.");

		return new LimeXMLDocument(nameValList, LimeXMLUtils.getSchemaURI(file));
	}
	
	
}
