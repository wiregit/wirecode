
package com.limegroup.gnutella.metadata;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLSchemaRepository;
import com.limegroup.gnutella.xml.LimeXMLUtils;

/**
 * Utility class that creates a <tt>LimeXMLDocument</tt> from a file.
 */
public class MetaDataReader {
	
	private MetaDataReader(){}

	/**
	 * Generates a LimeXMLDocument from this file, only parsing it if it's the given schemaURI.
	 */
	public static LimeXMLDocument readDocument(File file) throws IOException {
	    MetaData data = MetaData.parse(file);
		if(data == null)
		    throw new IOException("unable to parse file");
		
		List nameValList = data.toNameValueList();
		if(nameValList.isEmpty())
			throw new IOException("invalid/no data.");
		
		if(LimeXMLSchemaRepository.instance().getSchema(data.getSchema()) == null)
             throw new IOException("schema: " + data.getSchema() + " doesn't exist");

		return new LimeXMLDocument(nameValList, data.getSchema());
	}
	
	
}
