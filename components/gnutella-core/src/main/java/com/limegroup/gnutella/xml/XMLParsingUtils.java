package com.limegroup.gnutella.xml;

import com.limegroup.gnutella.util.Comparators;
import com.limegroup.gnutella.Assert;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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
    private static ThreadLocal _reader = new ThreadLocal();
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
        if(_reader.get()==null) _reader.set(XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser"));
        XMLReader reader = (XMLReader)_reader.get();   
        final Result result = new Result();
        synchronized(_reader) {
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
        }
        result.schemaURI = "http://www.limewire.com/schemas/"+result.type+".xsd";
        return result;
    }
    /**
     * Splits an aggregated XML string into individual XML strings
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
    static String plural(String type) {
        if(type.endsWith("y")) return type.substring(0,type.length()-1)+"ies";
        return type+"s";
    }
//     public static void main(String[] args) throws Exception { //UNIT TEST
//         //test parse
//         String xml = "<?xml version=\"1.0\"?>"+
//             "<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">"+
//             "<audio genre=\"Rock\" identifier=\"def1.txt\" bitrate=\"190\"/>"+
//             "<audio genre=\"Classical\" identifier=\"def2.txt\" bitrate=\"2192\"/>"+
//             "<audio genre=\"Blues\" identifier=\"def.txt\" bitrate=\"192\"/></audios>";
//         Result r = parse(xml);
//         Assert.that(r.schemaURI.equals("http://www.limewire.com/schemas/audio.xsd"));
//         Assert.that(r.type.equals("audio"));
//         Assert.that(r.canonicalKeyPrefix.equals("audios__audio__"));
//         List list = new ArrayList();
//         Map map = new TreeMap(Comparators.stringComparator());
//         map.put("audios__audio__genre__","Rock");
//         map.put("audios__audio__identifier__","def1.txt");
//         map.put("audios__audio__bitrate__","190");
//         list.add(map);
//         map = new TreeMap(Comparators.stringComparator());
//         map.put("audios__audio__genre__","Classical");
//         map.put("audios__audio__identifier__","def2.txt");
//         map.put("audios__audio__bitrate__","2192");
//         list.add(map);
//         map = new TreeMap(Comparators.stringComparator());
//         map.put("audios__audio__genre__","Blues");
//         map.put("audios__audio__identifier__","def.txt");
//         map.put("audios__audio__bitrate__","192");
//         list.add(map);
//         Assert.that(r.canonicalAttributeMaps.equals(list));
//         //test plural
//         Assert.that(plural("book").equals("books"));
//         Assert.that(plural("property").equals("properties"));
//         //test split
//         String xml1 = "<?xml version='1.0'><text>one</text>";
//         String xml2 = "<?xml version='1.0'><text>two</text>";
//         String xml3 = "<?xml version='1.0'><text>three</text>";
//         List split = split(xml1+xml2+xml3);
//         Iterator i = split.iterator();
//         Assert.that(i.next().equals(xml1));
//         Assert.that(i.next().equals(xml2));
//         Assert.that(i.next().equals(xml3));
//         Assert.that(!i.hasNext());
//         System.out.println("XMLParsingUtils passed unit test");
//     }
}
