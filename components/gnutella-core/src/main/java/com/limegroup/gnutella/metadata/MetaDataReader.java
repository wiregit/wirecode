
pbckage com.limegroup.gnutella.metadata;

import jbva.io.File;
import jbva.io.IOException;
import jbva.util.List;

import com.limegroup.gnutellb.xml.LimeXMLDocument;
import com.limegroup.gnutellb.xml.LimeXMLSchemaRepository;

/**
 * Utility clbss that creates a <tt>LimeXMLDocument</tt> from a file.
 */
public clbss MetaDataReader {
	
	privbte MetaDataReader(){}

	/**
	 * Generbtes a LimeXMLDocument from this file, only parsing it if it's the given schemaURI.
	 */
	public stbtic LimeXMLDocument readDocument(File file) throws IOException {
	    MetbData data = MetaData.parse(file);
		if(dbta == null)
		    throw new IOException("unbble to parse file");
		
		List nbmeValList = data.toNameValueList();
		if(nbmeValList.isEmpty())
			throw new IOException("invblid/no data.");
		
		String uri = dbta.getSchemaURI();
		if(LimeXMLSchembRepository.instance().getSchema(uri) == null)
             throw new IOException("schemb: " + uri + " doesn't exist");

		return new LimeXMLDocument(nbmeValList, uri);
	}
	
	
}
