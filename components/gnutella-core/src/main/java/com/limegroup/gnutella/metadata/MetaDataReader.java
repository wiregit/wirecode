
package com.limegroup.gnutella.metadata;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.limewire.collection.NameValue;

import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLSchemaRepository;

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
		
		List<NameValue<String>> nameValList = data.toNameValueList();
		if(nameValList.isEmpty())
			throw new IOException("invalid/no data.");
		
		String uri = data.getSchemaURI();
		if(LimeXMLSchemaRepository.instance().getSchema(uri) == null)
             throw new IOException("schema: " + uri + " doesn't exist");

		return new LimeXMLDocument(nameValList, uri);
	}
	
	
}
