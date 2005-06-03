package com.limegroup.gnutella.gui.xml;

import junit.framework.Test;

import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.xml.*;

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;


public class XMLUtilsTest extends BaseTestCase {

    public XMLUtilsTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(XMLUtilsTest.class);
    }
    
    public void testGetDisplayList() throws Exception {
        List docValues = new LinkedList();
        docValues.add(new NameValue("audios__audio__licensetype__", "Should Be Hidden"));
        docValues.add(new NameValue("audios__audio__license__", "verify at here"));
        docValues.add(new NameValue("audios__audio__seconds__", "123"));
        docValues.add(new NameValue("audios__audio__action__", "Should also be hidden"));
        docValues.add(new NameValue("audios__audio__title__", "The Title"));
        docValues.add(new NameValue("audios__audio__artist__", "An Artist"));
        
        LimeXMLDocument doc = new LimeXMLDocument(docValues, "http://www.limewire.com/schemas/audio.xsd");
        
        // It is shown in order of the fields in the schema, not fields in the document.
        List display = XMLUtils.getDisplayList(doc);
        assertEquals(4, display.size());
        String title = (String)display.remove(0);
        String artist = (String)display.remove(0);
        String length =  (String)display.remove(0);
        String license = (String)display.remove(0);
        
        assertEquals("Title: The Title", title);
        assertEquals("Artist: An Artist", artist);
        assertEquals("Length: 2:03", length);
        assertEquals("Copyright: verify at here", license);
    }
    
    public void testGetTitleForSchemaFromField() {
        assertEquals("Audio", XMLUtils.getTitleForSchemaFromField("audios__audio__field__"));
        assertEquals("Video", XMLUtils.getTitleForSchemaFromField("videos__video__field__"));
        assertEquals("Schema", XMLUtils.getTitleForSchemaFromField("schemas__schema__field__"));
        assertEquals("Sam", XMLUtils.getTitleForSchemaFromField("berlin__sam__asdfoih__"));
    }
    
    public void testGetTitleForSchemaURI() {
        assertEquals("Audio", XMLUtils.getTitleForSchemaURI("http://www.limewire.com/schemas/audio.xsd"));
        assertEquals("Video", XMLUtils.getTitleForSchemaURI("http://www.limewire.com/schemas/video.xsd"));
        assertEquals(null, XMLUtils.getTitleForSchemaURI("http://www.death.com"));
    }
    
    public void testGetTitleForSchema() {
        assertEquals("Audio", XMLUtils.getTitleForSchema(
                 LimeXMLSchemaRepository.instance().getSchema("http://www.limewire.com/schemas/audio.xsd")));
        assertEquals("Video", XMLUtils.getTitleForSchema(
                 LimeXMLSchemaRepository.instance().getSchema("http://www.limewire.com/schemas/video.xsd")));
    }   

    public void testEnglishResource() throws Exception {
        assertEquals("Title",  XMLUtils.getResource("videos__video__title__"));
        assertEquals("Director", XMLUtils.getResource("videos__video__director__"));
        assertEquals("Producer", XMLUtils.getResource("videos__video__producer__"));
        assertEquals("Studio", XMLUtils.getResource("videos__video__studio__"));
        assertEquals("Stars", XMLUtils.getResource("videos__video__stars__"));
    }

    public void testJapaneseResource() throws Exception {
        try {
            setLanguage("ja", "JP");
            assertEquals("\u984c\u540d", XMLUtils.getResource("videos__video__title__"));
            assertEquals("\u30c7\u30a3\u30ec\u30af\u30bf\u30fc", XMLUtils.getResource("videos__video__director__"));
            assertEquals("\u30d7\u30ed\u30c7\u30e5\u30fc\u30b5\u30fc", XMLUtils.getResource("videos__video__producer__"));
            assertEquals("\u30b9\u30bf\u30b8\u30aa", XMLUtils.getResource("videos__video__studio__"));
            assertEquals("\u661f", XMLUtils.getResource("videos__video__stars__"));
        } finally {
            setLanguage("en", "");
        }
    }
    
    private void setLanguage(String language, String country) throws Exception {
        ApplicationSettings.LANGUAGE.setValue(language);
        ApplicationSettings.COUNTRY.setValue(country);
        PrivilegedAccessor.invokeMethod(Class.forName("com.limegroup.gnutella.gui.ResourceManager"), 
                                        "resetLocaleOptions", new Object[0]);
        PrivilegedAccessor.invokeMethod(XMLUtils.class, "loadBundles", new Object[0]);
    }

}








