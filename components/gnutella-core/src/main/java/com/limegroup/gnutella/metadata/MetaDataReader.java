
package com.limegroup.gnutella.metadata;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLSchemaRepository;
import com.limegroup.gnutella.xml.LimeXMLUtils;

/**
 * Utility class that creates a <tt>LimeXMLDocument</tt> 
 * from a file
 */
public class MetaDataReader {
	
	private MetaDataReader(){}

	/**
	 * Generates a LimeXMLDocument of the file's id3 data.
	 */
	public static LimeXMLDocument readDocument(File file) throws IOException {
		
		MetaData data = MetaData.parse(file);
		List nameValList = data.toNameValueList();
		if(nameValList.isEmpty())
			throw new IOException("invalid/no data.");
		
		if(LimeXMLSchemaRepository.instance().getSchema(AudioMetaData.schemaURI) == null)
             throw new IOException("no audio schema");

		return new LimeXMLDocument(nameValList, LimeXMLUtils.getSchemaURI(file));
	}
	
	
}
