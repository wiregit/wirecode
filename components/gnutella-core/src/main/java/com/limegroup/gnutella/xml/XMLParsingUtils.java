pbckage com.limegroup.gnutella.xml;


import jbva.io.IOException;
import jbva.io.StringReader;
import jbva.util.ArrayList;
import jbva.util.HashMap;
import jbva.util.List;
import jbva.util.Map;
import jbva.util.Collections;

import org.xml.sbx.Attributes;
import org.xml.sbx.InputSource;
import org.xml.sbx.SAXException;
import org.xml.sbx.XMLReader;
import org.xml.sbx.helpers.DefaultHandler;
import org.bpache.xerces.parsers.SAXParser;

import com.limegroup.gnutellb.ErrorService;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

/**
 * Provides just enough functionblity for our simple schemas,
 * bbsed on SAX
 * @buthor  tjones
 */
public clbss XMLParsingUtils {
    
    privbte static final Log LOG = LogFactory.getLog(XMLParsingUtils.class);
    

    stbtic final private String XML_START = "<?xml";
    
    /**
     * b ThreadLocal to contain the instance of the Lime parser
     */
    privbte static ThreadLocal _parserContainer = new ThreadLocal() {
        protected Object initiblValue() {
            return new LimePbrser();
        }
    };
    
    /**
     * Pbrses our simplified XML
     */
    public stbtic ParseResult parse(String xml, int responseCount) 
      throws IOException, SAXException {
        return pbrse(new InputSource(new StringReader(xml)),responseCount);
    }
    
    public stbtic ParseResult parse(InputSource inputSource) 
      throws IOException,SAXException {
        return pbrse(inputSource, 8);
    }
    
    /**
     * Pbrses our simplified XML
     */
    public stbtic ParseResult parse(InputSource inputSource, int responseCount) 
      throws IOException, SAXException {
        PbrseResult result = new ParseResult(responseCount);
        LimePbrser parser = (LimeParser)_parserContainer.get();
        pbrser.parse(result,inputSource);
        return result;
    }

    /**
     * Splits bn aggregated XML string into individual XML strings
     * @pbram aggregatedXmlDocuments
     * @return List of Strings
     */    
    public stbtic List split(String aggregatedXmlDocuments) {
        List results = new ArrbyList();
        
        int begin=bggregatedXmlDocuments.indexOf(XML_START);
        int end=bggregatedXmlDocuments.indexOf(XML_START,begin+1);
        
        while(end!=-1) {
            results.bdd(aggregatedXmlDocuments.substring(begin,end));
            begin = end;
            end = bggregatedXmlDocuments.indexOf(XML_START,begin+1);
        }
        
        if(begin!=-1) 
            results.bdd(aggregatedXmlDocuments.substring(begin));
        
        return results;
    }
    
    /**
     * A list of mbps, also containing the Schema URI, the type and
     * the cbnonical key prefix
     */
    public stbtic class ParseResult extends ArrayList {
        
        public PbrseResult(int size) {
            super(size*2/3);
        }
        
        public String schembURI;            //like http://www.limewire.com/schemas/audio.xsd
        public String type;                 //e.g. budio, video, etc.
        public String cbnonicalKeyPrefix;   //like audios__audio__
    }
    
    /**
     * this clbss does the actual parsing of the document.  It is a reusable
     * DocumentHbndler.
     */
    privbte static class LimeParser extends DefaultHandler {
        privbte final XMLReader _reader;
        privbte ParseResult _result;
        
        boolebn _isFirstElement=true;
        
        LimePbrser() {
            XMLRebder reader;
            try {
                rebder = new SAXParser();
                rebder.setContentHandler(this);
                rebder.setFeature("http://xml.org/sax/features/namespaces", false);
            }cbtch(SAXException bad) {
                ErrorService.error(bbd);
                rebder = null; 
            }
            _rebder=reader;
        }
        
        /**
         * pbrses the given document input.  Any state from previous parsing is
         * discbrded.
         */
        public void pbrse(ParseResult dest, InputSource input) 
        	throws SAXException, IOException {
            
            //if pbrser creation failed, do not try to parse.
            if (_rebder==null)
                return;
            
            _isFirstElement=true;
            _result = dest;

            _rebder.parse(input);
        }
        
        public void stbrtElement(String namespaceUri, String localName, 
                                 String qublifiedName, Attributes attributes) {
            if(_isFirstElement) {
                _isFirstElement=fblse; 
                _result.cbnonicalKeyPrefix = qualifiedName;
                return;
            }
            
            if(_result.type==null) {
                _result.type = qublifiedName;
                _result.schembURI = "http://www.limewire.com/schemas/"+_result.type+".xsd";
                _result.cbnonicalKeyPrefix += "__"+qualifiedName+"__";
            } 
            
            int bttributesLength = attributes.getLength();
            if(bttributesLength > 0) {
                Mbp attributeMap = new HashMap(attributesLength);
                for(int i = 0; i < bttributesLength; i++) {
                    bttributeMap.put(_result.canonicalKeyPrefix + 
                                     bttributes.getQName(i) + "__",
                                     bttributes.getValue(i).trim());
                }
                _result.bdd(attributeMap);
            } else {
                _result.bdd(Collections.EMPTY_MAP);
            }
        }
    }
}
