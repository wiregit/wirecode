package com.limegroup.gnutella.xml;

import java.util.*;
import java.io.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class XMLParsingUtils {
    public static class Result {
        public String schemaURI;
        public String type;
        public List canonicalAttributeMaps = new ArrayList();
    }
    public static Result parse(String xml) throws Exception {
        XMLReader reader = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
        final Result result = new Result();
        reader.setContentHandler(new DefaultHandler() {
                boolean isFirstElement=true;
                String canonicalPrefix;
                public void startElement(String namespaceUri, String localName, 
                                         String qualifiedName, Attributes attributes) {
                    if(isFirstElement) {isFirstElement=false; return;}
                    if(result.type==null) {
                        result.type = localName;
                        canonicalPrefix = plural(localName)+"__"+localName+"__";
                    } 
                    Map attributeMap = new HashMap();
                    int count = attributes.getLength();
                    for(int i=0; i<count; i++) 
                        attributeMap.put(canonicalPrefix + attributes.getLocalName(i) + "__",
                                         attributes.getValue(i));
                    result.canonicalAttributeMaps.add(attributeMap);
                }
            });
        InputSource inputSource = new InputSource(new StringReader(xml));
        reader.parse(inputSource);
        result.schemaURI = "http://www.limewire.com/schemas/"+plural(result.type)+".xsd";
        return result;
    }
    //implementation
    private static String plural(String type) {
        if(type.endsWith("y")) return type.substring(0,type.length()-1)+"ies";
        return type+"s";
    }
    public static void main(String[] args) throws Exception {
        Result r = parse("<?xml version=\"1.0\"?>"+
                         "<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audios.xsd\">"+
                         "<audio genre=\"Rock\" identifier=\"def1.txt\" bitrate=\"190\"/>"+
                         "<audio genre=\"Classical\" identifier=\"def2.txt\" bitrate=\"2192\"/>"+
                         "<audio genre=\"Blues\" identifier=\"def.txt\" bitrate=\"192\"/></audios>");
        System.out.println("Schema URI: " + r.schemaURI);
        System.out.println("Type: " + r.type);
        System.out.println(r.canonicalAttributeMaps);
        System.out.println(plural("property"));
        System.out.println(plural("book"));
    }
}
