package com.limegroup.gnutella.gui.search;

import junit.framework.Test;

import com.google.inject.AbstractModule;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.MediaType;
import com.limegroup.gnutella.gui.GUIBaseTestCase;
import com.limegroup.gnutella.gui.GuiCoreMediator;

public class NamedMediaTypeTest extends GUIBaseTestCase {

    public NamedMediaTypeTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(NamedMediaTypeTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                requestStaticInjection(GuiCoreMediator.class);
            }
        });
    }
    

    public void testGetFromDescription() {
        NamedMediaType.getFromDescription(MediaType.SCHEMA_ANY_TYPE);
        NamedMediaType.getFromDescription(MediaType.SCHEMA_AUDIO);
    }
    
    public void testGetFromExtension() {
        NamedMediaType.getFromExtension("txt");
    }

    public void testGetFromMediaType() {
        NamedMediaType.getFromMediaType(MediaType.getImageMediaType());
    }

}
