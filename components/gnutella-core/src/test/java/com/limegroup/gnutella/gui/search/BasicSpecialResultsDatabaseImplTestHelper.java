package com.limegroup.gnutella.gui.search;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.stubs.SimpleFileManager;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;

/**
 * Provides utility methods for creating the database so we don't have to use
 * inheritance for this and can have BaseTestCases instead of LimeBastTests.
 */
final class BasicSpecialResultsDatabaseImplTestHelper {
    
    private Injector injector;

    void setUp() throws Exception {
        injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(FileManager.class).to(SimpleFileManager.class);
            }
        });        
    }  
    
    final BasicSpecialResultsDatabaseImpl newDatabase(String buf) {
        return new BasicSpecialResultsDatabaseImpl(injector.getInstance(LimeXMLDocumentFactory.class), buf);        
    }
}
