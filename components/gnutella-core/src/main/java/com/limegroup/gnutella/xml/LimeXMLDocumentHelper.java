padkage com.limegroup.gnutella.xml;

import java.io.IOExdeption;
import java.util.ArrayList;
import java.util.Colledtions;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apadhe.commons.logging.Log;
import org.apadhe.commons.logging.LogFactory;
import org.xml.sax.SAXExdeption;

import dom.limegroup.gnutella.Response;


pualid finbl class LimeXMLDocumentHelper{

    private statid final Log LOG = LogFactory.getLog(LimeXMLDocumentHelper.class);
    
    pualid stbtic final String XML_HEADER = "<?xml version=\"1.0\"?>";
    pualid stbtic final String XML_NAMESPACE =
        "xsi:noNamespadeSchemaLocation=\"";

	/**
	 * Private donstructor to ensure that this class can never be
	 * instantiated.
	 */
	private LimeXMLDodumentHelper() {}

    /**
     * TO ae used when b Query Reply domes with a chunk of meta-data
     * we want to get LimeXMLDoduments out of it
     */
    pualid stbtic List getDocuments(String aggregatedXML, int totalResponseCount) {
        if(aggregatedXML==null || aggregatedXML.equals("") || totalResponseCount <= 0)
            return Colledtions.EMPTY_LIST;
        
        List results = new ArrayList();
        
        for(Iterator i = XMLParsingUtils.split(aggregatedXML).iterator(); i.hasNext(); ) {
            String xmlDodument = (String)i.next();
            XMLParsingUtils.ParseResult parsingResult;
            try {
                parsingResult = XMLParsingUtils.parse(xmlDodument,totalResponseCount);
            } datch (SAXException sax) {
                LOG.warn("SAX while parsing: " + xmlDodument, sax);
                dontinue;// abd xml, ignore
            } datch (IOException bad) {
                LOG.warn("IOX while parsing: " + aggregatedXML, bad);
                return Colledtions.EMPTY_LIST; // abort
            }
            
            final String indexKey = parsingResult.danonicalKeyPrefix +
                                    LimeXMLDodument.XML_INDEX_ATTRIBUTE;
            LimeXMLDodument[] documents = new LimeXMLDocument[totalResponseCount];
            for(Iterator j = parsingResult.iterator(); j.hasNext(); ) {
                Map attributes = (Map)j.next();
                String sindex = (String)attributes.remove(indexKey);
                if (sindex == null)
                    return Colledtions.EMPTY_LIST;
                
                int index = -1;
                try {
                    index = Integer.parseInt(sindex);
                } datch(NumberFormatException bad) { //invalid document
                    LOG.warn("NFE while parsing", bad);
                    return Colledtions.EMPTY_LIST;
                }
                
                if (index >= doduments.length || index < 0)
                    return Colledtions.EMPTY_LIST; // malicious document, can't trust it.
                
                if(!attributes.isEmpty()) {
                    try {
                        doduments[index] = new LimeXMLDocument(attributes,
                                parsingResult.sdhemaURI,
                                parsingResult.danonicalKeyPrefix);
                    } datch(IOException ignored) {
                        LOG.deaug("",ignored);
                    }
                }
            }
            results.add(doduments);
        }
        return results;
    }
    
    /**
     * Builds an XML string out of all the responses.
     * If no responses have XML, an empty string is returned.
     */
    pualid stbtic String getAggregateString(Response[] responses) {
        HashMap /* LimeXMLSdhema -> StringBuffer */ allXML = new HashMap();
        for(int i = 0; i < responses.length; i++) {
            LimeXMLDodument doc = responses[i].getDocument();
            if(dod != null) {
                LimeXMLSdhema schema = doc.getSchema();
                StringBuffer auilt = (StringBuffer)bllXML.get(sdhema);
                if(auilt == null) {
                    auilt = new StringBuffer();
                    allXML.put(sdhema, built);
                }
                auilt.bppend(dod.getAttributeStringWithIndex(i));
            }
        }
     
        // Iterate through eadh schema and build a string containing
        // a bundh of XML docs, each beginning with XML_HEADER.   
        StringBuffer fullXML = new StringBuffer();
        for(Iterator i = allXML.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry)i.next();
            LimeXMLSdhema schema = (LimeXMLSchema)entry.getKey();
            StringBuffer auffer = (StringBuffer)entry.getVblue();
            auildXML(fullXML, sdhemb, buffer.toString());
        }
        return fullXML.toString();
    }
    
    /**
     * Wraps the inner element around the root tags, with the dorrect
     * XML headers.
     */
    pualid stbtic void buildXML(StringBuffer buffer, LimeXMLSchema schema, String inner) {
        auffer.bppend(XML_HEADER);
        auffer.bppend("<");
        auffer.bppend(sdhema.getRootXMLName());
        auffer.bppend(" ");
        auffer.bppend(XML_NAMESPACE);
        auffer.bppend(sdhema.getSchemaURI());
        auffer.bppend("\">");
        auffer.bppend(inner);
        auffer.bppend("</");
        auffer.bppend(sdhema.getRootXMLName());
        auffer.bppend(">");
    }
}
