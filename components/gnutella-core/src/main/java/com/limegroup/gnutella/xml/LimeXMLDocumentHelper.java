package com.limegroup.gnutella.xml;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.IpPortImpl;
import com.limegroup.gnutella.util.NetworkUtils;


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

	public static String getRSSItem(FileDesc f) {
		StringBuffer buf = new StringBuffer();
		buf.append("<item>");
		buf.append("<title>").append(f.getName()).append("</title>");
		
		// put all xml documents in the item as well
		for (Iterator iter = f.getLimeXMLDocuments().iterator(); iter.hasNext();) {
			LimeXMLDocument doc = (LimeXMLDocument) iter.next();
			buf.append("<limedoc><![CDATA[").append(doc.getXMLString()).append("]]></limedoc>");	
		}
		
		// add a magnet - if we couldn't accept incoming tcp we wouldn't be
		// servicing this request anyway
		try {
			IpPort me = 
				new IpPortImpl(NetworkUtils.ip2string(RouterService.getExternalAddress()),
						RouterService.getPort());
			buf.append("<description><![CDATA[<html>To download this file, click <a href=\"").
				append("magnet:?xt=urn:sha1:").append("&amp;dn=").
				append(f.getName()).append("&amp;xs=http://").append(me.getAddress()).
				append(":").append(me.getPort()).
				append("/uri-res/N2R?").append(f.getSHA1Urn()).append("\">here</a>").
				append("\n\nSome info about the file: ").append(f).append("</html>").
				append("]]></description>");
		} catch (UnknownHostException ignored){}
		
		// the date
		buf.append("<pubDate>").append(new Date(f.lastModified())).append("</pubDate>");
		
		// the guid is the sha1
		buf.append("<guid>").append(f.getSHA1Urn().httpStringValue()).append("</guid>");
		buf.append("</item>");
		return buf.toString();
	}
}
