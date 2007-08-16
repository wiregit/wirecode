package com.limegroup.gnutella.xml;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.xml.sax.SAXException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.licenses.LicenseFactory;

@Singleton
public class LimeXMLDocumentFactoryImpl implements LimeXMLDocumentFactory {

    private final LicenseFactory licenseFactory;

    @Inject
    public LimeXMLDocumentFactoryImpl(LicenseFactory licenseFactory) {
        this.licenseFactory = licenseFactory;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.xml.LimeXMLDocumentFactory#createLimeXMLDocument(java.lang.String)
     */
    public LimeXMLDocument createLimeXMLDocument(String xml)
            throws SAXException, SchemaNotFoundException, IOException {
        return new LimeXMLDocument(xml, licenseFactory);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.xml.LimeXMLDocumentFactory#createLimeXMLDocument(java.util.Map, java.lang.String, java.lang.String)
     */
    public LimeXMLDocument createLimeXMLDocument(
            Map<String, String> map, String schemaURI, String keyPrefix)
            throws IOException {
        return new LimeXMLDocument(map, schemaURI, keyPrefix, licenseFactory);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.xml.LimeXMLDocumentFactory#createLimeXMLDocument(java.util.Collection, java.lang.String)
     */
    public LimeXMLDocument createLimeXMLDocument(
            Collection<? extends Entry<String, String>> nameValueList,
            String schemaURI) {
        return new LimeXMLDocument(nameValueList, schemaURI, licenseFactory);
    }

}
