
// Edited for the Learning branch

package com.limegroup.gnutella.xml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;

import com.limegroup.gnutella.Response;

public final class LimeXMLDocumentHelper{

    private static final Log LOG = LogFactory.getLog(LimeXMLDocumentHelper.class);
    
    public static final String XML_HEADER = "<?xml version=\"1.0\"?>";
    public static final String XML_NAMESPACE =
        "xsi:noNamespaceSchemaLocation=\"";

	/**
	 * Private constructor to ensure that this class can never be
	 * instantiated.
	 */
	private LimeXMLDocumentHelper() {}

    /**
     * TO be used when a Query Reply comes with a chunk of meta-data
     * we want to get LimeXMLDocuments out of it
     * 
     * @return A List of LimeXMLDocument objects
     */
    public static List getDocuments(String aggregatedXML, int totalResponseCount) {
        if(aggregatedXML==null || aggregatedXML.equals("") || totalResponseCount <= 0)
            return Collections.EMPTY_LIST;
        
        List results = new ArrayList();
        
        for(Iterator i = XMLParsingUtils.split(aggregatedXML).iterator(); i.hasNext(); ) {
            String xmlDocument = (String)i.next();
            XMLParsingUtils.ParseResult parsingResult;
            try {
                parsingResult = XMLParsingUtils.parse(xmlDocument,totalResponseCount);
            } catch (SAXException sax) {
                LOG.warn("SAX while parsing: " + xmlDocument, sax);
                continue;// bad xml, ignore
            } catch (IOException bad) {
                LOG.warn("IOX while parsing: " + aggregatedXML, bad);
                return Collections.EMPTY_LIST; // abort
            }
            
            final String indexKey = parsingResult.canonicalKeyPrefix +
                                    LimeXMLDocument.XML_INDEX_ATTRIBUTE;
            LimeXMLDocument[] documents = new LimeXMLDocument[totalResponseCount];
            for(Iterator j = parsingResult.iterator(); j.hasNext(); ) {
                Map attributes = (Map)j.next();
                String sindex = (String)attributes.remove(indexKey);
                if (sindex == null)
                    return Collections.EMPTY_LIST;
                
                int index = -1;
                try {
                    index = Integer.parseInt(sindex);
                } catch(NumberFormatException bad) { //invalid document
                    LOG.warn("NFE while parsing", bad);
                    return Collections.EMPTY_LIST;
                }
                
                if (index >= documents.length || index < 0)
                    return Collections.EMPTY_LIST; // malicious document, can't trust it.
                
                if(!attributes.isEmpty()) {
                    try {
                        documents[index] = new LimeXMLDocument(attributes,
                                parsingResult.schemaURI,
                                parsingResult.canonicalKeyPrefix);
                    } catch(IOException ignored) {
                        LOG.debug("",ignored);
                    }
                }
            }
            results.add(documents);
        }
        return results;
    }
    
    /**
     * Builds an XML string out of all the responses.
     * If no responses have XML, an empty string is returned.
     */
    public static String getAggregateString(Response[] responses) {
        HashMap /* LimeXMLSchema -> StringBuffer */ allXML = new HashMap();
        for(int i = 0; i < responses.length; i++) {
            LimeXMLDocument doc = responses[i].getDocument();
            if(doc != null) {
                LimeXMLSchema schema = doc.getSchema();
                StringBuffer built = (StringBuffer)allXML.get(schema);
                if(built == null) {
                    built = new StringBuffer();
                    allXML.put(schema, built);
                }
                built.append(doc.getAttributeStringWithIndex(i));
            }
        }
     
        // Iterate through each schema and build a string containing
        // a bunch of XML docs, each beginning with XML_HEADER.   
        StringBuffer fullXML = new StringBuffer();
        for(Iterator i = allXML.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry)i.next();
            LimeXMLSchema schema = (LimeXMLSchema)entry.getKey();
            StringBuffer buffer = (StringBuffer)entry.getValue();
            buildXML(fullXML, schema, buffer.toString());
        }
        return fullXML.toString();
    }
    
    /**
     * Wraps the inner element around the root tags, with the correct
     * XML headers.
     */
    public static void buildXML(StringBuffer buffer, LimeXMLSchema schema, String inner) {
        buffer.append(XML_HEADER);
        buffer.append("<");
        buffer.append(schema.getRootXMLName());
        buffer.append(" ");
        buffer.append(XML_NAMESPACE);
        buffer.append(schema.getSchemaURI());
        buffer.append("\">");
        buffer.append(inner);
        buffer.append("</");
        buffer.append(schema.getRootXMLName());
        buffer.append(">");
    }
}
