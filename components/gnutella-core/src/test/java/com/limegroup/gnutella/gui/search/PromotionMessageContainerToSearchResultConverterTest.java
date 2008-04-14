package com.limegroup.gnutella.gui.search;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;

import org.limewire.promotion.containers.PromotionMessageContainer;

import com.google.inject.Injector;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.SpeedConstants;
import com.limegroup.gnutella.gui.GUIBaseTestCase;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;

/**
 * Make sure that the search results created properly can be used in the client,
 * in regards to normal properties and XML properties.
 */
public class PromotionMessageContainerToSearchResultConverterTest extends GUIBaseTestCase {

    public PromotionMessageContainerToSearchResultConverterTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(PromotionMessageContainerToSearchResultConverterTest.class);
    }
    
    private PromotionMessageContainer con;
    private PromotionMessageContainerToSearchResultConverter converter;
    
    // The properties
    private final String name = "the name";
    private final String url = "http://the.url.com";
    private final String vendor = "the vendor";
    private final int size = 1234;
    private final long creationTime = 123123L;
    private final String query = "the keyword";
    private final String type = "audio";
    private final String artist = "Jesse Rubenfeld";
    private final String album = "uhh.... ???";
    private final String genre = "rap";  // gansta, that is
    private final String title = "where's my 40?";
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        con = new PromotionMessageContainer();
        Injector injector = LimeTestUtils.createInjector();
        LimeXMLDocumentFactory xmlDocFactory = injector.getInstance(LimeXMLDocumentFactory.class);
        ApplicationServices applicationServices = injector.getInstance(ApplicationServices.class);
        converter = new PromotionMessageContainerToSearchResultConverter(xmlDocFactory, applicationServices);
    }

    public void testName() {
        con.setDescription(name);
        PromotionSearchResult result = (PromotionSearchResult)converter.convert(con, query);
        assertEquals("name", name, result.getFilenameNoExtension());
    }
    
    public void testUrl() {
        con.setURL(url);
        PromotionSearchResult result = (PromotionSearchResult)converter.convert(con, query);
        assertTrue("should start with '?url=" + url, result.getURL().startsWith("?url=" + url));
    }
    
    public void testLinkUrl() {
        con.setURL(url);
        PromotionSearchResult result = (PromotionSearchResult)converter.convert(con, query);
        assertTrue("should start with '?url=" + url, result.getLinkUrl().startsWith("?url=" + url));
    }
    
    public void testVendor() {
        Map<String,String> props = new HashMap<String,String>();
        props.put("vendor", vendor);
        con.setProperties(props);
        PromotionSearchResult result = (PromotionSearchResult)converter.convert(con, query);
        assertEquals("vendor", vendor, result.getVendor());
    }
    
    public void testSize() {
        Map<String,String> props = new HashMap<String,String>();
        props.put("size", String.valueOf(size));
        con.setProperties(props);
        PromotionSearchResult result = (PromotionSearchResult)converter.convert(con, query);
        assertEquals("speed", SpeedConstants.THIRD_PARTY_SPEED_INT, result.getSpeed());
    }   
    
    public void testCreationTime() {
        Map<String,String> props = new HashMap<String,String>();
        props.put("creation_time", String.valueOf(creationTime));
        con.setProperties(props);
        PromotionSearchResult result = (PromotionSearchResult)converter.convert(con, query);
        assertEquals("creation time", creationTime, result.getCreationTime());
    }
    
    public void testArtist() {
        Map<String,String> props = new HashMap<String,String>();
        props.put("artist", artist);
        con.setProperties(props);
        PromotionSearchResult result = (PromotionSearchResult)converter.convert(con, query);
        LimeXMLDocument doc = result.getXMLDocument();
        assertEquals("xmlDoc", "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio artist=\"Jesse Rubenfeld\"/></audios>", doc.getXMLString());
        assertEquals("artist", artist, doc.getValue("audios__audio__artist__"));
    } 
    
    public void testAlbum() {
        Map<String,String> props = new HashMap<String,String>();
        props.put("album", album);
        con.setProperties(props);
        PromotionSearchResult result = (PromotionSearchResult)converter.convert(con, query);
        LimeXMLDocument doc = result.getXMLDocument();
        assertEquals("album", album, doc.getValue("audios__audio__album__"));
    } 
    
    public void testGenre() {
        Map<String,String> props = new HashMap<String,String>();
        props.put("genre", genre);
        con.setProperties(props);
        PromotionSearchResult result = (PromotionSearchResult)converter.convert(con, query);
        LimeXMLDocument doc = result.getXMLDocument();
        assertEquals("genre", genre, doc.getValue("audios__audio__genre__"));
    } 
    
    public void testTitle() {
        Map<String,String> props = new HashMap<String,String>();
        props.put("title", title);
        con.setProperties(props);
        PromotionSearchResult result = (PromotionSearchResult)converter.convert(con, query);
        LimeXMLDocument doc = result.getXMLDocument();
        assertEquals("title", title, doc.getValue("audios__audio__title__"));
    } 

}
