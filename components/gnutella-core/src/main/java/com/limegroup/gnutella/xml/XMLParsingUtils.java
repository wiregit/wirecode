package com.limegroup.gnutella.xml;

import com.limegroup.gnutella.util.Comparators;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Provides just enough functionality for our simple schemas,
 * based on SAX
 * @author  tjones
 */
public class XMLParsingUtils {
    public static class Result {
        public String schemaURI;                              //like http://www.limewire.com/schemas/audio.xsd
        public String type;                                   //e.g. audio, video, etc.
        public String canonicalKeyPrefix;                     //like audios__audio__
        public List canonicalAttributeMaps = new ArrayList(); //one attribute map per element in xml
    }
    /**
     * Parses our simplified XML
     * @param xml 
     * @return Result data structure
     * @exception IOException
     * @exception SAXException
     */
    public static Result parse(String xml) throws IOException, SAXException {
        return parse(new InputSource(new StringReader(xml)));
    }
    /**
     * Parses our simplified XML
     * @param InputSource 
     * @return Result data structure
     * @exception IOException
     * @exception SAXException
     */
    public static Result parse(InputSource inputSource) throws IOException, SAXException {
        XMLReader reader = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
        final Result result = new Result();
        reader.setContentHandler(new DefaultHandler() {
                boolean isFirstElement=true;
                public void startElement(String namespaceUri, String localName, 
                                         String qualifiedName, Attributes attributes) {
                    if(isFirstElement) {isFirstElement=false; return;}
                    if(result.type==null) {
                        result.type = localName;
                        result.canonicalKeyPrefix = plural(localName)+"__"+localName+"__";
                    } 
                    Map attributeMap = new TreeMap(Comparators.stringComparator());
                    int count = attributes.getLength();
                    for(int i=0; i<count; i++) 
                        attributeMap.put(result.canonicalKeyPrefix + 
                                         attributes.getLocalName(i) + "__",
                                         attributes.getValue(i));
                    result.canonicalAttributeMaps.add(attributeMap);
                }
            });
        reader.parse(inputSource);
        result.schemaURI = "http://www.limewire.com/schemas/"+result.type+".xsd";
        return result;
    }
    /**
     * Splits aggregated XML string into individual XML strings
     * @param aggregatedXmlDocuments
     * @return List of Strings
     */
    public static List split(String aggregatedXmlDocuments) {
        List results = new ArrayList();
        int begin=aggregatedXmlDocuments.indexOf("<?xml");
        if(begin==-1) return Collections.EMPTY_LIST;
        int end=aggregatedXmlDocuments.indexOf("<?xml",begin+1);
        while(true) {
            if(end==-1) {
                results.add(aggregatedXmlDocuments.substring(begin));
                break;
            }
            results.add(aggregatedXmlDocuments.substring(begin,end));
            begin = end;
            end = aggregatedXmlDocuments.indexOf("<?xml",begin+1);
        }
        return results;
    }
    private static String plural(String type) {
        if(type.endsWith("y")) return type.substring(0,type.length()-1)+"ies";
        return type+"s";
    }
//     public static void main(String[] args) throws Exception {
//         String xml = "<?xml version=\"1.0\"?>"+
//             "<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audios.xsd\">"+
//             "<audio genre=\"Rock\" identifier=\"def1.txt\" bitrate=\"190\"/>"+
//             "<audio genre=\"Classical\" identifier=\"def2.txt\" bitrate=\"2192\"/>"+
//             "<audio genre=\"Blues\" identifier=\"def.txt\" bitrate=\"192\"/></audios>";
//         Result r = parse(xml);
//         System.out.println("Schema URI: " + r.schemaURI);
//         System.out.println("Type: " + r.type);
//         System.out.println(r.canonicalAttributeMaps);
//         System.out.println(plural("property"));
//         System.out.println(plural("book"));
//         List split = split(xml+xml+xml);
//         System.out.println(split);
//     }
}
