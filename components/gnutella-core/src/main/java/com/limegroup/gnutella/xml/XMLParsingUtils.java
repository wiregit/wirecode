package com.limegroup.gnutella.xml;


import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import com.limegroup.gnutella.ErrorService;

/**
 * Provides just enough functionality for our simple schemas,
 * based on SAX
 * @author  tjones
 */
public class XMLParsingUtils {
    

    static final private String XML_START = "<?xml";
    
    /**
     * a ThreadLocal to contain the instance of the SAX parser
     */
    private static ThreadLocal _reader = new ThreadLocal() {
        protected Object initialValue() {
            try{
                return 
            		XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
            }catch(SAXException bad) {
                ErrorService.error(bad);
                return null; 
            }
        }
    };
    
    /**
     * Parses our simplified XML
     */
    public static ParseResult parse(String xml) throws IOException, SAXException {
        return parse(new InputSource(new StringReader(xml)));
    }
    
    /**
     * Parses our simplified XML
     */
    public static ParseResult parse(InputSource inputSource) throws IOException, SAXException {
        
        final ParseResult result = new ParseResult();
        
        //if parser creation failed, return empty results for everything.
        XMLReader reader = (XMLReader)_reader.get();   
        if (reader == null)
            return result;
        
        reader.setContentHandler(new DefaultHandler() {
                boolean isFirstElement=true;
                public void startElement(String namespaceUri, String localName, 
                                         String qualifiedName, Attributes attributes) {
                    
                    if(isFirstElement) {
                        isFirstElement=false; 
                        result.canonicalKeyPrefix = localName;
                        return;
                    }
                    
                    if(result.type==null) {
                        result.type = localName;
                        result.schemaURI = "http://www.limewire.com/schemas/"+result.type+".xsd";
                        result.canonicalKeyPrefix += "__"+localName+"__";
                    } 
                    
                    Map attributeMap = new HashMap();
                    for(int i=0; i<attributes.getLength(); i++) {
                        attributeMap.put(result.canonicalKeyPrefix + 
                                         attributes.getLocalName(i) + "__",
                                         attributes.getValue(i));
                    }
                    result.canonicalAttributeMaps.add(attributeMap);
                }
            });
        reader.parse(inputSource);
        return result;
    }

    /**
     * Splits an aggregated XML string into individual XML strings
     * @param aggregatedXmlDocuments
     * @return List of Strings
     */    
    public static List split(String aggregatedXmlDocuments) {
        List results = new ArrayList();
        
        int begin=aggregatedXmlDocuments.indexOf(XML_START);
        int end=aggregatedXmlDocuments.indexOf(XML_START,begin+1);
        
        while(end!=-1) {
            results.add(aggregatedXmlDocuments.substring(begin,end));
            begin = end;
            end = aggregatedXmlDocuments.indexOf(XML_START,begin+1);
        }
        
        if(begin!=-1) 
            results.add(aggregatedXmlDocuments.substring(begin));
        
        return results;
    }
    
    /**
     * A tuple containing the Schema URI, the type, the canonical key prefix
     * and a list of maps.
     */
    public static class ParseResult {
        public String schemaURI;                              //like http://www.limewire.com/schemas/audio.xsd
        public String type;                                   //e.g. audio, video, etc.
        public String canonicalKeyPrefix;                     //like audios__audio__
        public List canonicalAttributeMaps = new ArrayList(); //one attribute map per element in xml
    }
}
