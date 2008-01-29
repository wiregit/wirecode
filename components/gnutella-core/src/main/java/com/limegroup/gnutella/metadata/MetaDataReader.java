package com.limegroup.gnutella.metadata;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.limewire.collection.NameValue;
import org.limewire.service.ErrorService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;
import com.limegroup.gnutella.xml.LimeXMLSchema;
import com.limegroup.gnutella.xml.LimeXMLSchemaRepository;

/**
 * Utility class that creates a <tt>LimeXMLDocument</tt> from a file.
 */
@Singleton
public class MetaDataReader {

    private final LimeXMLDocumentFactory limeXMLDocumentFactory;
    private final LimeXMLSchemaRepository limeXMLSchemaRepository;

    @Inject
    MetaDataReader(LimeXMLDocumentFactory limeXMLDocumentFactory,
            LimeXMLSchemaRepository limeXMLSchemaRepository) {
        this.limeXMLDocumentFactory = limeXMLDocumentFactory;
        this.limeXMLSchemaRepository = limeXMLSchemaRepository;
    }

    /**
     * Generates a LimeXMLDocument from this file, only parsing it if it's the
     * given schemaURI.
     */
    public LimeXMLDocument readDocument(File file) throws IOException {
        MetaReader data = MetaDataFactory.parse(file);
        if (data == null)
            throw new IOException("unable to parse file");

        List<NameValue<String>> nameValList = data.toNameValueList();
        if (nameValList.isEmpty())
            throw new IOException("invalid/no data.");

        String uri = data.getSchemaURI();
        LimeXMLSchema schema = limeXMLSchemaRepository.getSchema(uri);
        if (schema == null || schema.getCanonicalizedFields().isEmpty())
            throw new IOException("schema: " + uri + " doesn't exist");
        
        try {
            return limeXMLDocumentFactory.createLimeXMLDocument(nameValList, uri);
        } catch(IllegalArgumentException iae) {
            // Wrap this into an IOException since calling classes will
            // know to ignore, but for now we still want to debug this
            ErrorService.error(iae); // remove if not harmful
            throw (IOException)new IOException().initCause(iae);
        }
    }

}
