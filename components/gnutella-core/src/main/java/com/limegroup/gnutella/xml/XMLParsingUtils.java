package com.limegroup.gnutella.xml;


import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

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
     * a ThreadLocal to contain the instance of the Lime parser
     */
    private static ThreadLocal _parserContainer = new ThreadLocal() {
        protected Object initialValue() {
            return new LimeParser();
        }
    };
    
    /**
     * Parses our simplified XML
     */
    public static ParseResult parse(String xml, int responseCount) 
      throws IOException, SAXException {
        return parse(new InputSource(new StringReader(xml)),responseCount);
    }
    
    public static ParseResult parse(InputSource inputSource) 
      throws IOException,SAXException {
        return parse(inputSource, 8);
    }
    
    /**
     * Parses our simplified XML
     */
    public static ParseResult parse(InputSource inputSource, int responseCount) 
      throws IOException, SAXException {
        ParseResult result = new ParseResult(responseCount);
        LimeParser parser = (LimeParser)_parserContainer.get();
        parser.parse(result,inputSource);
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
     * A list of maps, also containing the Schema URI, the type and
     * the canonical key prefix
     */
    public static class ParseResult extends ArrayList {
        
        public ParseResult(int size) {
            super(size*2/3);
        }
        
        public String schemaURI;            //like http://www.limewire.com/schemas/audio.xsd
        public String type;                 //e.g. audio, video, etc.
        public String canonicalKeyPrefix;   //like audios__audio__
    }
    
    /**
     * this class does the actual parsing of the document.  It is a reusable
     * DocumentHandler.
     */
    private static class LimeParser extends DefaultHandler {
        private final XMLReader _reader;
        private ParseResult _result;
        
        boolean _isFirstElement=true;
        
        LimeParser() {
            XMLReader reader;
            try {
                reader =
                    XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
                reader.setContentHandler(this);
            }catch(SAXException bad) {
                ErrorService.error(bad);
                reader = null; 
            }
            _reader=reader;
        }
        
        /**
         * parses the given document input.  Any state from previous parsing is
         * discarded.
         */
        public void parse(ParseResult dest, InputSource input) 
        	throws SAXException, IOException {
            
            //if parser creation failed, do not try to parse.
            if (_reader==null)
                return;
            
            _isFirstElement=true;
            _result = dest;

            _reader.parse(input);
        }
        
        public void startElement(String namespaceUri, String localName, 
                                 String qualifiedName, Attributes attributes) {
            
            if(_isFirstElement) {
                _isFirstElement=false; 
                _result.canonicalKeyPrefix = localName;
                return;
            }
            
            if(_result.type==null) {
                _result.type = localName;
                _result.schemaURI = "http://www.limewire.com/schemas/"+_result.type+".xsd";
                _result.canonicalKeyPrefix += "__"+localName+"__";
            } 
            
            int attributesLength = attributes.getLength();
            if(attributesLength > 0) {
                Map attributeMap = new HashMap(attributesLength);
                for(int i = 0; i < attributesLength; i++) {
                    attributeMap.put(_result.canonicalKeyPrefix + 
                                     attributes.getLocalName(i) + "__",
                                     attributes.getValue(i).trim());
                }
                _result.add(attributeMap);
            } else {
                _result.add(Collections.EMPTY_MAP);
            }
        }
    }
}
