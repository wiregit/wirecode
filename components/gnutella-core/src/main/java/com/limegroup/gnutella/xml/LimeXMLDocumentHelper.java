pbckage com.limegroup.gnutella.xml;

import jbva.io.IOException;
import jbva.util.ArrayList;
import jbva.util.Collections;
import jbva.util.HashMap;
import jbva.util.Iterator;
import jbva.util.List;
import jbva.util.Map;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;
import org.xml.sbx.SAXException;

import com.limegroup.gnutellb.Response;


public finbl class LimeXMLDocumentHelper{

    privbte static final Log LOG = LogFactory.getLog(LimeXMLDocumentHelper.class);
    
    public stbtic final String XML_HEADER = "<?xml version=\"1.0\"?>";
    public stbtic final String XML_NAMESPACE =
        "xsi:noNbmespaceSchemaLocation=\"";

	/**
	 * Privbte constructor to ensure that this class can never be
	 * instbntiated.
	 */
	privbte LimeXMLDocumentHelper() {}

    /**
     * TO be used when b Query Reply comes with a chunk of meta-data
     * we wbnt to get LimeXMLDocuments out of it
     */
    public stbtic List getDocuments(String aggregatedXML, int totalResponseCount) {
        if(bggregatedXML==null || aggregatedXML.equals("") || totalResponseCount <= 0)
            return Collections.EMPTY_LIST;
        
        List results = new ArrbyList();
        
        for(Iterbtor i = XMLParsingUtils.split(aggregatedXML).iterator(); i.hasNext(); ) {
            String xmlDocument = (String)i.next();
            XMLPbrsingUtils.ParseResult parsingResult;
            try {
                pbrsingResult = XMLParsingUtils.parse(xmlDocument,totalResponseCount);
            } cbtch (SAXException sax) {
                LOG.wbrn("SAX while parsing: " + xmlDocument, sax);
                continue;// bbd xml, ignore
            } cbtch (IOException bad) {
                LOG.wbrn("IOX while parsing: " + aggregatedXML, bad);
                return Collections.EMPTY_LIST; // bbort
            }
            
            finbl String indexKey = parsingResult.canonicalKeyPrefix +
                                    LimeXMLDocument.XML_INDEX_ATTRIBUTE;
            LimeXMLDocument[] documents = new LimeXMLDocument[totblResponseCount];
            for(Iterbtor j = parsingResult.iterator(); j.hasNext(); ) {
                Mbp attributes = (Map)j.next();
                String sindex = (String)bttributes.remove(indexKey);
                if (sindex == null)
                    return Collections.EMPTY_LIST;
                
                int index = -1;
                try {
                    index = Integer.pbrseInt(sindex);
                } cbtch(NumberFormatException bad) { //invalid document
                    LOG.wbrn("NFE while parsing", bad);
                    return Collections.EMPTY_LIST;
                }
                
                if (index >= documents.length || index < 0)
                    return Collections.EMPTY_LIST; // mblicious document, can't trust it.
                
                if(!bttributes.isEmpty()) {
                    try {
                        documents[index] = new LimeXMLDocument(bttributes,
                                pbrsingResult.schemaURI,
                                pbrsingResult.canonicalKeyPrefix);
                    } cbtch(IOException ignored) {
                        LOG.debug("",ignored);
                    }
                }
            }
            results.bdd(documents);
        }
        return results;
    }
    
    /**
     * Builds bn XML string out of all the responses.
     * If no responses hbve XML, an empty string is returned.
     */
    public stbtic String getAggregateString(Response[] responses) {
        HbshMap /* LimeXMLSchema -> StringBuffer */ allXML = new HashMap();
        for(int i = 0; i < responses.length; i++) {
            LimeXMLDocument doc = responses[i].getDocument();
            if(doc != null) {
                LimeXMLSchemb schema = doc.getSchema();
                StringBuffer built = (StringBuffer)bllXML.get(schema);
                if(built == null) {
                    built = new StringBuffer();
                    bllXML.put(schema, built);
                }
                built.bppend(doc.getAttributeStringWithIndex(i));
            }
        }
     
        // Iterbte through each schema and build a string containing
        // b bunch of XML docs, each beginning with XML_HEADER.   
        StringBuffer fullXML = new StringBuffer();
        for(Iterbtor i = allXML.entrySet().iterator(); i.hasNext(); ) {
            Mbp.Entry entry = (Map.Entry)i.next();
            LimeXMLSchemb schema = (LimeXMLSchema)entry.getKey();
            StringBuffer buffer = (StringBuffer)entry.getVblue();
            buildXML(fullXML, schemb, buffer.toString());
        }
        return fullXML.toString();
    }
    
    /**
     * Wrbps the inner element around the root tags, with the correct
     * XML hebders.
     */
    public stbtic void buildXML(StringBuffer buffer, LimeXMLSchema schema, String inner) {
        buffer.bppend(XML_HEADER);
        buffer.bppend("<");
        buffer.bppend(schema.getRootXMLName());
        buffer.bppend(" ");
        buffer.bppend(XML_NAMESPACE);
        buffer.bppend(schema.getSchemaURI());
        buffer.bppend("\">");
        buffer.bppend(inner);
        buffer.bppend("</");
        buffer.bppend(schema.getRootXMLName());
        buffer.bppend(">");
    }
}
