package com.limegroup.gnutella.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import junit.framework.Test;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.util.BaseTestCase;

/**
 * Tests for the XMLParsingUtils class
 */
public class XMLParsingUtilsTest extends BaseTestCase {
    
    public XMLParsingUtilsTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(XMLParsingUtilsTest.class);
    }
    
    public void testSplit() throws Exception {
        String xml1 = "<?xml version='1.0'><text>one</text>";
        String xml2 = "<?xml version='1.0'><text>two</text>";
        String xml3 = "<?xml version='1.0'><text>three</text>";
        Iterator i = XMLParsingUtils.split(xml1+xml2+xml3).iterator();
        Assert.that(i.next().equals(xml1));
        Assert.that(i.next().equals(xml2));
        Assert.that(i.next().equals(xml3));
        Assert.that(!i.hasNext());
        
        assertEquals(3,XMLParsingUtils.split("<?xml<?xml<?xml").size());
        assertEquals(1,XMLParsingUtils.split("<xml?xml<?xml<?xm").size());
        assertTrue(XMLParsingUtils.split("<xml ?><<><?xm>l").isEmpty());
    }
    
    public void testParseValid() throws Exception {
        String xml = "<?xml version=\"1.0\"?>"+
        "<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">"+
        "<audio genre=\"Rock\" identifier=\"def1.txt\" bitrate=\"190\"/>"+
        "<audio genre=\"Classical\" identifier=\"def2.txt\" bitrate=\"2192\"/>"+
        "<audio genre=\"Blues\" identifier=\"def.txt\" bitrate=\"192\"/></audios>";
        XMLParsingUtils.ParseResult r = XMLParsingUtils.parse(xml,3);
        Assert.that(r.schemaURI.equals("http://www.limewire.com/schemas/audio.xsd"));
        Assert.that(r.type.equals("audio"));
        Assert.that(r.canonicalKeyPrefix.equals("audios__audio__"));
        List list = new ArrayList();
        HashMap map = new HashMap();
        map.put("audios__audio__genre__","Rock");
        map.put("audios__audio__identifier__","def1.txt");
        map.put("audios__audio__bitrate__","190");
        list.add(map);
        map = new HashMap();
        map.put("audios__audio__genre__","Classical");
        map.put("audios__audio__identifier__","def2.txt");
        map.put("audios__audio__bitrate__","2192");
        list.add(map);
        map = new HashMap();
        map.put("audios__audio__genre__","Blues");
        map.put("audios__audio__identifier__","def.txt");
        map.put("audios__audio__bitrate__","192");
        list.add(map);
        Assert.that(r.equals(list));
        
        String invalid = "<?xml version=\"1.0\"?>"+
        "<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">"+
        "<video something=\"something\"/></audios>";
        
        r = XMLParsingUtils.parse(invalid,1);
        assertFalse(r.isEmpty());
        assertNotNull(r.canonicalKeyPrefix);
        assertNotNull(r.schemaURI);
        
        String nested ="<?xml version=\"1.0\"?>"+
        "<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">"+
        "<audio genre=\"Rock\" identifier=\"def1.txt\" bitrate=\"190\">"+
        "<audio genre=\"Classical\" identifier=\"def2.txt\" bitrate=\"2192\">"+
        "<audio genre=\"Blues\" identifier=\"def.txt\" bitrate=\"192\"/></audio></audio></audios>";
        
        r = XMLParsingUtils.parse(nested,3);
        assertFalse(r.isEmpty());
        assertNotNull(r.canonicalKeyPrefix);
        assertNotNull(r.schemaURI);
        Assert.that(r.equals(list));
        
        String empty1 = "<?xml version=\"1.0\"?>"+
        "<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">"+
        "</audios>";
        
        r = XMLParsingUtils.parse(empty1,1);
        assertTrue(r.isEmpty());
        assertNotNull(r.canonicalKeyPrefix);
        assertNull(r.schemaURI);
        
        String empty2 = "<?xml version=\"1.0\"?>"+
        "<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"/>";
        
        r = XMLParsingUtils.parse(empty2,1);
        assertTrue(r.isEmpty());
        assertNotNull(r.canonicalKeyPrefix);
        assertNull(r.schemaURI);
        
    }
    
    public void testParseInvalid() throws Exception {
        XMLParsingUtils.ParseResult result;
        String invalid =
            "<?xml asdf asdf>";
        
        try {
            result = XMLParsingUtils.parse(invalid,1);
            fail("parsed invalid xml");
        }catch(SAXException expected){}
        
        
        invalid = "<?xml version=\"1.0\"?>"+
        "<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">"+
        "<audio genre=\"Rock\" identifier=\"def1.txt\" bitrate=\"190\"/>";
        
        try {
            result = XMLParsingUtils.parse(invalid,1);
            fail("parsed w/o closing audios tag");
        }catch(SAXException expected){}
        
        invalid = "<?xml version=\"1.0\"?>"+
        "<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">"+
        "<audio genre=\"Rock\" identifier=\"def1.txt\" bitrate=\"190\"></audios>";
        
        try {
            result = XMLParsingUtils.parse(invalid,1);
            fail("parsed with non-null not-closed audio element");
        }catch(SAXException expected){}
        
        invalid = "<?xml version=\"1.0\"?>"+
        "<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">"+
        "<audio genre=\"Rock\" identifier=\"def1.txt bitrate=\"190\"/></audios>";
        
        try {
            result = XMLParsingUtils.parse(invalid,1);
            fail("parsed with malformed attribute");
        }catch(SAXException expected){}
    }

}
