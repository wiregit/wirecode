package com.limegroup.gnutella.xml;

import java.util.*;
import java.io.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

import com.limegroup.gnutella.util.Comparators;

public class XMLParsingUtils {
    public static class Result {
        public String schemaURI;
        public String type;
        public String canonicalKeyPrefix;
        public List canonicalAttributeMaps = new ArrayList();
    }
    public static List split(String aggregatedXmlDocuments) {
        List results = new ArrayList();
        String[] split = aggregatedXmlDocuments.split("<\\?xml");
        for(int i=1; i<split.length; i++) results.add("<?xml" + split[i]);
        return results;
    }
    public static Result parse(String xml) throws IOException, SAXException {
        return parse(new InputSource(new StringReader(xml)));
    }
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
                        attributeMap.put(result.canonicalKeyPrefix + attributes.getLocalName(i) + "__",
                                         attributes.getValue(i));
                    result.canonicalAttributeMaps.add(attributeMap);
                }
            });
        reader.parse(inputSource);
        result.schemaURI = "http://www.limewire.com/schemas/"+result.type+".xsd";
        return result;
    }
    //implementation
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
