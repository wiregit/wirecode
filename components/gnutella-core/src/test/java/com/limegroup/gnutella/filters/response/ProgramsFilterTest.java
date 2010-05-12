package com.limegroup.gnutella.filters.response;

import java.io.IOException;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.Category;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.api.search.SearchResult;
import org.limewire.gnutella.tests.ActivityCallbackStub;
import org.limewire.inject.AbstractModule;
import org.limewire.util.BaseTestCase;
import org.limewire.util.MatchAndCopy;
import org.xml.sax.SAXException;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.LimeWireCoreModule;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;
import com.limegroup.gnutella.xml.LimeXMLNames;
import com.limegroup.gnutella.xml.SchemaNotFoundException;

public class ProgramsFilterTest extends BaseTestCase {
    
    public ProgramsFilterTest(String name) {
        super(name);
    }
    
    public void testIntegration() throws SAXException, SchemaNotFoundException, IOException {
        Injector injector = Guice.createInjector(new LimeWireCoreModule(), new AbstractModule() {
            @Override
            protected void configure() {
                bind(ActivityCallback.class).to(ActivityCallbackStub.class);
            }
        });
        
        ProgramsFilter filter = injector.getInstance(ProgramsFilter.class);
        
        LimeXMLDocumentFactory xmlFactory = injector.getInstance(LimeXMLDocumentFactory.class);
        LimeXMLDocument document = xmlFactory.createLimeXMLDocument("<?xml version=\"1.0\"?><torrents xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/torrent.xsd\"><torrent infohash=\"TDC4GYOQXZPSUB7KR6SQKLS2USAJPZ7W\" trackers=\"http://torrent.ubuntu.com:6969/announce http://tracker.openbittorrent.com:80/announce http://red.tracker.prq.to:80/announce\" length=\"723488768\" name=\"ubuntu-9.10-desktop-i386.iso\"/></torrents>");
        
        Mockery context = new Mockery();
        final SearchResult result = context.mock(SearchResult.class);
        context.checking(new Expectations() {{
            allowing(result).getCategory();
            will(returnValue(Category.TORRENT));
            allowing(result);
        }});
        
        filter.allow(result, document);
    }
    
    public void testBasic() {
        Mockery context = new Mockery();
        
        final CategoryManager manager = context.mock(CategoryManager.class);
        
        final Response docResponse = context.mock(Response.class);
        final Response imageResponse = context.mock(Response.class);
        final Response progResponse = context.mock(Response.class);
        
        final SearchResult docResult = context.mock(SearchResult.class);
        final SearchResult imageResult = context.mock(SearchResult.class);
        final SearchResult progResult = context.mock(SearchResult.class);
        
        ProgramsFilter filter = new ProgramsFilter(manager);
        
        context.checking(new Expectations() {{
            allowing(manager).getCategoryForFilename("a.doctor");
            will(returnValue(Category.DOCUMENT));
            allowing(manager).getCategoryForFilename("sadfsad.a.emogin");
            will(returnValue(Category.IMAGE));
            allowing(manager).getCategoryForFilename("sad.perrogie");
            will(returnValue(Category.PROGRAM));

            
            allowing(docResponse).getName();
            will(returnValue("a.doctor"));
            allowing(imageResponse).getName();
            will(returnValue("sadfsad.a.emogin"));
            allowing(progResponse).getName();
            will(returnValue("sad.perrogie"));

            allowing(docResult).getCategory();
            will(returnValue(Category.DOCUMENT));
            allowing(imageResult).getCategory();
            will(returnValue(Category.IMAGE));
            allowing(progResult).getCategory();
            will(returnValue(Category.PROGRAM));
       
            
            allowing(docResponse);
            allowing(imageResponse);
            allowing(progResponse);
        }});
        
        assertTrue(filter.allow(null, docResponse));
        assertTrue(filter.allow(null, imageResponse));
        assertFalse(filter.allow(null, progResponse));
        
        assertTrue(filter.allow(docResult, null));
        assertTrue(filter.allow(imageResult, null));
        assertFalse(filter.allow(progResult, null));
        
        context.assertIsSatisfied();
    }
    
    @SuppressWarnings("unchecked")
    public void testMultipleFilesForBT() {
        Mockery context = new Mockery();
        
        final CategoryManager manager = context.mock(CategoryManager.class);
        final SearchResult result = context.mock(SearchResult.class);        
        final LimeXMLDocument document = context.mock(LimeXMLDocument.class);       
        
        ProgramsFilter filter = new ProgramsFilter(manager);
        
        final MatchAndCopy<List> pathsCollector
            = new MatchAndCopy<List>(List.class);
        
        context.checking(new Expectations() {{
            one(manager).containsCategory(with(same(Category.PROGRAM)), with(pathsCollector));
            will(returnValue(true));
            
            one(manager).containsCategory(with(same(Category.PROGRAM)), with(pathsCollector));
            will(returnValue(false));
            
            allowing(result).getCategory();
            will(returnValue(Category.TORRENT));
            
            allowing(document).getValue(LimeXMLNames.TORRENT_FILE_PATHS);
            will(returnValue("/path1///path2///path3"));
            
            allowing(document);
        }});
        
        // Check the responses of the filter are consistent with the category checker
        assertFalse(filter.allow(result, document));
        assertTrue(filter.allow(result, document));
        
        context.assertIsSatisfied();
        
        // Ensure the correct paths were passed for checking
        List<String> paths1 = pathsCollector.getMatches().get(0);
        List<String> paths2 = pathsCollector.getMatches().get(0);
        assertEquals("path1", paths1.get(0));
        assertEquals("path2", paths1.get(1));
        assertEquals("path3", paths1.get(2));
        assertEquals("path1", paths2.get(0));
        assertEquals("path2", paths2.get(1));
        assertEquals("path3", paths2.get(2));
    }

}
